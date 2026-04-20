package com.crawler;

import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;
import com.crawler.view.MainWindow;

import javafx.embed.swing.JFXPanel;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        new JFXPanel();
        FlatOneDarkIJTheme.setup();

        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Component.accentColor", new Color(0x0078D4));
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("TabbedPane.tabHeight", 32);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
