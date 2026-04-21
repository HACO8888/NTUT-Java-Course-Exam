package com.bigtwo.net;

import java.util.List;
import java.util.Map;

public interface MessageListener {
    void onRoomCreated(String roomCode, int seatIndex);
    void onRoomJoined(String roomCode, int seatIndex, List<Map<String, Object>> players);
    void onPlayerJoined(String playerName, int seatIndex);
    void onPlayerLeft(int seatIndex);
    void onOwnerChanged(int newOwnerSeat, String newOwnerName);
    void onAIAdded(int seatIndex, String name);
    void onAIRemoved(int seatIndex);
    void onGameStarted(Map<String, Object> data);
    void onPlayResult(Map<String, Object> data);
    void onPassResult(Map<String, Object> data);
    void onYourTurn(boolean canPass, boolean mustIncludeClubThree);
    void onPlayError(String message);
    void onGameOver(String winnerName, int winnerSeat);
    void onPlayerDisconnected(int seatIndex, boolean replacedByAI, String aiName);
    void onError(String message);
}
