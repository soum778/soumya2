package indexer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * the posting list of inverted index has the following format
 * pageID1:frequency:position1,position2,... pageID2:frequency2:position1,position2....
 */
class PostingListHandler {
    /**
     * pageID -> frequency
     */
    private HashMap<Integer, Integer> frequencyRecord = new HashMap<>();
    /**
     * pageID -> {position}
     */
    private HashMap<Integer, LinkedList<Integer>> positionsRecord = new HashMap<>();

    PostingListHandler(String invertedIndex) {
        String[] details = invertedIndex.split(" ");
        for (String detail : details) {
            if (detail.equals("")) {
                continue;
            }
            String[] content = detail.split(":");
            int pageID = Integer.parseInt(content[0]);
            frequencyRecord.put(pageID, Integer.valueOf(content[1]));
            positionsRecord.put(pageID, new LinkedList<>());
            String[] listOfPosition = content[2].split(",");
            for (String pos : listOfPosition) {
                positionsRecord.get(pageID).addLast(Integer.valueOf(pos));
            }
        }
    }

    /**
     * get relation: pageID -> frequency
     * @return a hashMap
     */
    HashMap<Integer, Integer> getFrequencyRecord() {
        return frequencyRecord;
    }

    /**
     * get relation: pageID -> {position}
     * @return a hashMap
     */
    HashMap<Integer, LinkedList<Integer>> getPositionsRecord() {
        return positionsRecord;
    }

    /**
     * remove record of a particular page in the posting list if that pageId is present
     * @param pageID page id
     * @return true if the posting list become empty after remove a page
     */
    boolean removeRecord(int pageID){
        frequencyRecord.remove(pageID);
        positionsRecord.remove(pageID);
        return (frequencyRecord.size() == 0);
    }

    /**
     * add a word
     * @param pageID page ID
     * @param pos position
     * @return current term frequency of the page after adding that word
     */
    int addWord(int pageID, int pos) {
        if (frequencyRecord.containsKey(pageID)) {
            frequencyRecord.replace(pageID, frequencyRecord.get(pageID) + 1);
        } else {
            frequencyRecord.put(pageID, 1);
            positionsRecord.put(pageID, new LinkedList<>());
        }
        positionsRecord.get(pageID).addLast(pos);
        return positionsRecord.get(pageID).size();
    }

    /**
     * convert the two hashMap back to a posting list
     * @return a String representation of posting list
     */
    public String toString(){
        StringBuilder result = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : frequencyRecord.entrySet()) {
            int pageID = entry.getKey();
            int frequency = entry.getValue();
            result.append(pageID);
            result.append(":");
            result.append(frequency);
            result.append(":");
            for (int pos : positionsRecord.get(pageID)) {
                result.append(pos);
                result.append(",");
            }
            result.deleteCharAt(result.length()-1);
            result.append(" ");
        }
        if (result.length() > 0) {
            result.deleteCharAt(result.length()-1);
        }
        return String.valueOf(result);
    }

    public static void main(String[] args) {
        System.out.println("test case 1");
        PostingListHandler reader = new PostingListHandler("");
        System.out.println(reader.toString());
        reader.addWord(20, 6);
        System.out.println(reader.toString());
        reader.addWord(25, 6);
        System.out.println(reader.toString());
        reader.addWord(25, 20);
        System.out.println(reader.toString());

        System.out.println("\ntest case 2");
        PostingListHandler temp = new PostingListHandler("20:2:5,6 25:2:6,20");
        System.out.println(temp.toString());
        temp.removeRecord(22);
        System.out.println(temp.toString());
        temp.removeRecord(25);
        System.out.println(temp.toString());
    }
}
