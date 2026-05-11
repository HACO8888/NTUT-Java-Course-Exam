package com.stockbell.one;

public final class WatchItem {
    public enum Direction { HIGH, LOW }

    private final String symbol;
    private String name;
    private double highThreshold;
    private double lowThreshold;
    private Quote lastQuote;
    private Direction lastDirection;

    public WatchItem(String symbol, String name, double highThreshold, double lowThreshold) {
        this.symbol = symbol;
        this.name = name;
        this.highThreshold = highThreshold;
        this.lowThreshold = lowThreshold;
    }

    public String symbol() { return symbol; }
    public String name() { return name; }
    public double highThreshold() { return highThreshold; }
    public double lowThreshold() { return lowThreshold; }
    public Quote lastQuote() { return lastQuote; }
    public Direction lastDirection() { return lastDirection; }

    public void setName(String name) { this.name = name; }
    public void setHighThreshold(double v) { this.highThreshold = v; }
    public void setLowThreshold(double v) { this.lowThreshold = v; }
    public void setLastQuote(Quote q) { this.lastQuote = q; }
    public void setLastDirection(Direction d) { this.lastDirection = d; }
}
