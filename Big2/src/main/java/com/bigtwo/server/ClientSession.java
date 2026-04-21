package com.bigtwo.server;

import org.java_websocket.WebSocket;

public class ClientSession {
    private final WebSocket conn;
    private String playerName;
    private int seatIndex = -1;
    private Room room;

    public ClientSession(WebSocket conn) {
        this.conn = conn;
    }

    public WebSocket getConn() { return conn; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String name) { this.playerName = name; }
    public int getSeatIndex() { return seatIndex; }
    public void setSeatIndex(int idx) { this.seatIndex = idx; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public void send(String json) {
        if (conn != null && conn.isOpen()) {
            conn.send(json);
        }
    }
}
