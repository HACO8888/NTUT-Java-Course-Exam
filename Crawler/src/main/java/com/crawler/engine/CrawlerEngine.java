package com.crawler.engine;

import com.crawler.model.*;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CrawlerEngine {
    private CrawlConfig config;
    private final CrawlStatistics stats = new CrawlStatistics();
    private UrlFrontier frontier;
    private PageFetcher fetcher;
    private ContentParser parser;
    private RobotsHandler robots;
    private ExecutorService threadPool;
    private Thread coordinatorThread;

    private volatile CrawlState state = CrawlState.IDLE;
    private volatile boolean paused = false;
    private volatile boolean stopped = false;
    private final Object pauseLock = new Object();

    private final List<CrawlResult> results = Collections.synchronizedList(new ArrayList<>());
    private SiteNode rootNode;

    private Consumer<CrawlResult> onResult;
    private Consumer<LogEntry> onLog;
    private Runnable onStateChange;
    private BiConsumer<SiteNode, String> onNodeDiscovered;

    public void setOnResult(Consumer<CrawlResult> onResult) { this.onResult = onResult; }
    public void setOnLog(Consumer<LogEntry> onLog) { this.onLog = onLog; }
    public void setOnStateChange(Runnable onStateChange) { this.onStateChange = onStateChange; }
    public void setOnNodeDiscovered(BiConsumer<SiteNode, String> onNodeDiscovered) { this.onNodeDiscovered = onNodeDiscovered; }

    public void start(CrawlConfig config) {
        this.config = config;
        this.stopped = false;
        this.paused = false;
        results.clear();

        frontier = new UrlFrontier(config.getTraversalStrategy());
        fetcher = new PageFetcher();
        parser = new ContentParser();
        robots = new RobotsHandler(config.getTimeoutMs());
        stats.reset();

        rootNode = new SiteNode(config.getSeedUrl());
        frontier.add(config.getSeedUrl(), 0);

        threadPool = Executors.newFixedThreadPool(config.getThreadCount());
        setState(CrawlState.RUNNING);
        log(LogEntry.Level.INFO, "開始爬取: " + config.getSeedUrl());

        coordinatorThread = new Thread(this::coordinatorLoop, "crawler-coordinator");
        coordinatorThread.setDaemon(true);
        coordinatorThread.start();
    }

    private void coordinatorLoop() {
        try {
            while (!stopped) {
                checkPause();

                if (stats.getTotalPages() >= config.getMaxPages()) {
                    log(LogEntry.Level.INFO, "已達最大頁數限制: " + config.getMaxPages());
                    break;
                }

                UrlFrontier.UrlEntry entry = frontier.poll();
                if (entry == null) {
                    if (stats.getActiveThreads() == 0) {
                        break;
                    }
                    Thread.sleep(100);
                    continue;
                }

                stats.setQueueSize(frontier.size());

                if (entry.getDepth() > config.getMaxDepth()) continue;
                if (stats.getTotalPages() >= config.getMaxPages()) break;

                stats.incrementActiveThreads();
                threadPool.submit(() -> crawlPage(entry));
            }
        } catch (InterruptedException ignored) {
        } finally {
            shutdownPool();
            if (!stopped) {
                setState(CrawlState.COMPLETED);
                log(LogEntry.Level.INFO, String.format("爬取完成。共 %d 頁，%d 錯誤",
                        stats.getPagesSuccess(), stats.getPagesError()));
            }
        }
    }

    private void crawlPage(UrlFrontier.UrlEntry entry) {
        if (stopped) { stats.decrementActiveThreads(); return; }
        try {
            checkPause();

            if (config.isRespectRobotsTxt() && !robots.isAllowed(entry.getUrl(), config.getUserAgent())) {
                log(LogEntry.Level.WARN, "被 robots.txt 阻擋: " + entry.getUrl());
                return;
            }

            PageFetcher.FetchResult fetchResult = fetcher.fetch(entry.getUrl(), config);

            if (fetchResult.getError() != null) {
                stats.incrementError();
                log(LogEntry.Level.ERROR, String.format("擷取失敗 %s: %s",
                        entry.getUrl(), fetchResult.getError().getMessage()));
                return;
            }

            if (!fetchResult.isSuccess()) {
                stats.incrementError();
                log(LogEntry.Level.WARN, String.format("HTTP %d: %s",
                        fetchResult.getStatusCode(), entry.getUrl()));
            } else {
                stats.incrementSuccess();
            }

            stats.addBytes(fetchResult.getContentLength());

            if (fetchResult.getDocument() != null) {
                CrawlResult result = parser.parse(
                        entry.getUrl(), fetchResult.getDocument(), entry.getDepth(),
                        fetchResult.getResponseTimeMs(), fetchResult.getStatusCode(),
                        fetchResult.getContentLength(), fetchResult.getContentType(), config);

                results.add(result);

                SiteNode node = new SiteNode(entry.getUrl());
                node.setTitle(result.getTitle());
                node.setStatusCode(result.getStatusCode());

                if (entry.getDepth() < config.getMaxDepth()) {
                    for (String link : result.getLinks()) {
                        frontier.add(link, entry.getDepth() + 1, entry.getUrl());
                    }
                }
                stats.setQueueSize(frontier.size());

                fireOnResult(result);
                fireOnNodeDiscovered(node, entry.getParentUrl());

                log(LogEntry.Level.INFO, String.format("[%d] %s - %s (%dms)",
                        fetchResult.getStatusCode(), result.getTitle(), entry.getUrl(),
                        fetchResult.getResponseTimeMs()));
            }
        } catch (Exception e) {
            stats.incrementError();
            log(LogEntry.Level.ERROR, "例外: " + entry.getUrl() + " - " + e.getMessage());
        } finally {
            stats.decrementActiveThreads();
        }
    }

    public void pause() {
        if (state != CrawlState.RUNNING) return;
        paused = true;
        setState(CrawlState.PAUSED);
        log(LogEntry.Level.INFO, "爬取已暫停");
    }

    public void resume() {
        if (state != CrawlState.PAUSED) return;
        paused = false;
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
        setState(CrawlState.RUNNING);
        log(LogEntry.Level.INFO, "爬取已繼續");
    }

    public void stop() {
        stopped = true;
        paused = false;
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
        setState(CrawlState.STOPPED);
        log(LogEntry.Level.INFO, "爬取已停止");
        if (coordinatorThread != null) {
            coordinatorThread.interrupt();
        }
    }

    private void shutdownPool() {
        if (threadPool != null) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
            }
        }
    }

    private void checkPause() {
        synchronized (pauseLock) {
            while (paused && !stopped) {
                try {
                    pauseLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void setState(CrawlState newState) {
        this.state = newState;
        if (onStateChange != null) {
            SwingUtilities.invokeLater(onStateChange);
        }
    }

    private void log(LogEntry.Level level, String message) {
        LogEntry entry = new LogEntry(level, message);
        if (onLog != null) {
            SwingUtilities.invokeLater(() -> onLog.accept(entry));
        }
    }

    private void fireOnResult(CrawlResult result) {
        if (onResult != null) {
            SwingUtilities.invokeLater(() -> onResult.accept(result));
        }
    }

    private void fireOnNodeDiscovered(SiteNode node, String parentUrl) {
        if (onNodeDiscovered != null) {
            SwingUtilities.invokeLater(() -> onNodeDiscovered.accept(node, parentUrl));
        }
    }

    public CrawlState getState() { return state; }
    public CrawlStatistics getStatistics() { return stats; }
    public SiteNode getRootNode() { return rootNode; }
    public List<CrawlResult> getResults() { return new ArrayList<>(results); }
    public CrawlConfig getConfig() { return config; }
}
