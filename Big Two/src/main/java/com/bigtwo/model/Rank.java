package com.bigtwo.model;

import lombok.Getter;

/**
 * 撲克牌點數
 * 大老二的點數順序：2 > A > K > Q > J > 10 > 9 > 8 > 7 > 6 > 5 > 4 > 3
 */
@Getter
public enum Rank {
    THREE("3", 3, 1),
    FOUR("4", 4, 2),
    FIVE("5", 5, 3),
    SIX("6", 6, 4),
    SEVEN("7", 7, 5),
    EIGHT("8", 8, 6),
    NINE("9", 9, 7),
    TEN("10", 10, 8),
    JACK("J", 11, 9),
    QUEEN("Q", 12, 10),
    KING("K", 13, 11),
    ACE("A", 14, 12),
    TWO("2", 15, 13);

    private final String display;
    private final int value;
    private final int bigTwoRank;

    Rank(String display, int value, int bigTwoRank) {
        this.display = display;
        this.value = value;
        this.bigTwoRank = bigTwoRank;
    }

    /**
     * 判斷是否比另一個點數大
     */
    public boolean isHigherThan(Rank other) {
        return this.bigTwoRank > other.bigTwoRank;
    }

    /**
     * 取得下個點數（用於順子檢查）
     */
    public Rank next() {
        int nextOrdinal = this.ordinal() + 1;
        if (nextOrdinal < Rank.values().length) {
            return Rank.values()[nextOrdinal];
        }
        return null;
    }
}
