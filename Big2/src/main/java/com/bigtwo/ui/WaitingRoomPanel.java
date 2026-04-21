package com.bigtwo.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class WaitingRoomPanel extends JPanel {

    private static final Color PANEL_BG = new Color(255, 255, 255, 10);
    private static final Color DIVIDER = new Color(255, 255, 255, 18);
    private static final Font FONT_BOLD = new Font("Noto Sans TC", Font.BOLD, 14);
    private static final Font FONT_UI = new Font("Noto Sans TC", Font.PLAIN, 13);

    private final JLabel roomCodeLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel("等待玩家加入...", SwingConstants.CENTER);
    private final JButton startBtn;
    private final JButton leaveBtn;

    private final JPanel[] seatPanels = new JPanel[4];
    private final JLabel[] seatNameLabels = new JLabel[4];
    private final JLabel[] seatStatusLabels = new JLabel[4];
    private final JButton[] aiButtons = new JButton[4];

    private boolean isCreator = false;
    private final String[] seatNames = new String[4];
    private final boolean[] seatIsAI = new boolean[4];
    private final boolean[] seatOccupied = new boolean[4];

    public interface WaitingRoomCallback {
        void onStartGame();
        void onLeaveRoom();
        void onAddAI(int slot);
        void onRemoveAI(int slot);
    }

    private WaitingRoomCallback callback;

    public WaitingRoomPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        startBtn = pillButton("開始遊戲", new Color(56, 189, 110), Color.WHITE);
        leaveBtn = pillButton("離開房間", new Color(230, 100, 60), Color.WHITE);

        buildUI();
    }

    public void setCallback(WaitingRoomCallback cb) { this.callback = cb; }

    public void setRoomCode(String code) {
        roomCodeLabel.setText(code);
    }

    public void setIsCreator(boolean creator) {
        this.isCreator = creator;
        startBtn.setVisible(creator);
        updateSeatButtons();
    }

    public void setSeat(int index, String name, boolean ai, boolean occupied) {
        seatNames[index] = name;
        seatIsAI[index] = ai;
        seatOccupied[index] = occupied;
        updateSeatDisplay(index);
    }

    public void clearSeat(int index) {
        seatNames[index] = null;
        seatIsAI[index] = false;
        seatOccupied[index] = false;
        updateSeatDisplay(index);
    }

    public void updateStartButton() {
        int count = 0;
        for (int i = 0; i < 4; i++) {
            if (seatOccupied[i] || seatIsAI[i]) count++;
        }
        startBtn.setEnabled(count >= 3);
        statusLabel.setText(count >= 3 ? "可以開始遊戲！" : "等待玩家加入... (" + count + "/4)");
    }

    private void buildUI() {
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(60, 0, 0, 0));

        JLabel title = new JLabel("等待室");
        title.setFont(new Font("Noto Sans TC", Font.BOLD, 28));
        title.setForeground(new Color(235, 235, 245));
        title.setAlignmentX(CENTER_ALIGNMENT);
        center.add(title);

        center.add(Box.createVerticalStrut(12));

        JLabel codeTitle = new JLabel("房間代碼");
        codeTitle.setFont(FONT_UI);
        codeTitle.setForeground(new Color(140, 140, 165));
        codeTitle.setAlignmentX(CENTER_ALIGNMENT);
        center.add(codeTitle);

        center.add(Box.createVerticalStrut(4));

        roomCodeLabel.setFont(new Font("Arial", Font.BOLD, 48));
        roomCodeLabel.setForeground(new Color(99, 179, 255));
        roomCodeLabel.setAlignmentX(CENTER_ALIGNMENT);
        center.add(roomCodeLabel);

        center.add(Box.createVerticalStrut(24));

        JPanel seatsContainer = glassPanel();
        seatsContainer.setLayout(new GridLayout(2, 2, 12, 12));
        seatsContainer.setBorder(new EmptyBorder(20, 20, 20, 20));
        seatsContainer.setMaximumSize(new Dimension(500, 280));
        seatsContainer.setAlignmentX(CENTER_ALIGNMENT);

        for (int i = 0; i < 4; i++) {
            seatsContainer.add(buildSeatPanel(i));
        }

        center.add(seatsContainer);
        center.add(Box.createVerticalStrut(16));

        statusLabel.setFont(FONT_BOLD);
        statusLabel.setForeground(new Color(255, 215, 80));
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);
        center.add(statusLabel);

        center.add(Box.createVerticalStrut(16));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setOpaque(false);
        startBtn.addActionListener(e -> { if (callback != null) callback.onStartGame(); });
        leaveBtn.addActionListener(e -> { if (callback != null) callback.onLeaveRoom(); });
        btnRow.add(startBtn);
        btnRow.add(leaveBtn);
        startBtn.setVisible(false);
        startBtn.setEnabled(false);

        center.add(btnRow);
        add(center, BorderLayout.CENTER);
    }

    private JPanel buildSeatPanel(int index) {
        JPanel panel = new JPanel(new BorderLayout(6, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 6));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(255, 255, 255, 15));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        panel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel nameLabel = new JLabel("空位", SwingConstants.LEFT);
        nameLabel.setFont(FONT_BOLD);
        nameLabel.setForeground(new Color(120, 120, 150));
        seatNameLabels[index] = nameLabel;

        JLabel statusLbl = new JLabel("座位 " + (index + 1), SwingConstants.LEFT);
        statusLbl.setFont(new Font("Noto Sans TC", Font.PLAIN, 11));
        statusLbl.setForeground(new Color(100, 100, 130));
        seatStatusLabels[index] = statusLbl;

        JPanel textPanel = new JPanel(new BorderLayout(0, 2));
        textPanel.setOpaque(false);
        textPanel.add(nameLabel, BorderLayout.NORTH);
        textPanel.add(statusLbl, BorderLayout.SOUTH);

        JButton aiBtn = pillButton("+ AI", new Color(70, 100, 200), Color.WHITE);
        aiBtn.setFont(new Font("Noto Sans TC", Font.BOLD, 11));
        aiBtn.setBorder(new EmptyBorder(4, 12, 4, 12));
        aiBtn.setVisible(false);
        final int slot = index;
        aiBtn.addActionListener(e -> {
            if (callback == null) return;
            if (seatIsAI[slot]) callback.onRemoveAI(slot);
            else callback.onAddAI(slot);
        });
        aiButtons[index] = aiBtn;

        panel.add(textPanel, BorderLayout.CENTER);
        panel.add(aiBtn, BorderLayout.EAST);
        seatPanels[index] = panel;

        return panel;
    }

    private void updateSeatDisplay(int index) {
        if (seatOccupied[index] || seatIsAI[index]) {
            seatNameLabels[index].setText(seatNames[index]);
            seatNameLabels[index].setForeground(seatIsAI[index] ? new Color(180, 140, 255) : new Color(100, 220, 150));
            seatStatusLabels[index].setText(seatIsAI[index] ? "AI" : "玩家");
            if (isCreator && seatIsAI[index]) {
                aiButtons[index].setText("移除");
                aiButtons[index].setVisible(true);
            } else {
                aiButtons[index].setVisible(false);
            }
        } else {
            seatNameLabels[index].setText("空位");
            seatNameLabels[index].setForeground(new Color(120, 120, 150));
            seatStatusLabels[index].setText("座位 " + (index + 1));
            if (isCreator) {
                aiButtons[index].setText("+ AI");
                aiButtons[index].setVisible(true);
            } else {
                aiButtons[index].setVisible(false);
            }
        }
        updateStartButton();
    }

    private void updateSeatButtons() {
        for (int i = 0; i < 4; i++) updateSeatDisplay(i);
    }

    private static JPanel glassPanel() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
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

    private static JButton pillButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            private boolean hovered;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
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
        btn.setBorder(new EmptyBorder(10, 28, 10, 28));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
