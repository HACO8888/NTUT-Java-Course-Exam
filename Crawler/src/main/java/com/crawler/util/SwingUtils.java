package com.crawler.util;

import javax.swing.*;
import java.awt.*;

public class SwingUtils {

    public static JPanel makeCard(String title, String value) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(new Color(0x2d2d2d));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x3c3c3c), 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 28f));
        valueLabel.setForeground(new Color(0x61AFEF));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 12f));
        titleLabel.setForeground(new Color(0x7f848e));

        card.add(valueLabel, BorderLayout.CENTER);
        card.add(titleLabel, BorderLayout.SOUTH);

        return card;
    }

    public static JLabel getValueLabel(JPanel card) {
        return (JLabel) ((BorderLayout) card.getLayout()).getLayoutComponent(BorderLayout.CENTER);
    }

    public static JButton createToolButton(String text, String tooltip) {
        JButton btn = new JButton(text);
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setFont(btn.getFont().deriveFont(14f));
        return btn;
    }
}
