package com.bigtwo.ui;

import com.bigtwo.ai.AIPlayer;
import com.bigtwo.game.BigTwoGame;
import com.bigtwo.model.*;
import com.bigtwo.net.GameClient;
import com.bigtwo.net.JsonUtil;
import com.bigtwo.net.MessageListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.util.*;
import java.util.List;

public class GameWindow extends JFrame implements MessageListener {

    // ── Design tokens ────────────────────────────────────────────────
    private static final Color BG_TOP   = new Color(18, 22, 38);
    private static final Color BG_BOT   = new Color(10, 14, 26);
    private static final Color PANEL_BG = new Color(255, 255, 255, 10);
    private static final Color DIVIDER  = new Color(255, 255, 255, 18);
    private static final Color ACCENT   = new Color(99, 179, 255);

    private static final Font FONT_BOLD  = new Font("Noto Sans TC", Font.BOLD,  14);
    private static final Font FONT_UI    = new Font("Noto Sans TC", Font.PLAIN, 13);
    private static final Font FONT_SMALL = new Font("Noto Sans TC", Font.PLAIN, 11);

    // ── Mode ─────────────────────────────────────────────────────────
    private boolean offlineMode = false;

    // ── Offline game ─────────────────────────────────────────────────
    private BigTwoGame game;

    // ── Online state ─��───────────────────────���───────────────────────
    private GameClient client;
    private String myName = "";
    private int mySeat = -1;
    private int playerCount = 4;
    private int currentSeat = -1;
    private String[] playerNames;
    private int[] cardCounts;
    private boolean myTurn = false;
    private boolean canPassOnline = false;

    // ── CardLayout panels ────────────────────────────────────────────
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final LobbyPanel lobbyPanel = new LobbyPanel();
    private final WaitingRoomPanel waitingPanel = new WaitingRoomPanel();
    private JPanel gamePanel;

    // ── Game UI ───────────────────────────────────��──────────────────
    private PlayerInfoPanel[] infoPanels;
    private final HandPanel humanHandPanel = new HandPanel(true);
    private FanPanel[] fanPanels;
    private final TablePanel tablePanel = new TablePanel();

    private final JButton playBtn    = pillButton("出 牌",  new Color(56, 189, 110), Color.WHITE);
    private final JButton passBtn    = pillButton("Pass",   new Color(230, 100,  60), Color.WHITE);
    private final JButton newGameBtn = pillButton("新遊戲",  new Color(70,  100, 200), Color.WHITE);
    private final JButton backBtn    = pillButton("返回大廳", new Color(80,  80, 120),  Color.WHITE);

    private final JLabel   statusLabel  = new JLabel("", SwingConstants.CENTER);
    private final JLabel   handTypeChip = new JLabel("", SwingConstants.CENTER);
    private final JTextArea logArea     = new JTextArea();
    private final int[] winsCount = new int[4];

    public GameWindow() {
        super("大老二 · Big Two Online");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1020, 780);
        setMinimumSize(new Dimension(900, 700));
        setLocationRelativeTo(null);

        JPanel root = darkBg();
        root.setLayout(new BorderLayout());
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        setContentPane(root);

        contentPanel.setOpaque(false);
        root.add(contentPanel, BorderLayout.CENTER);

        contentPanel.add(lobbyPanel, "lobby");

        lobbyPanel.setCallback(new LobbyPanel.LobbyCallback() {
            public void onCreateRoom(String name) { connectAndCreateRoom(name); }
            public void onJoinRoom(String name, String code) { connectAndJoinRoom(name, code); }
            public void onOfflineMode() { startOfflineMode(); }
        });

        contentPanel.add(waitingPanel, "waiting");
        waitingPanel.setCallback(new WaitingRoomPanel.WaitingRoomCallback() {
            public void onStartGame() { if (client != null) client.startGame(); }
            public void onLeaveRoom() { leaveRoom(); }
            public void onAddAI(int slot) { if (client != null) client.addAI(slot); }
            public void onRemoveAI(int slot) { if (client != null) client.removeAI(slot); }
        });

