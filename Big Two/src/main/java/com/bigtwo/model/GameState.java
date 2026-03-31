package com.bigtwo.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 遊戲狀態類別（用於傳輸給前端）
 */
@Data
@NoArgsConstructor
public class GameState {
    private boolean started;
    private boolean finished;
    private int currentPlayerId;
    private String currentPlayerName;
    private List<PlayerInfo> players;
    private Play lastPlay;
    private int passCount;
    private int round;
    private String message;
    private boolean canPlay;
    private boolean canPass;
    private List<Card> myCards;
    private boolean isMyTurn;

    @Data
    @NoArgsConstructor
    public static class PlayerInfo {
        private int id;
        private String name;
        private int cardCount;
        private boolean isHuman;
        private boolean isCurrent;
        private int finishRank;
        private boolean finished;
    }

    /**
     * 從遊戲引擎建立遊戲狀態
     */
    public static GameState fromGame(BigTwoGame game, int playerId) {
        GameState state = new GameState();
        state.started = game.isStarted();
        state.finished = game.isFinished();
        state.currentPlayerId = game.getCurrentPlayerId();
        state.currentPlayerName = game.getCurrentPlayer() != null ? 
                game.getCurrentPlayer().getName() : "";
        state.lastPlay = game.getLastPlay();
        state.passCount = game.getPassCount();
        state.round = game.getRound();
        state.message = game.getLastMessage();

        // 玩家資訊
        state.players = new ArrayList<>();
        for (Player player : game.getPlayers()) {
            PlayerInfo info = new PlayerInfo();
            info.id = player.getId();
            info.name = player.getName();
            info.cardCount = player.getCardCount();
            info.isHuman = player.isHuman();
            info.isCurrent = player.getId() == game.getCurrentPlayerId();
            info.finishRank = player.getFinishRank();
            info.finished = player.isFinished();
            state.players.add(info);
        }

        // 當前玩家資訊
        Player currentPlayer = game.getPlayer(playerId);
        if (currentPlayer != null) {
            state.myCards = currentPlayer.getHand();
            state.isMyTurn = game.getCurrentPlayerId() == playerId;
            state.canPlay = state.isMyTurn && !currentPlayer.isFinished();
            state.canPass = state.canPlay && game.canPass();
        }

        return state;
    }
}
