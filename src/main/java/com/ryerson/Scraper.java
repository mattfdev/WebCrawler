package com.ryerson;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class Scraper implements Supplier<Elements> {

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    private final String[] searchQuery;
    private String baseUrl;

    Scraper(String urlToParse, String[] query) {
        baseUrl = urlToParse;
        searchQuery = query;
    }

    public Elements get() {
        return processBaseUrlPage();
    }

    /**
     * Retrieve an arrray of HTML link elements for a given baseUrl. Hard coded to process Bing results, and filter out irrelevant elements.
     * @return an array of elements that point to HTML links.
     */
    public Elements retrieveInitialSeedResults() {
        try {
            Document document = Jsoup.connect(baseUrl).get();
            document.select("a[href*=#]").remove();
            document.select("a[id]").remove();
            // Filter out bing ad content.
            document.select("a[href*=bat.bing.com]").remove();
            document.select("a[href*=advertise.bingads]").remove();
            document.select("a[href*=go.microsoft]").remove();
            document.select("a[href*=choice.microsoft]").remove();
            return document.select("a[href*=http]"); // a with href
        } catch (IOException ex) {
            System.out.println("Exception encountered : " + ex);
            return null;
        }
    }

    /**
     * Store HTMl page in database, and return any potentially intresting links to future sites to crawl.
     * @return possibly relevant urls to add to crawling queue.
     */
    public Elements processBaseUrlPage() {
        try {
            Document webpage = Jsoup.connect(baseUrl).get();
            // Store in DB
            return filterEncounterUrls(webpage.select("a[href*=http]"));
        } catch (Exception ex) {
            System.out.println("Error occurred when trying to parse wepage " + baseUrl+ " : " + ex);
            return null;
        }
    }

    // Filter out irrelevant, and internally referring links.
    private Elements filterEncounterUrls(Elements htmlLinks) {
        Iterator<Element> elementFinder = htmlLinks.iterator();
        while(elementFinder.hasNext()) {
            Element link = elementFinder.next();
            String anchorText = link.html();
            String anchorLink = link.attr("href");
            boolean relevantLink = Arrays.stream(searchQuery).anyMatch(anchorText::contains)
                    || Arrays.stream(searchQuery).anyMatch(anchorLink::contains);
            boolean internalLink = anchorLink.contains(baseUrl);
            if (!relevantLink || internalLink) elementFinder.remove();
        }
        return htmlLinks;
    }
}

