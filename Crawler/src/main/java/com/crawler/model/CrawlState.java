package com.crawler.model;

public enum CrawlState {
    IDLE("閒置"),
    RUNNING("執行中"),
    PAUSED("已暫停"),
    STOPPED("已停止"),
    COMPLETED("已完成");

    private final String label;

    CrawlState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
