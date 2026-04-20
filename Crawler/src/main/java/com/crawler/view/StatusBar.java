package com.crawler.view;

import com.crawler.model.CrawlState;
import com.crawler.model.CrawlStatistics;

import javax.swing.*;
import java.awt.*;

public class StatusBar extends JPanel {
    private final JLabel stateLabel = new JLabel("閒置");
    private final JLabel pagesLabel = new JLabel("頁數: 0");
    private final JLabel errorsLabel = new JLabel("錯誤: 0");
    private final JLabel speedLabel = new JLabel("速度: 0.0 頁/秒");
    private final JLabel elapsedLabel = new JLabel("時間: 00:00");
    private final JLabel queueLabel = new JLabel("佇列: 0");
    private final JLabel bytesLabel = new JLabel("資料: 0 B");

    public StatusBar() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBackground(new Color(0x007ACC));
        setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        Color fg = Color.WHITE;

        JLabel[] labels = { stateLabel, pagesLabel, errorsLabel, speedLabel,
                elapsedLabel, queueLabel, bytesLabel };
        for (int i = 0; i < labels.length; i++) {
            labels[i].setFont(font);
            labels[i].setForeground(fg);
            add(labels[i]);
            if (i < labels.length - 1) {
                add(Box.createHorizontalStrut(8));
                JLabel sep = new JLabel("|");
                sep.setFont(font);
                sep.setForeground(new Color(255, 255, 255, 120));
                add(sep);
                add(Box.createHorizontalStrut(8));
            }
        }
        add(Box.createHorizontalGlue());
    }

    public void update(CrawlStatistics stats, CrawlState state) {
        stateLabel.setText(state.getLabel());
        pagesLabel.setText("頁數: " + stats.getTotalPages());
        errorsLabel.setText("錯誤: " + stats.getPagesError());
        speedLabel.setText(String.format("速度: %.1f 頁/秒", stats.getPagesPerSecond()));
        elapsedLabel.setText("時間: " + stats.getFormattedElapsed());
        queueLabel.setText("佇列: " + stats.getQueueSize());
        bytesLabel.setText("資料: " + stats.getFormattedBytes());
    }
}
