package com.bigtwo.service;

import com.bigtwo.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 遊戲服務
 */
@Service
public class GameService {

    @Autowired
    private AIPlayerService aiService;

    private final Map<String, BigTwoGame> games = new ConcurrentHashMap<>();
    private final Map<String, ScheduledExecutorService> aiExecutors = new ConcurrentHashMap<>();

    /**
     * 建立新遊戲
     */
    public BigTwoGame createGame(String gameId, String playerName) {
        BigTwoGame game = new BigTwoGame();
        game.initPlayers(playerName);
        games.put(gameId, game);
        return game;
    }

    /**
     * 取得遊戲
     */
    public BigTwoGame getGame(String gameId) {
        return games.get(gameId);
    }

    /**
     * 開始遊戲
     */
    public GameState startGame(String gameId) {
        BigTwoGame game = games.get(gameId);
        if (game == null) return null;

        game.start();
        
        // 啟動 AI 排程器
        startAIExecutor(gameId);
        
        return GameState.fromGame(game, 0);
    }

    /**
     * 取得遊戲狀態
     */
    public GameState getGameState(String gameId, int playerId) {
        BigTwoGame game = games.get(gameId);
        if (game == null) return null;
        return GameState.fromGame(game, playerId);
    }

    /**
     * 玩家出牌
     */
    public GameState playCards(String gameId, int playerId, List<Card> cards) {
        BigTwoGame game = games.get(gameId);
        if (game == null) return null;

        boolean success = game.playCards(playerId, cards);
        if (!success) {
            GameState state = GameState.fromGame(game, playerId);
            state.setMessage("出牌失敗：" + game.getLastMessage());
            return state;
        }

        // 如果遊戲結束，停止 AI
        if (game.isFinished()) {
            stopAIExecutor(gameId);
        }

        return GameState.fromGame(game, playerId);
    }

    /**
     * 玩家 Pass
     */
    public GameState pass(String gameId, int playerId) {
        BigTwoGame game = games.get(gameId);
        if (game == null) return null;

        game.pass(playerId, playerId);
        return GameState.fromGame(game, playerId);
    }

    /**
     * 重新開始
     */
    public GameState restart(String gameId) {
        BigTwoGame game = games.get(gameId);
        if (game == null) return null;

        game.restart();
        startAIExecutor(gameId);
        return GameState.fromGame(game, 0);
    }

    /**
     * 啟動 AI 排程器
     */
    private void startAIExecutor(String gameId) {
        // 停止現有的
        stopAIExecutor(gameId);
        
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        aiExecutors.put(gameId, executor);
        
        // 每1秒檢查一次是否需要AI出牌
        executor.scheduleAtFixedRate(() -> {
            try {
                processAITurn(gameId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * 停止 AI 排程器
     */
    private void stopAIExecutor(String gameId) {
        ScheduledExecutorService executor = aiExecutors.remove(gameId);
        if (executor != null) {
            executor.shutdown();
        }
    }

    /**
     * 處理 AI 回合
     */
    private void processAITurn(String gameId) {
        BigTwoGame game = games.get(gameId);
        if (game == null || !game.isStarted() || game.isFinished()) {
            return;
        }

        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null || currentPlayer.isHuman() || currentPlayer.isFinished()) {
            return;
        }

        // AI 決定要出的牌
        List<Card> play = aiService.decidePlay(
                currentPlayer, 
                game.getLastPlay(), 
                game.isFreePlay(), 
                game.getRound()
        );

        if (play != null && !play.isEmpty()) {
            // AI 出牌
            game.playCards(currentPlayer.getId(), play);
        } else {
            // AI Pass
            game.pass(currentPlayer.getId(), currentPlayer.getId());
        }

        // 如果遊戲結束，停止 AI
        if (game.isFinished()) {
            stopAIExecutor(gameId);
        }
    }

    /**
     * 刪除遊戲
     */
    public void removeGame(String gameId) {
        stopAIExecutor(gameId);
        games.remove(gameId);
    }
}
