package com.ryerson;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Hello world!
 *
 */
public class Crawler {

    private static final int numberOfHosts = 50;
    private static final int maxiumCrawlDepth = 4;
    private static final int maxiumumCrawledPages = 5000;
    private static String[] searchQuery;
    private static ArrayBlockingQueue<String> crawlingQueue;
    private static ConcurrentSkipListSet<String> visitedLinks = new ConcurrentSkipListSet<>();


    public static void main( String[] args ) throws IOException, InterruptedException, ExecutionException {
        crawlingQueue = new ArrayBlockingQueue<>(maxiumumCrawledPages, true, initializeWebCrawlerSeed());
        ExecutorService pool = Executors.newFixedThreadPool(5);
        List <Future<Elements>> threadLinks = new ArrayList<>();
        while (!crawlingQueue.isEmpty()) {
            String link = crawlingQueue.take();
            if (visitedLinks.contains(link)) {
                System.out.println("Dupe link, skip parse step");
                continue;
            }
            Future<Elements> futureLinks = pool.submit(new Scraper(link, searchQuery));
            Elements links = futureLinks.get();
            if (links != null) {
                for (Element elem : links) {
                    System.out.println("Link is " + elem.attr("href") + " : " + elem.html());
                    if (isLinkAllowedToBeQueued(link)) {
                        crawlingQueue.add(elem.html());
                    } else System.out.println("Dupe link found, skip.");
                }
            }
            visitedLinks.add(link);
        }
    }

    private static boolean isLinkAllowedToBeQueued(String link) {
        return  !visitedLinks.contains(link) && (visitedLinks.size() < maxiumumCrawledPages);
    }

    /**
     * Retrieve top 100 results from Bing for a user inputted query.
     * @return set containing urls of the top 100 results of a search term.
     */
    private static Set<String> initializeWebCrawlerSeed() {
        StringBuilder initialSeedUrl = new StringBuilder("https://www.bing.com/search?q=");
        Set<String> unique_links = new HashSet<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Please enter a search term to crawl the web for: ");
        try {
            String userInput = reader.readLine();
            searchQuery = userInput.split(" ");

            for (int i = 0; i < searchQuery.length; i++) {
                initialSeedUrl.append(searchQuery[i]);
                if (i == searchQuery.length - 1) continue;
                initialSeedUrl.append("+");
            }
            initialSeedUrl.append("&count=50");

            Scraper initialScraper = new Scraper(initialSeedUrl.toString(), searchQuery);
            Elements seedResults = initialScraper.retrieveInitialSeedResults();

            // Retrieve another 50 results and combine all 100 results into a single elements array list.
            initialScraper.setBaseUrl(initialSeedUrl.append("&first=51&FORM=PORE").toString());
            Elements additionalResults = initialScraper.retrieveInitialSeedResults();
            seedResults.addAll(additionalResults);

            System.out.println("The number of results collected for the query is: " + seedResults.size());
            unique_links.addAll(seedResults.select("a").eachAttr("href"));
            return unique_links;
        } catch (IOException ex) {
            System.out.println("Encountered IOException: " + ex);
            return unique_links;
        }
    }

}
