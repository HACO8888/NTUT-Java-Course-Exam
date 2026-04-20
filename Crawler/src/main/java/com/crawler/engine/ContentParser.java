package com.crawler.engine;

import com.crawler.model.CrawlConfig;
import com.crawler.model.CrawlResult;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ContentParser {

    public CrawlResult parse(String url, Document doc, int depth,
                             long responseTimeMs, int statusCode, long contentLength,
                             String contentType, CrawlConfig config) {
        String title = doc.title();
        String textContent = doc.text();
        String htmlContent = doc.html();

        if (config.getExtractionSelector() != null && !config.getExtractionSelector().isEmpty()) {
            try {
                Elements selected = doc.select(config.getExtractionSelector());
                textContent = selected.text();
            } catch (Exception ignored) {
            }
        }

        List<String> links = extractLinks(doc, url, config);
        List<String> images = extractImages(doc, url);

        if (contentType == null || contentType.isEmpty()) {
            contentType = "text/html";
        }

        return new CrawlResult(url, statusCode, title, contentLength,
                responseTimeMs, depth, contentType,
                htmlContent, textContent, links, images);
    }

    private List<String> extractLinks(Document doc, String baseUrl, CrawlConfig config) {
        List<String> links = new ArrayList<>();
        Elements anchors = doc.select("a[href]");
        Pattern includePattern = compilePattern(config.getIncludePattern());
        Pattern excludePattern = compilePattern(config.getExcludePattern());

        String seedDomain = config.getSeedDomain();

        for (Element a : anchors) {
            String href = a.absUrl("href");
            if (href.isEmpty()) continue;

            try {
                URL parsed = new URL(href);
                if (!parsed.getProtocol().startsWith("http")) continue;
                if (config.isSameDomainOnly() && !parsed.getHost().toLowerCase().equals(seedDomain)) continue;
                href = removeFragment(href);
            } catch (MalformedURLException e) {
                continue;
            }

            if (includePattern != null && !includePattern.matcher(href).find()) continue;
            if (excludePattern != null && excludePattern.matcher(href).find()) continue;

            links.add(href);
        }
        return links;
    }

    private List<String> extractImages(Document doc, String baseUrl) {
        List<String> images = new ArrayList<>();
        Elements imgs = doc.select("img[src]");
        for (Element img : imgs) {
            String src = img.absUrl("src");
            if (!src.isEmpty()) {
                images.add(src);
            }
        }
        return images;
    }

    private String removeFragment(String url) {
        int idx = url.indexOf('#');
        return idx >= 0 ? url.substring(0, idx) : url;
    }

    private Pattern compilePattern(String regex) {
        if (regex == null || regex.trim().isEmpty()) return null;
        try {
            return Pattern.compile(regex);
        } catch (Exception e) {
            return null;
        }
    }
}
