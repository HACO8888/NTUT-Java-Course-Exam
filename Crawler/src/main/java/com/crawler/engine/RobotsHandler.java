package com.crawler.engine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RobotsHandler {
    private final ConcurrentHashMap<String, List<String>> cache = new ConcurrentHashMap<>();
    private final int timeoutMs;

    public RobotsHandler(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean isAllowed(String urlStr, String userAgent) {
        try {
            URL url = new URL(urlStr);
            String domain = url.getProtocol() + "://" + url.getHost()
                    + (url.getPort() != -1 && url.getPort() != url.getDefaultPort()
                    ? ":" + url.getPort() : "");
            List<String> disallowed = cache.computeIfAbsent(domain, this::fetchRobotsTxt);
            String path = url.getPath();
            if (path.isEmpty()) path = "/";
            for (String rule : disallowed) {
                if (path.startsWith(rule)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private List<String> fetchRobotsTxt(String domain) {
        List<String> disallowed = new ArrayList<>();
        HttpURLConnection conn = null;
        try {
            URL robotsUrl = new URL(domain + "/robots.txt");
            conn = (HttpURLConnection) robotsUrl.openConnection();
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestProperty("User-Agent", "JavaCrawler/1.0");

            if (conn.getResponseCode() != 200) {
                return disallowed;
            }

            boolean relevantSection = false;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("#") || line.isEmpty()) continue;

                    if (line.toLowerCase().startsWith("user-agent:")) {
                        String agent = line.substring(11).trim();
                        relevantSection = agent.equals("*");
                    } else if (relevantSection && line.toLowerCase().startsWith("disallow:")) {
                        String path = line.substring(9).trim();
                        if (!path.isEmpty()) {
                            disallowed.add(path);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return disallowed;
    }

    public void clear() {
        cache.clear();
    }
}
