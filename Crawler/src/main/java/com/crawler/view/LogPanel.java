package com.crawler.view;

import com.crawler.model.LogEntry;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

public class LogPanel extends JPanel {
    private final JTextPane textPane;
    private final StyledDocument doc;
    private final JCheckBox autoScrollCheck;
    private final JCheckBox showInfo;
    private final JCheckBox showWarn;
    private final JCheckBox showError;
    private int entryCount = 0;
    private static final int MAX_ENTRIES = 5000;
    private final java.util.ArrayDeque<Integer> lineEndPositions = new java.util.ArrayDeque<>();

    private final SimpleAttributeSet infoStyle;
    private final SimpleAttributeSet warnStyle;
    private final SimpleAttributeSet errorStyle;
    private final SimpleAttributeSet timeStyle;

    public LogPanel() {
        setLayout(new BorderLayout());

        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        textPane.setBackground(new Color(0x1e1e1e));
        doc = textPane.getStyledDocument();

        infoStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(infoStyle, new Color(0xABB2BF));

        warnStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(warnStyle, new Color(0xE5C07B));

        errorStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(errorStyle, new Color(0xE06C75));

        timeStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(timeStyle, new Color(0x5C6370));

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        autoScrollCheck = new JCheckBox("自動捲動", true);
        showInfo = new JCheckBox("INFO", true);
        showWarn = new JCheckBox("WARN", true);
        showError = new JCheckBox("ERROR", true);
        JButton clearBtn = new JButton("清除");
        clearBtn.addActionListener(e -> clear());

        controlPanel.add(autoScrollCheck);
        controlPanel.add(Box.createHorizontalStrut(12));
        controlPanel.add(showInfo);
        controlPanel.add(showWarn);
        controlPanel.add(showError);
        controlPanel.add(Box.createHorizontalStrut(12));
        controlPanel.add(clearBtn);

        add(controlPanel, BorderLayout.NORTH);
        add(new JScrollPane(textPane), BorderLayout.CENTER);
    }

    public void appendLog(LogEntry entry) {
        if (!shouldShow(entry.getLevel())) return;

        if (entryCount >= MAX_ENTRIES) {
            try {
                Integer firstEnd = lineEndPositions.pollFirst();
                if (firstEnd != null) {
                    doc.remove(0, firstEnd);
                    int removed = firstEnd;
                    java.util.ArrayDeque<Integer> adjusted = new java.util.ArrayDeque<>();
                    for (Integer pos : lineEndPositions) {
                        adjusted.add(pos - removed);
                    }
                    lineEndPositions.clear();
                    lineEndPositions.addAll(adjusted);
                }
            } catch (BadLocationException ignored) {
            }
        } else {
            entryCount++;
        }

        try {
            doc.insertString(doc.getLength(), entry.getFormattedTime() + " ", timeStyle);
            SimpleAttributeSet style = getStyleForLevel(entry.getLevel());
            doc.insertString(doc.getLength(),
                    String.format("[%-5s] ", entry.getLevel()), style);
            doc.insertString(doc.getLength(), entry.getMessage() + "\n", style);
            lineEndPositions.addLast(doc.getLength());
        } catch (BadLocationException ignored) {
        }

        if (autoScrollCheck.isSelected()) {
            textPane.setCaretPosition(doc.getLength());
        }
    }

    private boolean shouldShow(LogEntry.Level level) {
        switch (level) {
            case INFO: return showInfo.isSelected();
            case WARN: return showWarn.isSelected();
            case ERROR: return showError.isSelected();
            default: return true;
        }
    }

    private SimpleAttributeSet getStyleForLevel(LogEntry.Level level) {
        switch (level) {
            case WARN: return warnStyle;
            case ERROR: return errorStyle;
            default: return infoStyle;
        }
    }

    public void clear() {
        try {
            doc.remove(0, doc.getLength());
            entryCount = 0;
            lineEndPositions.clear();
        } catch (BadLocationException ignored) {
        }
    }
}
