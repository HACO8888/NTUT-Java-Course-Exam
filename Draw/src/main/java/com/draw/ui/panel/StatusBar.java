package com.draw.ui.panel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Bottom status bar: tool name | zoom (clickable) | canvas size | cursor position.
 */
public class StatusBar extends JPanel {
    private final JLabel zoomLabel   = makeLabel("100%");
    private final JLabel sizeLabel   = makeLabel("800 × 600");
    private final JLabel cursorLabel = makeLabel("0, 0");
    private final JLabel toolLabel   = makeLabel("Pencil");
    private final JLabel layerLabel  = makeLabel("Layer 1");

    private Consumer<Double> zoomCallback;

    public StatusBar() {
        setBackground(new Color(0x007ACC));
        setLayout(new FlowLayout(FlowLayout.LEFT, 12, 3));
        setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        setPreferredSize(new Dimension(Integer.MAX_VALUE, 26));

        add(toolLabel);
        add(makeSep());
        add(layerLabel);
        add(makeSep());
        add(zoomLabel);
        add(makeSep());
        add(sizeLabel);
        add(makeSep());
        add(cursorLabel);

        // Click on zoom to enter custom value
        zoomLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        zoomLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        zoomLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String input = JOptionPane.showInputDialog(StatusBar.this, "Enter zoom %:", "Zoom", JOptionPane.PLAIN_MESSAGE);
                if (input == null) return;
                try {
                    double z = Double.parseDouble(input.replace("%", "").trim()) / 100.0;
                    if (z > 0 && zoomCallback != null) zoomCallback.accept(z);
                } catch (NumberFormatException ignored) {}
            }
        });
    }

    public void setZoomCallback(Consumer<Double> cb) { this.zoomCallback = cb; }

    public void setZoom(double zoom) {
        zoomLabel.setText(String.format("%.0f%%", zoom * 100));
    }

    public void setCanvasSize(int w, int h) {
        sizeLabel.setText(w + " × " + h);
    }

    public void setCursorPos(int x, int y) {
        cursorLabel.setText(x + ", " + y);
    }

    public void setTool(String name) {
        toolLabel.setText(name);
    }

    public void setLayerName(String name) {
        layerLabel.setText(name);
    }

    private static JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return l;
    }

    private static JLabel makeSep() {
        JLabel s = new JLabel("|");
        s.setForeground(new Color(255, 255, 255, 100));
        s.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return s;
    }
}
