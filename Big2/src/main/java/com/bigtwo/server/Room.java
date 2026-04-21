package com.bigtwo.server;

import com.bigtwo.ai.AIPlayer;
import com.bigtwo.model.*;
import com.bigtwo.net.JsonUtil;

import java.util.*;
import java.util.concurrent.*;

public class Room {
    public enum State { WAITING, PLAYING, FINISHED }

    private final String roomCode;
    private final ClientSession[] seats = new ClientSession[4];
    private final boolean[] isAI = new boolean[4];
    private final String[] names = new String[4];
    private State state = State.WAITING;
    private ServerBigTwoGame game;
    private ClientSession creator;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private int aiCounter = 0;

    public Room(String roomCode, ClientSession creator) {
        this.roomCode = roomCode;
        this.creator = creator;
    }

    public String getRoomCode() { return roomCode; }
    public State getState() { return state; }
    public ClientSession getCreator() { return creator; }

    public synchronized int getPlayerCount() {
        int count = 0;
        for (int i = 0; i < 4; i++) {
            if (seats[i] != null || isAI[i]) count++;
        }
        return count;
    }

    private int getAICount() {
        int c = 0;
        for (int i = 0; i < 4; i++) if (isAI[i]) c++;
        return c;
    }

    public synchronized int addPlayer(ClientSession session) {
        for (int i = 0; i < 4; i++) {
            if (seats[i] == null && !isAI[i]) {
                seats[i] = session;
                names[i] = session.getPlayerName();
                session.setSeatIndex(i);
                session.setRoom(this);
                broadcastPlayerJoined(session.getPlayerName(), i);
                return i;
            }
        }
        return -1;
    }

    public synchronized void removePlayer(ClientSession session) {
        int seat = session.getSeatIndex();
        if (seat >= 0 && seat < 4 && seats[seat] == session) {
            seats[seat] = null;
            names[seat] = null;
            session.setSeatIndex(-1);
            session.setRoom(null);

            if (state == State.PLAYING) {
                isAI[seat] = true;
                names[seat] = "電腦 " + (char)('A' + getAICount() - 1);
                broadcastPlayerDisconnected(seat, true);
                if (game != null && game.getCurrentSeat() == seat) {
                    triggerAITurn();
                }
            } else {
                broadcastPlayerLeft(seat);
                if (session == creator) {
                    reassignCreator();
                }
            }
        }
    }

    public synchronized boolean addAI(int slot) {
        if (slot < 0 || slot >= 4 || seats[slot] != null || isAI[slot]) return false;
        isAI[slot] = true;
        names[slot] = "電腦 " + (char)('A' + aiCounter++);

        Map<String, Object> msg = JsonUtil.msg("AI_ADDED");
        msg.put("seatIndex", slot);
        msg.put("name", names[slot]);
        broadcast(JsonUtil.toJson(msg));
        return true;
    }

    public synchronized boolean removeAI(int slot) {
        if (slot < 0 || slot >= 4 || !isAI[slot]) return false;
        isAI[slot] = false;
        names[slot] = null;

        Map<String, Object> msg = JsonUtil.msg("AI_REMOVED");
        msg.put("seatIndex", slot);
        broadcast(JsonUtil.toJson(msg));
        return true;
    }

    public synchronized String startGame(ClientSession requester) {
        if (requester != creator) return "只有房主可以開始遊戲！";
        int count = getPlayerCount();
        if (count < 3) return "至少需要 3 位玩家！";
        if (state != State.WAITING) return "遊戲已經在進行中！";

        // Collect active seats
        List<Integer> activeSeats = new ArrayList<>();
        List<String> activeNames = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (seats[i] != null || isAI[i]) {
                activeSeats.add(i);
                activeNames.add(names[i]);
            }
        }

        // Compact seats to 0..N-1 for the game engine
        int playerCount = activeSeats.size();
        game = new ServerBigTwoGame(playerCount, activeNames);
        state = State.PLAYING;

