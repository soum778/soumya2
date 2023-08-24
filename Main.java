package spider;

import indexer.InvertedIndex;
import indexer.PageProperty;
import util.Converter;

public class Main {

    private static void removePage(int pageID) {
        InvertedIndex.getInstance().clearRecord(pageID);
        PageProperty.getInstance().delEntry(pageID);
    }

    /**
     * don't run it again as the db is already here
     * @param args nothing
     */
    public static void main(String[] args) {
        String url = "https://www.cse.ust.hk";;
        Spider spider1 = new Spider(url);
        spider1.BFS(100);
        spider1.printAll("spider_result.txt");

//        continue fetch page base on previous not yet fetched child links
//        can un-comment the following line
        Spider spider2 = null;
        for (int i = 0; i < 180; i++) {
            spider2 = new Spider(Converter.readRemainingQueue("remainingQueue.txt"));
            spider2.BFS(100);
        }
        System.out.println("printing");
        //printing takes too long since so many page is in the database
//        spider2.printAll("spider_result.txt");
    }
}
