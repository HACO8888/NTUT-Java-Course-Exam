package com.bigtwo.service;

import com.bigtwo.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * AI 玩家服務
 */
@Service
public class AIPlayerService {

    /**
     * AI 決定要出的牌
     */
    public List<Card> decidePlay(Player player, Play lastPlay, boolean freePlay, int round) {
        List<Card> hand = new ArrayList<>(player.getHand());
        Collections.sort(hand);

        // 第一回合必須出梅花3
        if (round == 1 && freePlay) {
            return playWithClubThree(hand);
        }

        // 自由出牌，出最小的
        if (freePlay || lastPlay == null) {
            return playSmallest(hand);
        }

        // 根據上一手牌型決定
        List<Card> play = findPlayForType(hand, lastPlay);
        if (play != null) {
            return play;
        }

        // 無法出牌，回傳 null（會Pass）
        return null;
    }

    /**
     * 第一回合必須出包含梅花3的牌
     */
    private List<Card> playWithClubThree(List<Card> hand) {
        Card clubThree = hand.stream()
                .filter(Card::isClubThree)
                .findFirst()
                .orElse(null);

        if (clubThree == null) return null;

        // 優先只出梅花3（單張）
        return Collections.singletonList(clubThree);
    }

    /**
     * 出最小的牌
     */
    private List<Card> playSmallest(List<Card> hand) {
        if (hand.isEmpty()) return null;
        
        // 優先出單張最小的牌
        return Collections.singletonList(hand.get(0));
    }

    /**
     * 根據牌型找出可以出的牌
     */
    private List<Card> findPlayForType(List<Card> hand, Play lastPlay) {
        HandType type = lastPlay.getHandType();
        Card representative = lastPlay.getRepresentativeCard();

        switch (type) {
            case SINGLE:
                return findHigherSingle(hand, representative);
            case PAIR:
                return findHigherPair(hand, representative);
            case STRAIGHT:
                return findHigherStraight(hand, representative);
            case FLUSH:
                return findHigherFlush(hand, representative);
            case FULL_HOUSE:
                return findHigherFullHouse(hand, representative);
            case FOUR_OF_KIND:
                return findHigherFourOfAKind(hand, representative);
            case STRAIGHT_FLUSH:
                return findHigherStraightFlush(hand, representative);
            default:
                return null;
        }
    }

    /**
     * 找出比指定牌大的單張
     */
    private List<Card> findHigherSingle(List<Card> hand, Card target) {
        for (Card card : hand) {
            if (card.isHigherThan(target)) {
                return Collections.singletonList(card);
            }
        }
        return null;
    }

    /**
     * 找出比指定對子大的對子
     */
    private List<Card> findHigherPair(List<Card> hand, Card target) {
        List<List<Card>> pairs = findAllPairs(hand);
        for (List<Card> pair : pairs) {
            if (pair.get(0).getRank().isHigherThan(target.getRank())) {
                return pair;
            }
        }
        return null;
    }

    /**
     * 找出比指定順子大的順子
     */
    private List<Card> findHigherStraight(List<Card> hand, Card target) {
        List<List<Card>> straights = findAllStraights(hand);
        for (List<Card> straight : straights) {
            // 取得這個順子的最大牌
            Card maxCard = getStraightMaxCard(straight);
            if (maxCard.isHigherThan(target)) {
                return straight;
            }
        }
        return null;
    }

    /**
     * 找出比指定同花大的同花
     */
    private List<Card> findHigherFlush(List<Card> hand, Card target) {
        List<List<Card>> flushes = findAllFlushes(hand);
        for (List<Card> flush : flushes) {
            // 比最大牌
            Card maxCard = flush.get(flush.size() - 1);
            if (maxCard.isHigherThan(target)) {
                return flush;
            }
            // 點數相同比花色
            if (maxCard.getRank() == target.getRank() && 
                maxCard.getSuit().isHigherThan(target.getSuit())) {
                return flush;
            }
        }
        return null;
    }

    /**
     * 找出比指定葫蘆大的葫蘆
     */
    private List<Card> findHigherFullHouse(List<Card> hand, Card target) {
        List<List<Card>> fullHouses = findAllFullHouses(hand);
        for (List<Card> fullHouse : fullHouses) {
            Card tripleCard = getFullHouseTripleCard(fullHouse);
            if (tripleCard.getRank().isHigherThan(target.getRank())) {
                return fullHouse;
            }
        }
        return null;
    }

    /**
     * 找出比指定鐵支大的鐵支
     */
    private List<Card> findHigherFourOfAKind(List<Card> hand, Card target) {
        List<List<Card>> quads = findAllFourOfAKinds(hand);
        for (List<Card> quad : quads) {
            Card quadCard = getFourOfAKindCard(quad);
            if (quadCard.getRank().isHigherThan(target.getRank())) {
                return quad;
            }
        }
        return null;
    }

    /**
     * 找出比指定同花順大的同花順
     */
    private List<Card> findHigherStraightFlush(List<Card> hand, Card target) {
        List<List<Card>> straightFlushes = findAllStraightFlushes(hand);
        for (List<Card> straightFlush : straightFlushes) {
            Card maxCard = getStraightMaxCard(straightFlush);
            if (maxCard.isHigherThan(target)) {
                return straightFlush;
            }
        }
        return null;
    }

    // ============== 輔助方法 ==============

    private List<List<Card>> findAllPairs(List<Card> hand) {
        List<List<Card>> pairs = new ArrayList<>();
        for (int i = 0; i < hand.size() - 1; i++) {
            if (hand.get(i).getRank() == hand.get(i + 1).getRank()) {
                pairs.add(new ArrayList<>(List.of(hand.get(i), hand.get(i + 1))));
            }
        }
        return pairs;
    }