        // Send GAME_STARTED to each human player
        for (int gi = 0; gi < playerCount; gi++) {
            int origSeat = activeSeats.get(gi);
            if (seats[origSeat] != null) {
                seats[origSeat].setSeatIndex(gi);
                sendGameStarted(seats[origSeat], gi, playerCount, activeNames);
            }
            // Remap: store game-index in names/isAI arrays temporarily
        }

        // Remap internal arrays to game indices
        ClientSession[] newSeats = new ClientSession[4];
        boolean[] newAI = new boolean[4];
        String[] newNames = new String[4];
        for (int gi = 0; gi < playerCount; gi++) {
            int origSeat = activeSeats.get(gi);
            newSeats[gi] = seats[origSeat];
            newAI[gi] = isAI[origSeat];
            newNames[gi] = activeNames.get(gi);
        }
        System.arraycopy(newSeats, 0, seats, 0, 4);
        System.arraycopy(newAI, 0, isAI, 0, 4);
        System.arraycopy(newNames, 0, names, 0, 4);
        for (int i = playerCount; i < 4; i++) {
            seats[i] = null;
            isAI[i] = false;
            names[i] = null;
        }

        // Send YOUR_TURN to first player
        int firstSeat = game.getCurrentSeat();
        sendYourTurn(firstSeat);

        if (isAI[firstSeat]) {
            triggerAITurn();
        }

