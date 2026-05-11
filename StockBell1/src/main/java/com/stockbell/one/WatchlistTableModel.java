package com.stockbell.one;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public final class WatchlistTableModel extends AbstractTableModel {
    private static final String[] COLS = {
            "代號", "名稱", "現價", "漲跌", "漲跌%", "今高", "今低", "高門檻", "低門檻", "狀態"
    };
    private final List<WatchItem> items = new ArrayList<>();

    public List<WatchItem> items() { return items; }

    public WatchItem get(int row) {
        return items.get(row);
    }

    public int indexOfSymbol(String symbol) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).symbol().equalsIgnoreCase(symbol)) return i;
        }
        return -1;
    }

    public void add(WatchItem item) {
        items.add(item);
        fireTableRowsInserted(items.size() - 1, items.size() - 1);
    }

    public void remove(int row) {
        if (row < 0 || row >= items.size()) return;
        items.remove(row);
        fireTableRowsDeleted(row, row);
    }

    public void updateQuote(int row, Quote q) {
        if (row < 0 || row >= items.size()) return;
        items.get(row).setLastQuote(q);
        if (q.name() != null && !q.name().isBlank()
                && !q.name().equalsIgnoreCase(items.get(row).symbol())
                && items.get(row).name().equals(items.get(row).symbol())) {
            items.get(row).setName(q.name());
        }
        fireTableRowsUpdated(row, row);
    }

    public void updateThresholds(int row, double high, double low) {
        if (row < 0 || row >= items.size()) return;
        WatchItem it = items.get(row);
        it.setHighThreshold(high);
        it.setLowThreshold(low);
        it.setLastDirection(null);
        fireTableRowsUpdated(row, row);
    }

    @Override public int getRowCount() { return items.size(); }
    @Override public int getColumnCount() { return COLS.length; }
    @Override public String getColumnName(int c) { return COLS[c]; }
    @Override public boolean isCellEditable(int r, int c) { return false; }

    @Override
    public Object getValueAt(int row, int col) {
        WatchItem it = items.get(row);
        Quote q = it.lastQuote();
        return switch (col) {
            case 0 -> it.symbol();
            case 1 -> it.name();
            case 2 -> q == null ? "-" : String.format("%.2f", q.price());
            case 3 -> q == null ? "-" : String.format("%+.2f", q.change());
            case 4 -> q == null ? "-" : String.format("%+.2f%%", q.changePercent());
            case 5 -> q == null ? "-" : String.format("%.2f", q.dayHigh());
            case 6 -> q == null ? "-" : String.format("%.2f", q.dayLow());
            case 7 -> String.format("%.2f", it.highThreshold());
            case 8 -> String.format("%.2f", it.lowThreshold());
            case 9 -> it.lastDirection() == null ? "正常"
                    : (it.lastDirection() == WatchItem.Direction.HIGH ? "▲ 突破高點" : "▼ 跌破低點");
            default -> "";
        };
    }
}
