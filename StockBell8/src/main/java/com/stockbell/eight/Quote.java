package com.stockbell.eight;

public record Quote(String symbol, String name, double price, double previousClose,
                    double dayHigh, double dayLow, long volume, long timestamp) {
    public double change() { return price - previousClose; }
    public double changePercent() {
        if (previousClose <= 0) return 0;
        return (price - previousClose) / previousClose * 100.0;
    }
}
