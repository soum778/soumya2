package retriever;

import indexer.InvertedIndex;
import indexer.PageProperty;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PageRank {

    private static final PageRank INSTANCE = new PageRank();
    private final InvertedIndex invertedIndex = InvertedIndex.getInstance();
    private final PageProperty pageProperty = PageProperty.getInstance();
    private RocksDB pageRankDb;

    public static PageRank getInstance() {
        return INSTANCE;
    }

    private PageRank(){
        try {
            pageRankDb = RocksDB.open(new Options().setCreateIfMissing(true), "database/pageRankDB");
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public double getPageRank(int pageID) {
        try {
            return Double.parseDouble(new String(pageRankDb.get(String.valueOf(pageID).getBytes())));
        } catch (RocksDBException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void preCalculatePageRank(int iteration, double dampingFactor){
        HashMap<Integer, Double> pastPageRankResult = new HashMap<>();
        HashMap<Integer, Double> currentPageRankResult = new HashMap<>();
        List<Integer> allPages = pageProperty.getAllPageID();
        for (Integer pageID: allPages) {
            pastPageRankResult.put(pageID, 1.0);
            currentPageRankResult.put(pageID, 0.0);
        }

        for(int i = 0; i < iteration; i++) {
            for (Integer pageID : allPages) {
                String[] parentIDs = PreProcessor.getInstance().getParentIDs(pageID);

                if (parentIDs != null) {
                    for (String parentID : parentIDs) {
                        if (parentID.equals("")) {
                            continue;
                        }
                        if (invertedIndex.getChildIDs(Integer.parseInt(parentID)) != null) {
                            Double partialScore = pastPageRankResult.get(Integer.parseInt(parentID)) / invertedIndex.getChildIDs(Integer.parseInt(parentID)).length;
                            currentPageRankResult.put(pageID, currentPageRankResult.get(pageID)+partialScore);
                        }
                    }
                    double RankPage = (1.0 - dampingFactor) + (dampingFactor * currentPageRankResult.get(pageID));
                    currentPageRankResult.put(pageID, RankPage);

                }

            }
            if (i == iteration-1) {
                for (Map.Entry<Integer, Double> entry: currentPageRankResult.entrySet()) {
                    try {
                        System.out.println(entry.getKey() +": "+entry.getValue());
                        pageRankDb.put(String.valueOf(entry.getKey()).getBytes(), String.valueOf(entry.getValue()).getBytes());
                    } catch (RocksDBException e) {
                        e.printStackTrace();
                    }
                }
            }
            //reset the PageRank values if not final iteration
            else {
                System.out.println(currentPageRankResult);
                double diff = 0.0;
                for (int pageId : allPages) {
                    diff += Math.abs(pastPageRankResult.get(pageId) - currentPageRankResult.get(pageId));
                }
                System.out.println("in " + i + " iteration, diff = " + diff);
                for (Integer pageID : allPages) {
                    pastPageRankResult.put(pageID, currentPageRankResult.get(pageID));
                    currentPageRankResult.put(pageID, 0.0);
                }
            }
        }
    }

    public static void main(String[] args){
        PageRank pageRank = PageRank.getInstance();
        //don't run it again as the db is already here
//        pageRank.preCalculatePageRank(30, 0.85);
        System.out.println(pageRank.getPageRank(1));
    }

}
