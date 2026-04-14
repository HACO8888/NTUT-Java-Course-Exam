package com.draw.ui.dialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Dialog for resizing the canvas.
 */
public class CanvasSizeDialog extends JDialog {
    private final JSpinner widthSpinner;
    private final JSpinner heightSpinner;
    private final JToggleButton lockBtn = new JToggleButton("🔗");
    private boolean confirmed = false;
    private double aspectRatio;

    public CanvasSizeDialog(Frame parent, int currentW, int currentH) {
        super(parent, "Canvas Size", true);
        this.aspectRatio = (double) currentW / currentH;

        widthSpinner  = new JSpinner(new SpinnerNumberModel(currentW, 1, 8192, 1));
        heightSpinner = new JSpinner(new SpinnerNumberModel(currentH, 1, 8192, 1));

        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Width (px):"), c);
        c.gridx = 1; form.add(widthSpinner, c);
        c.gridx = 2; form.add(lockBtn, c);
        c.gridx = 0; c.gridy = 1; form.add(new JLabel("Height (px):"), c);
        c.gridx = 1; form.add(heightSpinner, c);

        add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        JButton ok = new JButton("Apply");
        btns.add(cancel);
        btns.add(ok);
        add(btns, BorderLayout.SOUTH);

        // Aspect ratio lock
        lockBtn.setSelected(false);
        lockBtn.setFocusPainted(false);
        widthSpinner.addChangeListener(e -> {
            if (lockBtn.isSelected()) {
                int w = (int) widthSpinner.getValue();
                heightSpinner.setValue((int)(w / aspectRatio));
            }
        });

        ok.addActionListener(e -> { confirmed = true; dispose(); });
        cancel.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(ok);
        pack();
        setLocationRelativeTo(parent);
    }

    public boolean isConfirmed() { return confirmed; }
    public int getCanvasWidth()  { return (int) widthSpinner.getValue(); }
    public int getCanvasHeight() { return (int) heightSpinner.getValue(); }
}
