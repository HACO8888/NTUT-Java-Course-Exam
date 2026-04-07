package com.bigtwo.model;

public enum HandType {
    SINGLE(1),
    PAIR(2),
    STRAIGHT(5),
    FLUSH(5),
    FULL_HOUSE(5),
    FOUR_OF_A_KIND(5),
    STRAIGHT_FLUSH(5);

    private final int cardCount;

    HandType(int cardCount) {
        this.cardCount = cardCount;
    }

    public int getCardCount() { return cardCount; }
}
