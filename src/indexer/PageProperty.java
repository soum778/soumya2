package indexer;

import org.rocksdb.*;
import spider.WebInfoSeeker;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class PageProperty {
    private static final PageProperty INSTANCE = new PageProperty();
    private RocksDB pagePropDB;
    private final List<ColumnFamilyHandle> handles = new Vector<>();
    private int numOfPageFetched;

    static public PageProperty getInstance() {
        return INSTANCE;
    }

    /**
     * open db file
     */
    private PageProperty(){
        String PATH = "database/pagePropDB";
        try {
            List<ColumnFamilyDescriptor> colFamily = new Vector<>();
            colFamily.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY));
            colFamily.add(new ColumnFamilyDescriptor("url".getBytes()));
            colFamily.add(new ColumnFamilyDescriptor("lastDateOfModification".getBytes()));
            colFamily.add(new ColumnFamilyDescriptor("size".getBytes()));
            DBOptions options = new DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true);
            pagePropDB = RocksDB.open(options, PATH, colFamily, handles);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        numOfPageFetched = getNumOfPageInDb();
    }

    int getNumOfPageFetched() {
        return numOfPageFetched;
    }

    public String getTitle(int pageID) {
        try {
            byte[] url = pagePropDB.get(handles.get(0), String.valueOf(pageID).getBytes());
            if (url == null) {
                return null;
            }
            return new String(url);
        } catch (RocksDBException e) {
            e.printStackTrace();
            System.out.println("this should not happened");
            return null;
        }
    }

    public void delEntry(int pageID){
        byte[] key = String.valueOf(pageID).getBytes();
        for(ColumnFamilyHandle h: handles){
            try {
                pagePropDB.delete(h, key);
            } catch (RocksDBException e) {
                e.printStackTrace();
            }
        }
    }

    public String getUrl(int pageID) {
        try {
            byte[] url = pagePropDB.get(handles.get(1), String.valueOf(pageID).getBytes());
            if (url == null) {
                return null;
            }
            return new String(url);
        } catch (RocksDBException e) {
            e.printStackTrace();
            System.out.println("this should not happened");
            return null;
        }
    }

    public String getSize(int pageID) {
        try {
            byte[] url = pagePropDB.get(handles.get(3), String.valueOf(pageID).getBytes());
            if (url == null) {
                return null;
            }
            return new String(url);
        } catch (RocksDBException e) {
            e.printStackTrace();
            System.out.println("this should not happened");
            return null;
        }
    }

    public String getLastModificationTime(int pageID) {
        try {
            return new String(pagePropDB.get(handles.get(2), String.valueOf(pageID).getBytes()));
        } catch (RocksDBException e) {
            e.printStackTrace();
            System.out.println("this should not happened");
            return null;
        }
    }

    /**
     * store page info into database
     * @param pageID ID of the page
     * @param url link of the page
     */
    public void store(int pageID, String url){

        WebInfoSeeker seeker = new WebInfoSeeker(url);
        try {
            pagePropDB.put(handles.get(0), Integer.toString(pageID).getBytes(), seeker.getTitle().getBytes());
            pagePropDB.put(handles.get(1), Integer.toString(pageID).getBytes(), url.getBytes());
            pagePropDB.put(handles.get(2), Integer.toString(pageID).getBytes(), seeker.getLastModificationTime().getBytes());
            pagePropDB.put(handles.get(3), Integer.toString(pageID).getBytes(), seeker.getPageSize().getBytes());
            numOfPageFetched++;
        } catch (RocksDBException e) {
            System.out.println("this should not happened");
            e.printStackTrace();
        }
    }

    private void printAll() throws RocksDBException {
        try (PrintWriter writer = new PrintWriter("allPage.txt")) {
            RocksIterator iterator = pagePropDB.newIterator();
            for(iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                writer.print(new String(iterator.key())+": ");
                writer.print(new String(pagePropDB.get(handles.get(0), iterator.key())));
                writer.print("\t");
                writer.print(new String(pagePropDB.get(handles.get(1), iterator.key())));
                writer.print("\t");
                writer.print(new String(pagePropDB.get(handles.get(2), iterator.key())));
                writer.print("\t");
                writer.print(new String(pagePropDB.get(handles.get(3), iterator.key())));
                writer.println();
            }
        } catch (FileNotFoundException e) { e.printStackTrace(); }

    }

    private int getNumOfPageInDb() {
        int num = 0;
        RocksIterator iterator = pagePropDB.newIterator();
        for(iterator.seekToFirst(); iterator.isValid(); iterator.next()){
            num++;
        }
        return num;
    }

    /**
     * get all pageID in the database
     * @return list of pageID
     */
    public List<Integer> getAllPageID() {
        List<Integer> pageIDs = new LinkedList<>();
        RocksIterator iterator = pagePropDB.newIterator();
        for(iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
            int ID = Integer.parseInt(new String(iterator.key()));
            pageIDs.add(ID);
        }
        return pageIDs;
    }

    public static void main(String[] args) throws RocksDBException {
        PageProperty pageProperty = getInstance();
        pageProperty.printAll();
    }
}
