package com.ryerson;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class Scraper {

    Scraper() {

    }

    /**
     * Retrieve an arrray of HTML link elements for a given url. Hard coded to process Bing results, and filter out irrelevant elements.
     * @param url
     * @return
     */
    public Elements retrieveInitialSeedResults(String url) {
        try {
            Document document = Jsoup.connect(url).get();
            //document.select("a[href*=#]").remove();
            document.select("a[id]").remove();
            document.select("a[href*=bat.bing.com]").remove();
            document.select("a[href*=choice.microsoft]").remove();
            Elements links = document.select("a[href*=http]"); // a with href
            return links;
        } catch (IOException ex) {
            System.out.println("Exception encountered : " + ex);
            return null;
        }
    }
}

