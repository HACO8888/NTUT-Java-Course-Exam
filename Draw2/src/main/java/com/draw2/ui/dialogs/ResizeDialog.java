package com.draw2.ui.dialogs;

import javax.swing.*;
import java.awt.*;

public class ResizeDialog extends JDialog {

    private final JSpinner widthSpinner;
    private final JSpinner heightSpinner;
    private final JCheckBox aspectRatioCheck = new JCheckBox("維持長寬比", true);
    private boolean confirmed;
    private final int origW, origH;

    public ResizeDialog(Frame owner, int currentW, int currentH) {
        super(owner, "調整大小", true);
        origW = currentW;
        origH = currentH;
        widthSpinner  = new JSpinner(new SpinnerNumberModel(currentW, 1, 8000, 1));
        heightSpinner = new JSpinner(new SpinnerNumberModel(currentH, 1, 8000, 1));

        widthSpinner.addChangeListener(e -> {
            if (aspectRatioCheck.isSelected()) {
                int nw = (Integer) widthSpinner.getValue();
                int nh = (int)((long) nw * origH / origW);
                heightSpinner.setValue(Math.max(1, nh));
            }
        });

        build();
        pack();
        setLocationRelativeTo(owner);
    }

    private void build() {
        JPanel form = new JPanel(new GridLayout(3, 2, 6, 6));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
        form.add(new JLabel("寬度 (px):"));
        form.add(widthSpinner);
        form.add(new JLabel("高度 (px):"));
        form.add(heightSpinner);
        form.add(aspectRatioCheck);
        form.add(new JLabel(""));

        JButton ok = new JButton("確定");
        JButton cancel = new JButton("取消");
        ok.addActionListener(e -> { confirmed = true; dispose(); });
        cancel.addActionListener(e -> dispose());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(ok);
        btns.add(cancel);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(btns, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);
    }

    public boolean isConfirmed() { return confirmed; }
    public int getNewWidth()     { return (Integer) widthSpinner.getValue(); }
    public int getNewHeight()    { return (Integer) heightSpinner.getValue(); }
}
