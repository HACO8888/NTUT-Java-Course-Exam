package com.draw.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * A custom color picker that shows a hue ring and a brightness/saturation square.
 */
public class HSBColorWheel extends JComponent {
    private float hue = 0f;
    private float saturation = 1f;
    private float brightness = 1f;

    private BufferedImage wheelCache;
    private int lastSize = -1;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private enum DragArea { NONE, RING, SQUARE }
    private DragArea dragging = DragArea.NONE;

    public HSBColorWheel() {
        setPreferredSize(new Dimension(200, 200));
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { handleMouse(e, true); }
            @Override public void mouseDragged(MouseEvent e) { handleMouse(e, false); }
            @Override public void mouseReleased(MouseEvent e) { dragging = DragArea.NONE; }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    private void handleMouse(MouseEvent e, boolean press) {
        int size = Math.min(getWidth(), getHeight());
        double cx = size / 2.0, cy = size / 2.0;
        double dx = e.getX() - cx, dy = e.getY() - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        double outerR = size / 2.0 - 2;
        double innerR = outerR * 0.65;

        if (press) {
            if (dist >= innerR && dist <= outerR) dragging = DragArea.RING;
            else if (dist < innerR) dragging = DragArea.SQUARE;
        }

        if (dragging == DragArea.RING) {
            Color old = getColor();
            hue = (float) (Math.atan2(dy, dx) / (2 * Math.PI));
            if (hue < 0) hue += 1f;
            wheelCache = null; // invalidate square part
            repaint();
            pcs.firePropertyChange("color", old, getColor());
        } else if (dragging == DragArea.SQUARE) {
            Color old = getColor();
            // Map inside circle to S/B
            double halfSide = innerR * Math.sqrt(2) / 2 * 0.9;
            saturation = (float) Math.max(0, Math.min(1, (dx + halfSide) / (2 * halfSide)));
            brightness = (float) Math.max(0, Math.min(1, 1 - (dy + halfSide) / (2 * halfSide)));
            repaint();
            pcs.firePropertyChange("color", old, getColor());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        int size = Math.min(getWidth(), getHeight());
        if (size != lastSize || wheelCache == null) {
            wheelCache = buildWheel(size);
            lastSize = size;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(wheelCache, 0, 0, null);

        double cx = size / 2.0, cy = size / 2.0;
        double outerR = size / 2.0 - 2;
        double innerR = outerR * 0.65;
        double halfSide = innerR * Math.sqrt(2) / 2 * 0.9;

        // Draw S/B square
        for (int y = (int)(cy - halfSide); y <= (int)(cy + halfSide); y++) {
            for (int x = (int)(cx - halfSide); x <= (int)(cx + halfSide); x++) {
                float s = (float)((x - (cx - halfSide)) / (2 * halfSide));
                float b = (float)(1 - (y - (cy - halfSide)) / (2 * halfSide));
                g2.setColor(Color.getHSBColor(hue, s, b));
                g2.fillRect(x, y, 1, 1);
            }
        }

        // Draw SB cursor
        int sx = (int)(cx - halfSide + saturation * 2 * halfSide);
        int sy = (int)(cy - halfSide + (1 - brightness) * 2 * halfSide);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(sx - 5, sy - 5, 10, 10);
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(sx - 5, sy - 5, 10, 10);

        // Draw hue cursor on ring
        double angle = hue * 2 * Math.PI;
        double midR = (outerR + innerR) / 2;
        int hx = (int)(cx + Math.cos(angle) * midR);
        int hy = (int)(cy + Math.sin(angle) * midR);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(hx - 5, hy - 5, 10, 10);
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(hx - 5, hy - 5, 10, 10);

        g2.dispose();
    }

    private BufferedImage buildWheel(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        double cx = size / 2.0, cy = size / 2.0;
        double outerR = size / 2.0 - 2;
        double innerR = outerR * 0.65;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x - cx, dy = y - cy;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > outerR) continue;
                if (dist >= innerR) {
                    float h = (float)(Math.atan2(dy, dx) / (2 * Math.PI));
                    if (h < 0) h += 1f;
                    Color c = Color.getHSBColor(h, 1f, 1f);
                    img.setRGB(x, y, c.getRGB());
                }
                // inner square will be drawn at paint time
            }
        }
        return img;
    }

    public Color getColor() {
        return Color.getHSBColor(hue, saturation, brightness);
    }

    public void setColor(Color c) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        wheelCache = null;
        repaint();
    }

    public float getHue() { return hue; }
    public float getSaturation() { return saturation; }
    public float getBrightness() { return brightness; }

    public void addPropertyChangeListener(PropertyChangeListener l) { pcs.addPropertyChangeListener(l); }
    public void removePropertyChangeListener(PropertyChangeListener l) { pcs.removePropertyChangeListener(l); }
}
