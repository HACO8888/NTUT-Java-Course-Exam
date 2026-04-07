package com.bigtwo.game;

import com.bigtwo.model.*;
import java.util.*;

public class BigTwoGame {
    private final List<Player> players = new ArrayList<>();
    private int currentPlayerIndex;
    private Play lastPlay;
    private int lastPlayerIndex; // who made the last non-pass play
    private boolean gameOver;
    private Player winner;
    private final List<String> log = new ArrayList<>();

    public BigTwoGame() {
        players.add(new Player("你", true));
        players.add(new Player("電腦 A", false));
        players.add(new Player("電腦 B", false));
        players.add(new Player("電腦 C", false));
    }

    public void startNewGame() {
        Deck deck = new Deck();
        List<List<Card>> hands = deck.dealToPlayers(4);
        for (int i = 0; i < 4; i++) {
            players.get(i).setHand(hands.get(i));
        }

        // Find who has 梅3 (Club of Three) - they go first
        currentPlayerIndex = 0;
        for (int i = 0; i < players.size(); i++) {
            for (Card c : players.get(i).getHand()) {
                if (c.getRank() == Rank.THREE && c.getSuit() == Suit.CLUBS) {
                    currentPlayerIndex = i;
                }
            }
        }

        lastPlay = null;
        lastPlayerIndex = currentPlayerIndex;
        gameOver = false;
        winner = null;
        log.clear();
        log.add("遊戲開始！" + players.get(currentPlayerIndex).getName() + " 持有梅3，先手出牌。");
    }

    public boolean isFirstTurn() {
        return lastPlay == null;
    }

    public boolean mustIncludeClubThree() {
        return lastPlay == null;
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public Play getLastPlay() {
        return lastPlay;
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public Player getWinner() {
        return winner;
    }

    public List<String> getLog() {
        return Collections.unmodifiableList(log);
    }

    /**
     * Attempt a play for the current player. Returns error message or null if OK.
     */
    public String submitPlay(Play play) {
        Player player = getCurrentPlayer();

        if (play.isPass()) {
            // Cannot pass if you're the one who made the last play (you control the round)
            if (lastPlay == null || lastPlayerIndex == currentPlayerIndex) {
                return "你必須出牌，不能 Pass！";
            }
            log.add(player.getName() + " Pass");
            advanceTurn();
            return null;
        }

        if (!play.isValid()) {
            return "無效的牌型！";
        }

        // First turn must include Club Three
        if (lastPlay == null) {
            boolean hasClubThree = false;
            for (Card c : play.getCards()) {
                if (c.getRank() == Rank.THREE && c.getSuit() == Suit.CLUBS) {
                    hasClubThree = true;
                    break;
                }
            }
            if (!hasClubThree) {
                return "第一手必須包含梅花3！";
            }
        }

        // Must beat last play (if there is one and it's not your own)
        if (lastPlay != null && lastPlayerIndex != currentPlayerIndex) {
            if (!play.beats(lastPlay)) {
                return "出的牌必須大於上一手！";
            }
        }

        // Remove cards from player hand
        player.removeCards(play.getCards());
        log.add(player.getName() + " 出牌: " + play);
        lastPlay = play;
        lastPlayerIndex = currentPlayerIndex;

        if (player.hasWon()) {
            gameOver = true;
            winner = player;
            log.add("🎉 " + player.getName() + " 獲勝！");
            return null;
        }

        advanceTurn();
        return null;
    }

    private void advanceTurn() {
        int start = currentPlayerIndex;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        } while (players.get(currentPlayerIndex).hasWon() && currentPlayerIndex != start);

        // If we've looped back to the last player who played, reset the table
        if (currentPlayerIndex == lastPlayerIndex && lastPlay != null) {
            log.add("--- 新的一輪，桌面清空 ---");
            lastPlay = null;
        }
    }

    /**
     * Returns true if the current player can pass.
     */
    public boolean canPass() {
        return lastPlay != null && lastPlayerIndex != currentPlayerIndex;
    }
}
