package com.draw.ui.dialog;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for creating a new document with custom size.
 */
public class NewDocumentDialog extends JDialog {
    private final JSpinner widthSpinner  = new JSpinner(new SpinnerNumberModel(800, 1, 8192, 1));
    private final JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(600, 1, 8192, 1));
    private boolean confirmed = false;

    public NewDocumentDialog(Frame parent) {
        super(parent, "New Document", true);
        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.add(new JLabel("Width (px):"));
        form.add(widthSpinner);
        form.add(new JLabel("Height (px):"));
        form.add(heightSpinner);
        add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        JButton ok = new JButton("Create");
        btns.add(cancel);
        btns.add(ok);
        add(btns, BorderLayout.SOUTH);

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
