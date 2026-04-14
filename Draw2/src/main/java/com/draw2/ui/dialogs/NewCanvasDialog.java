package com.draw2.ui.dialogs;

import javax.swing.*;
import java.awt.*;

public class NewCanvasDialog extends JDialog {

    private final JSpinner widthSpinner  = new JSpinner(new SpinnerNumberModel(800, 1, 8000, 1));
    private final JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(600, 1, 8000, 1));
    private boolean confirmed;

    public NewCanvasDialog(Frame owner) {
        super(owner, "新增畫布", true);
        build();
        pack();
        setLocationRelativeTo(owner);
    }

    private void build() {
        JPanel form = new JPanel(new GridLayout(2, 2, 6, 6));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
        form.add(new JLabel("寬度 (px):"));
        form.add(widthSpinner);
        form.add(new JLabel("高度 (px):"));
        form.add(heightSpinner);

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
    public int getCanvasWidth()  { return (Integer) widthSpinner.getValue(); }
    public int getCanvasHeight() { return (Integer) heightSpinner.getValue(); }
}
