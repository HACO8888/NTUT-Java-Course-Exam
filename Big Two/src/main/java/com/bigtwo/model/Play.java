package com.bigtwo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一次出牌的動作
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Play {
    private int playerId;
    private String playerName;
    private List<Card> cards;
    private HandType handType;

    /**
     * 分析牌型
     */
    public static HandType analyzeHandType(List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            return HandType.INVALID;
        }

        int count = cards.size();
        Collections.sort(cards);

        switch (count) {
            case 1:
                return HandType.SINGLE;
            case 2:
                return isPair(cards) ? HandType.PAIR : HandType.INVALID;
            case 5:
                if (isStraightFlush(cards)) return HandType.STRAIGHT_FLUSH;
                if (isFourOfAKind(cards)) return HandType.FOUR_OF_KIND;
                if (isFullHouse(cards)) return HandType.FULL_HOUSE;
                if (isFlush(cards)) return HandType.FLUSH;
                if (isStraight(cards)) return HandType.STRAIGHT;
                return HandType.INVALID;
            default:
                return HandType.INVALID;
        }
    }

    /**
     * 檢查是否為對子
     */
    private static boolean isPair(List<Card> cards) {
        return cards.get(0).getRank() == cards.get(1).getRank();
    }

    /**
     * 檢查是否為順子
     */
    private static boolean isStraight(List<Card> cards) {
        // 特殊情況：A2345 是最小的順子
        boolean isSpecialStraight = cards.get(0).getRank() == Rank.THREE &&
                cards.get(1).getRank() == Rank.FOUR &&
                cards.get(2).getRank() == Rank.FIVE &&
                cards.get(3).getRank() == Rank.ACE &&
                cards.get(4).getRank() == Rank.TWO;

        if (isSpecialStraight) return true;

        // 一般順子檢查
        for (int i = 1; i < cards.size(); i++) {
            Rank prevRank = cards.get(i - 1).getRank();
            Rank currRank = cards.get(i).getRank();
            if (currRank.ordinal() != prevRank.ordinal() + 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * 檢查是否為同花
     */
    private static boolean isFlush(List<Card> cards) {
        Suit suit = cards.get(0).getSuit();
        return cards.stream().allMatch(c -> c.getSuit() == suit);
    }

    /**
     * 檢查是否為葫蘆（3+2）
     */
    private static boolean isFullHouse(List<Card> cards) {
        // 排序後可能是 AAABB 或 AABBB
        Rank rank1 = cards.get(0).getRank();
        Rank rank2 = cards.get(4).getRank();

        boolean case1 = cards.get(0).getRank() == rank1 &&
                cards.get(1).getRank() == rank1 &&
                cards.get(2).getRank() == rank1 &&
                cards.get(3).getRank() == rank2 &&
                cards.get(4).getRank() == rank2;

        boolean case2 = cards.get(0).getRank() == rank1 &&
                cards.get(1).getRank() == rank1 &&
                cards.get(2).getRank() == rank2 &&
                cards.get(3).getRank() == rank2 &&
                cards.get(4).getRank() == rank2;

        return case1 || case2;
    }

    /**
     * 檢查是否為鐵支（4+1）
     */
    private static boolean isFourOfAKind(List<Card> cards) {
        Rank rank1 = cards.get(0).getRank();
        Rank rank4 = cards.get(3).getRank();

        // AAAAB
        boolean case1 = cards.get(0).getRank() == rank1 &&
                cards.get(1).getRank() == rank1 &&
                cards.get(2).getRank() == rank1 &&
                cards.get(3).getRank() == rank1;

        // ABBBB
        boolean case2 = cards.get(1).getRank() == rank4 &&
                cards.get(2).getRank() == rank4 &&
                cards.get(3).getRank() == rank4 &&
                cards.get(4).getRank() == rank4;

        return case1 || case2;
    }

    /**
     * 檢查是否為同花順
     */
    private static boolean isStraightFlush(List<Card> cards) {
        return isFlush(cards) && isStraight(cards);
    }

    /**
     * 取得牌型的代表性卡牌（用於比較大小）
     */
    public Card getRepresentativeCard() {
        if (cards == null || cards.isEmpty()) return null;
        Collections.sort(cards);

        switch (handType) {
            case SINGLE:
                return cards.get(0);
            case PAIR:
                return cards.get(0);
            case STRAIGHT:
            case STRAIGHT_FLUSH:
                // 順子以最大牌為代表，但 A2345 以 5 為代表
                if (isSpecialStraight()) {
                    return cards.get(2); // 5
                }
                return cards.get(cards.size() - 1);
            case FLUSH:
                // 同花以最大牌為代表
                return cards.get(cards.size() - 1);
            case FULL_HOUSE:
                // 葫蘆以三條的牌為代表
                return getFullHouseTriple();
            case FOUR_OF_KIND:
                // 鐵支以四條的牌為代表
                return getFourOfAKindQuad();
            default:
                return cards.get(cards.size() - 1);
        }
    }

    /**
     * 檢查是否為 A2345 特殊順子
     */
    private boolean isSpecialStraight() {
        return cards.get(0).getRank() == Rank.THREE &&
                cards.get(4).getRank() == Rank.TWO;
    }

    /**
     * 取得葫蘆的三條牌
     */
    private Card getFullHouseTriple() {
        if (cards.get(0).getRank() == cards.get(2).getRank()) {
            return cards.get(0);
        }
        return cards.get(2);
    }

    /**
     * 取得鐵支的四條牌
     */
    private Card getFourOfAKindQuad() {
        if (cards.get(0).getRank() == cards.get(3).getRank()) {
            return cards.get(0);
        }
        return cards.get(1);
    }

    /**
     * 判斷這手牌是否比另一手牌大
     */
    public boolean isHigherThan(Play other) {
        // 不同牌型不能比較（除非是Pass）
        if (other == null || other.getHandType() == null) {
            return true;
        }

        // 相同牌型比代表牌
        if (this.handType == other.handType) {
            Card thisCard = this.getRepresentativeCard();
            Card otherCard = other.getRepresentativeCard();

            if (thisCard.getRank() != otherCard.getRank()) {
                return thisCard.getRank().isHigherThan(otherCard.getRank());
            }
            // 點數相同比花色
            return thisCard.getSuit().isHigherThan(otherCard.getSuit());
        }

        // 不同牌型，以牌型大小決定
        return this.handType.isHigherThan(other.handType);
    }

    /**
     * 建立一個 Pass 動作
     */
    public static Play pass(int playerId, String playerName) {
        Play pass = new Play();
        pass.setPlayerId(playerId);
        pass.setPlayerName(playerName);
        pass.setCards(new ArrayList<>());
        pass.setHandType(null);
        return pass;
    }

    public boolean isPass() {
        return cards == null || cards.isEmpty();
    }
}
