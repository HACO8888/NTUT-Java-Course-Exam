package com.crawler.engine;

import com.crawler.model.CrawlConfig.TraversalStrategy;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class UrlFrontier {
    private final Deque<UrlEntry> queue = new ArrayDeque<>();
    private final Set<String> seen = new HashSet<>();
    private TraversalStrategy strategy;

    public UrlFrontier(TraversalStrategy strategy) {
        this.strategy = strategy;
    }

    public synchronized void add(String url, int depth) {
        add(url, depth, null);
    }

    public synchronized void add(String url, int depth, String parentUrl) {
        String normalized = normalize(url);
        if (normalized != null && seen.add(normalized)) {
            queue.addLast(new UrlEntry(normalized, depth, parentUrl));
        }
    }

    public synchronized UrlEntry poll() {
        if (queue.isEmpty()) return null;
        return strategy == TraversalStrategy.BFS ? queue.pollFirst() : queue.pollLast();
    }

    public synchronized boolean hasSeen(String url) {
        return seen.contains(normalize(url));
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    public synchronized void clear() {
        queue.clear();
        seen.clear();
    }

    private String normalize(String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            URL u = new URL(url);
            String host = u.getHost().toLowerCase();
            String path = u.getPath();
            if (path.endsWith("/") && path.length() > 1) {
                path = path.substring(0, path.length() - 1);
            }
            String query = u.getQuery();
            StringBuilder sb = new StringBuilder();
            sb.append(u.getProtocol()).append("://").append(host);
            if (u.getPort() != -1 && u.getPort() != u.getDefaultPort()) {
                sb.append(":").append(u.getPort());
            }
            sb.append(path);
            if (query != null && !query.isEmpty()) {
                sb.append("?").append(query);
            }
            return sb.toString();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static class UrlEntry {
        private final String url;
        private final int depth;
        private final String parentUrl;

        public UrlEntry(String url, int depth) {
            this(url, depth, null);
        }

        public UrlEntry(String url, int depth, String parentUrl) {
            this.url = url;
            this.depth = depth;
            this.parentUrl = parentUrl;
        }

        public String getUrl() { return url; }
        public int getDepth() { return depth; }
        public String getParentUrl() { return parentUrl; }
    }
}
