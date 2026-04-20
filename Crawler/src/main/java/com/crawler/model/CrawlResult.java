package com.crawler.model;

import java.util.ArrayList;
import java.util.List;

public class CrawlResult {
    private final String url;
    private final int statusCode;
    private final String title;
    private final long contentLength;
    private final long responseTimeMs;
    private final int depth;
    private final int linkCount;
    private final int imageCount;
    private final String contentType;
    private final String htmlContent;
    private final String textContent;
    private final List<String> links;
    private final List<String> images;
    private final long timestamp;

    public CrawlResult(String url, int statusCode, String title, long contentLength,
                       long responseTimeMs, int depth, String contentType,
                       String htmlContent, String textContent,
                       List<String> links, List<String> images) {
        this.url = url;
        this.statusCode = statusCode;
        this.title = title;
        this.contentLength = contentLength;
        this.responseTimeMs = responseTimeMs;
        this.depth = depth;
        this.contentType = contentType;
        this.htmlContent = htmlContent;
        this.textContent = textContent;
        this.links = links != null ? new ArrayList<>(links) : new ArrayList<>();
        this.images = images != null ? new ArrayList<>(images) : new ArrayList<>();
        this.linkCount = this.links.size();
        this.imageCount = this.images.size();
        this.timestamp = System.currentTimeMillis();
    }

    public String getUrl() { return url; }
    public int getStatusCode() { return statusCode; }
    public String getTitle() { return title; }
    public long getContentLength() { return contentLength; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public int getDepth() { return depth; }
    public int getLinkCount() { return linkCount; }
    public int getImageCount() { return imageCount; }
    public String getContentType() { return contentType; }
    public String getHtmlContent() { return htmlContent; }
    public String getTextContent() { return textContent; }
    public List<String> getLinks() { return links; }
    public List<String> getImages() { return images; }
    public long getTimestamp() { return timestamp; }
}
