package com.bigtwo.model;

import java.util.*;

public class Play {
    private final List<Card> cards;
    private final HandType handType;

    public Play(List<Card> cards) {
        this.cards = new ArrayList<>(cards);
        Collections.sort(this.cards);
        this.handType = determineHandType();
    }

    public List<Card> getCards() { return Collections.unmodifiableList(cards); }
    public HandType getHandType() { return handType; }
    public boolean isPass() { return cards.isEmpty(); }

    private HandType determineHandType() {
        int n = cards.size();
        if (n == 1) return HandType.SINGLE;
        if (n == 2) return isPair() ? HandType.PAIR : null;
        if (n == 5) {
            boolean flush = isFlush();
            boolean straight = isStraight();
            if (flush && straight) return HandType.STRAIGHT_FLUSH;
            if (isFourOfAKind()) return HandType.FOUR_OF_A_KIND;
            if (isFullHouse()) return HandType.FULL_HOUSE;
            if (flush) return HandType.FLUSH;
            if (straight) return HandType.STRAIGHT;
        }
        return null;
    }

    public boolean isValid() {
        return handType != null;
    }

    private boolean isPair() {
        return cards.size() == 2 && cards.get(0).getRank() == cards.get(1).getRank();
    }

    private boolean isStraight() {
        if (cards.size() != 5) return false;
        List<Integer> vals = new ArrayList<>();
        for (Card c : cards) vals.add(c.getRank().getValue());
        Collections.sort(vals);
        // Check for A-2-3-4-5 (special straight: ranks 11,12,0,1,2 -> sorted 0,1,2,11,12)
        if (vals.equals(Arrays.asList(0, 1, 2, 11, 12))) return true;
        for (int i = 1; i < vals.size(); i++) {
            if (vals.get(i) != vals.get(i - 1) + 1) return false;
        }
        return true;
    }

    private boolean isFlush() {
        if (cards.size() != 5) return false;
        Suit s = cards.get(0).getSuit();
        for (Card c : cards) {
            if (c.getSuit() != s) return false;
        }
        return true;
    }

    private boolean isFullHouse() {
        if (cards.size() != 5) return false;
        Map<Rank, Integer> freq = getFrequencies();
        return freq.containsValue(3) && freq.containsValue(2);
    }

    private boolean isFourOfAKind() {
        if (cards.size() != 5) return false;
        return getFrequencies().containsValue(4);
    }

    private Map<Rank, Integer> getFrequencies() {
        Map<Rank, Integer> map = new EnumMap<>(Rank.class);
        for (Card c : cards) map.merge(c.getRank(), 1, Integer::sum);
        return map;
    }

    /**
     * Get the key card that determines ranking for 5-card hands.
     */
    public Card getKeyCard() {
        switch (handType) {
            case SINGLE:
            case PAIR:
                return cards.get(cards.size() - 1);
            case STRAIGHT:
            case STRAIGHT_FLUSH: {
                // Highest card in the straight; handle A-2-3-4-5 edge case
                List<Integer> vals = new ArrayList<>();
                for (Card c : cards) vals.add(c.getRank().getValue());
                Collections.sort(vals);
                if (vals.equals(Arrays.asList(0, 1, 2, 11, 12))) {
                    // Highest is 5 (index 2 = value 2 = FIVE)
                    for (Card c : cards) if (c.getRank() == Rank.FIVE) return c;
                }
                return cards.get(cards.size() - 1);
            }
            case FLUSH:
                return cards.get(cards.size() - 1);
            case FULL_HOUSE: {
                // Key is the triplet rank's highest card
                Map<Rank, Integer> freq = getFrequencies();
                Rank tripletRank = null;
                for (Map.Entry<Rank, Integer> e : freq.entrySet()) {
                    if (e.getValue() == 3) tripletRank = e.getKey();
                }
                Card key = null;
                for (Card c : cards) {
                    if (c.getRank() == tripletRank) {
                        if (key == null || c.compareTo(key) > 0) key = c;
                    }
                }
                return key;
            }
            case FOUR_OF_A_KIND: {
                Map<Rank, Integer> freq = getFrequencies();
                Rank quadRank = null;
                for (Map.Entry<Rank, Integer> e : freq.entrySet()) {
                    if (e.getValue() == 4) quadRank = e.getKey();
                }
                Card key = null;
                for (Card c : cards) {
                    if (c.getRank() == quadRank) {
                        if (key == null || c.compareTo(key) > 0) key = c;
                    }
                }
                return key;
            }
            default:
                return cards.get(cards.size() - 1);
        }
    }

    /**
     * Five-card hand rank order: STRAIGHT < FLUSH < FULL_HOUSE < FOUR_OF_A_KIND < STRAIGHT_FLUSH
     */
    private int fiveCardRank() {
        switch (handType) {
            case STRAIGHT:      return 0;
            case FLUSH:         return 1;
            case FULL_HOUSE:    return 2;
            case FOUR_OF_A_KIND:return 3;
            case STRAIGHT_FLUSH:return 4;
            default:            return -1;
        }
    }

    /**
     * Returns true if this play beats the other play.
     */
    public boolean beats(Play other) {
        if (other == null || other.isPass()) return true;
        if (this.isPass()) return false;
        if (this.cards.size() != other.cards.size()) return false;
        if (this.handType == null || other.handType == null) return false;

        if (cards.size() == 5) {
            int myRank = fiveCardRank();
            int theirRank = other.fiveCardRank();
            if (myRank != theirRank) return myRank > theirRank;
            // Same type: compare key cards
            return this.getKeyCard().compareTo(other.getKeyCard()) > 0;
        }

        // Single or Pair: compare key cards
        return this.getKeyCard().compareTo(other.getKeyCard()) > 0;
    }

    @Override
    public String toString() {
        if (isPass()) return "Pass";
        return cards.toString() + " [" + handType + "]";
    }
}
