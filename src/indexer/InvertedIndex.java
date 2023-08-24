package indexer;

import org.rocksdb.*;

import spider.WebInfoSeeker;
import util.Converter;
import util.Word;

import java.util.*;
import java.util.stream.Collectors;

/*
inverted index
word ID -> {pageID, fred, {occurrence}}
Forward index
Page-ID -> {ChildID}, {keywords}, {title words}, maxTF
*/

public class InvertedIndex {

    private static InvertedIndex INSTANCE = new InvertedIndex();
    private Indexer indexer = Indexer.getInstance();
    private PageProperty pageProperty = PageProperty.getInstance();
    private RocksDB pageDetailDb, wordIdDb;
    private List<ColumnFamilyHandle> handles = new Vector<>();

    public static InvertedIndex getInstance() {
        return INSTANCE;
    }

    private InvertedIndex(){
        Options options = new Options();
        options.setCreateIfMissing(true);
        try {
            wordIdDb = RocksDB.open(options, "database/wordFreqdb");
            List<ColumnFamilyDescriptor> colFamily = new Vector<>();
            colFamily.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY));
            colFamily.add(new ColumnFamilyDescriptor("bodyWords".getBytes()));
            colFamily.add(new ColumnFamilyDescriptor("titleWords".getBytes()));
            colFamily.add(new ColumnFamilyDescriptor("maxTF".getBytes()));
            DBOptions options2 = new DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true);
            pageDetailDb = RocksDB.open(options2, "database/pageDetailDB", colFamily, handles);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    /**
     * get all page that contain a particular keyword
     * @param wordID wordID of that particular word
     * @return set of pageID
     */
    public Set<Integer> getRelatedPage(int wordID) {
        HashMap<Integer, Integer> map = getPostingList(wordID);
        assert map != null;
        return map.keySet();
    }

    public LinkedList<Integer> getWordPositionsInPage(int wordID, int pageID) {
        try {
            byte[] record = wordIdDb.get(Converter.idTobyteArray(wordID));
            HashMap<Integer, LinkedList<Integer>> pos = new PostingListHandler(new String(record)).getPositionsRecord();
            return pos.get(pageID);
        } catch (RocksDBException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * the stem word from a title of a page
     * @param pageID page ID of a page
     * @return a hashSet of those words
     */
    public Set<String> getTitleWords(int pageID) {
        try {
            byte[] content = pageDetailDb.get(handles.get(2), Converter.idTobyteArray(pageID));
            String[] words = new String(content).split(" ");
            return new HashSet<>(Arrays.asList(words));
        } catch (RocksDBException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * get df(j), the program will crash if no such word appeared in the record
     * @param word a specific stemmed word
     * @return df(j)
     */
    private int getDocumentFrequency(String word) {
        try {
            int wordID = indexer.searchIDByWord(word, false);
            String record = new String(wordIdDb.get(Converter.idTobyteArray(wordID)));
            return record.split(" ").length;
        } catch (RocksDBException e) {
            e.printStackTrace();
            return -1;
        }

    }

    /**
     * get tf(i, j), the program will crash if the pageID is invalid or no such word in that page
     * @param word a specific stemmed word
     * @param pageID page ID
     * @return tf(i, j)
     */
    public int getFreqOfWordInParticularPage(String word, int pageID) {
        int wordID = indexer.searchIDByWord(word, false);
        HashMap<Integer, Integer> recordDetails = getPostingList(wordID);
        assert recordDetails != null;
        return recordDetails.get(pageID);
    }

    /**
     * get idf(j), the program will crash if no such word appeared in the record
     * @param word a specific stemmed word
     * @return idf(j)
     */
    private double getIdf(String word) {
        double N = pageProperty.getNumOfPageFetched();
        double df = getDocumentFrequency(word);
        return Math.log(N/df)/Math.log(2);
    }

    /**
     * get the term frequency of the most frequent term in document j
     * @return max Tf(j)
     */
    private int getMaxTf(int pageID) {
        try {
            byte[] content = pageDetailDb.get(handles.get(3), Converter.idTobyteArray(pageID));
            return Integer.parseInt(new String(content));
        } catch (RocksDBException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * get term weight w(i,j)
     * the program will crash if the pageID is invalid or no such word in that page
     * @param word word i
     * @param pageID page j
     * @return w(i,j)
     */
    public double getTermWeight(String word, int pageID) {
        return ((double)getFreqOfWordInParticularPage(word, pageID) / getMaxTf(pageID)) * getIdf(word);
    }

    /**
     * get all unique keyword of a page
     * @param pageID pageID
     * @return an array of string contain those word
     */
    public String[] getKeyWords(int pageID) {
        try {
            byte[] content = pageDetailDb.get(handles.get(1), Converter.idTobyteArray(pageID));
            return new String(content).split(" ");
        } catch (RocksDBException e) {
            e.printStackTrace();
            return null;
        }
    }

    private HashMap<Integer, Integer> getPostingList(int wordID) {
        try {
            byte[] record = wordIdDb.get(Converter.idTobyteArray(wordID));
            return new PostingListHandler(new String(record)).getFrequencyRecord();
        } catch (RocksDBException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * get keyword in sorting order according to appear freq in page
     * @param pageID page id
     * @return hashmap of word -> freq
     */
    public LinkedHashMap<String, Integer> getSortedWordFreqWordList (int pageID){
        HashMap<String, Integer> keywordList = new HashMap<>();
        String[] keywords = getKeyWords(pageID);
        for (String keyword : keywords) {
            Integer wordFreq = getFreqOfWordInParticularPage(keyword, pageID);
            keywordList.put(keyword, wordFreq);
        }

        return keywordList.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }


    /**
     * clear record in inverted index which related to a page
     * @param pageID page ID
     */
    public void clearRecord(int pageID) {
        String[] words = getKeyWords(pageID);
        for (String word : words) {
            try {
                if (word.equals("")) {
                    continue;
                }
                int wordID = indexer.searchIDByWord(word,false);
                byte[] postingList = wordIdDb.get(Converter.idTobyteArray(wordID));
                PostingListHandler newRecord = new PostingListHandler(new String(postingList));
                boolean resultInEmptyPostingList = newRecord.removeRecord(pageID);
                if (resultInEmptyPostingList) {
                    wordIdDb.delete(Converter.idTobyteArray(wordID));
                    indexer.deleteEntry(wordID);
                } else {
                    wordIdDb.put(Converter.idTobyteArray(wordID), newRecord.toString().getBytes());
                }
            } catch (RocksDBException e) {
                e.printStackTrace();
            }
        }
        for (ColumnFamilyHandle handle : handles) {
            try {
                pageDetailDb.delete(handle, Converter.idTobyteArray(pageID));
            } catch (RocksDBException e) {
                e.printStackTrace();
            }
        }
    }

    public String[] getChildIDs(int pageID) {
        String listOfChild;
        try {
            listOfChild = new String(pageDetailDb.get(handles.get(0), Converter.idTobyteArray(pageID)));
            return  listOfChild.split(" ");
        } catch (RocksDBException e) {
            e.printStackTrace();
            return null;
        }
    }

    public enum Status {WithinDB, OutsideDB, All}
    /**
     * get all children page by page ID
     * @param pageID page ID
     * @return list containing all the children link
     */
    public List<String> getAllChildPage(int pageID, Status status) {
        List<String> pages = new LinkedList<>();
        String[] childIDs = getChildIDs(pageID);

        for (String childID : childIDs) {
            if (childID.equals("")) {
                continue;
            }
            String l = indexer.searchURLByID(Integer.parseInt(childID));
            boolean canIgnore = false;
            switch (status) {
                case All:
                    break;
                case WithinDB:
                    if (pageProperty.getUrl(Integer.parseInt(childID)) == null) {
                        canIgnore = true;
                    }
                    break;
                case OutsideDB:
                    if (pageProperty.getUrl(Integer.parseInt(childID)) != null) {
                        canIgnore = true;
                    }
                    break;
            }
            if(canIgnore) continue;
            if (l == null) { throw new IllegalStateException(); }
            pages.add(l);
        }
        return pages;
    }

     /**
     * get a string contain child page of a page by pageID
     * (only page that in the database will count), separate by \n
     * @param pageID page of
     * @return the string
     */
    public String getChildPages(int pageID) {
        StringBuilder result = new StringBuilder();
        String[] childIDs = getChildIDs(pageID);

        for (String childID : childIDs) {
            if (childID.equals("")) {
                continue;
            }
            String l = pageProperty.getUrl(Integer.parseInt(childID));
            if (l == null) {
                continue;
            }
            result.append(l).append("\n");
        }
        return String.valueOf(result);
    }

    private void storeWordFreq(int pageID, Vector<String> keywords) {
        int maxFreq = 0;
        for (int i = 0; i < keywords.size(); i++) {
            Integer wordID = indexer.searchIDByWord(keywords.get(i), true);
            byte[] content;
            try {
                content = wordIdDb.get(Converter.idTobyteArray(wordID));
                PostingListHandler temp;
                if (content == null) {
                    temp = new PostingListHandler("");
                } else {
                    temp = new PostingListHandler(new String(content));
                }
                int tf = temp.addWord(pageID, i);
                content = temp.toString().getBytes();
                wordIdDb.put(Converter.idTobyteArray(wordID), content);
                if (tf > maxFreq) {
                    maxFreq = tf;
                }
            } catch (RocksDBException e) {
                e.printStackTrace();
            }
        }
        try {
            pageDetailDb.put(handles.get(3), Converter.idTobyteArray(pageID), String.valueOf(maxFreq).getBytes());
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public void store(int pageID, String url){
        try {
            WebInfoSeeker seeker = new WebInfoSeeker(url);
            //separate title word and content word
            Vector<String> keyWords = seeker.getKeywords();
            List<String> titleWord = Word.phraseString(pageProperty.getTitle(pageID));
            for (String s : titleWord) {
                try {
                    if (s.equals(keyWords.get(0))) {
                        keyWords.remove(0);
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    break;
                }
            }
            ///////////////////wordID -> {pageID freq}, pageID ->{tf max}/////////////////////////
            storeWordFreq(pageID, keyWords);
            //////////////////// Page-ID -> {title Words}/////////////////////
            LinkedHashSet<String> uniqueKeyWords0 = new LinkedHashSet<>(titleWord);
            titleWord.clear();
            titleWord.addAll(uniqueKeyWords0);
            byte[] content = null;
            for (String word : titleWord) {
                if (content == null) {
                    content = word.getBytes();
                } else {
                    content = (new String(content) + " " + word).getBytes();
                }
            }
            if (content == null) {
                content = "".getBytes();
            }
            pageDetailDb.put(handles.get(2), Integer.toString(pageID).getBytes(), content);
            //////////////////// Page-ID -> {keywords}////////////////////////
            LinkedHashSet<String> uniqueKeyWords = new LinkedHashSet<>(keyWords);
            keyWords.clear();
            keyWords.addAll(uniqueKeyWords);
            content = null;
            for (String keyWord : keyWords) {
                if (content == null) {
                    content = keyWord.getBytes();
                } else {
                    content = (new String(content) + " " + keyWord).getBytes();
                }
            }
            if (content == null) {             // if the web no not have any word (avoid crash the program)
                content = "".getBytes();
            }
            pageDetailDb.put(handles.get(1), Integer.toString(pageID).getBytes(), content);
            //////////////////pageID -> {child ID}//////////////////////////
            List<String> child = seeker.getChildLinks();
            Set<String> childPage = new HashSet<>(child);

            content = null;
            for (String link : childPage) {
                if (link.equals(url)) {
                    continue;
                }
                int childID = indexer.searchIDByURL(link, true);
                if (content == null) {
                    content = Integer.toString(childID).getBytes();
                } else {
                    content = (new String(content) + " " + childID).getBytes();
                }
            }
            if (content == null) {             // if the web no not have any child (avoid crash the program)
                content = "".getBytes();
            }
            pageDetailDb.put(handles.get(0), Integer.toString(pageID).getBytes(), content);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    private enum Type {Content, Child, Title, WordID, MaxTf}

    private void printAll(Type situation) throws RocksDBException {
        //Page-ID -> {keywords}
        if(situation == Type.Content) {
            RocksIterator iter = pageDetailDb.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println("page ID: " + new String(iter.key()) + '\n'
                        + new String(pageDetailDb.get(handles.get(1), iter.key())) + "\n");

            }
        }

        if(situation == Type.Child) {
            RocksIterator iter = pageDetailDb.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println("ParentID: " + new String(iter.key()) + '\n' +
                        "Child ID: " + new String(pageDetailDb.get(handles.get(0), iter.key())) + "\n");
            }
        }

        if(situation == Type.Title) {
            RocksIterator iter = pageDetailDb.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println("ParentID: " + new String(iter.key()) + '\n' +
                        "Child ID: " + new String(pageDetailDb.get(handles.get(2), iter.key())) + "\n");
            }
        }

        if(situation == Type.MaxTf) {
            RocksIterator iter = pageDetailDb.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println("pageID: " + new String(iter.key()) + '\n' +
                        "MaxTf: " + new String(pageDetailDb.get(handles.get(3), iter.key())) + "\n");
            }
        }

        if (situation == Type.WordID) {
            RocksIterator iter = wordIdDb.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println("wordID: " + new String(iter.key()) + '\n' +
                        "appear at: " + new String(iter.value()) + "\n");
            }
        }
    }

    public static void main(String[] args) throws RocksDBException{
        InvertedIndex invertedIndex = getInstance();
//        System.out.println(invertedIndex.getFreqOfWordInParticularPage("rainbow", 17));
//        System.out.println(invertedIndex.getDocumentFrequency("rainbow"));
//        System.out.println(invertedIndex.getIdf("rainbow"));
//        System.out.println(invertedIndex.getMaxTf(17));
//        System.out.println(invertedIndex.getTermWeight("minor",14080));
//        System.out.println(invertedIndex.getRelatedPage(95));
        invertedIndex.printAll(Type.Content);
//
    }
}
