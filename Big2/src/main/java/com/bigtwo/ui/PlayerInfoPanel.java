package com.bigtwo.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

public class PlayerInfoPanel extends JPanel {
    private static final Color ACTIVE_BG   = new Color(255, 200, 50, 40);
    private static final Color INACTIVE_BG = new Color(0, 0, 0, 50);
    private static final Color ACTIVE_RING = new Color(255, 200, 50);

    private String playerName;
    private int cardCount;
    private int wins;
    private boolean active;
    private final Color avatarColor;
    private final String initials;

    private final JLabel nameLabel  = new JLabel();
    private final JLabel countLabel = new JLabel();

    private static final Color[] AVATAR_COLORS = {
        new Color(80,  170, 120),  // you
        new Color(100, 140, 220),  // AI A
        new Color(200, 120,  60),  // AI B
        new Color(180,  80, 140),  // AI C
    };

    public PlayerInfoPanel(String name, int playerIdx) {
        this.playerName  = name;
        this.avatarColor = AVATAR_COLORS[playerIdx % AVATAR_COLORS.length];
        this.initials    = name.substring(0, 1);

        setOpaque(false);
        setLayout(new BorderLayout(8, 0));
        setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 12));

        nameLabel.setFont(new Font("Microsoft JhengHei", Font.BOLD, 13));
        nameLabel.setForeground(Color.WHITE);

        countLabel.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 11));
        countLabel.setForeground(new Color(180, 180, 200));

        // Avatar placeholder (actual drawing done in paintComponent)
        JPanel avatarPlaceholder = new JPanel() {
            @Override public Dimension getPreferredSize() { return new Dimension(36, 36); }
            @Override protected void paintComponent(Graphics g) {}
        };
        avatarPlaceholder.setOpaque(false);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 1));
        textPanel.setOpaque(false);
        textPanel.add(nameLabel);
        textPanel.add(countLabel);

        add(avatarPlaceholder, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);
    }

    public void update(String name, int cardCount, boolean active) {
        this.playerName = name;
        this.cardCount  = cardCount;
        this.active     = active;
        nameLabel.setText(name);
        nameLabel.setForeground(active ? new Color(255, 220, 60) : Color.WHITE);
        countLabel.setText(cardCount + " 張");
        repaint();
    }

    public void addWin() {
        wins++;
        repaint();
    }

    public void resetWins() {
        wins = 0;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        // Background pill
        g2.setColor(active ? ACTIVE_BG : INACTIVE_BG);
        g2.fillRoundRect(0, 0, w, h, 20, 20);
        if (active) {
            g2.setColor(ACTIVE_RING);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 20, 20);
        }

        // Avatar circle
        int av = 32, ax = 8, ay = (h - av) / 2;
        g2.setColor(avatarColor);
        g2.fill(new Ellipse2D.Float(ax, ay, av, av));
        if (active) {
            g2.setColor(ACTIVE_RING);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new Ellipse2D.Float(ax, ay, av, av));
        }

        // Initial
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Microsoft JhengHei", Font.BOLD, 15));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(initials, ax + (av - fm.stringWidth(initials)) / 2,
                      ay + av / 2 + fm.getAscent() / 2 - 2);

        // Active dot
        if (active) {
            g2.setColor(new Color(80, 255, 80));
            g2.fillOval(ax + av - 9, ay + av - 9, 10, 10);
            g2.setColor(new Color(30, 180, 30));
            g2.setStroke(new BasicStroke(1f));
            g2.drawOval(ax + av - 9, ay + av - 9, 10, 10);
        }

        // Win badge (top-right of avatar)
        if (wins > 0) {
            int bx = ax + av - 6;
            int by = ay - 6;
            int bs = 18;
            g2.setColor(new Color(255, 180, 30));
            g2.fillOval(bx, by, bs, bs);
            g2.setColor(new Color(100, 60, 0));
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            FontMetrics wfm = g2.getFontMetrics();
            String ws = String.valueOf(wins);
            g2.drawString(ws, bx + (bs - wfm.stringWidth(ws)) / 2, by + bs - 4);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
