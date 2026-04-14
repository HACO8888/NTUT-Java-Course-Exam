package com.draw.ui.panel;

import com.draw.util.HSBColorWheel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Color picker panel with foreground/background swatches, HSB wheel, hex input,
 * opacity slider, and recent color swatches.
 *
 * - Click the FG (front) swatch to edit the primary drawing color.
 * - Click the BG (back) swatch to edit the secondary/background color.
 * - Press the ⇄ button (or X key via MainWindow) to swap FG ↔ BG.
 */
public class ColorPanel extends JPanel {

    private final HSBColorWheel wheel = new HSBColorWheel();
    private final JTextField hexField = new JTextField("#000000", 8);
    private final JPanel swatchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
    private final JSlider alphaSlider = makeSlider(0, 255, 255);

    private final List<Color> recentColors = new ArrayList<>();
    private static final int MAX_RECENT = 10;

    // Foreground / background
    private Color fgColor = Color.BLACK;
    private Color bgColor = Color.WHITE;
    private boolean editingFg = true;  // true = wheel edits FG, false = wheel edits BG

    private final FgBgSwatchWidget fgBgWidget = new FgBgSwatchWidget();

    private Consumer<Color> fgColorCallback;
    private Consumer<Color> bgColorCallback;

    private boolean updating = false;

