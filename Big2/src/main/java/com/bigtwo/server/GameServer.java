package com.bigtwo.server;

import com.bigtwo.model.Card;
import com.bigtwo.net.JsonUtil;
import com.sun.net.httpserver.HttpServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer extends WebSocketServer {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<WebSocket, ClientSession> sessions = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public GameServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        sessions.put(conn, new ClientSession(conn));
        System.out.println("Client connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        ClientSession session = sessions.remove(conn);
        if (session != null && session.getRoom() != null) {
            Room room = session.getRoom();
            room.removePlayer(session);
            if (room.isEmpty()) {
                rooms.remove(room.getRoomCode());
                room.shutdown();
            }
        }
        System.out.println("Client disconnected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        ClientSession session = sessions.get(conn);
        if (session == null) return;

        try {
            Map<String, Object> msg = JsonUtil.parseJson(message);
            String type = JsonUtil.getString(msg, "type");
            if (type == null) return;

            switch (type) {
                case "CREATE_ROOM": handleCreateRoom(session, msg); break;
                case "JOIN_ROOM":   handleJoinRoom(session, msg); break;
                case "LEAVE_ROOM":  handleLeaveRoom(session); break;
                case "START_GAME":  handleStartGame(session); break;
                case "ADD_AI":      handleAddAI(session, msg); break;
                case "REMOVE_AI":   handleRemoveAI(session, msg); break;
                case "PLAY_CARDS":  handlePlayCards(session, msg); break;
                case "PASS":        handlePass(session); break;
            }
        } catch (Exception e) {
            sendError(session, "伺服器錯誤：" + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Big Two WebSocket Server started on port " + getPort());
    }

    // ── Handlers ─────────────────────────────────────────────────────

    private void handleCreateRoom(ClientSession session, Map<String, Object> msg) {
        String name = JsonUtil.getString(msg, "playerName");
        if (name == null || name.trim().isEmpty()) {
            sendError(session, "請輸入玩家名稱！");
            return;
        }
        session.setPlayerName(name.trim());

        String code = generateRoomCode();
        Room room = new Room(code, session);
        rooms.put(code, room);
        int seat = room.addPlayer(session);

        Map<String, Object> reply = JsonUtil.msg("ROOM_CREATED");
        reply.put("roomCode", code);
        reply.put("seatIndex", seat);
        session.send(JsonUtil.toJson(reply));
    }

    private void handleJoinRoom(ClientSession session, Map<String, Object> msg) {
        String code = JsonUtil.getString(msg, "roomCode");
        String name = JsonUtil.getString(msg, "playerName");
        if (code == null || name == null) {
            sendError(session, "缺少房間代碼或玩家名稱！");
            return;
        }
        code = code.trim().toUpperCase();
        session.setPlayerName(name.trim());

        Room room = rooms.get(code);
        if (room == null) {
            sendError(session, "找不到房間 " + code + "！");
            return;
        }
        if (room.getState() != Room.State.WAITING) {
            sendError(session, "遊戲已經開始了！");
            return;
        }
        if (room.getPlayerCount() >= 4) {
            sendError(session, "房間已滿！");
            return;
        }

        int seat = room.addPlayer(session);
        if (seat < 0) {
            sendError(session, "無法加入房間！");
            return;
        }

        Map<String, Object> reply = JsonUtil.msg("ROOM_JOINED");
        reply.put("roomCode", code);
        reply.put("seatIndex", seat);
        reply.put("players", room.getPlayerList());
        session.send(JsonUtil.toJson(reply));
    }

    private void handleLeaveRoom(ClientSession session) {
        Room room = session.getRoom();
        if (room != null) {
            room.removePlayer(session);
            if (room.isEmpty()) {
                rooms.remove(room.getRoomCode());
                room.shutdown();
            }
        }
    }

    private void handleStartGame(ClientSession session) {
        Room room = session.getRoom();
        if (room == null) { sendError(session, "你不在任何房間中！"); return; }
        String err = room.startGame(session);
        if (err != null) sendError(session, err);
    }

    private void handleAddAI(ClientSession session, Map<String, Object> msg) {
        Room room = session.getRoom();
        if (room == null || room.getCreator() != session) {
            sendError(session, "只有房主可以加入 AI！");
            return;
        }
        int slot = JsonUtil.getInt(msg, "slotIndex");
        if (!room.addAI(slot)) sendError(session, "無法在此位置加入 AI！");
    }

    private void handleRemoveAI(ClientSession session, Map<String, Object> msg) {
        Room room = session.getRoom();
        if (room == null || room.getCreator() != session) {
            sendError(session, "只有房主可以移除 AI！");
            return;
        }
        int slot = JsonUtil.getInt(msg, "slotIndex");
        if (!room.removeAI(slot)) sendError(session, "無法移除此位置的 AI！");
    }

    @SuppressWarnings("unchecked")
    private void handlePlayCards(ClientSession session, Map<String, Object> msg) {
        Room room = session.getRoom();
        if (room == null) { sendError(session, "你不在任何房間中！"); return; }
        List<Object> cardList = JsonUtil.getList(msg, "cards");
        List<Card> cards = JsonUtil.parseCardList(cardList);
        String err = room.handlePlay(session, cards);
        if (err != null) {
            Map<String, Object> reply = JsonUtil.msg("PLAY_ERROR");
            reply.put("message", err);
            session.send(JsonUtil.toJson(reply));
        }
    }

    private void handlePass(ClientSession session) {
        Room room = session.getRoom();
        if (room == null) { sendError(session, "你不在任何房間中！"); return; }
        String err = room.handlePass(session);
        if (err != null) {
            Map<String, Object> reply = JsonUtil.msg("PLAY_ERROR");
            reply.put("message", err);
            session.send(JsonUtil.toJson(reply));
        }
    }

    private void sendError(ClientSession session, String message) {
        Map<String, Object> msg = JsonUtil.msg("ERROR");
        msg.put("message", message);
        session.send(JsonUtil.toJson(msg));
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        String code;
        do {
            StringBuilder sb = new StringBuilder(4);
            for (int i = 0; i < 4; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
            code = sb.toString();
        } while (rooms.containsKey(code));
        return code;
    }

    // ── Static file server for web client ────────────────────────────

    private static int wsPortForConfig;

    public static void startHttpServer(int httpPort, int wsPort) {
        wsPortForConfig = wsPort;
        try {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);

            httpServer.createContext("/api/config", exchange -> {
                String json = "{\"wsPort\":" + wsPortForConfig + "}";
                byte[] data = json.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, data.length);
                exchange.getResponseBody().write(data);
                exchange.getResponseBody().close();
            });

            httpServer.createContext("/api/download", exchange -> {
                try {
                    java.io.File jarFile = new java.io.File(
                            GameServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                    if (!jarFile.isFile()) {
                        String msg = "Desktop version not available in dev mode";
                        exchange.sendResponseHeaders(404, msg.length());
                        exchange.getResponseBody().write(msg.getBytes());
                        exchange.getResponseBody().close();
                        return;
                    }
                    byte[] data = java.nio.file.Files.readAllBytes(jarFile.toPath());
                    exchange.getResponseHeaders().set("Content-Type", "application/java-archive");
                    exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"big2.jar\"");
                    exchange.sendResponseHeaders(200, data.length);
                    exchange.getResponseBody().write(data);
                    exchange.getResponseBody().close();
                } catch (Exception ex) {
                    String err = "Download failed";
                    exchange.sendResponseHeaders(500, err.length());
                    exchange.getResponseBody().write(err.getBytes());
                    exchange.getResponseBody().close();
                }
            });

            httpServer.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/")) path = "/index.html";

                InputStream is = GameServer.class.getResourceAsStream("/web" + path);
                if (is == null) {
                    String notFound = "404 Not Found";
                    exchange.sendResponseHeaders(404, notFound.length());
                    exchange.getResponseBody().write(notFound.getBytes());
                    exchange.getResponseBody().close();
                    return;
                }

                String contentType = "text/html";
                if (path.endsWith(".css")) contentType = "text/css";
                else if (path.endsWith(".js")) contentType = "application/javascript";
                else if (path.endsWith(".png")) contentType = "image/png";
                else if (path.endsWith(".jpg")) contentType = "image/jpeg";

                byte[] data = is.readAllBytes();
                is.close();
                exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
                exchange.sendResponseHeaders(200, data.length);
                exchange.getResponseBody().write(data);
                exchange.getResponseBody().close();
            });
            httpServer.start();
            System.out.println("HTTP Server started on port " + httpPort + " (Web client: http://localhost:" + httpPort + ")");
        } catch (IOException e) {
            System.err.println("Failed to start HTTP server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Read from env vars first (for cloud deployments like Zeabur)
        int wsPort = parseEnvPort("PORT", 5555);
        int httpPort = parseEnvPort("HTTP_PORT", 5556);

        if (args.length > 0) {
            try { wsPort = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }
        if (args.length > 1) {
            try { httpPort = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
        }

        GameServer server = new GameServer(wsPort);
        server.start();
        startHttpServer(httpPort, wsPort);
    }

    private static int parseEnvPort(String envName, int defaultPort) {
        String val = System.getenv(envName);
        if (val != null && !val.isEmpty()) {
            try { return Integer.parseInt(val.trim()); } catch (NumberFormatException ignored) {}
        }
        return defaultPort;
    }
}
