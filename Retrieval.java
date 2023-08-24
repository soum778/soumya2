package retriever;

import indexer.Indexer;
import indexer.InvertedIndex;
import util.Word;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Retrieval {

    private final Indexer indexer = Indexer.getInstance();
    private final InvertedIndex invertedIndex = InvertedIndex.getInstance();
    private final PreProcessor preProcessor = PreProcessor.getInstance();
    private final PageRank pageRank = PageRank.getInstance();

    private final LinkedHashMap<Integer, Double> top50Result = new LinkedHashMap<>();

    public Retrieval(String query) { //pass value here
        Set<String> afterProcessQuery = processQuery(query);

        if (!afterProcessQuery.isEmpty()) {  //0.7*cosine sim + 0.3*page rank + 0.2(if title match)
            HashMap<Integer, Double> allResultList = cosineSimilarity(afterProcessQuery);
            combineWithPageRank(allResultList);
            titleMatch(afterProcessQuery, allResultList);
            retrievalTop50(allResultList);
        }
    }

    public LinkedHashMap<Integer, Double> getResult() {
        return top50Result;
    } //get result here

    private Set<String> processQuery(String query){
        Set<String> set = new HashSet<>();
        Matcher matcher = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(query);
        while (matcher.find()) {
            String modifiedWord = matcher.group(1).replace("\"", ""); // Add .replace("\"", "") to remove surrounding quotes.
            if (modifiedWord.contains(" ")) {
                //process phrase
                String[] phrase = modifiedWord.split(" ");
                StringBuilder modifiedPhrase = new StringBuilder();
                for(String word: phrase){
                    modifiedPhrase.append(Word.porterAlgorithm(word));
                    modifiedPhrase.append(" ");
                }
                modifiedPhrase.deleteCharAt(modifiedPhrase.length() - 1);
                set.add(modifiedPhrase.toString());

            } else if (Word.isMeaningfulWord(modifiedWord))
                set.add(Word.porterAlgorithm(modifiedWord));
        }
        System.out.println("The query word: " + set);
        return set;
    }

    private HashMap<Integer, Double> cosineSimilarity(Set<String> afterProcessQuery){
        HashMap<Integer, Double> allResultList = new HashMap<>();
        int numOfQueryWord = afterProcessQuery.size();

        for (String queryWord : afterProcessQuery) {
            if (queryWord.contains(" ")) { //if phrase
                String[] phrase = queryWord.split(" "); //each word
                Integer[] wordID = new Integer[phrase.length]; // each word of their wordID
                Set<Integer> commonDocID = null; // to store the common docID,eg hong, kong both store in [1, 129, 2, 130, 131, 4, 132,...]
                boolean phraseInKeyword = true;  // all phrase words should appear in keyword, eg phrase "hkust abc", abc is not in keyword-> ignore it


                // check all phrase words should appear in keyword
                for(int wordOrder = 0; wordOrder < phrase.length; wordOrder++){
                    wordID[wordOrder] = indexer.searchIDByWord(phrase[wordOrder], false);
                    if (wordID[wordOrder] == -1) {
                        phraseInKeyword = false;
                        break;
                    }
                }
                if (!phraseInKeyword) continue;
                else numOfQueryWord += phrase.length-1;

                // to find the common docID
                for(int wordOrder = 0; wordOrder < phrase.length; wordOrder++){
                    if (commonDocID == null) commonDocID = invertedIndex.getRelatedPage(wordID[wordOrder]);
                    else commonDocID.retainAll(invertedIndex.getRelatedPage(wordID[wordOrder]));
                }

                // to check the case -> no more than 3 words apart
                assert commonDocID != null;
                for(Integer docID: commonDocID){
                    List<LinkedList<Integer>> wordPositions = new LinkedList<>();
                    for(int wordOrder = 0; wordOrder < phrase.length; wordOrder++) {
                        wordPositions.add(invertedIndex.getWordPositionsInPage(wordID[wordOrder], docID));
                    }

                    boolean adjacencyConditionsFulfil = true;
                    for (int i = 0; i < wordPositions.size() - 1; i++) {
                        if (findSmallestSeparation(wordPositions.get(i), wordPositions.get(i + 1)) > 3) {
                            adjacencyConditionsFulfil = false;
                            break;
                        }
                    }

                    if(adjacencyConditionsFulfil) {
                        for (String s : phrase) {
                            double partialScore = invertedIndex.getTermWeight(s, docID);
                            allResultList.merge(docID, partialScore, Double::sum);
                        }
                    }
                }

            }
            else {
                Integer currentWordID = indexer.searchIDByWord(queryWord, false);
                if(currentWordID != -1) {
                    Set<Integer> pageIDs = invertedIndex.getRelatedPage(currentWordID);

                    for (Integer pageID : pageIDs) {
                        double partialScore = invertedIndex.getTermWeight(queryWord, pageID);
                        allResultList.merge(pageID, partialScore, Double::sum);
                    }
                }
            }
        }
        double queryLength = Math.sqrt(numOfQueryWord);

        for (Integer pageID: allResultList.keySet()) {
            double documentLength = preProcessor.getDocLength(pageID);
            double afterNormalized = allResultList.get(pageID)/(documentLength*queryLength);

            allResultList.put(pageID, afterNormalized);

        }

        return allResultList;
    }

    private int findSmallestSeparation(LinkedList<Integer> array1, LinkedList<Integer> array2) {
        int array1Length = array1.size(), array2Length = array2.size();
        int array1CurrentIndex = 0, array2CurrentIndex = 0;

        int result = Integer.MAX_VALUE;
        while (array1CurrentIndex < array1Length && array2CurrentIndex < array2Length) {
            int tempDiff = array2.get(array2CurrentIndex) - array1.get(array1CurrentIndex);
            if (tempDiff > 0){
                if (tempDiff < result){
                    result = tempDiff;
                }
            }

            if (array1.get(array1CurrentIndex) < array2.get(array2CurrentIndex))
                array1CurrentIndex++;
            else
                array2CurrentIndex++;
        }
        return result;
    }

    private void combineWithPageRank(HashMap<Integer, Double> cosineSimilarityResult){
        for (Integer docID: cosineSimilarityResult.keySet()) {
            double updatedScore = cosineSimilarityResult.get(docID) * 0.7 + pageRank.getPageRank(docID) * 0.3;
            cosineSimilarityResult.put(docID, updatedScore);
        }
    }

    private void titleMatch (Set<String> afterProcessQuery, HashMap<Integer, Double> cosineSimAddPageRankResult){
        for (Integer pageID: cosineSimAddPageRankResult.keySet()) {
            boolean foundQueryInTitle = false;
            Set<String> titleWord = invertedIndex.getTitleWords(pageID);

            for (String queryWord : afterProcessQuery) {
                if (queryWord.contains(" ")) {
                    String[] phrase = queryWord.split(" ");
                    foundQueryInTitle = true;

                    for(String word: phrase){               //if whole phrase in title-> + 0.2
                        if (!titleWord.contains(word)) {
                            foundQueryInTitle = false;
                            break;
                        }
                    }

                    if (foundQueryInTitle) {
                        break;
                    }
                }
                else if (titleWord.contains(queryWord)) {   //if one of the query word in title -> + 0.2
                    foundQueryInTitle = true;
                    break;
                }
            }

            if (foundQueryInTitle) {
                cosineSimAddPageRankResult.put(pageID, cosineSimAddPageRankResult.get(pageID) + 0.2);
            }
        }

    }


    private void retrievalTop50(HashMap<Integer, Double> allResultList){
        //sort the related page by value
        LinkedHashMap<Integer, Double> sortedResultList =
                allResultList.entrySet()
                        .stream()
                        .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        int num = 0;
        for (Map.Entry<Integer, Double> entry : sortedResultList.entrySet()) {
            if (++num > 50) {
                break;
            }
            top50Result.put(entry.getKey(), entry.getValue());
        }
    }

    private void printAll(){
        System.out.println("PageID" +" "+ "Score");
        for (Map.Entry<Integer, Double> entry: top50Result.entrySet()) {
            System.out.println(entry.getKey() + ":  " +  Math.round(entry.getValue()*100000)/100000.00);
        }
    }

    public static void main(String[] args) {
        while (true){
            System.out.println("Enter query");
            Scanner scanner = new Scanner(System.in);  // Create a Scanner object
            String query = scanner.nextLine();  // Read user input
            if (query.length() < 1) {
                break;
            }
            long start = System.nanoTime();
            Retrieval newQuery = new Retrieval(query);
            newQuery.printAll();
            System.out.print("search take ");
            System.out.print((System.nanoTime() - start) / 1000000000.0);
            System.out.println("s");
        }
    }
}