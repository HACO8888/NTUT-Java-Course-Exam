package com.stockbell.one;

public record Kline(
        long timestamp,
        double open,
        double high,
        double low,
        double close,
        long volume
) {
}