    public ColorPanel() {
        setBackground(new Color(0x252526));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // --- FG/BG swatch row ---
        JPanel swatchRow = new JPanel();
        swatchRow.setBackground(new Color(0x252526));
        swatchRow.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
        swatchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        swatchRow.add(fgBgWidget);

        JButton swapBtn = new JButton("⇄");
        swapBtn.setFont(swapBtn.getFont().deriveFont(Font.PLAIN, 14f));
        swapBtn.setPreferredSize(new Dimension(30, 24));
        swapBtn.setFocusPainted(false);
        swapBtn.setToolTipText("Swap foreground/background (X)");
        swapBtn.addActionListener(e -> swapColors());
        swatchRow.add(swapBtn);

        JLabel fgLabel = new JLabel("FG");
        fgLabel.setForeground(new Color(0x888888));
        fgLabel.setFont(fgLabel.getFont().deriveFont(10f));
        swatchRow.add(fgLabel);

        add(swatchRow);
        add(Box.createVerticalStrut(6));

        // --- Color wheel ---
        wheel.setAlignmentX(CENTER_ALIGNMENT);
        wheel.setMaximumSize(new Dimension(200, 200));
        add(wheel);
        add(Box.createVerticalStrut(6));

        // --- Hex field ---
        JPanel hexRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        hexRow.setBackground(new Color(0x252526));
        hexRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        hexField.setBackground(new Color(0x3c3c3c));
        hexField.setForeground(new Color(0xCCCCCC));
        hexField.setCaretColor(new Color(0xCCCCCC));
        hexField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x555555)),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        hexField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        hexRow.add(new JLabel("Hex") {{
            setForeground(new Color(0xAAAAAA));
            setFont(getFont().deriveFont(11f));
        }});
        hexRow.add(hexField);
        add(hexRow);
        add(Box.createVerticalStrut(6));

        // --- Alpha slider ---
        JPanel alphaRow = new JPanel(new BorderLayout(4, 0));
        alphaRow.setBackground(new Color(0x252526));
        JLabel alphaLabel = new JLabel("Alpha");
        alphaLabel.setForeground(new Color(0xAAAAAA));
        alphaLabel.setFont(alphaLabel.getFont().deriveFont(11f));
        alphaRow.add(alphaLabel, BorderLayout.WEST);
        alphaRow.add(alphaSlider, BorderLayout.CENTER);
        alphaRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        add(alphaRow);
        add(Box.createVerticalStrut(6));

        // --- Recent colors ---
        JLabel recentLabel = new JLabel("Recent");
        recentLabel.setForeground(new Color(0xAAAAAA));
        recentLabel.setFont(recentLabel.getFont().deriveFont(11f));
        recentLabel.setAlignmentX(LEFT_ALIGNMENT);
        add(recentLabel);
        swatchPanel.setBackground(new Color(0x252526));
        swatchPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        add(swatchPanel);

        // --- Listeners ---
        wheel.addPropertyChangeListener("color", e -> {
            if (updating) return;
            Color c = applyAlpha(wheel.getColor());
            setActiveColor(c, false);
        });

        hexField.addActionListener(e -> {
            try {
                String hex = hexField.getText().replace("#", "");
                Color base = Color.decode("#" + hex);
                Color c = new Color(base.getRed(), base.getGreen(), base.getBlue(), alphaSlider.getValue());
                setActiveColor(c, true);
            } catch (Exception ignored) {}
        });

        alphaSlider.addChangeListener(e -> {
            if (updating) return;
            Color base = wheel.getColor();
            setActiveColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), alphaSlider.getValue()), true);
        });

        fgBgWidget.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                boolean clickedFg = fgBgWidget.isClickOnFg(e.getX(), e.getY());
                boolean clickedBg = fgBgWidget.isClickOnBg(e.getX(), e.getY());
                if (clickedFg && !clickedBg) {
                    editingFg = true;
                } else if (clickedBg && !clickedFg) {
                    editingFg = false;
                } else if (clickedFg) {
                    editingFg = true; // overlap → prefer FG
                }
                syncWheelToActive();
                fgBgWidget.repaint();
            }
        });
    }

    // ---- Public API ----

    public void setFgColorCallback(Consumer<Color> cb) { this.fgColorCallback = cb; }
    public void setBgColorCallback(Consumer<Color> cb) { this.bgColorCallback = cb; }

    /** Legacy single-callback support (fires on FG change). */
    public void setColorCallback(Consumer<Color> cb) { this.fgColorCallback = cb; }

    public Color getCurrentColor() { return fgColor; }
    public Color getFgColor()      { return fgColor; }
    public Color getBgColor()      { return bgColor; }

    public void setColorFromExternal(Color c) {
        fgColor = c;
        if (fgColorCallback != null) fgColorCallback.accept(fgColor);
        addRecentColor(c);
        editingFg = true;
        syncWheelToActive();
        fgBgWidget.repaint();
    }

    public void swapColors() {
        Color tmp = fgColor;
        fgColor = bgColor;
        bgColor = tmp;
        if (fgColorCallback != null) fgColorCallback.accept(fgColor);
        if (bgColorCallback != null) bgColorCallback.accept(bgColor);
        editingFg = true;
        syncWheelToActive();
        fgBgWidget.repaint();
    }

    public void addRecentColor(Color c) {
        recentColors.remove(c);
        recentColors.add(0, c);
        if (recentColors.size() > MAX_RECENT) recentColors.remove(recentColors.size() - 1);
        rebuildSwatches();
    }

    // ---- Private ----

    /** Update the active (FG or BG) color and fire callbacks. */
    private void setActiveColor(Color c, boolean syncWheel) {
        updating = true;
        if (editingFg) {
            fgColor = c;
            if (fgColorCallback != null) fgColorCallback.accept(fgColor);
        } else {
            bgColor = c;
            if (bgColorCallback != null) bgColorCallback.accept(bgColor);
        }
        // Update hex field
        Color rgb = editingFg ? fgColor : bgColor;
        hexField.setText(String.format("#%02X%02X%02X", rgb.getRed(), rgb.getGreen(), rgb.getBlue()));
        alphaSlider.setValue(rgb.getAlpha());
        if (syncWheel) wheel.setColor(rgb);
        fgBgWidget.repaint();
        updating = false;
    }

    /** Make wheel/hex/alpha show the currently-active (FG or BG) color. */
    private void syncWheelToActive() {
        updating = true;
        Color active = editingFg ? fgColor : bgColor;
        wheel.setColor(active);
        hexField.setText(String.format("#%02X%02X%02X",
                active.getRed(), active.getGreen(), active.getBlue()));
        alphaSlider.setValue(active.getAlpha());
        updating = false;
    }

    private Color applyAlpha(Color base) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), alphaSlider.getValue());
    }

    private void rebuildSwatches() {
        swatchPanel.removeAll();
        for (Color rc : recentColors) {
            JPanel swatch = new JPanel();
            swatch.setPreferredSize(new Dimension(18, 18));
            swatch.setBackground(rc);
            swatch.setBorder(BorderFactory.createLineBorder(new Color(0x555555)));
            swatch.setToolTipText(String.format("#%02X%02X%02X", rc.getRed(), rc.getGreen(), rc.getBlue()));
            swatch.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { setActiveColor(rc, true); }
            });
            swatchPanel.add(swatch);
        }
        swatchPanel.revalidate();
        swatchPanel.repaint();
    }

    private static JSlider makeSlider(int min, int max, int value) {
        JSlider s = new JSlider(min, max, value);
        s.setBackground(new Color(0x252526));
        s.setFocusable(false);
        return s;
    }

    // ---- FG/BG overlapping swatch widget ----

    private class FgBgSwatchWidget extends JComponent {
        // FG swatch occupies [0,0]→[24,24], BG is [12,8]→[36,32]
        private static final int SZ = 24;
        private static final int BG_OX = 12, BG_OY = 8;

        FgBgSwatchWidget() {
            setPreferredSize(new Dimension(42, 38));
            setMinimumSize(new Dimension(42, 38));
            setMaximumSize(new Dimension(42, 38));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Click FG/BG to select for editing");
        }

        boolean isClickOnFg(int x, int y) { return x < SZ && y < SZ; }
        boolean isClickOnBg(int x, int y) { return x >= BG_OX && y >= BG_OY && x < BG_OX + SZ && y < BG_OY + SZ; }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // BG swatch (drawn first = behind)
            g2.setColor(bgColor);
            g2.fillRect(BG_OX, BG_OY, SZ, SZ);
            // Checkerboard for transparent BG
            if (bgColor.getAlpha() < 255) drawChecker(g2, BG_OX, BG_OY, SZ, SZ);
            Color bgBorder = !editingFg ? new Color(0x007ACC) : new Color(0x555555);
            g2.setColor(bgBorder);
            g2.setStroke(new BasicStroke(!editingFg ? 2f : 1f));
            g2.drawRect(BG_OX, BG_OY, SZ, SZ);

            // FG swatch (drawn on top)
            g2.setColor(fgColor);
            g2.fillRect(0, 0, SZ, SZ);
            if (fgColor.getAlpha() < 255) drawChecker(g2, 0, 0, SZ, SZ);
            Color fgBorder = editingFg ? new Color(0x007ACC) : new Color(0x888888);
            g2.setColor(fgBorder);
            g2.setStroke(new BasicStroke(editingFg ? 2f : 1f));
            g2.drawRect(0, 0, SZ, SZ);

            g2.dispose();
        }

        private void drawChecker(Graphics2D g2, int ox, int oy, int w, int h) {
            int sz = 4;
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            for (int yy = 0; yy < h; yy += sz) {
                for (int xx = 0; xx < w; xx += sz) {
                    g2.setColor(((xx / sz + yy / sz) % 2 == 0) ? Color.LIGHT_GRAY : Color.WHITE);
                    g2.fillRect(ox + xx, oy + yy, sz, sz);
                }
            }
            g2.setComposite(old);
        }
    }
}
