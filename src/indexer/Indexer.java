package indexer;

import org.rocksdb.*;

import com.google.common.collect.HashBiMap;

import java.io.File;
import java.util.*;

public class Indexer {
    private enum IndexType {PageURLID, WordID}

    private static final Indexer INSTANCE = new Indexer();
    private RocksDB pageURLIDdb, wordIDdb;
    private Integer wordCount, URLCount;
    private final HashBiMap<Integer, String> pageIndexer = HashBiMap.create();
    private final HashBiMap<Integer, String> wordIndexer = HashBiMap.create();

    public static Indexer getInstance() {
        return INSTANCE;
    }

    /**
     * open all the database and place the record in hashBiMap and update the counters
     */
    private Indexer(){
        Options options = new Options().setCreateIfMissing(true);
        try {
            System.out.println("remember to place database directory and stopword.txt in: "+
                    new File("").getAbsolutePath());
            pageURLIDdb = RocksDB.open(options, "database/pageURLIDdb");
            wordIDdb = RocksDB.open(options, "database/wordIDdb");
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        wordCount = 0;
        URLCount = 0;
        addPageBiMap();
        addWordBiMap();
    }

    ///////Page////////
    private void addPageBiMap(){
        RocksIterator iter = pageURLIDdb.newIterator();
        for (iter.seekToFirst(); iter.isValid(); iter.next()) {
            updatePageBiMap(Integer.parseInt(new String(iter.key())), new String(iter.value()));
            URLCount++;
        }
    }

    private void updatePageBiMap(int pageID, String url){
        pageIndexer.put(pageID, url);
    }

    private void addPage(String url) {
        try {
            URLCount++;
            pageURLIDdb.put(Integer.toString(URLCount).getBytes(), url.getBytes());
            updatePageBiMap(URLCount, url);
        } catch (RocksDBException e) {
            e.printStackTrace();
            System.out.println("Fail to add page");
        }
    }

    /**
     * true:
     *     get ID from url, if no such ID (a newly appeared link), make one for it
     * false:
     *      get ID from url, if no such ID, return -1
     * @param url link
     * @param addIfMissing boolean
     * @return page ID, -1 if no such page
     */
    public Integer searchIDByURL(String url, boolean addIfMissing) {
        if (!(pageIndexer.containsValue(url))) {
            if (addIfMissing) {
                addPage(url);
                return URLCount;
            } else {
                return -1;
            }
        } else {
            return pageIndexer.inverse().get(url);
        }
    }

    String searchURLByID(int pageID) {
        return pageIndexer.getOrDefault(pageID, null);
    }

    ///////Word////////
    private void addWord(String word) {
        try {
            wordCount += 1;
            wordIDdb.put(Integer.toString(wordCount).getBytes(), word.getBytes());
            updateWordBiMap(wordCount, word);
        } catch (RocksDBException e) {
            e.printStackTrace();
            System.out.println("Fail to add word");
        }
    }

    private void addWordBiMap(){
        RocksIterator iter = wordIDdb.newIterator();
        for (iter.seekToFirst(); iter.isValid(); iter.next()) {
            updateWordBiMap(Integer.parseInt(new String(iter.key())), new String((iter.value())));
            wordCount++;
        }
    }

    /**
     * get all stem in the database
     * @return a list of sorted stem word
     */
    public List<String> getAllStemWord() {
        Set<String> result = new HashSet<>();
        RocksIterator iter = wordIDdb.newIterator();
        for (iter.seekToFirst(); iter.isValid(); iter.next()) {
            result.add(new String(iter.value()));
        }
        List<String> sortedList = new ArrayList<>(result);
        Collections.sort(sortedList);

        return sortedList;
    }

    private void updateWordBiMap(int wordID, String word){
        wordIndexer.put(wordID, word);
    }

    /**
     * true:
     *     get ID from word, if no such ID make one for it
     * false:
     *      get ID from word, if no such ID, return -1
     * @param word a word
     * @param addIfMissing boolean
     * @return word ID, -1 if no such word
     */
    public Integer searchIDByWord(String word, boolean addIfMissing) {
        if (!(wordIndexer.containsValue(word))) {
            if (addIfMissing) {
                addWord(word);
                return wordCount;
            } else {
                return -1;
            }
        } else{
            return wordIndexer.inverse().get(word);
        }
    }

    public String searchWordByID(int wordID) {
        return wordIndexer.getOrDefault(wordID, null);
    }

    //////////others///////////////////
    private void printAll(IndexType situation) {
        if (situation == IndexType.PageURLID) {
            RocksIterator iter = pageURLIDdb.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println("ID: " + new String(iter.key()) + '\n' + new String(iter.value()) + "\n");
            }
        }

        if (situation == IndexType.WordID) {
            RocksIterator iter = wordIDdb.newIterator();
            for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                System.out.println("Word ID: " + new String(iter.key()) + '\n' +
                        "Word: " + new String(iter.value()) + "\n");
            }
        }

    }

    void deleteEntry(int wordID) {
        try {
            wordIDdb.delete(String.valueOf(wordID).getBytes());
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        Indexer indexer = getInstance();
        indexer.printAll(IndexType.WordID);
    }
}