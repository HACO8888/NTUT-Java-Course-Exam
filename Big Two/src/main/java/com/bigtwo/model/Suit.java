package com.bigtwo.model;

import lombok.Getter;

/**
 * 撲克牌花色
 * 大老二的花色順序：黑桃 > 紅心 > 方塊 > 梅花
 */
@Getter
public enum Suit {
    CLUBS("梅花", "♣", 1),
    DIAMONDS("方塊", "♦", 2),
    HEARTS("紅心", "♥", 3),
    SPADES("黑桃", "♠", 4);

    private final String name;
    private final String symbol;
    private final int rank;

    Suit(String name, String symbol, int rank) {
        this.name = name;
        this.symbol = symbol;
        this.rank = rank;
    }

    /**
     * 判斷是否比另一個花色大
     */
    public boolean isHigherThan(Suit other) {
        return this.rank > other.rank;
    }
}
