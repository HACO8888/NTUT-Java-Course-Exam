package com.bigtwo.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;

/**
 * Draws face-down cards in a fan/arc layout for AI players.
 */
public class FanPanel extends JPanel {
    private static final int CW  = 52;   // card width
    private static final int CH  = 76;   // card height
    private static final int ARC = 10;
    private static final int RADIUS = 200;
    private static final double MAX_SPREAD_DEG = 44.0;

    private int cardCount = 0;

    public FanPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(260, 110));
    }

    public void setCardCount(int count) {
        this.cardCount = count;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (cardCount == 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        double pivotX = w / 2.0;
        double pivotY = h + RADIUS - 20; // pivot below visible area

        double spreadRad = Math.toRadians(Math.min(MAX_SPREAD_DEG, cardCount * 3.8));

        // Draw shadows first
        for (int i = 0; i < cardCount; i++) {
            double t = cardCount > 1 ? i / (double)(cardCount - 1) : 0.5;
            double angle = -spreadRad / 2.0 + t * spreadRad;

            double cx = pivotX + RADIUS * Math.sin(angle);
            double cy = pivotY - RADIUS * Math.cos(angle);

            AffineTransform old = g2.getTransform();
            g2.translate(cx, cy);
            g2.rotate(angle);
            g2.setColor(new Color(0, 0, 0, 35));
            g2.fillRoundRect(-CW / 2 + 3, -CH / 2 + 4, CW, CH, ARC, ARC);
            g2.setTransform(old);
        }

        // Draw cards
        for (int i = 0; i < cardCount; i++) {
            double t = cardCount > 1 ? i / (double)(cardCount - 1) : 0.5;
            double angle = -spreadRad / 2.0 + t * spreadRad;

            double cx = pivotX + RADIUS * Math.sin(angle);
            double cy = pivotY - RADIUS * Math.cos(angle);

            AffineTransform old = g2.getTransform();
            g2.translate(cx, cy);
            g2.rotate(angle);
            drawCardBack(g2);
            g2.setTransform(old);
        }

        // Card count badge (bottom right)
        String countStr = String.valueOf(cardCount);
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        int bw = fm.stringWidth(countStr) + 14;
        int bh = 22;
        int bx = w - bw - 6;
        int by = h - bh - 4;
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(bx, by, bw, bh, bh, bh);
        g2.setColor(new Color(200, 200, 220));
        g2.drawString(countStr, bx + 7, by + bh - 6);

        g2.dispose();
    }

    private void drawCardBack(Graphics2D g2) {
        // Card background
        GradientPaint gp = new GradientPaint(-CW/2f, -CH/2f, new Color(35, 65, 145),
                                              CW/2f,  CH/2f,  new Color(20, 42, 105));
        g2.setPaint(gp);
        g2.fillRoundRect(-CW / 2, -CH / 2, CW, CH, ARC, ARC);

        // Grid pattern
        g2.setColor(new Color(255, 255, 255, 20));
        g2.setStroke(new BasicStroke(0.8f));
        for (int x = -CW/2 + 5; x < CW/2 - 3; x += 7) {
            g2.drawLine(x, -CH/2 + 3, x, CH/2 - 3);
        }
        for (int y = -CH/2 + 5; y < CH/2 - 3; y += 7) {
            g2.drawLine(-CW/2 + 3, y, CW/2 - 3, y);
        }

        // Border
        g2.setColor(new Color(80, 120, 230, 160));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(-CW / 2, -CH / 2, CW - 1, CH - 1, ARC, ARC);
    }
}
