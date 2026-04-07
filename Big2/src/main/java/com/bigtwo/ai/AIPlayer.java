package com.bigtwo.ai;

import com.bigtwo.model.*;
import java.util.*;

/**
 * AI strategy:
 * - When free to lead: prefer playing pairs/5-card combos to clear the hand faster;
 *   avoid leading with 2s unless hand is small.
 * - When following: play the weakest valid beat to conserve strong cards.
 * - Near winning (≤4 cards): be more aggressive, play any valid beat.
 * - Avoid wasting high-value 5-card hands on low-ranked 5-card plays.
 */
public class AIPlayer {

    private static final Random RNG = new Random();

    /** Returns the play decision (may be a pass). */
    public static Play decide(Player player, Play lastPlay, boolean mustIncludeClubThree) {
        List<Card> hand = new ArrayList<>(player.getHand());

        // ── Opening move (must include 梅3) ──────────────────────────
        if (mustIncludeClubThree) {
            Card clubThree = findClubThree(hand);
            if (clubThree != null) {
                // Try pair with another 3 first
                for (Card c : hand) {
                    if (c != clubThree && c.getRank() == Rank.THREE) {
                        Play pair = new Play(Arrays.asList(clubThree, c));
                        if (pair.isValid()) return pair;
                    }
                }
                return new Play(Collections.singletonList(clubThree));
            }
        }

        // ── Free to lead ─────────────────────────────────────────────
        if (lastPlay == null) {
            return chooseLead(hand);
        }

        // ── Follow: must beat lastPlay ────────────────────────────────
        int size = lastPlay.getCards().size();
        boolean nearWin = hand.size() <= 5;

        List<Play> candidates = generatePlays(hand, size);
        List<Play> winners = new ArrayList<>();
        for (Play p : candidates) {
            if (p.beats(lastPlay)) winners.add(p);
        }

        if (winners.isEmpty()) return pass();

        // Sort by key card ascending (weakest first)
        winners.sort(Comparator.comparingInt(p -> p.getKeyCard().getValue()));

        // Near win: just play strongest to seal the deal
        if (nearWin) return winners.get(winners.size() - 1);

        // Otherwise play weakest valid beat, but avoid burning 2s on small plays
        for (Play p : winners) {
            if (!hasOnlyTwos(p) || hand.size() <= 3) return p;
        }

        return winners.get(0);
    }

    // ── Lead selection ────────────────────────────────────────────────
    private static Play chooseLead(List<Card> hand) {
        // 1. Try to lead with a pair (clears 2 cards efficiently)
        List<Play> pairs = new ArrayList<>();
        generatePairs(hand, pairs);
        // Filter out pairs of Twos unless few cards left
        List<Play> safePairs = new ArrayList<>();
        for (Play p : pairs) {
            if (p.getKeyCard().getRank() != Rank.TWO || hand.size() <= 4) safePairs.add(p);
        }
        if (!safePairs.isEmpty()) {
            safePairs.sort(Comparator.comparingInt(p -> p.getKeyCard().getValue()));
            return safePairs.get(0); // weakest pair
        }

        // 2. Try a 5-card combo (straight/flush/full house etc.)
        if (hand.size() >= 5) {
            List<Play> combos = new ArrayList<>();
            generate5CardPlays(hand, combos);
            if (!combos.isEmpty()) {
                // Prefer lower-ranked combo types; sort by type rank then key card
                combos.sort(Comparator
                    .comparingInt((Play p) -> fiveCardTypeRank(p.getHandType()))
                    .thenComparingInt(p -> p.getKeyCard().getValue()));
                Play combo = combos.get(0);
                // Only lead with 5-card if it's not a straight flush / four-of-a-kind (save those)
                if (combo.getHandType() == HandType.STRAIGHT || combo.getHandType() == HandType.FLUSH
                        || combo.getHandType() == HandType.FULL_HOUSE) {
                    return combo;
                }
            }
        }

        // 3. Single — avoid leading Twos unless no choice
        List<Card> singles = new ArrayList<>(hand);
        singles.sort(Comparator.comparingInt(Card::getValue));
        for (Card c : singles) {
            if (c.getRank() != Rank.TWO || hand.size() <= 2) {
                return new Play(Collections.singletonList(c));
            }
        }
        return new Play(Collections.singletonList(singles.get(0)));
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private static Card findClubThree(List<Card> hand) {
        for (Card c : hand) {
            if (c.getRank() == Rank.THREE && c.getSuit() == Suit.CLUBS) return c;
        }
        return null;
    }

    private static boolean hasOnlyTwos(Play p) {
        for (Card c : p.getCards()) if (c.getRank() != Rank.TWO) return false;
        return true;
    }

    private static int fiveCardTypeRank(HandType t) {
        switch (t) {
            case STRAIGHT:       return 0;
            case FLUSH:          return 1;
            case FULL_HOUSE:     return 2;
            case FOUR_OF_A_KIND: return 3;
            case STRAIGHT_FLUSH: return 4;
            default:             return 5;
        }
    }

    private static Play pass() {
        return new Play(Collections.emptyList());
    }

    // ── Play generation ───────────────────────────────────────────────
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
                Play p = new Play(Arrays.asList(hand.get(i), hand.get(j)));
                if (p.isValid()) result.add(p);
            }
        }
    }

    private static void generate5CardPlays(List<Card> hand, List<Play> result) {
        int n = hand.size();
        for (int i = 0; i < n - 4; i++)
            for (int j = i+1; j < n - 3; j++)
                for (int k = j+1; k < n - 2; k++)
                    for (int l = k+1; l < n - 1; l++)
                        for (int m = l+1; m < n; m++) {
                            Play p = new Play(Arrays.asList(
                                hand.get(i), hand.get(j), hand.get(k),
                                hand.get(l), hand.get(m)));
                            if (p.isValid()) result.add(p);
                        }
    }

    /** Random think time: 600–1600 ms */
    public static int thinkTime() {
        return 600 + RNG.nextInt(1000);
    }
}
