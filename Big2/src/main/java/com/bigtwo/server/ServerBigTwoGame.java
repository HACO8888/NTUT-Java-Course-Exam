package com.bigtwo.server;

import com.bigtwo.model.*;

import java.util.*;

public class ServerBigTwoGame {
    private final int playerCount;
    private final List<String> playerNames;
    private final List<List<Card>> hands;
    private int currentSeat;
    private Play lastPlay;
    private int lastPlaySeat;
    private boolean firstTurn;
    private boolean gameOver;
    private int winnerSeat = -1;

    public ServerBigTwoGame(int playerCount, List<String> playerNames) {
        this.playerCount = playerCount;
        this.playerNames = new ArrayList<>(playerNames);

        Deck deck = new Deck();
        if (playerCount == 3) {
            hands = deck.dealToPlayers3();
        } else {
            hands = deck.dealToPlayers(4);
        }

        currentSeat = findClubThreeOwner();
        lastPlay = null;
        lastPlaySeat = currentSeat;
        firstTurn = true;
        gameOver = false;
    }

    private int findClubThreeOwner() {
        for (int i = 0; i < playerCount; i++) {
            for (Card c : hands.get(i)) {
                if (c.getRank() == Rank.THREE && c.getSuit() == Suit.CLUBS) {
                    return i;
                }
            }
        }
        return 0;
    }

    public int getCurrentSeat() { return currentSeat; }
    public Play getLastPlay() { return lastPlay; }
    public boolean isFirstTurn() { return firstTurn; }
    public boolean isGameOver() { return gameOver; }
    public int getWinnerSeat() { return winnerSeat; }
    public List<Card> getHand(int seat) { return Collections.unmodifiableList(hands.get(seat)); }
    public int getHandSize(int seat) { return hands.get(seat).size(); }
    public int getPlayerCount() { return playerCount; }

    public boolean canPass() {
        return lastPlay != null && lastPlaySeat != currentSeat;
    }

    public boolean mustIncludeClubThree() {
        return firstTurn;
    }

    public String submitPlay(int seat, Play play) {
        if (seat != currentSeat) return "不是你的回合！";

        if (play.isPass()) {
            if (!canPass()) return "你必須出牌，不能 Pass！";
            advanceTurn();
            return null;
        }

        if (!play.isValid()) return "無效的牌型！";

        // Verify player actually has these cards
        List<Card> hand = hands.get(seat);
        for (Card c : play.getCards()) {
            if (!hand.contains(c)) return "你沒有這些牌！";
        }

        if (firstTurn) {
            boolean hasClubThree = false;
            for (Card c : play.getCards()) {
                if (c.getRank() == Rank.THREE && c.getSuit() == Suit.CLUBS) {
                    hasClubThree = true;
                    break;
                }
            }
            if (!hasClubThree) return "第一手必須包含梅花3！";
            firstTurn = false;
        }

        if (lastPlay != null && lastPlaySeat != currentSeat) {
            if (!play.beats(lastPlay)) return "出的牌必須大於上一手！";
        }

        hand.removeAll(play.getCards());
        lastPlay = play;
        lastPlaySeat = currentSeat;

        if (hand.isEmpty()) {
            gameOver = true;
            winnerSeat = currentSeat;
            return null;
        }

        advanceTurn();
        return null;
    }

    private void advanceTurn() {
        int start = currentSeat;
        do {
            currentSeat = (currentSeat + 1) % playerCount;
        } while (hands.get(currentSeat).isEmpty() && currentSeat != start);

        if (currentSeat == lastPlaySeat && lastPlay != null) {
            lastPlay = null;
        }
    }
}
