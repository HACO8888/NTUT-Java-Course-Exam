package com.bigtwo.model;

public enum Rank {
    THREE(0, "3"),
    FOUR(1, "4"),
    FIVE(2, "5"),
    SIX(3, "6"),
    SEVEN(4, "7"),
    EIGHT(5, "8"),
    NINE(6, "9"),
    TEN(7, "10"),
    JACK(8, "J"),
    QUEEN(9, "Q"),
    KING(10, "K"),
    ACE(11, "A"),
    TWO(12, "2");

    private final int value;
    private final String symbol;

    Rank(int value, String symbol) {
        this.value = value;
        this.symbol = symbol;
    }

    public int getValue() { return value; }
    public String getSymbol() { return symbol; }
}
