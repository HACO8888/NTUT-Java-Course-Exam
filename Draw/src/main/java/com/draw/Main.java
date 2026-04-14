package com.draw;

import com.draw.ui.MainWindow;
import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        // Install One Dark theme before any Swing components are created
        FlatOneDarkIJTheme.setup();

        // Fine-tune appearance
        UIManager.put("Component.accentColor", new Color(0x0078D4));
        UIManager.put("Component.arc", 6);
        UIManager.put("Button.arc", 6);
        UIManager.put("ScrollBar.width", 8);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("TabbedPane.tabHeight", 32);
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
