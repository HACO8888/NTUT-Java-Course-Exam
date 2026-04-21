package com.bigtwo;

import com.bigtwo.server.GameServer;
import com.bigtwo.ui.GameWindow;

import javax.swing.*;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--server")) {
            String[] serverArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
            GameServer.main(serverArgs);
            return;
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.setVisible(true);
        });
    }
}
