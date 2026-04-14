package com.draw.util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/**
 * Procedurally generated 24x24 icons for tool buttons.
 */
public class DrawIcons {

    public static ImageIcon pencil(Color color) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Pencil body
            int[] xp = {6, 18, 16, 4};
            int[] yp = {18, 6, 4, 16};
            g.drawPolygon(xp, yp, 4);
            // Tip
            g.drawLine(4, 16, 3, 21);
            g.drawLine(6, 18, 3, 21);
            // Eraser band
            g.setStroke(new BasicStroke(1.2f));
            g.drawLine(15, 5, 17, 7);
        });
    }

    public static ImageIcon brush(Color color) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Handle
            g.drawLine(5, 19, 15, 9);
            // Ferrule
            g.drawLine(14, 8, 16, 10);
            // Bristles
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(15, 9, 20, 4);
            g.setStroke(new BasicStroke(1.6f));
            g.drawOval(17, 6, 5, 5);
        });
    }

    public static ImageIcon eraser(Color color) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Eraser body
            g.drawRoundRect(4, 12, 16, 8, 3, 3);
            // Pink part
            g.fillRoundRect(4, 12, 8, 8, 3, 3);
            // Line dividing halves
            g.setColor(color.darker());
            g.drawLine(12, 12, 12, 20);
            g.setColor(color);
            // Handle line above
            g.drawLine(8, 8, 18, 8);
            g.drawLine(8, 8, 4, 12);
            g.drawLine(18, 8, 20, 12);
        });
    }

    public static ImageIcon line(Color color) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(5, 19, 19, 5);
            g.fillOval(4, 18, 3, 3);
            g.fillOval(18, 4, 3, 3);
        });
    }

    public static ImageIcon rectangle(Color color) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawRect(4, 6, 16, 12);
        });
    }

    public static ImageIcon ellipse(Color color) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawOval(3, 6, 18, 13);
        });
    }

    public static ImageIcon polygon(Color color) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int[] xs = {12, 20, 17, 7, 4};
            int[] ys = {3,  10, 20, 20, 10};
            g.drawPolygon(xs, ys, 5);
        });
    }

    public static ImageIcon fill(Color color) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Bucket body
            int[] bx = {8, 16, 18, 6};
            int[] by = {8, 8, 19, 19};
            g.drawPolygon(bx, by, 4);
            // Handle
            g.drawArc(8, 3, 8, 8, 0, 180);
            // Drop
            g.fillOval(18, 13, 5, 6);
        });
    }

    public static ImageIcon text(Color color) {
        return make(color, g -> {
            g.setFont(new Font("Serif", Font.BOLD, 16));
            FontMetrics fm = g.getFontMetrics();
            String t = "T";
            int x = (24 - fm.stringWidth(t)) / 2;
            int y = (24 + fm.getAscent() - fm.getDescent()) / 2;
            g.drawString(t, x, y);
            g.setStroke(new BasicStroke(1.5f));
            g.drawLine(4, 20, 20, 20);
        });
    }

    public static ImageIcon zoomIn(Color color) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawOval(4, 4, 13, 13);
            g.drawLine(15, 15, 20, 20);
            g.drawLine(10, 10, 12, 10);
            g.drawLine(11, 9, 11, 11);
        });
    }

    public static ImageIcon zoomOut(Color color) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawOval(4, 4, 13, 13);
            g.drawLine(15, 15, 20, 20);
            g.drawLine(9, 10, 13, 10);
        });
    }

    public static ImageIcon grid(Color color) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 4; i <= 20; i += 8) {
                g.drawLine(i, 4, i, 20);
                g.drawLine(4, i, 20, i);
            }
        });
    }

    public static ImageIcon layerAdd(Color color) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawRect(4, 8, 14, 10);
            g.drawLine(10, 2, 10, 7);
            g.drawLine(7, 4, 13, 4);
        });
    }

    public static ImageIcon layerDelete(Color color) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawRect(4, 8, 14, 10);
            g.drawLine(9, 12, 13, 16);
            g.drawLine(13, 12, 9, 16);
        });
    }

    public static ImageIcon eye(Color color, boolean visible) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (visible) {
                g.drawArc(2, 7, 20, 10, 0, 180);
                g.drawArc(2, 7, 20, 10, 180, 180);
                g.fillOval(9, 10, 6, 6);
                g.setColor(Color.BLACK);
                g.fillOval(11, 12, 2, 2);
            } else {
                g.drawLine(4, 4, 20, 20);
                g.drawArc(2, 7, 20, 10, 0, 180);
                g.drawArc(2, 7, 20, 10, 180, 180);
            }
        });
    }

    public static ImageIcon eyedropper(Color color) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Dropper body
            g.drawLine(5, 19, 12, 12);
            g.drawLine(12, 12, 18, 6);
            g.drawLine(14, 4, 20, 10);
            g.drawLine(18, 6, 20, 10);
            g.drawLine(14, 4, 18, 6);
            // Tip bulb
            g.fillOval(3, 17, 4, 4);
        });
    }

    public static ImageIcon spray(Color color) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Can body
            g.drawRoundRect(9, 10, 10, 12, 3, 3);
            // Nozzle
            g.drawLine(6, 8, 9, 10);
            g.drawLine(6, 8, 7, 6);
            // Dots representing spray
            g.fillOval(2, 3, 2, 2);
            g.fillOval(5, 2, 2, 2);
            g.fillOval(2, 7, 2, 2);
            g.fillOval(5, 6, 2, 2);
        });
    }

    public static ImageIcon move(Color color) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Four arrows
            g.drawLine(12, 3, 12, 21);  // vertical
            g.drawLine(3, 12, 21, 12);  // horizontal
            // Arrow tips
            int[] upX = {9, 12, 15};    int[] upY = {7, 3, 7};
            int[] dnX = {9, 12, 15};    int[] dnY = {17, 21, 17};
            int[] ltX = {7, 3, 7};      int[] ltY = {9, 12, 15};
            int[] rtX = {17, 21, 17};   int[] rtY = {9, 12, 15};
            g.drawPolyline(upX, upY, 3);
            g.drawPolyline(dnX, dnY, 3);
            g.drawPolyline(ltX, ltY, 3);
            g.drawPolyline(rtX, rtY, 3);
        });
    }

    public static ImageIcon crop(Color color) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Corner handles style
            g.drawLine(4, 4, 4, 16);
            g.drawLine(4, 16, 16, 16);
            g.drawLine(8, 4, 20, 4);
            g.drawLine(20, 4, 20, 16);
            // Dashed crop box
            float[] dash = {3f, 3f};
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, dash, 0f));
            g.drawRect(8, 8, 12, 12);
        });
    }

    public static ImageIcon undo(Color color) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(5, 7, 14, 11, 0, 200);
            int[] ax = {5, 4, 9}; int[] ay = {7, 12, 11};
            g.drawPolyline(ax, ay, 3);
        });
    }

    public static ImageIcon redo(Color color) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(5, 7, 14, 11, -20, -200);
            int[] ax = {19, 20, 15}; int[] ay = {7, 12, 11};
            g.drawPolyline(ax, ay, 3);
        });
    }

    public static ImageIcon select(Color color) {
        return make(color, g -> {
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    1f, new float[]{4f, 3f}, 0f));
            g.drawRect(4, 4, 16, 16);
            // Corner handles
            g.setStroke(new BasicStroke(1.5f));
            g.fillRect(3, 3, 3, 3);
            g.fillRect(18, 3, 3, 3);
            g.fillRect(3, 18, 3, 3);
            g.fillRect(18, 18, 3, 3);
        });
    }

    public static ImageIcon gradient(Color color) {
        return make(color, g -> {
            // Gradient bar
            for (int x = 4; x <= 20; x++) {
                float t = (x - 4f) / 16f;
                int alpha = (int)(255 * (1 - t));
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
                g.fillRect(x, 8, 1, 8);
            }
            g.setColor(color);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Arrow line
            g.drawLine(4, 12, 20, 12);
            // Arrowhead
            g.drawLine(16, 8, 20, 12);
            g.drawLine(16, 16, 20, 12);
        });
    }

    // --- helpers ---

    @FunctionalInterface
    private interface IconPainter {
        void paint(Graphics2D g);
    }

    private static ImageIcon make(Color color, IconPainter painter) {
        BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(color);
        painter.paint(g);
        g.dispose();
        return new ImageIcon(img);
    }
}
