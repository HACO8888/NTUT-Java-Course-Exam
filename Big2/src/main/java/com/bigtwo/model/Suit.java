package com.bigtwo.model;

public enum Suit {
    CLUBS(0, "梅"),
    DIAMONDS(1, "方"),
    HEARTS(2, "紅"),
    SPADES(3, "黑");

    private final int value;
    private final String symbol;

    Suit(int value, String symbol) {
        this.value = value;
        this.symbol = symbol;
    }

    public int getValue() { return value; }
    public String getSymbol() { return symbol; }
}
