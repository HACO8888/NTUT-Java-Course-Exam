package com.draw2.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

public class ColorPalette extends JPanel {

    // Classic MS Paint 28-color palette (2 rows × 14 cols)
    private static final Color[] PALETTE = {
        // Row 1 – dark tones
        new Color(0,0,0),       new Color(128,128,128), new Color(128,0,0),    new Color(128,128,0),
        new Color(0,128,0),     new Color(0,128,128),   new Color(0,0,128),    new Color(128,0,128),
        new Color(128,128,64),  new Color(0,64,64),     new Color(0,128,255),  new Color(0,64,128),
        new Color(128,64,0),    new Color(64,0,0),
        // Row 2 – light/bright tones
        new Color(255,255,255), new Color(192,192,192), new Color(255,0,0),    new Color(255,255,0),
        new Color(0,255,0),     new Color(0,255,255),   new Color(0,0,255),    new Color(255,0,255),
        new Color(255,255,128), new Color(0,255,128),   new Color(128,255,255),new Color(128,128,255),
        new Color(255,0,128),   new Color(255,128,64)
    };

    private Color foreColor = Color.BLACK;
    private Color backColor = Color.WHITE;

    private Consumer<Color> fgCallback = c -> {};
    private Consumer<Color> bgCallback = c -> {};

    private final FgBgWidget fgBgWidget;

    public ColorPalette() {
        setLayout(new BorderLayout(4, 0));
        setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
        setPreferredSize(new Dimension(0, 46));

        fgBgWidget = new FgBgWidget();
        add(fgBgWidget, BorderLayout.WEST);

        JPanel grid = new JPanel(new GridLayout(2, 14, 1, 1));
        grid.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        for (Color c : PALETTE) {
            grid.add(new ColorSwatch(c));
        }
        add(grid, BorderLayout.CENTER);
    }

    // ── public API ────────────────────────────────────────────────────────────

    public Color getForeColor() { return foreColor; }
    public Color getBackColor() { return backColor; }

    public void setForeColor(Color c) {
        foreColor = c;
        fgBgWidget.repaint();
        fgCallback.accept(c);
    }

    public void setBackColor(Color c) {
        backColor = c;
        fgBgWidget.repaint();
        bgCallback.accept(c);
    }

    public void setFgCallback(Consumer<Color> cb) { fgCallback = cb; }
    public void setBgCallback(Consumer<Color> cb) { bgCallback = cb; }

    public void swapColors() {
        Color tmp = foreColor;
        foreColor = backColor;
        backColor = tmp;
        fgBgWidget.repaint();
        fgCallback.accept(foreColor);
        bgCallback.accept(backColor);
    }

    // ── inner: FG/BG overlapping swatch ──────────────────────────────────────

    private class FgBgWidget extends JComponent {
        FgBgWidget() {
            setPreferredSize(new Dimension(52, 40));
            setToolTipText("左鍵：前景色  右鍵：背景色");
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        Color c = JColorChooser.showDialog(ColorPalette.this, "選擇前景色", foreColor);
                        if (c != null) setForeColor(c);
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        Color c = JColorChooser.showDialog(ColorPalette.this, "選擇背景色", backColor);
                        if (c != null) setBackColor(c);
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            // Background (secondary) swatch — offset
            g2.setColor(backColor);
            g2.fillRect(14, 12, 26, 26);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRect(14, 12, 26, 26);
            // Foreground (primary) swatch — on top
            g2.setColor(foreColor);
            g2.fillRect(2, 2, 26, 26);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRect(2, 2, 26, 26);
            g2.dispose();
        }
    }

    // ── inner: individual color swatch ───────────────────────────────────────

    private class ColorSwatch extends JComponent {
        private final Color color;
        private boolean hover;

        ColorSwatch(Color color) {
            this.color = color;
            setPreferredSize(new Dimension(18, 18));
            setToolTipText(String.format("RGB(%d,%d,%d)", color.getRed(), color.getGreen(), color.getBlue()));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }

                @Override public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        setForeColor(color);
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        setBackColor(color);
                    }
                }

                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        // double-click: custom color
                        Color c = JColorChooser.showDialog(ColorPalette.this, "選擇顏色", color);
                        if (c != null) {
                            if (SwingUtilities.isLeftMouseButton(e)) setForeColor(c);
                            else                                      setBackColor(c);
                        }
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(color);
            g.fillRect(0, 0, getWidth(), getHeight());
            if (hover) {
                g.setColor(Color.WHITE);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g.setColor(Color.DARK_GRAY);
                g.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
            } else {
                g.setColor(Color.DARK_GRAY);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        }
    }
}
