package com.crawler.model;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CrawlStatistics {
    private final AtomicInteger pagesSuccess = new AtomicInteger(0);
    private final AtomicInteger pagesError = new AtomicInteger(0);
    private final AtomicLong totalBytes = new AtomicLong(0);
    private final AtomicInteger queueSize = new AtomicInteger(0);
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private volatile long startTimeMs;

    public void reset() {
        pagesSuccess.set(0);
        pagesError.set(0);
        totalBytes.set(0);
        queueSize.set(0);
        activeThreads.set(0);
        startTimeMs = System.currentTimeMillis();
    }

    public void incrementSuccess() { pagesSuccess.incrementAndGet(); }
    public void incrementError() { pagesError.incrementAndGet(); }
    public void addBytes(long bytes) { totalBytes.addAndGet(bytes); }
    public void setQueueSize(int size) { queueSize.set(size); }
    public void incrementActiveThreads() { activeThreads.incrementAndGet(); }
    public void decrementActiveThreads() { activeThreads.decrementAndGet(); }

    public int getPagesSuccess() { return pagesSuccess.get(); }
    public int getPagesError() { return pagesError.get(); }
    public int getTotalPages() { return pagesSuccess.get() + pagesError.get(); }
    public long getTotalBytes() { return totalBytes.get(); }
    public int getQueueSize() { return queueSize.get(); }
    public int getActiveThreads() { return activeThreads.get(); }
    public long getStartTimeMs() { return startTimeMs; }

    public long getElapsedMs() {
        return System.currentTimeMillis() - startTimeMs;
    }

    public double getPagesPerSecond() {
        long elapsed = getElapsedMs();
        if (elapsed <= 0) return 0;
        return getTotalPages() * 1000.0 / elapsed;
    }

    public String getFormattedBytes() {
        long bytes = totalBytes.get();
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    public String getFormattedElapsed() {
        long seconds = getElapsedMs() / 1000;
        long m = seconds / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }
}
