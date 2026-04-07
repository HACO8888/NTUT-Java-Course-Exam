package com.bigtwo.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Player {
    private final String name;
    private final boolean isHuman;
    private final List<Card> hand = new ArrayList<>();

    public Player(String name, boolean isHuman) {
        this.name = name;
        this.isHuman = isHuman;
    }

    public String getName() { return name; }
    public boolean isHuman() { return isHuman; }
    public List<Card> getHand() { return hand; }
    public int getHandSize() { return hand.size(); }

    public void setHand(List<Card> cards) {
        hand.clear();
        hand.addAll(cards);
        Collections.sort(hand);
    }

    public void removeCards(List<Card> cards) {
        hand.removeAll(cards);
    }

    public boolean hasCard(Card card) {
        return hand.contains(card);
    }

    public boolean hasWon() {
        return hand.isEmpty();
    }
}
