package com.ryerson;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Hello world!
 *
 */
public class Crawler
{

    public static void main( String[] args ) throws IOException {
        Set<String> initialSeed = initializeWebCrawlerSeed();
        System.out.println(initialSeed.size());
        for (String link : initialSeed) {
            System.out.println(link);
        }
    }

    /**
     * Retrieve top 100 results from Bing for a user inputted query.
     * @return set containg urls of the top 100 resutls of a search term.
     */
    private static Set<String> initializeWebCrawlerSeed() {
        StringBuilder initialSeedUrl = new StringBuilder("https://www.bing.com/search?q=");
        Set<String> unique_links = new HashSet<String>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Please enter a search term to crawl the web for: ");
        try {
            String userInput = reader.readLine();
            String[] termsToSearch = userInput.split(" ");
            for (int i = 0; i < termsToSearch.length; i++) {
                initialSeedUrl.append(termsToSearch[i]);
                if (i == termsToSearch.length - 1) continue;
                initialSeedUrl.append("+");
            }
            initialSeedUrl.append("&count=50");

            Scraper initialScraper = new Scraper();
            Elements seedResults = initialScraper.retrieveInitialSeedResults(initialSeedUrl.toString());

            // Retrieve another 50 results and combine all 100 results into a single elements arraylist.
            initialSeedUrl.append("&first=51&FORM=PORE");
            Elements additionalResults = initialScraper.retrieveInitialSeedResults(initialSeedUrl.toString());
            seedResults.addAll(additionalResults);

            System.out.println("The number of results collected for the query is: " + seedResults.size());
            for (Element link : seedResults) {
                //System.out.println("Link is " + link.attr("href")+ "and inner text is " + link.html());
                unique_links.add(link.attr("href"));
            }
            return unique_links;
        } catch (IOException ex) {
            System.out.println("Encountered IOException: " + ex);
            return unique_links;
        }
    }


}
