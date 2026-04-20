package com.crawler.model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogEntry {
    public enum Level { INFO, WARN, ERROR }

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    private final long timestamp;
    private final Level level;
    private final String message;

    public LogEntry(Level level, String message) {
        this.timestamp = System.currentTimeMillis();
        this.level = level;
        this.message = message;
    }

    public long getTimestamp() { return timestamp; }
    public Level getLevel() { return level; }
    public String getMessage() { return message; }

    public String getFormattedTime() {
        synchronized (TIME_FORMAT) {
            return TIME_FORMAT.format(new Date(timestamp));
        }
    }

    @Override
    public String toString() {
        return String.format("[%s] [%s] %s", getFormattedTime(), level, message);
    }
}
