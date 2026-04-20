package com.crawler.engine;

import com.crawler.model.CrawlConfig;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class PageFetcher {
    private final Object rateLock = new Object();
    private long lastRequestTime = 0;

    public FetchResult fetch(String url, CrawlConfig config) {
        enforceRateLimit(config.getRequestDelayMs());
        long start = System.currentTimeMillis();
        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(config.getUserAgent())
                    .timeout(config.getTimeoutMs())
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .maxBodySize(5 * 1024 * 1024)
                    .execute();

            long elapsed = System.currentTimeMillis() - start;
            String contentType = response.contentType();
            byte[] body = response.bodyAsBytes();
            long contentLength = body != null ? body.length : 0;
            Document doc = response.parse();

            return new FetchResult(doc, response.statusCode(), contentLength,
                    elapsed, contentType, null);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return new FetchResult(null, -1, 0, elapsed, null, e);
        }
    }

    private void enforceRateLimit(long delayMs) {
        if (delayMs <= 0) return;
        synchronized (rateLock) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRequestTime;
            if (elapsed < delayMs) {
                try {
                    Thread.sleep(delayMs - elapsed);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            lastRequestTime = System.currentTimeMillis();
        }
    }

    public static class FetchResult {
        private final Document document;
        private final int statusCode;
        private final long contentLength;
        private final long responseTimeMs;
        private final String contentType;
        private final Exception error;

        public FetchResult(Document document, int statusCode, long contentLength,
                           long responseTimeMs, String contentType, Exception error) {
            this.document = document;
            this.statusCode = statusCode;
            this.contentLength = contentLength;
            this.responseTimeMs = responseTimeMs;
            this.contentType = contentType;
            this.error = error;
        }

        public Document getDocument() { return document; }
        public int getStatusCode() { return statusCode; }
        public long getContentLength() { return contentLength; }
        public long getResponseTimeMs() { return responseTimeMs; }
        public String getContentType() { return contentType; }
        public Exception getError() { return error; }
        public boolean isSuccess() { return error == null && statusCode >= 200 && statusCode < 400; }
    }
}
