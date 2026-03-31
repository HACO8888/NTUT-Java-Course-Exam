package com.bigtwo.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 大老二遊戲引擎
 */
@Getter
public class BigTwoGame {
    public static final int NUM_PLAYERS = 4;
    public static final int CARDS_PER_PLAYER = 13;

    private List<Player> players;
    private Deck deck;
    private boolean started;
    private boolean finished;
    private int currentPlayerIndex;
    private Play lastPlay;
    private int passCount;
    private int round;
    private String lastMessage;
    private int finishCounter;
    private boolean freePlay; // 是否為自由出牌（新回合）

    public BigTwoGame() {
        this.players = new ArrayList<>();
        this.deck = new Deck();
        this.started = false;
        this.finished = false;
        this.currentPlayerIndex = 0;
        this.passCount = 0;
        this.round = 1;
        this.finishCounter = 0;
        this.freePlay = true;
    }

    /**
     * 初始化玩家
     */
    public void initPlayers(String humanPlayerName) {
        players.clear();
        players.add(new Player(0, humanPlayerName, true));
        players.add(new Player(1, "電腦 1", false));
        players.add(new Player(2, "電腦 2", false));
        players.add(new Player(3, "電腦 3", false));
    }

    /**
     * 開始遊戲
     */
    public void start() {
        if (started) return;

        // 重置牌組
        deck.initialize();
        deck.shuffle();

        // 發牌
        List<List<Card>> hands = deck.deal(NUM_PLAYERS, CARDS_PER_PLAYER);
        for (int i = 0; i < NUM_PLAYERS; i++) {
            players.get(i).setHand(hands.get(i));
        }

        // 找出持有梅花3的玩家開始
        currentPlayerIndex = findClubThreeHolder();
        started = true;
        finished = false;
        lastPlay = null;
        passCount = 0;
        round = 1;
        finishCounter = 0;
        freePlay = true;

        lastMessage = players.get(currentPlayerIndex).getName() + " 持有梅花3，由他开始出牌";
    }

    /**
     * 找出持有梅花3的玩家
     */
    private int findClubThreeHolder() {
        for (int i = 0; i < NUM_PLAYERS; i++) {
            if (players.get(i).hasClubThree()) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 出牌
     */
    public boolean playCards(int playerId, List<Card> cards) {
        if (!started || finished) return false;
        if (getCurrentPlayerId() != playerId) return false;

        Player player = getCurrentPlayer();
        if (player.isFinished()) return false;

        // 檢查玩家是否有這些牌
        if (!player.hasCards(cards)) {
            lastMessage = "你沒有選擇的牌！";
            return false;
        }

        // 分析牌型
        HandType handType = Play.analyzeHandType(cards);
        if (handType == HandType.INVALID) {
            lastMessage = "無效的牌型！";
            return false;
        }

        // 如果是第一回合，必須包含梅花3
        if (round == 1 && freePlay) {
            boolean hasClubThree = cards.stream().anyMatch(Card::isClubThree);
            if (!hasClubThree) {
                lastMessage = "第一回合必須包含梅花3！";
                return false;
            }
        }

        // 如果不是自由出牌，必須符合規則
        Play currentPlay = new Play(playerId, player.getName(), cards, handType);
        if (!freePlay) {
            // 必須與上一手同牌型且更大
            if (handType != lastPlay.getHandType()) {
                lastMessage = "必須出相同牌型（" + lastPlay.getHandType().getName() + "）！";
                return false;
            }

            if (!currentPlay.isHigherThan(lastPlay)) {
                lastMessage = "必須出比上一手大的牌！";
                return false;
            }
        }

        // 出牌成功
        player.playCards(cards);
        lastPlay = currentPlay;
        lastMessage = player.getName() + " 出了 " + handType.getName() + ": " + formatCards(cards);

        // 檢查是否出完
        if (player.isEmpty()) {
            finishCounter++;
            player.setFinished(finishCounter);
            lastMessage += " （" + player.getName() + " 出完牌了！第" + finishCounter + "名）";

            // 檢查遊戲是否結束（只剩一個玩家）
            if (finishCounter >= NUM_PLAYERS - 1) {
                finished = true;
                // 標記最後一名
                for (Player p : players) {
                    if (!p.isFinished()) {
                        p.setFinished(NUM_PLAYERS);
                        break;
                    }
                }
            }
        }

        // 重置Pass計數
        passCount = 0;
        freePlay = false;

        // 下一個玩家
        nextPlayer();

        return true;
    }

    /**
     * Pass（不出牌）
     */
    public boolean pass(int playerId) {
        if (!started || finished) return false;
        if (getCurrentPlayerId() != playerId) return false;

        // 第一回合不能Pass
        if (round == 1 && freePlay) {
            lastMessage = "第一回合不能跳過！";
            return false;
        }

        // 如果上一手是自己出的，不能Pass
        if (lastPlay != null && lastPlay.getPlayerId() == playerId) {
            lastMessage = "上一手是你出的，不能跳過！";
            return false;
        }

        Player player = getCurrentPlayer();
        passCount++;
        lastMessage = player.getName() + " 跳過";

        // 如果三個人都Pass，新一輪開始
        if (passCount >= getActivePlayerCount() - 1) {
            round++;
            passCount = 0;
            freePlay = true;
            lastPlay = null;
            lastMessage += "，新的一輪開始！";
        }

        nextPlayer();
        return true;
    }

    /**
     * 取得活躍玩家數量（還沒出完牌的）
     */
    private int getActivePlayerCount() {
        return (int) players.stream().filter(p -> !p.isFinished()).count();
    }

    /**
     * 移動到下一個活躍玩家
     */
    private void nextPlayer() {
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % NUM_PLAYERS;
        } while (players.get(currentPlayerIndex).isFinished() && !finished);
    }

    /**
     * 取得當前玩家ID
     */
    public int getCurrentPlayerId() {
        return players.get(currentPlayerIndex).getId();
    }

    /**
     * 取得當前玩家
     */
    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    /**
     * 取得指定ID的玩家
     */
    public Player getPlayer(int id) {
        return players.stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElse(null);
    }

    /**
     * 檢查是否可以Pass
     */
    public boolean canPass() {
        if (round == 1 && freePlay) return false;
        if (lastPlay == null) return false;
        if (lastPlay.getPlayerId() == getCurrentPlayerId()) return false;
        return true;
    }

    /**
     * 格式化卡牌顯示
     */
    private String formatCards(List<Card> cards) {
        StringBuilder sb = new StringBuilder();
        for (Card card : cards) {
            sb.append(card.getSymbol()).append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * 重新開始遊戲
     */
    public void restart() {
        started = false;
        finished = false;
        start();
    }
}
