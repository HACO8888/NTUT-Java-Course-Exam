package com.draw2.ui;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;

public class StatusBar extends JPanel {

    private final JLabel cursorLabel = makeLabel("0, 0");
    private final JLabel sizeLabel   = makeLabel("800 × 600");
    private final JLabel zoomLabel   = makeLabel("100%");
    private final JLabel toolLabel   = makeLabel("鉛筆");

    public StatusBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 2));
        setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        setPreferredSize(new Dimension(0, 22));

        add(toolLabel);
        add(sep());
        add(new JLabel("游標:"));
        add(cursorLabel);
        add(sep());
        add(new JLabel("大小:"));
        add(sizeLabel);
        add(sep());
        add(zoomLabel);
    }

    public void setCursorPos(int x, int y) {
        cursorLabel.setText(x + ", " + y);
    }

    public void setCanvasSize(int w, int h) {
        sizeLabel.setText(w + " × " + h);
    }

    public void setZoom(double zoom) {
        zoomLabel.setText((int)(zoom * 100) + "%");
    }

    public void setTool(String name) {
        toolLabel.setText(name);
    }

    private static JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(11f));
        return l;
    }

    private static JSeparator sep() {
        JSeparator s = new JSeparator(SwingConstants.VERTICAL);
        s.setPreferredSize(new Dimension(1, 16));
        return s;
    }
}
