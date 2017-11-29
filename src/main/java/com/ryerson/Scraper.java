package com.ryerson;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scraper implements Supplier<Elements> {

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    private final String[] searchQuery;
    private String baseUrl;
    public static ConcurrentMap<String, Long> polite = new ConcurrentHashMap<>();
    public static ConcurrentMap<String, List<URI>> robots = new ConcurrentHashMap<>();


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
            URL url = new URL(baseUrl);
            String protocol = url.getProtocol();
            String domain = url.getHost();
            domain = domain.startsWith("www") ? domain : domain.substring(4);
            saveRobotText(domain, protocol);
            boolean shouldScrape = true;
            for (URI bannedSub : robots.get(domain)) {
                if (isBannedChild(bannedSub, url.toURI())){
                    System.out.println("Dont scrape page of domain " + domain);
                    shouldScrape = false;
                    break;
                }
            }
            if (!shouldScrape) return null;
            if (polite.get(domain) != null && System.currentTimeMillis() -  polite.get(domain) < 500 ) {
                Thread.sleep(500);
            }
            polite.replace(domain, System.currentTimeMillis());
            polite.putIfAbsent(domain, System.currentTimeMillis());
            Document webpage = Jsoup.connect(baseUrl).get();
            Crawler.documentStorage.put(baseUrl, webpage);
            return filterEncounterUrls(webpage.select("a[href*=http]"));
        } catch (Exception ex) {
            System.out.println("Error occurred when trying to parse wepage " + baseUrl+ " : " + ex);
            return null;
        }
    }

    // Process a domains robot.txt
    private void saveRobotText(String hostname, String protocol) {
        if (robots.get(hostname) == null) {
            String robotsText = protocol + "://" +  hostname + "/robots.txt";
            try(BufferedReader in = new BufferedReader(
                    new InputStreamReader(new URL(robotsText).openStream()))) {
                System.out.println("Robot text found for " + hostname);
                List<String> denied = new ArrayList<>();
                String line;
                final Pattern pattern = Pattern.compile("disallow:\\s*/(.*)");
                Matcher matcher;
                boolean robot_agent = false;
                while (((line = in.readLine()) != null)) {
                    line = line.toLowerCase();
                    if (robot_agent && line.replaceAll("\\s+","").equals("user-agent:*"))
                        break;

                    matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        denied.add(matcher.group(1));
                    }

                    if (line.matches("user-agent:\\s+\\*$")){
                        robot_agent = true;
                    }
                }

                List<URI> blockedPaths = new LinkedList<>();
                for (String s: denied) blockedPaths.add(new URI(hostname + s));
                //for (URI s : blockedPaths) System.out.println(s);
                robots.putIfAbsent(hostname, blockedPaths);
            } catch (Exception ex) {
                robots.putIfAbsent(hostname, new ArrayList<>());
            }
        } else {
            robots.putIfAbsent(hostname, new ArrayList<>());
        }
    }

    private static boolean isBannedChild(URI banned, URI current){
        return current.getPath().endsWith(banned.relativize(current).toString());
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

