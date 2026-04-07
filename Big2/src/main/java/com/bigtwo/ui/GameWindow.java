package com.bigtwo.ui;

import com.bigtwo.ai.AIPlayer;
import com.bigtwo.game.BigTwoGame;
import com.bigtwo.model.Card;
import com.bigtwo.model.HandType;
import com.bigtwo.model.Play;
import com.bigtwo.model.Player;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class GameWindow extends JFrame {

    // ── Design tokens ────────────────────────────────────────────────
    private static final Color BG_TOP    = new Color(18, 22, 38);
    private static final Color BG_BOT    = new Color(10, 14, 26);
    private static final Color PANEL_BG  = new Color(255, 255, 255, 10);
    private static final Color DIVIDER   = new Color(255, 255, 255, 18);
    private static final Color TEXT_PRI  = new Color(235, 235, 245);
    private static final Color TEXT_SEC  = new Color(140, 140, 165);
    private static final Color ACCENT    = new Color(99, 179, 255);

    private static final Font FONT_UI    = new Font("Microsoft JhengHei", Font.PLAIN, 13);
    private static final Font FONT_BOLD  = new Font("Microsoft JhengHei", Font.BOLD,  14);
    private static final Font FONT_SMALL = new Font("Microsoft JhengHei", Font.PLAIN, 11);

    // ── Game state ───────────────────────────────────────────────────
    private final BigTwoGame game = new BigTwoGame();

    // ── UI components ────────────────────────────────────────────────
    private final PlayerInfoPanel[] infoPanels = new PlayerInfoPanel[4];
    private final HandPanel   humanHandPanel   = new HandPanel(true);
    private final HandPanel[] aiHandPanels     = { new HandPanel(false), new HandPanel(false), new HandPanel(false) };
    private final TablePanel  tablePanel       = new TablePanel();

    private final JButton playBtn    = pillButton("出 牌",  new Color(56, 189, 110), Color.WHITE);
    private final JButton passBtn    = pillButton("Pass",   new Color(230, 100, 60), Color.WHITE);
    private final JButton newGameBtn = pillButton("新遊戲",  new Color(70, 100, 200), Color.WHITE);

    private final JLabel  statusLabel   = new JLabel("點「新遊戲」開始", SwingConstants.CENTER);
    private final JLabel  handTypeChip  = new JLabel("", SwingConstants.CENTER);
    private final JTextArea logArea     = new JTextArea();

    public GameWindow() {
        super("大老二 · Big Two");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1020, 780);
        setMinimumSize(new Dimension(900, 700));
        setLocationRelativeTo(null);
        buildUI();
        updateButtons();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Build UI
    // ─────────────────────────────────────────────────────────────────
    private void buildUI() {
        JPanel root = darkBg();
        root.setLayout(new BorderLayout(0, 0));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        setContentPane(root);

        // ── Panels ──
        infoPanels[0] = new PlayerInfoPanel("你",    0);
        infoPanels[1] = new PlayerInfoPanel("電腦 A", 1);
        infoPanels[2] = new PlayerInfoPanel("電腦 B", 2);
        infoPanels[3] = new PlayerInfoPanel("電腦 C", 3);

        root.add(buildTopBar(),     BorderLayout.NORTH);
        root.add(buildCenter(),     BorderLayout.CENTER);
        root.add(buildBottomArea(), BorderLayout.SOUTH);

        humanHandPanel.setOnSelectionChange(this::updateHandTypeChip);
    }

    // ── Top bar: title + 3 AI players ────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 0, 10, 0));

        // Title
        JLabel title = new JLabel("大老二");
        title.setFont(new Font("Microsoft JhengHei", Font.BOLD, 20));
        title.setForeground(TEXT_PRI);
        title.setBorder(new EmptyBorder(0, 4, 0, 0));

        // 3 AI players side by side
        JPanel aiRow = new JPanel(new GridLayout(1, 3, 10, 0));
        aiRow.setOpaque(false);

        String[] aiNames = { "電腦 A", "電腦 B", "電腦 C" };
        int[]    aiIdx   = { 1, 2, 3 };
        for (int i = 0; i < 3; i++) {
            JPanel slot = buildAISlot(i, aiIdx[i]);
            aiRow.add(slot);
        }

        bar.add(title,  BorderLayout.WEST);
        bar.add(aiRow,  BorderLayout.CENTER);
        return bar;
    }

    private JPanel buildAISlot(int aiIndex, int playerIndex) {
        JPanel slot = glassPanel();
        slot.setLayout(new BorderLayout(6, 4));
        slot.setBorder(new EmptyBorder(6, 8, 6, 8));

        infoPanels[playerIndex].setPreferredSize(new Dimension(0, 46));
        slot.add(infoPanels[playerIndex], BorderLayout.NORTH);

        JScrollPane sp = handScroll(aiHandPanels[aiIndex]);
        sp.setPreferredSize(new Dimension(0, CardPanel.CARD_HEIGHT + 20));
        slot.add(sp, BorderLayout.CENTER);

        return slot;
    }

    // ── Center: table (left) + log (right) ───────────────────────────
    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(10, 0));
        center.setOpaque(false);

        // Table area
        JPanel tableWrap = glassPanel();
        tableWrap.setLayout(new BorderLayout());
        tableWrap.setBorder(new EmptyBorder(8, 8, 8, 8));
        tableWrap.add(tablePanel, BorderLayout.CENTER);
        center.add(tableWrap, BorderLayout.CENTER);

        // Log panel
        JPanel logPanel = buildLogPanel();
        logPanel.setPreferredSize(new Dimension(210, 0));
        center.add(logPanel, BorderLayout.EAST);

        return center;
    }

    private JPanel buildLogPanel() {
        JPanel p = glassPanel();
        p.setLayout(new BorderLayout(0, 6));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel logTitle = new JLabel("出牌紀錄");
        logTitle.setFont(FONT_BOLD);
        logTitle.setForeground(TEXT_SEC);
        p.add(logTitle, BorderLayout.NORTH);

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

    // ── Bottom: status + human hand + buttons ────────────────────────
    private JPanel buildBottomArea() {
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(10, 0, 0, 0));

        // Status row
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

        // Human hand
        JPanel handWrap = glassPanel();
        handWrap.setLayout(new BorderLayout());
        handWrap.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane humanScroll = handScroll(humanHandPanel);
        humanScroll.setPreferredSize(new Dimension(0, CardPanel.CARD_HEIGHT + 26));
        handWrap.add(humanScroll, BorderLayout.CENTER);
        bottom.add(handWrap, BorderLayout.CENTER);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        btnRow.setOpaque(false);
        playBtn.addActionListener(e -> onPlay());
        passBtn.addActionListener(e -> onPass());
        newGameBtn.addActionListener(e -> onNewGame());
        btnRow.add(playBtn);
        btnRow.add(passBtn);
        btnRow.add(newGameBtn);
        bottom.add(btnRow, BorderLayout.SOUTH);

        return bottom;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Game logic hooks
    // ─────────────────────────────────────────────────────────────────
    private void onNewGame() {
        game.startNewGame();
        refreshUI();
        triggerAIIfNeeded();
    }

    private void onPlay() {
        if (game.isGameOver()) return;
        List<Card> selected = humanHandPanel.getSelectedCards();
        if (selected.isEmpty()) { showStatus("請選擇要出的牌！"); return; }
        Play play = new Play(selected);
        String err = game.submitPlay(play);
        if (err != null) { showStatus(err); return; }
        humanHandPanel.clearSelection();
        refreshUI();
        if (!game.isGameOver()) triggerAIIfNeeded();
    }

    private void onPass() {
        if (game.isGameOver()) return;
        String err = game.submitPlay(new Play(java.util.Collections.emptyList()));
        if (err != null) { showStatus(err); return; }
        refreshUI();
        if (!game.isGameOver()) triggerAIIfNeeded();
    }

    private void triggerAIIfNeeded() {
        if (game.isGameOver() || game.getCurrentPlayer().isHuman()) return;
        Timer t = new Timer(650, null);
        t.addActionListener(e -> {
            if (game.isGameOver() || game.getCurrentPlayer().isHuman()) { t.stop(); return; }
            Player ai = game.getCurrentPlayer();
            Play aiPlay = AIPlayer.decide(ai, game.getLastPlay(), game.mustIncludeClubThree());
            game.submitPlay(aiPlay);
            refreshUI();
            if (game.isGameOver() || game.getCurrentPlayer().isHuman()) t.stop();
        });
        t.setInitialDelay(650);
        t.start();
    }

    // ─────────────────────────────────────────────────────────────────
    //  UI refresh
    // ─────────────────────────────────────────────────────────────────
    private void refreshUI() {
        List<Player> players = game.getPlayers();

        humanHandPanel.setCards(players.get(0).getHand(), false);
        aiHandPanels[0].setCards(players.get(1).getHand(), true);
        aiHandPanels[1].setCards(players.get(2).getHand(), true);
        aiHandPanels[2].setCards(players.get(3).getHand(), true);

        int cur = game.getCurrentPlayerIndex();
        for (int i = 0; i < 4; i++) {
            infoPanels[i].update(players.get(i).getName(), players.get(i).getHandSize(), cur == i);
        }

        // Table
        Play last = game.getLastPlay();
        if (last != null && !last.isPass()) {
            // Find who played it (last non-pass player)
            String name = "";
            for (Player p : players) {
                if (!p.getHand().containsAll(last.getCards()) || p.getHand().isEmpty()) {
                    name = p.getName();
                }
            }
            tablePanel.setPlay(last, name, translateHandType(last.getHandType().name()));
        } else {
            tablePanel.setPlay(null, "", "");
        }

        // Log
        StringBuilder sb = new StringBuilder();
        List<String> logs = game.getLog();
        for (String entry : logs) sb.append(entry).append("\n");
        logArea.setText(sb.toString());
        logArea.setCaretPosition(logArea.getDocument().getLength());

        // Status
        if (game.isGameOver()) {
            Player w = game.getWinner();
            showStatus(w.isHuman() ? "恭喜你獲勝！" : w.getName() + " 獲勝！");
        } else if (game.getCurrentPlayer().isHuman()) {
            String hint = game.getLastPlay() == null
                ? "第一手必須包含梅花 3"
                : game.canPass() ? "出牌或 Pass" : "你控制桌面，請出牌";
            showStatus(hint);
        } else {
            showStatus(game.getCurrentPlayer().getName() + " 思考中…");
        }

        updateButtons();
        updateHandTypeChip();
    }

    private void updateButtons() {
        boolean human = !game.isGameOver()
            && game.getCurrentPlayer() != null
            && game.getCurrentPlayer().isHuman();
        playBtn.setEnabled(human);
        passBtn.setEnabled(human && game.canPass());
    }

    private void showStatus(String msg) { statusLabel.setText(msg); }

    private void updateHandTypeChip() {
        List<Card> sel = humanHandPanel.getSelectedCards();
        if (sel.isEmpty()) { handTypeChip.setText(""); return; }
        Play p = new Play(sel);
        handTypeChip.setText(p.isValid() ? translateHandType(p.getHandType().name()) : "無效牌型");
    }

    // ─────────────────────────────────────────────────────────────────
    //  Factory helpers
    // ─────────────────────────────────────────────────────────────────
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
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
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
        // Style scrollbar
        sp.getHorizontalScrollBar().setOpaque(false);
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 6));
        return sp;
    }

    private static JButton pillButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            private boolean hovered;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
            }
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
        btn.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
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