        cardLayout.show(contentPanel, "lobby");
        bindKeys();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Connection
    // ─────────────────────────────────────────────────────��───────────
    private static final String DEFAULT_SERVER = "api-big2.haco.tw";

    private void connectAndCreateRoom(String name) {
        if (name.isEmpty()) { showError("請輸入玩家名稱！"); return; }
        myName = name;
        connectToServer(DEFAULT_SERVER, () -> client.createRoom(name));
    }

    private void connectAndJoinRoom(String name, String code) {
        if (name.isEmpty()) { showError("請輸入玩家名稱！"); return; }
        if (code.isEmpty()) { showError("請輸入房間代碼！"); return; }
        myName = name;
        connectToServer(DEFAULT_SERVER, () -> client.joinRoom(code.toUpperCase(), name));
    }

    private void connectToServer(String server, Runnable onConnect) {
        try {
            if (!server.startsWith("ws://") && !server.startsWith("wss://")) {
                // No port specified and looks like a domain → try wss:// first, fallback ws://
                if (!server.contains(":") && server.contains(".")) {
                    connectWithFallback("wss://" + server, "ws://" + server, onConnect);
                    return;
                }
                server = "ws://" + server;
            }
            doConnect(server, onConnect);
        } catch (Exception e) {
            showError("連線錯誤：" + e.getMessage());
        }
    }

    private void connectWithFallback(String primary, String fallback, Runnable onConnect) {
        new Thread(() -> {
            try {
                client = new GameClient(new URI(primary));
                client.setListener(this);
                client.setConnectionLostTimeout(5);
                if (client.connectBlocking(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    SwingUtilities.invokeLater(onConnect);
                    return;
                }
            } catch (Exception ignored) {}
            // Fallback
            try {
                client = new GameClient(new URI(fallback));
                client.setListener(this);
                client.connectBlocking();
                SwingUtilities.invokeLater(onConnect);
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> showError("無法連接伺服器：" + e.getMessage()));
            }
        }).start();
    }

