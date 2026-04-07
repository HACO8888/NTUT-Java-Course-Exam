package com.bigtwo.ui;

import com.bigtwo.model.Card;
import com.bigtwo.model.Suit;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class CardPanel extends JPanel {
    public static final int CARD_WIDTH  = 66;
    public static final int CARD_HEIGHT = 96;
    private static final int ARC = 12;
    public static final int SHADOW = 4;
    public static final int MAX_LIFT = 12; // max upward movement when selected

    private final Card card;
    private boolean selected;
    private boolean faceDown;
    private boolean hovered;

    public CardPanel(Card card, boolean faceDown) {
        this.card = card;
        this.faceDown = faceDown;
        setPreferredSize(new Dimension(CARD_WIDTH + SHADOW, CARD_HEIGHT + SHADOW + MAX_LIFT));
        setOpaque(false);
    }

    public Card getCard() { return card; }
    public boolean isSelected() { return selected; }

    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
        repaint();
    }

    public void setFaceDown(boolean faceDown) {
        this.faceDown = faceDown;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int lift = selected ? MAX_LIFT : (hovered ? 4 : 0);
        int cardX = 0;
        int cardY = MAX_LIFT - lift;   // normal: MAX_LIFT, selected: 0, hovered: MAX_LIFT-4
        int shadowY = MAX_LIFT;        // shadow stays at rest position

        // Drop shadow
        int shadowAlpha = selected ? 90 : 50;
        g2.setColor(new Color(0, 0, 0, shadowAlpha));
        g2.fillRoundRect(cardX + SHADOW, shadowY + SHADOW - lift / 2, CARD_WIDTH - 2, CARD_HEIGHT - 2, ARC, ARC);

        RoundRectangle2D shape = new RoundRectangle2D.Float(cardX, cardY, CARD_WIDTH - 2, CARD_HEIGHT - 2, ARC, ARC);

        if (faceDown) {
            // Back design
            GradientPaint gp = new GradientPaint(cardX, cardY, new Color(30, 60, 130),
                cardX + CARD_WIDTH, cardY + CARD_HEIGHT, new Color(20, 40, 100));
            g2.setPaint(gp);
            g2.fill(shape);
            // Subtle cross-hatch pattern
            g2.setColor(new Color(255, 255, 255, 18));
            g2.setStroke(new BasicStroke(1f));
            for (int i = cardX + 6; i < cardX + CARD_WIDTH - 4; i += 7) {
                g2.drawLine(i, cardY + 4, i, cardY + CARD_HEIGHT - 6);
            }
            for (int j = cardY + 6; j < cardY + CARD_HEIGHT - 4; j += 7) {
                g2.drawLine(cardX + 4, j, cardX + CARD_WIDTH - 6, j);
            }
            // Border
            g2.setColor(new Color(80, 120, 220, 180));
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(shape);
        } else {
            // Selection glow
            if (selected) {
                g2.setColor(new Color(80, 200, 255, 60));
                g2.fillRoundRect(cardX - 3, cardY - 3, CARD_WIDTH + 4, CARD_HEIGHT + 4, ARC + 4, ARC + 4);
            }

            // Card background - slight gradient
            GradientPaint bgGrad = new GradientPaint(cardX, cardY, Color.WHITE,
                cardX, cardY + CARD_HEIGHT, new Color(245, 245, 248));
            g2.setPaint(bgGrad);
            g2.fill(shape);

            // Border
            if (selected) {
                g2.setColor(new Color(50, 160, 255));
                g2.setStroke(new BasicStroke(2f));
            } else if (hovered) {
                g2.setColor(new Color(180, 180, 200));
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(new Color(210, 210, 218));
                g2.setStroke(new BasicStroke(1f));
            }
            g2.draw(shape);

            Color suitColor = getSuitColor();
            String rankStr = card.getRank().getSymbol();
            String suitStr = getSuitChar();

            // Clip to card shape
            g2.setClip(shape);

            // Top-left: rank
            g2.setColor(suitColor);
            g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.drawString(rankStr, cardX + 5, cardY + 15);

            // Top-left: suit (small)
            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            g2.drawString(suitStr, cardX + 5, cardY + 27);

            // Center large suit
            g2.setFont(new Font("Arial", Font.BOLD, 30));
            FontMetrics fm = g2.getFontMetrics();
            int cx = cardX + (CARD_WIDTH - 2 - fm.stringWidth(suitStr)) / 2;
            int cy = cardY + (CARD_HEIGHT - 2) / 2 + fm.getAscent() / 2 - 6;
            // Subtle shadow on center icon
            g2.setColor(new Color(suitColor.getRed(), suitColor.getGreen(), suitColor.getBlue(), 30));
            g2.drawString(suitStr, cx + 1, cy + 1);
            g2.setColor(suitColor);
            g2.drawString(suitStr, cx, cy);

            // Bottom-right: rotated rank + suit
            g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.setColor(suitColor);
            int bx = cardX + CARD_WIDTH - 7;
            int by = cardY + CARD_HEIGHT - 16;
            g2.translate(bx, by);
            g2.rotate(Math.PI);
            g2.drawString(rankStr, 0, 0);
            g2.rotate(-Math.PI);
            g2.translate(-bx, -by);

            g2.setClip(null);
        }

        g2.dispose();
    }

    private Color getSuitColor() {
        Suit suit = card.getSuit();
        if (suit == Suit.HEARTS)   return new Color(210, 35, 42);
        if (suit == Suit.DIAMONDS) return new Color(210, 35, 42);
        return new Color(28, 28, 35);
    }

    private String getSuitChar() {
        switch (card.getSuit()) {
            case CLUBS:    return "♣";
            case DIAMONDS: return "♦";
            case HEARTS:   return "♥";
            case SPADES:   return "♠";
            default:       return "?";
        }
    }

    public static int getCardWidth()  { return CARD_WIDTH; }
    public static int getCardHeight() { return CARD_HEIGHT; }
}
