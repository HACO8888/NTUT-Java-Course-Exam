package com.bigtwo.ui;

import com.bigtwo.ai.AIPlayer;
import com.bigtwo.game.BigTwoGame;
import com.bigtwo.model.Card;
import com.bigtwo.model.Play;
import com.bigtwo.model.Player;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

public class GameWindow extends JFrame {

    // ── Design tokens ────────────────────────────────────────────────
    private static final Color BG_TOP   = new Color(18, 22, 38);
    private static final Color BG_BOT   = new Color(10, 14, 26);
    private static final Color PANEL_BG = new Color(255, 255, 255, 10);
    private static final Color DIVIDER  = new Color(255, 255, 255, 18);
    private static final Color ACCENT   = new Color(99, 179, 255);

    private static final Font FONT_BOLD  = new Font("Microsoft JhengHei", Font.BOLD,  14);
    private static final Font FONT_UI    = new Font("Microsoft JhengHei", Font.PLAIN, 13);
    private static final Font FONT_SMALL = new Font("Microsoft JhengHei", Font.PLAIN, 11);

    // ── Game ─────────────────────────────────────────────────────────
    private final BigTwoGame game = new BigTwoGame();

    // ── UI ───────────────────────────────────────────────────────────
    private final PlayerInfoPanel[] infoPanels = new PlayerInfoPanel[4];
    private final HandPanel   humanHandPanel   = new HandPanel(true);
    private final FanPanel[]  fanPanels        = { new FanPanel(), new FanPanel(), new FanPanel() };
    private final TablePanel  tablePanel       = new TablePanel();

    private final JButton playBtn    = pillButton("出 牌",  new Color(56, 189, 110), Color.WHITE);
    private final JButton passBtn    = pillButton("Pass",   new Color(230, 100,  60), Color.WHITE);
    private final JButton newGameBtn = pillButton("新遊戲",  new Color(70,  100, 200), Color.WHITE);

    private final JLabel   statusLabel  = new JLabel("點「新遊戲」開始", SwingConstants.CENTER);
    private final JLabel   handTypeChip = new JLabel("", SwingConstants.CENTER);
    private final JTextArea logArea     = new JTextArea();

    public GameWindow() {
        super("大老二 · Big Two");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1020, 780);
        setMinimumSize(new Dimension(900, 700));
        setLocationRelativeTo(null);
        buildUI();
        bindKeys();
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

        infoPanels[0] = new PlayerInfoPanel("你",    0);
        infoPanels[1] = new PlayerInfoPanel("電腦 A", 1);
        infoPanels[2] = new PlayerInfoPanel("電腦 B", 2);
        infoPanels[3] = new PlayerInfoPanel("電腦 C", 3);

        root.add(buildTopBar(),     BorderLayout.NORTH);
        root.add(buildCenter(),     BorderLayout.CENTER);
        root.add(buildBottomArea(), BorderLayout.SOUTH);

