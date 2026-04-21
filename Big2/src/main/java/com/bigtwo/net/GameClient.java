package com.bigtwo.net;

import com.bigtwo.model.Card;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.swing.*;
import java.net.URI;
import java.util.*;

public class GameClient extends WebSocketClient {

    private MessageListener listener;

    public GameClient(URI serverUri) {
        super(serverUri);
    }

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("Connected to server");
    }

    @Override
    public void onMessage(String message) {
        if (listener == null) return;
        SwingUtilities.invokeLater(() -> dispatch(message));
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Disconnected: " + reason);
        if (listener != null) {
            SwingUtilities.invokeLater(() -> listener.onError("與伺服器斷線！"));
        }
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
        if (listener != null) {
            SwingUtilities.invokeLater(() -> listener.onError("連線錯誤：" + ex.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private void dispatch(String message) {
        Map<String, Object> msg = JsonUtil.parseJson(message);
        String type = JsonUtil.getString(msg, "type");
        if (type == null) return;

        switch (type) {
            case "ROOM_CREATED":
                listener.onRoomCreated(
                    JsonUtil.getString(msg, "roomCode"),
                    JsonUtil.getInt(msg, "seatIndex"));
                break;
            case "ROOM_JOINED":
                listener.onRoomJoined(
                    JsonUtil.getString(msg, "roomCode"),
                    JsonUtil.getInt(msg, "seatIndex"),
                    (List<Map<String, Object>>) (List<?>) JsonUtil.getList(msg, "players"));
                break;
            case "PLAYER_JOINED":
                listener.onPlayerJoined(
                    JsonUtil.getString(msg, "playerName"),
                    JsonUtil.getInt(msg, "seatIndex"));
                break;
            case "PLAYER_LEFT":
                listener.onPlayerLeft(JsonUtil.getInt(msg, "seatIndex"));
                break;
            case "OWNER_CHANGED":
                listener.onOwnerChanged(
                    JsonUtil.getInt(msg, "newOwnerSeat"),
                    JsonUtil.getString(msg, "newOwnerName"));
                break;
            case "AI_ADDED":
                listener.onAIAdded(
                    JsonUtil.getInt(msg, "seatIndex"),
                    JsonUtil.getString(msg, "name"));
                break;
            case "AI_REMOVED":
                listener.onAIRemoved(JsonUtil.getInt(msg, "seatIndex"));
                break;
            case "GAME_STARTED":
                listener.onGameStarted(msg);
                break;
            case "PLAY_RESULT":
                listener.onPlayResult(msg);
                break;
            case "PASS_RESULT":
                listener.onPassResult(msg);
                break;
            case "YOUR_TURN":
                listener.onYourTurn(
                    JsonUtil.getBool(msg, "canPass"),
                    JsonUtil.getBool(msg, "mustIncludeClubThree"));
                break;
            case "PLAY_ERROR":
                listener.onPlayError(JsonUtil.getString(msg, "message"));
                break;
            case "GAME_OVER":
                listener.onGameOver(
                    JsonUtil.getString(msg, "winnerName"),
                    JsonUtil.getInt(msg, "winnerSeat"));
                break;
            case "PLAYER_DISCONNECTED":
                listener.onPlayerDisconnected(
                    JsonUtil.getInt(msg, "seatIndex"),
                    JsonUtil.getBool(msg, "replacedByAI"),
                    JsonUtil.getString(msg, "aiName"));
                break;
            case "ERROR":
                listener.onError(JsonUtil.getString(msg, "message"));
                break;
        }
    }

    // ── Convenience send methods ─────────────────────────────────────

    public void createRoom(String playerName) {
        Map<String, Object> msg = JsonUtil.msg("CREATE_ROOM");
        msg.put("playerName", playerName);
        send(JsonUtil.toJson(msg));
    }

    public void joinRoom(String roomCode, String playerName) {
        Map<String, Object> msg = JsonUtil.msg("JOIN_ROOM");
        msg.put("roomCode", roomCode);
        msg.put("playerName", playerName);
        send(JsonUtil.toJson(msg));
    }

    public void leaveRoom() {
        send(JsonUtil.toJson(JsonUtil.msg("LEAVE_ROOM")));
    }

    public void startGame() {
        send(JsonUtil.toJson(JsonUtil.msg("START_GAME")));
    }

    public void addAI(int slotIndex) {
        Map<String, Object> msg = JsonUtil.msg("ADD_AI");
        msg.put("slotIndex", slotIndex);
        send(JsonUtil.toJson(msg));
    }

    public void removeAI(int slotIndex) {
        Map<String, Object> msg = JsonUtil.msg("REMOVE_AI");
        msg.put("slotIndex", slotIndex);
        send(JsonUtil.toJson(msg));
    }

    public void playCards(List<Card> cards) {
        Map<String, Object> msg = JsonUtil.msg("PLAY_CARDS");
        msg.put("cards", JsonUtil.cardsToList(cards));
        send(JsonUtil.toJson(msg));
    }

    public void pass() {
        send(JsonUtil.toJson(JsonUtil.msg("PASS")));
    }
}
