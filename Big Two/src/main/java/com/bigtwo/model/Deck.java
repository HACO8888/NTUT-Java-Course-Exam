package com.bigtwo.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 牌組類別（一副52張撲克牌）
 */
public class Deck {
    private List<Card> cards;

    public Deck() {
        initialize();
    }

    /**
     * 初始化牌組
     */
    public void initialize() {
        cards = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
    }

    /**
     * 洗牌
     */
    public void shuffle() {
        Collections.shuffle(cards);
    }

    /**
     * 抽一張牌
     */
    public Card draw() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.remove(cards.size() - 1);
    }

    /**
     * 發牌給玩家
     */
    public List<List<Card>> deal(int numPlayers, int cardsPerPlayer) {
        List<List<Card>> hands = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            hands.add(new ArrayList<>());
        }

        for (int i = 0; i < cardsPerPlayer; i++) {
            for (int j = 0; j < numPlayers; j++) {
                Card card = draw();
                if (card != null) {
                    hands.get(j).add(card);
                }
            }
        }

        return hands;
    }

    /**
     * 取得剩餘牌數
     */
    public int size() {
        return cards.size();
    }

    /**
     * 檢查是否還有牌
     */
    public boolean isEmpty() {
        return cards.isEmpty();
    }
}