    private List<List<Card>> findAllStraights(List<Card> hand) {
        List<List<Card>> straights = new ArrayList<>();
        // 簡化實作：找出所有可能的5張順子
        for (int i = 0; i <= hand.size() - 5; i++) {
            List<Card> candidate = new ArrayList<>(hand.subList(i, i + 5));
            if (isValidStraight(candidate)) {
                straights.add(candidate);
            }
        }
        return straights;
    }

    private boolean isValidStraight(List<Card> cards) {
        if (cards.size() != 5) return false;
        // 檢查是否為連續5個點數
        for (int i = 1; i < 5; i++) {
            Rank prev = cards.get(i - 1).getRank();
            Rank curr = cards.get(i).getRank();
            if (curr.ordinal() != prev.ordinal() + 1) {
                // 特殊情況：A2345
                if (i == 3 && prev == Rank.FIVE && curr == Rank.ACE) {
                    // 繼續檢查最後一張是否是2
                    return cards.get(4).getRank() == Rank.TWO;
                }
                return false;
            }
        }
        return true;
    }

    private Card getStraightMaxCard(List<Card> straight) {
        // A2345 以 5 為最大
        if (straight.get(0).getRank() == Rank.THREE && 
            straight.get(4).getRank() == Rank.TWO) {
            return straight.get(2); // 5
        }
        return straight.get(straight.size() - 1);
    }

    private List<List<Card>> findAllFlushes(List<Card> hand) {
        List<List<Card>> flushes = new ArrayList<>();
        // 按花色分組
        for (Suit suit : Suit.values()) {
            List<Card> suited = hand.stream()
                    .filter(c -> c.getSuit() == suit)
                    .toList();
            if (suited.size() >= 5) {
                // 找所有5張組合
                flushes.addAll(generateCombinations(suited, 5));
            }
        }
        return flushes;
    }

    private List<List<Card>> findAllFullHouses(List<Card> hand) {
        List<List<Card>> fullHouses = new ArrayList<>();
        // 找出所有三條和對子
        List<List<Card>> triples = findAllTriples(hand);
        List<List<Card>> pairs = findAllPairs(hand);
        
        for (List<Card> triple : triples) {
            for (List<Card> pair : pairs) {
                if (triple.get(0).getRank() != pair.get(0).getRank()) {
                    List<Card> fullHouse = new ArrayList<>(triple);
                    fullHouse.addAll(pair);
                    fullHouses.add(fullHouse);
                }
            }
        }
        return fullHouses;
    }

    private List<List<Card>> findAllTriples(List<Card> hand) {
        List<List<Card>> triples = new ArrayList<>();
        for (int i = 0; i <= hand.size() - 3; i++) {
            if (hand.get(i).getRank() == hand.get(i + 1).getRank() &&
                hand.get(i).getRank() == hand.get(i + 2).getRank()) {
                triples.add(new ArrayList<>(List.of(
                    hand.get(i), hand.get(i + 1), hand.get(i + 2))));
            }
        }
        return triples;
    }

    private Card getFullHouseTripleCard(List<Card> fullHouse) {
        // 三條會是前3張或後3張
        if (fullHouse.get(0).getRank() == fullHouse.get(2).getRank()) {
            return fullHouse.get(0);
        }
        return fullHouse.get(2);
    }

    private List<List<Card>> findAllFourOfAKinds(List<Card> hand) {
        List<List<Card>> quads = new ArrayList<>();
        for (int i = 0; i <= hand.size() - 4; i++) {
            if (hand.get(i).getRank() == hand.get(i + 1).getRank() &&
                hand.get(i).getRank() == hand.get(i + 2).getRank() &&
                hand.get(i).getRank() == hand.get(i + 3).getRank()) {
                // 鐵支需要5張牌，加一張最小的單張
                for (Card kicker : hand) {
                    if (kicker.getRank() != hand.get(i).getRank()) {
                        List<Card> quad = new ArrayList<>(List.of(
                            hand.get(i), hand.get(i + 1), 
                            hand.get(i + 2), hand.get(i + 3), kicker));
                        quads.add(quad);
                        break;
                    }
                }
            }
        }
        return quads;
    }

    private Card getFourOfAKindCard(List<Card> quad) {
        return quad.get(0);
    }

    private List<List<Card>> findAllStraightFlushes(List<Card> hand) {
        List<List<Card>> straightFlushes = new ArrayList<>();
        // 先找所有同花，再檢查是否為順子
        for (Suit suit : Suit.values()) {
            List<Card> suited = hand.stream()
                    .filter(c -> c.getSuit() == suit)
                    .sorted()
                    .toList();
            if (suited.size() >= 5) {
                for (int i = 0; i <= suited.size() - 5; i++) {
                    List<Card> candidate = new ArrayList<>(suited.subList(i, i + 5));
                    if (isValidStraight(candidate)) {
                        straightFlushes.add(candidate);
                    }
                }
            }
        }
        return straightFlushes;
    }

    private List<List<Card>> generateCombinations(List<Card> cards, int k) {
        List<List<Card>> combinations = new ArrayList<>();
        generateCombinationsHelper(cards, k, 0, new ArrayList<>(), combinations);
        return combinations;
    }

    private void generateCombinationsHelper(List<Card> cards, int k, int start, 
                                           List<Card> current, List<List<Card>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i < cards.size(); i++) {
            current.add(cards.get(i));
            generateCombinationsHelper(cards, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
}
