package com.bigtwo.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    private final List<Card> cards = new ArrayList<>();

    public Deck() {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(rank, suit));
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public List<Card> deal(int numPlayers) {
        // Returns all cards; caller distributes
        return new ArrayList<>(cards);
    }

    public List<List<Card>> dealToPlayers(int numPlayers) {
        shuffle();
        List<List<Card>> hands = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) hands.add(new ArrayList<>());
        for (int i = 0; i < cards.size(); i++) {
            hands.get(i % numPlayers).add(cards.get(i));
        }
        for (List<Card> hand : hands) Collections.sort(hand);
        return hands;
    }
}
