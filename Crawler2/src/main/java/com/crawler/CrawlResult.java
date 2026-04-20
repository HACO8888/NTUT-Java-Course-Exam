package com.crawler;

public record CrawlResult(String url, String title, int depth, int linkCount, long responseTimeMs) {
}
