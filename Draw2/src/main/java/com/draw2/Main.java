package com.draw2;

import com.draw2.ui.MainWindow;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Use system look and feel for classic Paint feel
        try {
            // Try FlatLaf light for a clean modern look; fall back to system
            Class.forName("com.formdev.flatlaf.FlatLightLaf")
                 .getMethod("setup")
                 .invoke(null);
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
