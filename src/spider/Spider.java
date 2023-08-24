package spider;

import indexer.Indexer;
import indexer.InvertedIndex;
import indexer.PageProperty;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

class Spider {
    private final Indexer indexer;
    private final InvertedIndex invertedIndex;
    private final PageProperty pageProperty;
    private final Queue<String> queue;

    /**
     * fetch one link (use for beginning)
     * @param url the link
     */
    Spider(String url){
        indexer = Indexer.getInstance();
        invertedIndex = InvertedIndex.getInstance();
        pageProperty = PageProperty.getInstance();
        queue = new LinkedList<>();
        queue.add(url);
    }

    /**
     * fetch many link (use for subsequence fetch), is much quicker
     * @param queue the queue contain all the links
     */
    Spider(Queue<String> queue) {
        indexer = Indexer.getInstance();
        invertedIndex = InvertedIndex.getInstance();
        pageProperty = PageProperty.getInstance();
        this.queue = queue;
    }
    /**
     * do BFS
     * fetch n more page to the system, weather a page need to be fetch into the system
     * will be determine automatically.
     * n must not be too large, otherwise continuous access the same web server
     * too much in a short time will make the web server treat you as hacker
     * (connect to UST vpn can solve this problem)
     * Moreover, set n too large will cause stack overflow runtime error...
     * just set n around 1000 is secure.
     * and block access from you temporarily.
     * @param numOfPage n (required number of page to be fetch)
     */
    void BFS(int numOfPage){
        Set<String> discoveredPage = new HashSet<>();
        // these website have problem, will make the program in trouble, add them to discoveredPage
        // in order to avoid the spider reach those site.
        discoveredPage.add("https://home.cse.ust.hk/~rossiter/independent_studies_projects/classifier_reddit_bots/post_title_chart.html");
        discoveredPage.add("http://home.cse.ust.hk/~rossiter/independent_studies_projects/classifier_reddit_bots/post_title_chart.html");
        discoveredPage.add("http://www.cse.ust.hk/faculty/rossiter/independent_studies_projects/classifier_reddit_bots/post_title_chart.html");
        discoveredPage.add("http://www.cse.ust.hk/faculty/rossiter/independent_studies_projects/classifier_reddit_bots/comment_text_chart.html");
        discoveredPage.add("https://home.cse.ust.hk/~rossiter/independent_studies_projects/classifier_reddit_bots/comment_text_chart.html");
        discoveredPage.add("http://home.cse.ust.hk/~rossiter/independent_studies_projects/classifier_reddit_bots/comment_text_chart.html");
        discoveredPage.add("http://home.cse.ust.hk/~twinsen/OurGMM.m");
        discoveredPage.add("http://home.cse.ust.hk/~skiena/510/schedule");
        int pageFetched = 0;
        while (!queue.isEmpty()) {
            String site = queue.remove();
            if (pageFetched >= numOfPage) {
                break;
            }
            if (discoveredPage.contains(site)) {
                continue;
            }
            PageType type = fetchCase(site);
            discoveredPage.add(site);
            if (type == PageType.ignore) {
                continue;
            }
            int pageID = 0;
            if ( type != PageType.bypass) {
                pageFetched++;
                pageID = indexer.searchIDByURL(site, true);
                System.out.println(pageFetched);
                System.out.println(pageID +" handling " + site);
                if (type == PageType.updateOld) {
                    System.out.print("clear record ");
                    invertedIndex.clearRecord(pageID);
                }
                System.out.print("finding info ");
                pageProperty.store(pageID, site);
                System.out.print("indexing ");
                invertedIndex.store(pageID, site);
                System.out.print("processing ");
            }

            List<String> links;
            if(type == PageType.bypass){
                links = invertedIndex.getAllChildPage(pageID, InvertedIndex.Status.All);
            } else {
                links = new WebInfoSeeker(site).getChildLinks();
            }
            for (String link : links) {
                if (!queue.contains(link)) {
                    queue.add(link);
                }
            }
            System.out.println("complete ");
        }
        try (PrintWriter writer = new PrintWriter("remainingQueue.txt")) {
            System.out.println("save process");
            for (String link : queue) {
                writer.println(link);
            }
        } catch (FileNotFoundException e) { e.printStackTrace(); }
    }

    /**
     * type of pages:
     * ignore: should ignore by the spider
     * addNew: is unseen by the spider
     * updateOld: is seen by the spider, but the page need update
     * bypass: is seen by the spider, and the page not need update
     */
    private enum PageType {ignore, addNew, updateOld, bypass}

    /**
     * determine the type of page
     *  if not cse website: ignore
     * if the link is dead or need login access: ignore
     * if the link go to a non html page: ignore
     * if not in the local system: addNew
     * if in the local system:
     *      if last modification date of the page is later than (more than 1 day) the recorded in the index: updateOld
     *      else: bypass
     * @param url the url to the page
     * @return case
     */
    private PageType fetchCase(String url) {
        System.out.println("checking " + url);
        WebInfoSeeker seeker = new WebInfoSeeker(url);
        //ignore page that is not cse web page
        if (!seeker.isCSEWebpage()) {
            return PageType.ignore;
        }
        //ignore page that need login to access, or the link is dead
        if (!seeker.canAccess()) {
            return PageType.ignore;
        }
        //ignore page that is a not html page
        if (!seeker.isHtmlPage()) {
            return PageType.ignore;
        }
        //if site is not in the system
        Integer pageID = indexer.searchIDByURL(url,false);
        if (pageID == -1) {
            return PageType.addNew;
        }
        if (pageProperty.getUrl(pageID) == null) {
            return PageType.addNew;
        }
        //if need update
        String lastModify = pageProperty.getLastModificationTime(pageID);
        long diff = new Date(Date.parse(new WebInfoSeeker(url).getLastModificationTime())).getTime()
                        - new Date(Date.parse(lastModify)).getTime();
        // 86400000 is 1 day in nanosecond
        return (diff > 86400000) ? PageType.updateOld : PageType.bypass;
        //return PageType.updateOld;

    }

    /**
     * show details of all fetched page in a txt file
     * @param outputPath the path to the .txt file
     */
    void printAll(String outputPath) {
        try (PrintWriter writer = new PrintWriter(outputPath)) {
            List<Integer> maxID = pageProperty.getAllPageID();
            for (int ID : maxID) {
                String url = pageProperty.getUrl(ID);
                writer.println(ID);
                writer.println(pageProperty.getTitle(ID));
                writer.println(url);
                writer.print(pageProperty.getLastModificationTime(ID));
                writer.print(", ");
                writer.println(pageProperty.getSize(ID));
                String[] words = invertedIndex.getKeyWords(ID);

                for (String word : words) {
                    if (word.equals("")) {
                        continue;
                    }
                    writer.print(word + " ");
                    writer.print(InvertedIndex.getInstance().getFreqOfWordInParticularPage(word, ID) + "; ");
                }

                writer.println();
                //writer.println(invertedIndex.getChildPages(ID));
                writer.println("......................................................................");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}
