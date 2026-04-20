package com.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class WebCrawler {

    private final int maxDepth;
    private final int maxPages;
    private final int timeoutMs;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private ExecutorService executor;
    private Consumer<CrawlResult> onResult;
    private Consumer<String> onLog;
    private Runnable onFinish;

    public WebCrawler(int maxDepth, int maxPages, int timeoutMs) {
        this.maxDepth = maxDepth;
        this.maxPages = maxPages;
        this.timeoutMs = timeoutMs;
    }

    public void setOnResult(Consumer<CrawlResult> onResult) {
        this.onResult = onResult;
    }

    public void setOnLog(Consumer<String> onLog) {
        this.onLog = onLog;
    }

    public void setOnFinish(Runnable onFinish) {
        this.onFinish = onFinish;
    }

    public void start(String seedUrl) {
        if (running.get()) return;
        running.set(true);
        visited.clear();
        executor = Executors.newFixedThreadPool(8);

        executor.submit(() -> {
            try {
                crawl(seedUrl, 0);
            } finally {
                executor.shutdown();
                try {
                    executor.awaitTermination(30, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }
                running.set(false);
                if (onFinish != null) onFinish.run();
            }
        });
    }

    public void stop() {
        running.set(false);
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    private void crawl(String url, int depth) {
        if (!running.get()) return;
        if (depth > maxDepth) return;
        if (visited.size() >= maxPages) return;
        if (!visited.add(url)) return;

        log("爬取中 (深度 %d): %s".formatted(depth, url));

        long startTime = System.currentTimeMillis();
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; SimpleCrawler/1.0)")
                    .timeout(timeoutMs)
                    .followRedirects(true)
                    .get();

            long elapsed = System.currentTimeMillis() - startTime;
            String title = doc.title();
            List<String> childLinks = new ArrayList<>();

            for (Element link : doc.select("a[href]")) {
                String href = link.absUrl("href");
                if (isValidUrl(href) && !visited.contains(href)) {
                    childLinks.add(href);
                }
            }

            CrawlResult result = new CrawlResult(url, title, depth, childLinks.size(), elapsed);
            if (onResult != null) onResult.accept(result);

            if (depth < maxDepth && running.get()) {
                List<Future<?>> futures = new ArrayList<>();
                for (String child : childLinks) {
                    if (visited.size() >= maxPages || !running.get()) break;
                    futures.add(executor.submit(() -> crawl(child, depth + 1)));
                }
                for (Future<?> f : futures) {
                    try {
                        f.get();
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (IOException e) {
            log("錯誤: %s - %s".formatted(url, e.getMessage()));
        }
    }

    private boolean isValidUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            return "http".equals(scheme) || "https".equals(scheme);
        } catch (Exception e) {
            return false;
        }
    }

    private void log(String message) {
        if (onLog != null) onLog.accept(message);
    }
}
