package com.stockbell.one;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class KlineSeries {
    private final List<Kline> bars;

    public KlineSeries(List<Kline> bars) {
        this.bars = Collections.unmodifiableList(new ArrayList<>(bars));
    }

    public List<Kline> bars() {
        return bars;
    }

    public int size() {
        return bars.size();
    }

    public boolean isEmpty() {
        return bars.isEmpty();
    }

    public double[] closes() {
        double[] arr = new double[bars.size()];
        for (int i = 0; i < bars.size(); i++) arr[i] = bars.get(i).close();
        return arr;
    }

    public double minLow() {
        double min = Double.POSITIVE_INFINITY;
        for (Kline k : bars) if (k.low() < min) min = k.low();
        return min;
    }

    public double maxHigh() {
        double max = Double.NEGATIVE_INFINITY;
        for (Kline k : bars) if (k.high() > max) max = k.high();
        return max;
    }

    public long maxVolume() {
        long max = 0;
        for (Kline k : bars) if (k.volume() > max) max = k.volume();
        return max;
    }

    public double[] movingAverage(int period) {
        double[] closes = closes();
        double[] ma = new double[closes.length];
        for (int i = 0; i < closes.length; i++) {
            if (i < period - 1) {
                ma[i] = Double.NaN;
                continue;
            }
            double sum = 0;
            for (int j = i - period + 1; j <= i; j++) sum += closes[j];
            ma[i] = sum / period;
        }
        return ma;
    }
}