        humanHandPanel.setOnSelectionChange(this::updateHandTypeChip);
    }

    // ── Top bar ───────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel title = new JLabel("大老二");
        title.setFont(new Font("Microsoft JhengHei", Font.BOLD, 20));
        title.setForeground(new Color(235, 235, 245));
        title.setBorder(new EmptyBorder(0, 4, 0, 0));

        JPanel aiRow = new JPanel(new GridLayout(1, 3, 10, 0));
        aiRow.setOpaque(false);
        int[] aiPlayerIdx = { 1, 2, 3 };
        for (int i = 0; i < 3; i++) {
            aiRow.add(buildAISlot(i, aiPlayerIdx[i]));
        }

        bar.add(title,  BorderLayout.WEST);
        bar.add(aiRow,  BorderLayout.CENTER);
        return bar;
    }

    private JPanel buildAISlot(int fanIdx, int playerIdx) {
        JPanel slot = glassPanel();
        slot.setLayout(new BorderLayout(6, 4));
        slot.setBorder(new EmptyBorder(6, 8, 4, 8));

        infoPanels[playerIdx].setPreferredSize(new Dimension(0, 46));
        slot.add(infoPanels[playerIdx], BorderLayout.NORTH);

        fanPanels[fanIdx].setPreferredSize(new Dimension(0, 110));
        slot.add(fanPanels[fanIdx], BorderLayout.CENTER);
        return slot;
    }

    // ── Center ───────────────────────────────────────────────────────
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

    // ── Bottom ───────────────────────────────────────────────────────
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
        humanScroll.setPreferredSize(new Dimension(0, CardPanel.CARD_HEIGHT + CardPanel.MAX_LIFT + 30));
        handWrap.add(humanScroll, BorderLayout.CENTER);
        bottom.add(handWrap, BorderLayout.CENTER);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        btnRow.setOpaque(false);

        // Hint labels under buttons
        JLabel playHint = hintLabel("[Enter]");
        JLabel passHint = hintLabel("[Space]");

        JPanel playWrap = btnWrap(playBtn, playHint);
        JPanel passWrap = btnWrap(passBtn, passHint);

        playBtn.addActionListener(e -> onPlay());
        passBtn.addActionListener(e -> onPass());
        newGameBtn.addActionListener(e -> onNewGame());

        JPanel newGameWrap = btnWrap(newGameBtn, hintLabel("[⌘ + R]"));
        btnRow.add(playWrap);
        btnRow.add(passWrap);
        btnRow.add(newGameWrap);
        bottom.add(btnRow, BorderLayout.SOUTH);
        return bottom;
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

    // ─────────────────────────────────────────────────────────────────
    //  Keyboard shortcuts
    // ─────────────────────────────────────────────────────────────────
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
    //  Game actions
    // ─────────────────────────────────────────────────────────────────
    private void onNewGame() {
        game.startNewGame();
        refreshUI();
        triggerAIIfNeeded();
    }

    private void onPlay() {
        if (game.isGameOver() || !game.getCurrentPlayer().isHuman()) return;
        List<Card> selected = humanHandPanel.getSelectedCards();
        if (selected.isEmpty()) { showStatus("請選擇要出的牌！"); return; }
        String err = game.submitPlay(new Play(selected));
        if (err != null) { showStatus(err); return; }
        humanHandPanel.clearSelection();
        refreshUI();
        if (!game.isGameOver()) triggerAIIfNeeded();
    }

    private void onPass() {
        if (game.isGameOver() || !game.getCurrentPlayer().isHuman()) return;
        String err = game.submitPlay(new Play(java.util.Collections.emptyList()));
        if (err != null) { showStatus(err); return; }
        refreshUI();
        if (!game.isGameOver()) triggerAIIfNeeded();
    }

    // ─────────────────────────────────────────────────────────────────
    //  AI scheduling
    // ─────────────────────────────────────────────────────────────────
    private void triggerAIIfNeeded() {
        if (game.isGameOver() || game.getCurrentPlayer().isHuman()) return;
        scheduleNextAI();
    }

    private void scheduleNextAI() {
        int delay = AIPlayer.thinkTime();
        Timer t = new Timer(delay, null);
        t.setRepeats(false);
        t.addActionListener(e -> {
            if (game.isGameOver() || game.getCurrentPlayer().isHuman()) return;
            Play aiPlay = AIPlayer.decide(
                game.getCurrentPlayer(), game.getLastPlay(), game.mustIncludeClubThree());
            game.submitPlay(aiPlay);
            refreshUI();
            if (!game.isGameOver() && !game.getCurrentPlayer().isHuman()) scheduleNextAI();
        });
        t.start();
    }

    // ─────────────────────────────────────────────────────────────────
    //  UI refresh
    // ─────────────────────────────────────────────────────────────────
    private void refreshUI() {
        List<Player> players = game.getPlayers();

        humanHandPanel.setCards(players.get(0).getHand(), false);
        fanPanels[0].setCardCount(players.get(1).getHandSize());
        fanPanels[1].setCardCount(players.get(2).getHandSize());
        fanPanels[2].setCardCount(players.get(3).getHandSize());

        int cur = game.getCurrentPlayerIndex();
        for (int i = 0; i < 4; i++) {
            infoPanels[i].update(players.get(i).getName(), players.get(i).getHandSize(), cur == i);
        }

        // Table
        Play last = game.getLastPlay();
        if (last != null && !last.isPass()) {
            tablePanel.setPlay(last,
                game.getLastPlayedByName(),
                translateHandType(last.getHandType().name()));
        } else {
            tablePanel.setPlay(null, "", "");
        }

        // Log
        StringBuilder sb = new StringBuilder();
        for (String entry : game.getLog()) sb.append(entry).append("\n");
        logArea.setText(sb.toString());
        logArea.setCaretPosition(logArea.getDocument().getLength());

        // Status
        if (game.isGameOver()) {
            Player w = game.getWinner();
            int wi = players.indexOf(w);
            winsCount[wi]++;
            infoPanels[wi].addWin();
            showStatus(w.isHuman() ? "恭喜你獲勝！" : w.getName() + " 獲勝！");
            showResultDialog(w);
        } else if (game.getCurrentPlayer().isHuman()) {
            String hint = game.mustIncludeClubThree()
                ? "第一手必須包含梅花 3"
                : game.getLastPlay() == null
                    ? "你控制桌面，請出牌"
                    : game.canPass() ? "出牌或 Pass" : "你控制桌面，請出牌";
            showStatus(hint);
        } else {
            showStatus(game.getCurrentPlayer().getName() + " 思考中…");
        }

        updateButtons();
        updateHandTypeChip();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Result dialog
    // ─────────────────────────────────────────────────────────────────
    private void showResultDialog(Player winner) {
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

        // Icon + title
        boolean humanWon = winner.isHuman();
        JLabel iconLabel = new JLabel(humanWon ? "🏆" : "😔", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));

        JLabel titleLabel = new JLabel(humanWon ? "恭喜獲勝！" : "本局結束", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Microsoft JhengHei", Font.BOLD, 26));
        titleLabel.setForeground(humanWon ? new Color(255, 215, 60) : new Color(180, 180, 220));

        JLabel subLabel = new JLabel(humanWon ? "你擊敗了所有電腦！" : winner.getName() + " 贏得本局", SwingConstants.CENTER);
        subLabel.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 15));
        subLabel.setForeground(new Color(160, 160, 200));

        // Score board
        JPanel scorePanel = new JPanel(new GridLayout(1, 4, 10, 0));
        scorePanel.setOpaque(false);
        String[] names = { "你", "電腦A", "電腦B", "電腦C" };
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            JPanel cell = new JPanel(new BorderLayout(0, 3)) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean isWinner = game.getPlayers().get(idx) == winner;
                    g2.setColor(isWinner ? new Color(255,200,50,40) : new Color(255,255,255,10));
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                    if (isWinner) {
                        g2.setColor(new Color(255,200,50,120));
                        g2.setStroke(new BasicStroke(1f));
                        g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            cell.setOpaque(false);
            cell.setBorder(new EmptyBorder(8, 10, 8, 10));

            JLabel nameL = new JLabel(names[i], SwingConstants.CENTER);
            nameL.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 11));
            nameL.setForeground(new Color(160, 160, 200));

            // Count wins from infoPanels — we'll just store wins count separately
            JLabel winsL = new JLabel(getWinsText(i), SwingConstants.CENTER);
            winsL.setFont(new Font("Arial", Font.BOLD, 22));
            winsL.setForeground(i == 0 ? new Color(100,220,150) : new Color(200,200,230));

            JLabel winsLbl = new JLabel("勝", SwingConstants.CENTER);
            winsLbl.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 10));
            winsLbl.setForeground(new Color(120,120,160));

            cell.add(nameL,  BorderLayout.NORTH);
            cell.add(winsL,  BorderLayout.CENTER);
            cell.add(winsLbl,BorderLayout.SOUTH);
            scorePanel.add(cell);
        }

        // Buttons
        JButton playAgainBtn = pillButton("再來一局", new Color(56, 189, 110), Color.WHITE);
        JButton closeBtn     = pillButton("離開",     new Color(80, 80, 120),  Color.WHITE);
        playAgainBtn.addActionListener(e -> { dialog.dispose(); onNewGame(); });
        closeBtn.addActionListener(e -> System.exit(0));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setOpaque(false);
        btnRow.add(playAgainBtn);
        btnRow.add(closeBtn);

        JPanel topArea = new JPanel(new BorderLayout(0, 8));
        topArea.setOpaque(false);
        topArea.add(iconLabel,  BorderLayout.NORTH);
        topArea.add(titleLabel, BorderLayout.CENTER);
        topArea.add(subLabel,   BorderLayout.SOUTH);

        panel.add(topArea,    BorderLayout.NORTH);
        panel.add(scorePanel, BorderLayout.CENTER);
        panel.add(btnRow,     BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ── Win count helpers ─────────────────────────────────────────────
    private final int[] winsCount = new int[4];

    /** Call after addWin on infoPanels to also store locally for dialog. */
    private String getWinsText(int playerIdx) {
        return String.valueOf(winsCount[playerIdx]);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────
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

    // ── Factory helpers ───────────────────────────────────────────────
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
