package retriever;

import indexer.InvertedIndex;
import indexer.PageProperty;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;


public class PreProcessor {

    private static final PreProcessor instance = new PreProcessor();
    private final PageProperty pageProperty = PageProperty.getInstance();
    private final InvertedIndex invertedIndex = InvertedIndex.getInstance();
    /**
     * store doc length of each page : pageID -> document length
     */
    private RocksDB docLengthDB;
    /**
     * store pageID -> {parentID}
     */
    private RocksDB pageParentDB;

    public static PreProcessor getInstance() {
        return instance;
    }

    double getDocLength(int pageID) {
        try {
            return Double.parseDouble(new String(docLengthDB.get(String.valueOf(pageID).getBytes())));
        } catch (RocksDBException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private PreProcessor() {
        try {
            docLengthDB = RocksDB.open(new Options().setCreateIfMissing(true), "database/docLengthDB");
            pageParentDB = RocksDB.open(new Options().setCreateIfMissing(true), "database/pageParentDB");
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public String[] getParentIDs(int pageID) {
        try {
            return new String(pageParentDB.get(String.valueOf(pageID).getBytes())).split(" ");
        } catch (RocksDBException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * get a string contain parent page of a page by pageID
     * (only page that in the database will count), separate by \n
     * @param pageID page of
     * @return the string
     */
    public String getParentPages(int pageID) {
        StringBuilder result = new StringBuilder();
        String[] parentIDs = getParentIDs(pageID);
        for (String parentID : parentIDs) {
            if (parentID.equals("")) {
                continue;
            }
            String l = pageProperty.getUrl(Integer.parseInt(parentID));
            result.append(l).append("\n");
        }
        return String.valueOf(result);
    }

    private void prepareParentPageRelationship() throws RocksDBException {
        for (int parentID : pageProperty.getAllPageID()) {
            String[] chilIDs = invertedIndex.getChildIDs(parentID);
            for (String chilID : chilIDs) {
                if (chilID.equals("")) {
                    continue;
                }
                int childID = Integer.parseInt(chilID);
                if (pageProperty.getUrl(childID) == null) {
                    continue;
                }
                System.out.println(parentID + " have child " + childID);
                byte[] content;
                content = pageParentDB.get(String.valueOf(childID).getBytes());
                if (content == null) {
                    content = String.valueOf(parentID).getBytes();
                } else {
                    content = (new String(content) + " " + parentID).getBytes();
                }
                pageParentDB.put(String.valueOf(childID).getBytes(), content);
            }
        }
    }


    /**
     * pre-compute all doc length
     */
    private void preComputeDocumentLength() {
        try {
            for (int pageID : pageProperty.getAllPageID()) {
                System.out.println("calculate doc length of " + pageID);

                String[] keyWords = invertedIndex.getKeyWords(pageID);
                double documentLength = 0;
                for (String KeyWord : keyWords) {
                    if (KeyWord.equals("")) {
                        continue;
                    }
                    double termWeight = invertedIndex.getTermWeight(KeyWord, pageID);
                    documentLength += Math.pow(termWeight, 2);
                }
                documentLength = Math.sqrt(documentLength);
                System.out.println("save to db");
                docLengthDB.put(String.valueOf(pageID).getBytes(), String.valueOf(documentLength).getBytes());
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws RocksDBException {
        PreProcessor preProcessor = getInstance();
        //preProcessor.preComputeDocumentLength();
        //preProcessor.prepareParentPageRelationship();
        System.out.println(preProcessor.getParentPages(9956));
    }
}
