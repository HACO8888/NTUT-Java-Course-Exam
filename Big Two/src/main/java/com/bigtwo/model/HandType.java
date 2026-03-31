package com.bigtwo.model;

import lombok.Getter;

/**
 * 出牌類型
 */
@Getter
public enum HandType {
    SINGLE("單張", 1),
    PAIR("對子", 2),
    STRAIGHT("順子", 5),
    FLUSH("同花", 5),
    FULL_HOUSE("葫蘆", 5),
    FOUR_OF_KIND("鐵支", 5),
    STRAIGHT_FLUSH("同花順", 5),
    INVALID("無效", 0);

    private final String name;
    private final int cardCount;

    HandType(String name, int cardCount) {
        this.name = name;
        this.cardCount = cardCount;
    }

    /**
     * 判斷是否比另一個牌型大
     * 牌型大小：同花順 > 鐵支 > 葫蘆 > 同花 > 順子
     */
    public boolean isHigherThan(HandType other) {
        return this.ordinal() > other.ordinal();
    }
}