        return null;
    }

    public synchronized String handlePlay(ClientSession session, List<Card> cards) {
        if (state != State.PLAYING || game == null) return "遊戲尚未開始！";
        int seat = session.getSeatIndex();
        Play play = new Play(cards);
        String err = game.submitPlay(seat, play);
        if (err != null) return err;

        broadcastPlayResult(seat, play);

        if (game.isGameOver()) {
            broadcastGameOver();
            state = State.FINISHED;
        } else {
            int nextSeat = game.getCurrentSeat();
            sendYourTurn(nextSeat);
            if (isAI[nextSeat]) triggerAITurn();
        }
        return null;
    }

    public synchronized String handlePass(ClientSession session) {
        if (state != State.PLAYING || game == null) return "遊戲尚未開始！";
        int seat = session.getSeatIndex();
        Play pass = new Play(Collections.emptyList());
        String err = game.submitPlay(seat, pass);
        if (err != null) return err;

        broadcastPassResult(seat);

        if (game.isGameOver()) {
            broadcastGameOver();
            state = State.FINISHED;
        } else {
            int nextSeat = game.getCurrentSeat();
            sendYourTurn(nextSeat);
            if (isAI[nextSeat]) triggerAITurn();
        }
        return null;
    }

    private void triggerAITurn() {
        int delay = AIPlayer.thinkTime();
        scheduler.schedule(() -> {
            synchronized (this) {
                if (game == null || game.isGameOver()) return;
                int seat = game.getCurrentSeat();
                if (!isAI[seat]) return;

                Player aiPlayer = new Player(names[seat], false);
                aiPlayer.setHand(new ArrayList<>(game.getHand(seat)));
                Play aiPlay = AIPlayer.decide(aiPlayer, game.getLastPlay(), game.mustIncludeClubThree());

                if (aiPlay.isPass()) {
                    game.submitPlay(seat, aiPlay);
                    broadcastPassResult(seat);
                } else {
                    game.submitPlay(seat, aiPlay);
                    broadcastPlayResult(seat, aiPlay);
                }

                if (game.isGameOver()) {
                    broadcastGameOver();
                    state = State.FINISHED;
                } else {
                    int nextSeat = game.getCurrentSeat();
                    sendYourTurn(nextSeat);
                    if (isAI[nextSeat]) triggerAITurn();
                }
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    // ── Message builders ─────────────────────────────────────────────

    private void sendGameStarted(ClientSession session, int seatIdx, int playerCount, List<String> playerNames) {
        Map<String, Object> msg = JsonUtil.msg("GAME_STARTED");
        msg.put("playerCount", playerCount);
        msg.put("yourSeat", seatIdx);
        msg.put("yourHand", JsonUtil.cardsToList(game.getHand(seatIdx)));
        msg.put("currentSeat", game.getCurrentSeat());
        msg.put("firstTurn", game.isFirstTurn());

        List<Object> seatOrder = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("name", playerNames.get(i));
            p.put("seat", i);
            p.put("isAI", isAI[i]);
            p.put("cardCount", game.getHandSize(i));
            seatOrder.add(p);
        }
        msg.put("seatOrder", seatOrder);
        session.send(JsonUtil.toJson(msg));
    }

    private void sendYourTurn(int seat) {
        if (seat >= 0 && seats[seat] != null) {
            Map<String, Object> msg = JsonUtil.msg("YOUR_TURN");
            msg.put("canPass", game.canPass());
            msg.put("mustIncludeClubThree", game.mustIncludeClubThree());
            seats[seat].send(JsonUtil.toJson(msg));
        }
    }

    private void broadcastPlayResult(int seat, Play play) {
        Map<String, Object> msg = JsonUtil.msg("PLAY_RESULT");
        msg.put("seatIndex", seat);
        msg.put("playerName", names[seat]);
        msg.put("cards", JsonUtil.cardsToList(play.getCards()));
        msg.put("handType", play.getHandType().name());
        msg.put("cardsRemaining", game.getHandSize(seat));
        msg.put("nextSeat", game.getCurrentSeat());
        msg.put("newRound", game.getLastPlay() == null);
        broadcast(JsonUtil.toJson(msg));
    }

    private void broadcastPassResult(int seat) {
        Map<String, Object> msg = JsonUtil.msg("PASS_RESULT");
        msg.put("seatIndex", seat);
        msg.put("playerName", names[seat]);
        msg.put("nextSeat", game.getCurrentSeat());
        msg.put("newRound", game.getLastPlay() == null);
        broadcast(JsonUtil.toJson(msg));
    }

    private void broadcastGameOver() {
        int winSeat = game.getWinnerSeat();
        Map<String, Object> msg = JsonUtil.msg("GAME_OVER");
        msg.put("winnerName", names[winSeat]);
        msg.put("winnerSeat", winSeat);
        broadcast(JsonUtil.toJson(msg));
    }

    private void broadcastPlayerJoined(String name, int seat) {
        Map<String, Object> msg = JsonUtil.msg("PLAYER_JOINED");
        msg.put("playerName", name);
        msg.put("seatIndex", seat);
        broadcast(JsonUtil.toJson(msg));
    }

    private void broadcastPlayerLeft(int seat) {
        Map<String, Object> msg = JsonUtil.msg("PLAYER_LEFT");
        msg.put("seatIndex", seat);
        broadcast(JsonUtil.toJson(msg));
    }

    private void broadcastPlayerDisconnected(int seat, boolean replacedByAI) {
        Map<String, Object> msg = JsonUtil.msg("PLAYER_DISCONNECTED");
        msg.put("seatIndex", seat);
        msg.put("replacedByAI", replacedByAI);
        msg.put("aiName", names[seat]);
        broadcast(JsonUtil.toJson(msg));
    }

    private void broadcast(String json) {
        for (int i = 0; i < 4; i++) {
            if (seats[i] != null) {
                seats[i].send(json);
            }
        }
    }

    private void reassignCreator() {
        for (int i = 0; i < 4; i++) {
            if (seats[i] != null) {
                creator = seats[i];
                return;
            }
        }
        creator = null;
    }

    public synchronized boolean isEmpty() {
        for (int i = 0; i < 4; i++) {
            if (seats[i] != null) return false;
        }
        return true;
    }

    public synchronized List<Map<String, Object>> getPlayerList() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (seats[i] != null || isAI[i]) {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("name", names[i]);
                p.put("seat", i);
                p.put("isAI", isAI[i]);
                p.put("isCreator", seats[i] == creator);
                list.add(p);
            }
        }
        return list;
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
