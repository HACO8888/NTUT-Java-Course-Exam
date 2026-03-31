package com.bigtwo.controller;

import com.bigtwo.model.Card;
import com.bigtwo.model.GameState;
import com.bigtwo.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 遊戲控制器
 */
@Controller
public class GameController {

    @Autowired
    private GameService gameService;

    /**
     * 首頁
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * 建立新遊戲
     */
    @PostMapping("/api/game/create")
    @ResponseBody
    public ResponseEntity<?> createGame(@RequestBody Map<String, String> request) {
        String gameId = UUID.randomUUID().toString().substring(0, 8);
        String playerName = request.getOrDefault("playerName", "玩家");
        
        gameService.createGame(gameId, playerName);
        
        return ResponseEntity.ok(Map.of(
            "gameId", gameId,
            "message", "遊戲已建立"
        ));
    }

    /**
     * 開始遊戲
     */
    @PostMapping("/api/game/{gameId}/start")
    @ResponseBody
    public ResponseEntity<?> startGame(@PathVariable String gameId) {
        GameState state = gameService.startGame(gameId);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(state);
    }

    /**
     * 取得遊戲狀態
     */
    @GetMapping("/api/game/{gameId}/state")
    @ResponseBody
    public ResponseEntity<?> getGameState(@PathVariable String gameId, 
                                          @RequestParam(defaultValue = "0") int playerId) {
        GameState state = gameService.getGameState(gameId, playerId);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(state);
    }

    /**
     * 玩家出牌
     */
    @PostMapping("/api/game/{gameId}/play")
    @ResponseBody
    public ResponseEntity<?> playCards(@PathVariable String gameId, 
                                       @RequestBody PlayRequest request) {
        GameState state = gameService.playCards(gameId, request.getPlayerId(), request.getCards());
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(state);
    }

    /**
     * 玩家 Pass
     */
    @PostMapping("/api/game/{gameId}/pass")
    @ResponseBody
    public ResponseEntity<?> pass(@PathVariable String gameId, 
                                  @RequestBody Map<String, Integer> request) {
        int playerId = request.getOrDefault("playerId", 0);
        GameState state = gameService.pass(gameId, playerId);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(state);
    }

    /**
     * 重新開始
     */
    @PostMapping("/api/game/{gameId}/restart")
    @ResponseBody
    public ResponseEntity<?> restart(@PathVariable String gameId) {
        GameState state = gameService.restart(gameId);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(state);
    }

    /**
     * 出牌請求類別
     */
    public static class PlayRequest {
        private int playerId;
        private List<Card> cards;

        public int getPlayerId() {
            return playerId;
        }

        public void setPlayerId(int playerId) {
            this.playerId = playerId;
        }

        public List<Card> getCards() {
            return cards;
        }

        public void setCards(List<Card> cards) {
            this.cards = cards;
        }
    }
}