    private void doConnect(String uri, Runnable onConnect) throws Exception {
        client = new GameClient(new URI(uri));
        client.setListener(this);
        new Thread(() -> {
            try {
                client.connectBlocking();
                SwingUtilities.invokeLater(onConnect);
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> showError("無法連接伺服器：" + e.getMessage()));
            }
        }).start();
    }

    private void leaveRoom() {
        if (client != null) {
            client.leaveRoom();
            try { client.closeBlocking(); } catch (Exception ignored) {}
            client = null;
        }
        cardLayout.show(contentPanel, "lobby");
    }

    // ─────────────��─────────────────────────────��─────────────────────
    //  Offline mode
    // ──────────��───────────────��──────────────────────────────────────
    private void startOfflineMode() {
        offlineMode = true;
        playerCount = 4;
        game = new BigTwoGame();

        buildGamePanel(4, new String[]{"你", "電腦 A", "電腦 B", "電腦 C"});
        game.startNewGame();
        refreshOfflineUI();
        triggerAIIfNeeded();
    }

    private void onOfflineNewGame() {
        game.startNewGame();
        Arrays.fill(winsCount, 0);
        refreshOfflineUI();
        triggerAIIfNeeded();
    }

    private void onOfflinePlay() {
        if (game.isGameOver() || !game.getCurrentPlayer().isHuman()) return;
        List<Card> selected = humanHandPanel.getSelectedCards();
        if (selected.isEmpty()) { showStatus("請選擇要出的牌！"); return; }
        String err = game.submitPlay(new Play(selected));
        if (err != null) { showStatus(err); return; }
        humanHandPanel.clearSelection();
        refreshOfflineUI();
        if (!game.isGameOver()) triggerAIIfNeeded();
    }

    private void onOfflinePass() {
        if (game.isGameOver() || !game.getCurrentPlayer().isHuman()) return;
        String err = game.submitPlay(new Play(Collections.emptyList()));
        if (err != null) { showStatus(err); return; }
        refreshOfflineUI();
        if (!game.isGameOver()) triggerAIIfNeeded();
    }

    private void triggerAIIfNeeded() {
        if (game.isGameOver() || game.getCurrentPlayer().isHuman()) return;
        scheduleNextAI();
    }

    private void scheduleNextAI() {
        javax.swing.Timer t = new javax.swing.Timer(AIPlayer.thinkTime(), null);
        t.setRepeats(false);
        t.addActionListener(e -> {
            if (game.isGameOver() || game.getCurrentPlayer().isHuman()) return;
            Play aiPlay = AIPlayer.decide(game.getCurrentPlayer(), game.getLastPlay(), game.mustIncludeClubThree());
            game.submitPlay(aiPlay);
            refreshOfflineUI();
            if (!game.isGameOver() && !game.getCurrentPlayer().isHuman()) scheduleNextAI();
        });
        t.start();
    }

    private void refreshOfflineUI() {
        List<Player> players = game.getPlayers();

        humanHandPanel.setCards(players.get(0).getHand(), false);
        for (int i = 0; i < 3; i++) {
            fanPanels[i].setCardCount(players.get(i + 1).getHandSize());
        }

        int cur = game.getCurrentPlayerIndex();
        for (int i = 0; i < 4; i++) {
            infoPanels[i].update(players.get(i).getName(), players.get(i).getHandSize(), cur == i);
        }

        Play last = game.getLastPlay();
        if (last != null && !last.isPass()) {
            tablePanel.setPlay(last, game.getLastPlayedByName(), translateHandType(last.getHandType().name()));
        } else {
            tablePanel.setPlay(null, "", "");
        }

        StringBuilder sb = new StringBuilder();
        for (String entry : game.getLog()) sb.append(entry).append("\n");
        logArea.setText(sb.toString());
        logArea.setCaretPosition(logArea.getDocument().getLength());

        if (game.isGameOver()) {
            Player w = game.getWinner();
            int wi = players.indexOf(w);
            winsCount[wi]++;
            infoPanels[wi].addWin();
            showStatus(w.isHuman() ? "恭喜你獲勝！" : w.getName() + " 獲勝！");
            showResultDialog(w.getName(), w.isHuman());
        } else if (game.getCurrentPlayer().isHuman()) {
            String hint = game.mustIncludeClubThree() ? "第一手必須包含梅花 3"
                : game.getLastPlay() == null ? "你控制桌面，請出牌"
                : game.canPass() ? "出牌或 Pass" : "你控制桌面，請出牌";
            showStatus(hint);
        } else {
            showStatus(game.getCurrentPlayer().getName() + " 思考中…");
        }

        updateButtons();
        updateHandTypeChip();
    }

    // ──────────────────────────────────���──────────────────────────────
    //  MessageListener (Online mode)
    // ──���──────────���───────────────────────────────────────────────────
    @Override
    public void onRoomCreated(String roomCode, int seatIndex) {
        mySeat = seatIndex;
        waitingPanel.setRoomCode(roomCode);
        waitingPanel.setIsCreator(true);
        waitingPanel.setSeat(seatIndex, myName, false, true);
        cardLayout.show(contentPanel, "waiting");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onRoomJoined(String roomCode, int seatIndex, List<Map<String, Object>> players) {
        mySeat = seatIndex;
        waitingPanel.setRoomCode(roomCode);
        waitingPanel.setIsCreator(false);
        for (Map<String, Object> p : players) {
            int seat = JsonUtil.getInt(p, "seat");
            String name = JsonUtil.getString(p, "name");
            boolean ai = JsonUtil.getBool(p, "isAI");
            waitingPanel.setSeat(seat, name, ai, !ai);
        }
        waitingPanel.updateStartButton();
        cardLayout.show(contentPanel, "waiting");
    }

    @Override
    public void onPlayerJoined(String playerName, int seatIndex) {
        waitingPanel.setSeat(seatIndex, playerName, false, true);
    }

    @Override
    public void onPlayerLeft(int seatIndex) {
        waitingPanel.clearSeat(seatIndex);
    }

    @Override
    public void onOwnerChanged(int newOwnerSeat, String newOwnerName) {
        boolean iAmOwner = (newOwnerSeat == mySeat);
        waitingPanel.setIsCreator(iAmOwner);
        waitingPanel.updateStartButton();
    }

    @Override
    public void onAIAdded(int seatIndex, String name) {
        waitingPanel.setSeat(seatIndex, name, true, false);
    }

    @Override
    public void onAIRemoved(int seatIndex) {
        waitingPanel.clearSeat(seatIndex);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onGameStarted(Map<String, Object> data) {
        offlineMode = false;
        playerCount = JsonUtil.getInt(data, "playerCount");
        mySeat = JsonUtil.getInt(data, "yourSeat");
        currentSeat = JsonUtil.getInt(data, "currentSeat");

        List<Object> seatOrder = JsonUtil.getList(data, "seatOrder");
        playerNames = new String[playerCount];
        cardCounts = new int[playerCount];
        for (int i = 0; i < playerCount; i++) {
            Map<String, Object> p = (Map<String, Object>) seatOrder.get(i);
            playerNames[i] = JsonUtil.getString(p, "name");
            cardCounts[i] = JsonUtil.getInt(p, "cardCount");
        }

        List<Card> myHand = JsonUtil.parseCardList(JsonUtil.getList(data, "yourHand"));

        // Build game panel with remapped names (my seat at position 0)
        String[] displayNames = new String[playerCount];
        for (int i = 0; i < playerCount; i++) {
            int actualSeat = (mySeat + i) % playerCount;
            displayNames[i] = playerNames[actualSeat];
        }
        buildGamePanel(playerCount, displayNames);

        humanHandPanel.setCards(myHand, false);
        logArea.setText("遊戲開始！\n");

        // Update card counts and active indicator
        updateOnlineUI();
        cardLayout.show(contentPanel, "game");
    }

    @Override
    public void onPlayResult(Map<String, Object> data) {
        int seat = JsonUtil.getInt(data, "seatIndex");
        String name = JsonUtil.getString(data, "playerName");
        List<Card> cards = JsonUtil.parseCardList(JsonUtil.getList(data, "cards"));
        String handType = JsonUtil.getString(data, "handType");
        int remaining = JsonUtil.getInt(data, "cardsRemaining");
        currentSeat = JsonUtil.getInt(data, "nextSeat");
        boolean newRound = JsonUtil.getBool(data, "newRound");

        cardCounts[seat] = remaining;

        if (seat == mySeat) {
            // Remove played cards from hand display
            List<Card> currentHand = new ArrayList<>(humanHandPanel.getCards());
            currentHand.removeAll(cards);
            humanHandPanel.setCards(currentHand, false);
            humanHandPanel.clearSelection();
        }

        Play play = new Play(cards);
        tablePanel.setPlay(play, name, translateHandType(handType));
        logArea.append(name + " 出牌: " + play + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());

        if (newRound) {
            logArea.append("--- 新的一輪，桌面清空 ---\n");
            tablePanel.setPlay(null, "", "");
        }

        updateOnlineUI();
    }

    @Override
    public void onPassResult(Map<String, Object> data) {
        int seat = JsonUtil.getInt(data, "seatIndex");
        String name = playerNames[seat];
        currentSeat = JsonUtil.getInt(data, "nextSeat");
        boolean newRound = JsonUtil.getBool(data, "newRound");

        logArea.append(name + " Pass\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());

        if (newRound) {
            logArea.append("--- 新的一輪，桌面清空 ---\n");
            tablePanel.setPlay(null, "", "");
        }

        updateOnlineUI();
    }

    @Override
    public void onYourTurn(boolean canPass, boolean mustIncludeClubThree) {
        myTurn = true;
        canPassOnline = canPass;
        String hint = mustIncludeClubThree ? "第一手必須包���梅花 3"
            : !canPass ? "你控制桌面，請出牌" : "出牌或 Pass";
        showStatus(hint);
        updateButtons();
    }

    @Override
    public void onPlayError(String message) {
        showStatus(message);
    }

    @Override
    public void onGameOver(String winnerName, int winnerSeat) {
        myTurn = false;
        boolean iWon = (winnerSeat == mySeat);
        showStatus(iWon ? "恭喜你獲��！" : winnerName + " 獲勝！");
        logArea.append((iWon ? "恭喜你獲勝！" : winnerName + " 獲勝！") + "\n");
        showResultDialog(winnerName, iWon);
        updateButtons();
    }

    @Override
    public void onPlayerDisconnected(int seatIndex, boolean replacedByAI, String aiName) {
        if (replacedByAI && aiName != null) {
            playerNames[seatIndex] = aiName;
            logArea.append(aiName + " (AI) 接替斷線玩家\n");
        }
        updateOnlineUI();
    }

    @Override
    public void onError(String message) {
        showError(message);
    }

    private void updateOnlineUI() {
        if (playerNames == null || cardCounts == null) return;

        // Update opponent fan panels and info panels
        int opponentIdx = 0;
        for (int i = 0; i < playerCount; i++) {
            int actualSeat = (mySeat + i) % playerCount;
            if (i == 0) {
                // My info panel
                infoPanels[0].update(playerNames[actualSeat], cardCounts[actualSeat], currentSeat == actualSeat);
            } else {
                if (opponentIdx < fanPanels.length) {
                    fanPanels[opponentIdx].setCardCount(cardCounts[actualSeat]);
                    infoPanels[opponentIdx + 1].update(playerNames[actualSeat], cardCounts[actualSeat], currentSeat == actualSeat);
                }
                opponentIdx++;
            }
        }

        if (currentSeat != mySeat) {
            myTurn = false;
            showStatus(playerNames[currentSeat] + " 的回合…");
        }
        updateButtons();
    }

    // ──────────────────────────────���──────────────────────────────────
    //  Build game panel (supports 3 or 4 players)
    // ──────��─────────────────────────���───────────────────────────���────
    private void buildGamePanel(int numPlayers, String[] names) {
        if (gamePanel != null) contentPanel.remove(gamePanel);

        int opponents = numPlayers - 1;
        infoPanels = new PlayerInfoPanel[numPlayers];
        fanPanels = new FanPanel[opponents];

        for (int i = 0; i < numPlayers; i++) {
            infoPanels[i] = new PlayerInfoPanel(names[i], i);
        }
        for (int i = 0; i < opponents; i++) {
            fanPanels[i] = new FanPanel();
        }

        gamePanel = new JPanel(new BorderLayout(0, 0));
        gamePanel.setOpaque(false);

        gamePanel.add(buildTopBar(opponents), BorderLayout.NORTH);
        gamePanel.add(buildCenter(), BorderLayout.CENTER);
        gamePanel.add(buildBottomArea(), BorderLayout.SOUTH);

        humanHandPanel.setOnSelectionChange(this::updateHandTypeChip);
        contentPanel.add(gamePanel, "game");
    }

    private JPanel buildTopBar(int opponents) {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel title = new JLabel("大老二");
        title.setFont(new Font("Noto Sans TC", Font.BOLD, 20));
        title.setForeground(new Color(235, 235, 245));
        title.setBorder(new EmptyBorder(0, 4, 0, 0));

        JPanel aiRow = new JPanel(new GridLayout(1, opponents, 10, 0));
        aiRow.setOpaque(false);
        for (int i = 0; i < opponents; i++) {
            aiRow.add(buildOpponentSlot(i));
        }

        bar.add(title, BorderLayout.WEST);
        bar.add(aiRow, BorderLayout.CENTER);
        return bar;
    }

    private JPanel buildOpponentSlot(int idx) {
        JPanel slot = glassPanel();
        slot.setLayout(new BorderLayout(6, 4));
        slot.setBorder(new EmptyBorder(6, 8, 4, 8));

        infoPanels[idx + 1].setPreferredSize(new Dimension(0, 46));
        slot.add(infoPanels[idx + 1], BorderLayout.NORTH);

        fanPanels[idx].setPreferredSize(new Dimension(0, 110));
        slot.add(fanPanels[idx], BorderLayout.CENTER);
        return slot;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(10, 0));
        center.setOpaque(false);

        JPanel tableWrap = glassPanel();
        tableWrap.setLayout(new BorderLayout());
        tableWrap.setBorder(new EmptyBorder(8, 8, 8, 8));
        tableWrap.add(tablePanel, BorderLayout.CENTER);
        center.add(tableWrap, BorderLayout.CENTER);
        center.add(buildLogPanel(), BorderLayout.EAST);
        return center;
    }

    private JPanel buildLogPanel() {
        JPanel p = glassPanel();
        p.setLayout(new BorderLayout(0, 6));
        p.setPreferredSize(new Dimension(210, 0));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("出牌紀錄");
        title.setFont(FONT_BOLD);
        title.setForeground(new Color(140, 140, 165));
        p.add(title, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setFont(FONT_SMALL);
        logArea.setBackground(new Color(0, 0, 0, 0));
        logArea.setForeground(new Color(180, 180, 210));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setOpaque(false);
        logArea.setBorder(null);

        JScrollPane sp = new JScrollPane(logArea);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(null);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildBottomArea() {
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel statusRow = new JPanel(new BorderLayout(10, 0));
        statusRow.setOpaque(false);
        infoPanels[0].setPreferredSize(new Dimension(160, 48));
        statusRow.add(infoPanels[0], BorderLayout.WEST);
        statusLabel.setFont(FONT_BOLD);
        statusLabel.setForeground(new Color(255, 215, 80));
        statusRow.add(statusLabel, BorderLayout.CENTER);
        handTypeChip.setFont(FONT_UI);
        handTypeChip.setForeground(ACCENT);
        handTypeChip.setPreferredSize(new Dimension(120, 0));
        statusRow.add(handTypeChip, BorderLayout.EAST);
        bottom.add(statusRow, BorderLayout.NORTH);

        JPanel handWrap = glassPanel();
        handWrap.setLayout(new BorderLayout());
        handWrap.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane humanScroll = handScroll(humanHandPanel);
        humanScroll.setPreferredSize(new Dimension(0, CardPanel.CARD_HEIGHT + CardPanel.MAX_LIFT + 30));
        handWrap.add(humanScroll, BorderLayout.CENTER);
        bottom.add(handWrap, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        btnRow.setOpaque(false);

        playBtn.addActionListener(e -> onPlay());
        passBtn.addActionListener(e -> onPass());
        newGameBtn.addActionListener(e -> onNewGame());
        backBtn.addActionListener(e -> onBackToLobby());

        btnRow.add(btnWrap(playBtn, hintLabel("[Enter]")));
        btnRow.add(btnWrap(passBtn, hintLabel("[Space]")));
        btnRow.add(btnWrap(newGameBtn, hintLabel("[⌘+R]")));
        btnRow.add(btnWrap(backBtn, hintLabel("")));
        bottom.add(btnRow, BorderLayout.SOUTH);
        return bottom;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Game actions (unified)
    // ───���─────────────────────────────────────────────────���───────────
    private void onPlay() {
        if (offlineMode) { onOfflinePlay(); return; }
        if (!myTurn || client == null) return;
        List<Card> selected = humanHandPanel.getSelectedCards();
        if (selected.isEmpty()) { showStatus("請選擇要出的�����"); return; }
        myTurn = false;
        client.playCards(selected);
        updateButtons();
    }

    private void onPass() {
        if (offlineMode) { onOfflinePass(); return; }
        if (!myTurn || client == null) return;
        myTurn = false;
        client.pass();
        updateButtons();
    }

    private void onNewGame() {
        if (offlineMode) { onOfflineNewGame(); return; }
    }

    private void onBackToLobby() {
        leaveRoom();
        offlineMode = false;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Keyboard shortcuts
    // ─────────────��─────────────────────────────────��─────────────────
    private void bindKeys() {
        JRootPane rp = getRootPane();
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "play");
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "pass");
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "newgame");
        rp.getActionMap().put("play", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { onPlay(); }
        });
        rp.getActionMap().put("pass", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { onPass(); }
        });
        rp.getActionMap().put("newgame", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { onNewGame(); }
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  Result dialog
    // ─────────────────────────────────────────────────────────────────
    private void showResultDialog(String winnerName, boolean iWon) {
        JDialog dialog = new JDialog(this, true);
        dialog.setUndecorated(true);

        JPanel panel = new JPanel(new BorderLayout(0, 20)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 20, 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(new Color(255, 255, 255, 40));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 24, 24);
                g2.dispose();
            }
        };
        panel.setOpaque(true);
        panel.setBackground(new Color(15, 20, 40));
        panel.setBorder(new EmptyBorder(36, 48, 28, 48));

        JLabel iconLabel = new JLabel(iWon ? "\uD83C\uDFC6" : "\uD83D\uDE14", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));

        JLabel titleLabel = new JLabel(iWon ? "恭喜獲勝！" : "本局結束", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Noto Sans TC", Font.BOLD, 26));
        titleLabel.setForeground(iWon ? new Color(255, 215, 60) : new Color(180, 180, 220));

        JLabel subLabel = new JLabel(iWon ? "你擊敗了所有對手！" : winnerName + " 贏得本局", SwingConstants.CENTER);
        subLabel.setFont(new Font("Noto Sans TC", Font.PLAIN, 15));
        subLabel.setForeground(new Color(160, 160, 200));

        JPanel topArea = new JPanel(new BorderLayout(0, 8));
        topArea.setOpaque(false);
        topArea.add(iconLabel, BorderLayout.NORTH);
        topArea.add(titleLabel, BorderLayout.CENTER);
        topArea.add(subLabel, BorderLayout.SOUTH);

        JButton closeBtn = pillButton("確定", new Color(56, 189, 110), Color.WHITE);
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnRow.setOpaque(false);
        btnRow.add(closeBtn);

        panel.add(topArea, BorderLayout.NORTH);
        panel.add(btnRow, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ─────────────────────────────��───────────────────────────────────
    //  Helpers
    // ──���───────────────���──────────────────────────────────────────────
    private void updateButtons() {
        if (offlineMode) {
            boolean human = !game.isGameOver()
                && game.getCurrentPlayer() != null
                && game.getCurrentPlayer().isHuman();
            playBtn.setEnabled(human);
            passBtn.setEnabled(human && game.canPass());
            newGameBtn.setVisible(true);
            backBtn.setVisible(true);
        } else {
            playBtn.setEnabled(myTurn);
            passBtn.setEnabled(myTurn && canPassOnline);
            newGameBtn.setVisible(false);
            backBtn.setVisible(true);
        }
    }

    private void showStatus(String msg) { statusLabel.setText(msg); }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "錯誤", JOptionPane.ERROR_MESSAGE);
    }

    private void updateHandTypeChip() {
        List<Card> sel = humanHandPanel.getSelectedCards();
        if (sel.isEmpty()) { handTypeChip.setText(""); return; }
        Play p = new Play(sel);
        handTypeChip.setText(p.isValid() ? translateHandType(p.getHandType().name()) : "無效牌型");
    }

    // ── Factory helpers ────────────���──────────────────────────────────
    private static JPanel darkBg() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, BG_TOP, 0, getHeight(), BG_BOT));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
    }

    private static JPanel glassPanel() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PANEL_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(DIVIDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
    }

    private static JScrollPane handScroll(JPanel content) {
        JScrollPane sp = new JScrollPane(content);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(null);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 5));
        return sp;
    }

    private JLabel hintLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Arial", Font.PLAIN, 10));
        l.setForeground(new Color(120, 120, 150));
        return l;
    }

    private JPanel btnWrap(JButton btn, JLabel hint) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        p.add(btn, BorderLayout.CENTER);
        p.add(hint, BorderLayout.SOUTH);
        return p;
    }

    private static JButton pillButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            private boolean hovered;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = isEnabled() ? bg : new Color(80, 80, 100);
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                if (hovered && isEnabled()) {
                    g2.setColor(new Color(255, 255, 255, 30));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                }
                g2.dispose();
                super.paintComponent(g);
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        btn.setFont(new Font("Noto Sans TC", Font.BOLD, 14));
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setBorder(new EmptyBorder(9, 26, 9, 26));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private static String translateHandType(String type) {
        switch (type) {
            case "SINGLE":           return "單張";
            case "PAIR":             return "對子";
            case "STRAIGHT":         return "順子";
            case "FLUSH":            return "同花";
            case "FULL_HOUSE":       return "葫蘆";
            case "FOUR_OF_A_KIND":   return "四條";
            case "STRAIGHT_FLUSH":   return "同花順";
            default:                 return type;
        }
    }
}
