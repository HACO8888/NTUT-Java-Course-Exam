package com.draw.ui.panel;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Panel with sliders for brush size, opacity, and hardness.
 */
public class BrushSettingsPanel extends JPanel {
    private final JSlider sizeSlider     = makeSlider(1, 200, 10);
    private final JSlider opacitySlider  = makeSlider(0, 100, 100);
    private final JSlider hardnessSlider = makeSlider(0, 100, 80);

    private Consumer<float[]> changeCallback; // [size, opacity, hardness]

    public BrushSettingsPanel() {
        setBackground(new Color(0x252526));
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(2, 0, 0, 4);
        lc.gridx = 0; lc.gridy = GridBagConstraints.RELATIVE;

        GridBagConstraints sc = new GridBagConstraints();
        sc.fill = GridBagConstraints.HORIZONTAL;
        sc.weightx = 1.0;
        sc.insets = new Insets(2, 0, 0, 0);
        sc.gridx = 1; sc.gridy = GridBagConstraints.RELATIVE;

        addRow("Size", sizeSlider, lc, sc);
        addRow("Opacity", opacitySlider, lc, sc);
        addRow("Hardness", hardnessSlider, lc, sc);

        ChangeListener cl = e -> fireChange();
        sizeSlider.addChangeListener(cl);
        opacitySlider.addChangeListener(cl);
        hardnessSlider.addChangeListener(cl);
    }

    private void addRow(String label, JSlider slider, GridBagConstraints lc, GridBagConstraints sc) {
        JLabel lbl = new JLabel(label);
        lbl.setForeground(new Color(0xCCCCCC));
        lbl.setFont(lbl.getFont().deriveFont(11f));
        add(lbl, lc);
        add(slider, sc);
    }

    private void fireChange() {
        if (changeCallback != null) {
            changeCallback.accept(new float[]{
                getBrushSize(), getOpacity(), getHardness()
            });
        }
    }

    public void setChangeCallback(Consumer<float[]> cb) { this.changeCallback = cb; }

    public float getBrushSize() { return sizeSlider.getValue(); }
    public float getOpacity()  { return opacitySlider.getValue() / 100f; }
    public float getHardness() { return hardnessSlider.getValue() / 100f; }

    private static JSlider makeSlider(int min, int max, int value) {
        JSlider s = new JSlider(min, max, value);
        s.setBackground(new Color(0x252526));
        s.setFocusable(false);
        return s;
    }
}
