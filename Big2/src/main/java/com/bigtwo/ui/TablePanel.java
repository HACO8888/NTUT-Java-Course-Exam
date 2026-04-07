package com.bigtwo.ui;

import com.bigtwo.model.Card;
import com.bigtwo.model.Play;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

public class TablePanel extends JPanel {
    private Play currentPlay;
    private String playerName = "";
    private String handTypeName = "";

    public TablePanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(420, 200));
    }

    public void setPlay(Play play, String playerName, String handTypeName) {
        this.currentPlay = play;
        this.playerName = playerName;
        this.handTypeName = handTypeName;
        repaint();
    }

    public void setPlay(Play play) {
        setPlay(play, "", "");
    }

    public void setLabel(String label) {
        // kept for compatibility; parse into playerName + handTypeName if needed
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth(), h = getHeight();

        // Dark glass background
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRoundRect(0, 0, w, h, 24, 24);
        g2.setColor(new Color(255, 255, 255, 15));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, 24, 24);

        if (currentPlay == null || currentPlay.isPass()) {
            // Empty state
            g2.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 15));
            g2.setColor(new Color(255, 255, 255, 60));
            String msg = "桌面空白 — 等待出牌";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2 + fm.getAscent() / 2 - 4);
        } else {
            List<Card> cards = currentPlay.getCards();
            int n = cards.size();
            int cw = CardPanel.CARD_WIDTH - 2;
            int ch = CardPanel.CARD_HEIGHT - 2;
            int gap = 8;
            int totalW = n * cw + (n - 1) * gap;
            int startX = (w - totalW) / 2;
            int startY = (h - ch) / 2 - 6;

            // Draw shadow batch first
            g2.setColor(new Color(0, 0, 0, 40));
            for (int i = 0; i < n; i++) {
                g2.fillRoundRect(startX + i * (cw + gap) + 3, startY + 4, cw, ch, 12, 12);
            }
            // Draw cards
            for (int i = 0; i < n; i++) {
                drawCard(g2, cards.get(i), startX + i * (cw + gap), startY, cw, ch);
            }

            // Player + hand type label below cards
            if (!playerName.isEmpty()) {
                g2.setFont(new Font("Microsoft JhengHei", Font.BOLD, 12));
                String info = playerName + " · " + handTypeName;
                FontMetrics fm = g2.getFontMetrics();
                int lx = (w - fm.stringWidth(info)) / 2;
                int ly = startY + ch + 18;

                // Pill background
                g2.setColor(new Color(255, 255, 255, 20));
                g2.fillRoundRect(lx - 10, ly - fm.getAscent() - 2, fm.stringWidth(info) + 20, fm.getHeight() + 4, 10, 10);

                g2.setColor(new Color(220, 220, 255));
                g2.drawString(info, lx, ly);
            }
        }

        g2.dispose();
    }

    private void drawCard(Graphics2D g2, Card card, int x, int y, int w, int h) {
        RoundRectangle2D shape = new RoundRectangle2D.Float(x, y, w, h, 12, 12);

        GradientPaint bg = new GradientPaint(x, y, Color.WHITE, x, y + h, new Color(244, 244, 248));
        g2.setPaint(bg);
        g2.fill(shape);
        g2.setColor(new Color(210, 210, 220));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(shape);

        Color suitColor = getSuitColor(card);
        String rankStr = card.getRank().getSymbol();
        String suitStr = getSuitChar(card);

        g2.setClip(shape);

        g2.setColor(suitColor);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString(rankStr, x + 4, y + 14);
        g2.setFont(new Font("Arial", Font.PLAIN, 10));
        g2.drawString(suitStr, x + 4, y + 25);

        g2.setFont(new Font("Arial", Font.BOLD, 28));
        FontMetrics fm = g2.getFontMetrics();
        int cx = x + (w - fm.stringWidth(suitStr)) / 2;
        int cy = y + h / 2 + fm.getAscent() / 2 - 6;
        g2.setColor(new Color(suitColor.getRed(), suitColor.getGreen(), suitColor.getBlue(), 25));
        g2.drawString(suitStr, cx + 1, cy + 1);
        g2.setColor(suitColor);
        g2.drawString(suitStr, cx, cy);

        g2.setClip(null);
    }

    private Color getSuitColor(Card card) {
        switch (card.getSuit()) {
            case HEARTS:
            case DIAMONDS: return new Color(210, 35, 42);
            default:       return new Color(28, 28, 35);
        }
    }

    private String getSuitChar(Card card) {
        switch (card.getSuit()) {
            case CLUBS:    return "♣";
            case DIAMONDS: return "♦";
            case HEARTS:   return "♥";
            case SPADES:   return "♠";
            default:       return "?";
        }
    }
}
