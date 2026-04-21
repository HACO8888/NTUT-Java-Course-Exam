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

    /**
     * 3-player deal: all 52 cards dealt, player with Club 3 gets 18 cards, others get 17.
     */
    public List<List<Card>> dealToPlayers3() {
        shuffle();
        List<List<Card>> hands = new ArrayList<>();
        for (int i = 0; i < 3; i++) hands.add(new ArrayList<>());
        // Deal 51 cards evenly (17 each)
        for (int i = 0; i < 51; i++) {
            hands.get(i % 3).add(cards.get(i));
        }
        // Find who has Club 3
        Card lastCard = cards.get(51);
        int clubThreeOwner = -1;
        for (int i = 0; i < 3; i++) {
            for (Card c : hands.get(i)) {
                if (c.getRank() == Rank.THREE && c.getSuit() == Suit.CLUBS) {
                    clubThreeOwner = i;
                    break;
                }
            }
            if (clubThreeOwner >= 0) break;
        }
        // The last card might itself be Club 3
        if (clubThreeOwner < 0 && lastCard.getRank() == Rank.THREE && lastCard.getSuit() == Suit.CLUBS) {
            // Give to player 0 (they'll have Club 3)
            hands.get(0).add(lastCard);
        } else if (clubThreeOwner >= 0) {
            hands.get(clubThreeOwner).add(lastCard);
        } else {
            hands.get(0).add(lastCard);
        }
        for (List<Card> hand : hands) Collections.sort(hand);
        return hands;
    }
}
