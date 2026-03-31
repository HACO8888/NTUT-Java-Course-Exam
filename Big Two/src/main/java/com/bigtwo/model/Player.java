package com.bigtwo.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 玩家類別
 */
@Data
@NoArgsConstructor
public class Player {
    private int id;
    private String name;
    private List<Card> hand;
    private boolean isHuman;
    private int finishRank; // 0表示未結束，1表示第一名，以此類推

    public Player(int id, String name, boolean isHuman) {
        this.id = id;
        this.name = name;
        this.isHuman = isHuman;
        this.hand = new ArrayList<>();
        this.finishRank = 0;
    }

    /**
     * 設定手牌
     */
    public void setHand(List<Card> cards) {
        this.hand = new ArrayList<>(cards);
        sortHand();
    }

    /**
     * 整理手牌（依大小排序）
     */
    public void sortHand() {
        Collections.sort(hand);
    }

    /**
     * 取得手牌數量
     */
    public int getCardCount() {
        return hand.size();
    }

    /**
     * 出掉指定的牌
     */
    public boolean playCards(List<Card> cards) {
        return hand.removeAll(cards);
    }

    /**
     * 是否有指定的牌
     */
    public boolean hasCards(List<Card> cards) {
        return hand.containsAll(cards);
    }

    /**
     * 檢查是否手牌出完
     */
    public boolean isEmpty() {
        return hand.isEmpty();
    }

    /**
     * 檢查是否有梅花3
     */
    public boolean hasClubThree() {
        return hand.stream().anyMatch(Card::isClubThree);
    }

    /**
     * 取得最小的牌
     */
    public Card getSmallestCard() {
        if (hand.isEmpty()) return null;
        return hand.get(0);
    }

    /**
     * 設定完成名次
     */
    public void setFinished(int rank) {
        this.finishRank = rank;
    }

    /**
     * 檢查是否已經完成遊戲
     */
    public boolean isFinished() {
        return finishRank > 0;
    }

    @Override
    public String toString() {
        return name + " (" + hand.size() + "張)";
    }
}
