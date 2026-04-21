package com.bigtwo.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LobbyPanel extends JPanel {

    private static final Color BG_TOP = new Color(18, 22, 38);
    private static final Color BG_BOT = new Color(10, 14, 26);
    private static final Color PANEL_BG = new Color(255, 255, 255, 10);
    private static final Color DIVIDER = new Color(255, 255, 255, 18);
    private static final Font FONT_TITLE = new Font("Microsoft JhengHei", Font.BOLD, 36);
    private static final Font FONT_SUBTITLE = new Font("Microsoft JhengHei", Font.PLAIN, 14);
    private static final Font FONT_BOLD = new Font("Microsoft JhengHei", Font.BOLD, 14);
    private static final Font FONT_UI = new Font("Microsoft JhengHei", Font.PLAIN, 13);

    private final JTextField nameField = styledField("請輸入你的名稱");
    private final JTextField serverField = styledField("api-big2.haco.tw");
    private final JTextField roomCodeField = styledField("輸入房間代碼");

    public interface LobbyCallback {
        void onCreateRoom(String playerName, String serverAddress);
        void onJoinRoom(String playerName, String serverAddress, String roomCode);
        void onOfflineMode();
    }

    private LobbyCallback callback;

    public LobbyPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        buildUI();
    }

    public void setCallback(LobbyCallback cb) { this.callback = cb; }

    private void buildUI() {
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(80, 0, 0, 0));

        JLabel title = new JLabel("大老二 Online");
        title.setFont(FONT_TITLE);
        title.setForeground(new Color(235, 235, 245));
        title.setAlignmentX(CENTER_ALIGNMENT);
        center.add(title);

        center.add(Box.createVerticalStrut(6));

        JLabel subtitle = new JLabel("線上多人大老二");
        subtitle.setFont(FONT_SUBTITLE);
        subtitle.setForeground(new Color(140, 140, 165));
        subtitle.setAlignmentX(CENTER_ALIGNMENT);
        center.add(subtitle);

        center.add(Box.createVerticalStrut(40));

        JPanel form = glassPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(30, 40, 30, 40));
        form.setMaximumSize(new Dimension(420, 500));
        form.setAlignmentX(CENTER_ALIGNMENT);

        form.add(fieldLabel("玩家名稱"));
        form.add(Box.createVerticalStrut(6));
        form.add(nameField);
        form.add(Box.createVerticalStrut(16));

        form.add(fieldLabel("伺服器位址"));
        form.add(Box.createVerticalStrut(6));
        form.add(serverField);
        form.add(Box.createVerticalStrut(24));

        JButton createBtn = pillButton("建立房間", new Color(56, 189, 110), Color.WHITE);
        createBtn.setAlignmentX(CENTER_ALIGNMENT);
        createBtn.setMaximumSize(new Dimension(340, 44));
        createBtn.addActionListener(e -> {
            if (callback != null) callback.onCreateRoom(nameField.getText().trim(), serverField.getText().trim());
        });
        form.add(createBtn);
        form.add(Box.createVerticalStrut(12));

        form.add(fieldLabel("房間代碼"));
        form.add(Box.createVerticalStrut(6));
        form.add(roomCodeField);
        form.add(Box.createVerticalStrut(10));

        JButton joinBtn = pillButton("加入房間", new Color(99, 179, 255), Color.WHITE);
        joinBtn.setAlignmentX(CENTER_ALIGNMENT);
        joinBtn.setMaximumSize(new Dimension(340, 44));
        joinBtn.addActionListener(e -> {
            if (callback != null)
                callback.onJoinRoom(nameField.getText().trim(), serverField.getText().trim(), roomCodeField.getText().trim());
        });
        form.add(joinBtn);
        form.add(Box.createVerticalStrut(20));

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(340, 1));
        sep.setForeground(new Color(255, 255, 255, 30));
        form.add(sep);
        form.add(Box.createVerticalStrut(16));

        JButton offlineBtn = pillButton("單機模式", new Color(80, 80, 120), Color.WHITE);
        offlineBtn.setAlignmentX(CENTER_ALIGNMENT);
        offlineBtn.setMaximumSize(new Dimension(340, 44));
        offlineBtn.addActionListener(e -> {
            if (callback != null) callback.onOfflineMode();
        });
        form.add(offlineBtn);

        center.add(form);
        add(center, BorderLayout.CENTER);
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BOLD);
        l.setForeground(new Color(180, 180, 210));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JTextField styledField(String placeholder) {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 8));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(255, 255, 255, 25));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        field.setOpaque(false);
        field.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 14));
        field.setForeground(new Color(230, 230, 245));
        field.setCaretColor(new Color(99, 179, 255));
        field.setBorder(new EmptyBorder(10, 14, 10, 14));
        field.setMaximumSize(new Dimension(340, 42));
        field.setAlignmentX(LEFT_ALIGNMENT);

        field.putClientProperty("placeholder", placeholder);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(new Color(230, 230, 245));
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(new Color(120, 120, 150));
                }
            }
        });
        if (placeholder.startsWith("localhost") || placeholder.contains("haco.tw")) {
            field.setForeground(new Color(230, 230, 245));
        } else {
            field.setText(placeholder);
            field.setForeground(new Color(120, 120, 150));
        }

        return field;
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
            @Override
            public boolean isOpaque() { return false; }
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
            @Override
            protected void paintBorder(Graphics g) {}
        };
        btn.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setBorder(new EmptyBorder(10, 28, 10, 28));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public String getFieldText(JTextField field) {
        String text = field.getText();
        String placeholder = (String) field.getClientProperty("placeholder");
        if (text.equals(placeholder)) return "";
        return text;
    }
}
