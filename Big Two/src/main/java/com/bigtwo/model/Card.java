package com.bigtwo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 撲克牌類別
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card implements Comparable<Card> {
    private Suit suit;
    private Rank rank;

    /**
     * 取得卡牌的完整名稱
     */
    public String getName() {
        return suit.getName() + rank.getDisplay();
    }

    /**
     * 取得卡牌的符號表示
     */
    public String getSymbol() {
        return suit.getSymbol() + rank.getDisplay();
    }

    /**
     * 檢查是否是梅花3（遊戲開始必須出的牌）
     */
    public boolean isClubThree() {
        return suit == Suit.CLUBS && rank == Rank.THREE;
    }

    /**
     * 檢查是否是2（最大點數）
     */
    public boolean isTwo() {
        return rank == Rank.TWO;
    }

    /**
     * 比較兩張牌的大小
     * 先比點數，再比花色
     */
    @Override
    public int compareTo(Card other) {
        // 先比點數
        if (this.rank != other.rank) {
            return Integer.compare(this.rank.getBigTwoRank(), other.rank.getBigTwoRank());
        }
        // 點數相同則比花色
        return Integer.compare(this.suit.getRank(), other.suit.getRank());
    }

    /**
     * 判斷是否比另一張牌大
     */
    public boolean isHigherThan(Card other) {
        return this.compareTo(other) > 0;
    }

    /**
     * 判斷是否與另一張牌同點數
     */
    public boolean isSameRank(Card other) {
        return this.rank == other.rank;
    }

    /**
     * 判斷是否與另一張牌同花色
     */
    public boolean isSameSuit(Card other) {
        return this.suit == other.suit;
    }

    @Override
    public String toString() {
        return getSymbol();
    }
}
