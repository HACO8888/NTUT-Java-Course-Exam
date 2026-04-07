package com.bigtwo.ai;

import com.bigtwo.model.*;
import java.util.*;

public class AIPlayer {

    /**
     * Decide what play to make. Returns a Play (possibly a pass).
     */
    public static Play decide(Player player, Play lastPlay, boolean mustIncludeClubThree) {
        List<Card> hand = new ArrayList<>(player.getHand());

        if (mustIncludeClubThree || lastPlay == null) {
            // Find Club Three
            Card clubThree = null;
            for (Card c : hand) {
                if (c.getRank() == Rank.THREE && c.getSuit() == Suit.CLUBS) {
                    clubThree = c;
                    break;
                }
            }
            if (clubThree != null) {
                // Try to play with Club Three; find smallest valid play containing it
                Play single = new Play(Collections.singletonList(clubThree));
                if (single.isValid()) return single;
            }
        }

        if (lastPlay == null) {
            // Free to play anything — play smallest single
            return new Play(Collections.singletonList(hand.get(0)));
        }

        int size = lastPlay.getCards().size();

        // Try to beat with same card count
        List<Play> candidates = generatePlays(hand, size);
        Play best = null;
        for (Play p : candidates) {
            if (p.beats(lastPlay)) {
                if (best == null || p.getKeyCard().compareTo(best.getKeyCard()) < 0) {
                    best = p;
                }
            }
        }

        if (best != null) return best;

        // Pass
        return new Play(Collections.emptyList());
    }

    private static List<Play> generatePlays(List<Card> hand, int size) {
        List<Play> result = new ArrayList<>();
        if (size == 1) {
            for (Card c : hand) result.add(new Play(Collections.singletonList(c)));
        } else if (size == 2) {
            generatePairs(hand, result);
        } else if (size == 5) {
            generate5CardPlays(hand, result);
        }
        return result;
    }

    private static void generatePairs(List<Card> hand, List<Play> result) {
        for (int i = 0; i < hand.size(); i++) {
            for (int j = i + 1; j < hand.size(); j++) {
                List<Card> pair = Arrays.asList(hand.get(i), hand.get(j));
                Play p = new Play(pair);
                if (p.isValid()) result.add(p);
            }
        }
    }

    private static void generate5CardPlays(List<Card> hand, List<Play> result) {
        // Combinations of 5 from hand
        int n = hand.size();
        for (int i = 0; i < n - 4; i++)
            for (int j = i + 1; j < n - 3; j++)
                for (int k = j + 1; k < n - 2; k++)
                    for (int l = k + 1; l < n - 1; l++)
                        for (int m = l + 1; m < n; m++) {
                            List<Card> combo = Arrays.asList(
                                hand.get(i), hand.get(j), hand.get(k),
                                hand.get(l), hand.get(m));
                            Play p = new Play(combo);
                            if (p.isValid()) result.add(p);
                        }
    }
}
