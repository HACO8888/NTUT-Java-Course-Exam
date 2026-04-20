package com.crawler.model;

public class CrawlConfig {
    private String seedUrl = "";
    private int maxDepth = 3;
    private int maxPages = 100;
    private int threadCount = 8;
    private long requestDelayMs = 200;
    private int timeoutMs = 10000;
    private String userAgent = "JavaCrawler/1.0";
    private boolean respectRobotsTxt = true;
    private TraversalStrategy traversalStrategy = TraversalStrategy.BFS;
    private String includePattern = "";
    private String excludePattern = "";
    private String extractionSelector = "";
    private boolean downloadResources = false;
    private boolean sameDomainOnly = true;

    public enum TraversalStrategy {
        BFS("廣度優先"), DFS("深度優先");

        private final String label;

        TraversalStrategy(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public String getSeedUrl() { return seedUrl; }
    public void setSeedUrl(String seedUrl) { this.seedUrl = seedUrl; }

    public int getMaxDepth() { return maxDepth; }
    public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }

    public int getMaxPages() { return maxPages; }
    public void setMaxPages(int maxPages) { this.maxPages = maxPages; }

    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }

    public long getRequestDelayMs() { return requestDelayMs; }
    public void setRequestDelayMs(long requestDelayMs) { this.requestDelayMs = requestDelayMs; }

    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public boolean isRespectRobotsTxt() { return respectRobotsTxt; }
    public void setRespectRobotsTxt(boolean respectRobotsTxt) { this.respectRobotsTxt = respectRobotsTxt; }

    public TraversalStrategy getTraversalStrategy() { return traversalStrategy; }
    public void setTraversalStrategy(TraversalStrategy traversalStrategy) { this.traversalStrategy = traversalStrategy; }

    public String getIncludePattern() { return includePattern; }
    public void setIncludePattern(String includePattern) { this.includePattern = includePattern; }

    public String getExcludePattern() { return excludePattern; }
    public void setExcludePattern(String excludePattern) { this.excludePattern = excludePattern; }

    public String getExtractionSelector() { return extractionSelector; }
    public void setExtractionSelector(String extractionSelector) { this.extractionSelector = extractionSelector; }

    public boolean isDownloadResources() { return downloadResources; }
    public void setDownloadResources(boolean downloadResources) { this.downloadResources = downloadResources; }

    public boolean isSameDomainOnly() { return sameDomainOnly; }
    public void setSameDomainOnly(boolean sameDomainOnly) { this.sameDomainOnly = sameDomainOnly; }

    public String getSeedDomain() {
        try {
            return new java.net.URL(seedUrl).getHost().toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }
}
