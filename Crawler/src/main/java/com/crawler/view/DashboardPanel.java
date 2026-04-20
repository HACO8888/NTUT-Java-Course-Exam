package com.crawler.view;

import com.crawler.model.CrawlStatistics;
import com.crawler.util.SwingUtils;

import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {
    private final JPanel successCard;
    private final JPanel errorCard;
    private final JPanel speedCard;
    private final JPanel queueCard;
    private final JPanel threadsCard;
    private final JPanel bytesCard;
    private final JPanel elapsedCard;
    private final JProgressBar progressBar;
    private int maxPages = 100;

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        successCard = SwingUtils.makeCard("成功頁數", "0");
        errorCard = SwingUtils.makeCard("錯誤頁數", "0");
        speedCard = SwingUtils.makeCard("頁/秒", "0.0");
        queueCard = SwingUtils.makeCard("佇列大小", "0");
        threadsCard = SwingUtils.makeCard("活動執行緒", "0");
        bytesCard = SwingUtils.makeCard("已下載", "0 B");
        elapsedCard = SwingUtils.makeCard("經過時間", "00:00");

        JPanel cardsPanel = new JPanel(new GridLayout(2, 4, 12, 12));
        cardsPanel.setOpaque(false);
        cardsPanel.add(successCard);
        cardsPanel.add(errorCard);
        cardsPanel.add(speedCard);
        cardsPanel.add(queueCard);
        cardsPanel.add(threadsCard);
        cardsPanel.add(bytesCard);
        cardsPanel.add(elapsedCard);

        JPanel progressPanel = new JPanel(new BorderLayout(8, 0));
        progressPanel.setOpaque(false);
        progressPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        JLabel progressLabel = new JLabel("爬取進度");
        progressLabel.setForeground(new Color(0x7f848e));
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(0, 24));
        progressPanel.add(progressLabel, BorderLayout.WEST);
        progressPanel.add(progressBar, BorderLayout.CENTER);

        // add a large title at top
        JLabel titleLabel = new JLabel("\u2746 網站爬蟲儀表板");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        titleLabel.setForeground(new Color(0x61AFEF));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(cardsPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(progressPanel, BorderLayout.SOUTH);
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
        progressBar.setMaximum(maxPages);
    }

    public void updateStats(CrawlStatistics stats) {
        SwingUtils.getValueLabel(successCard).setText(String.valueOf(stats.getPagesSuccess()));
        SwingUtils.getValueLabel(errorCard).setText(String.valueOf(stats.getPagesError()));
        SwingUtils.getValueLabel(speedCard).setText(String.format("%.1f", stats.getPagesPerSecond()));
        SwingUtils.getValueLabel(queueCard).setText(String.valueOf(stats.getQueueSize()));
        SwingUtils.getValueLabel(threadsCard).setText(String.valueOf(stats.getActiveThreads()));
        SwingUtils.getValueLabel(bytesCard).setText(stats.getFormattedBytes());
        SwingUtils.getValueLabel(elapsedCard).setText(stats.getFormattedElapsed());

        int total = stats.getTotalPages();
        progressBar.setValue(Math.min(total, maxPages));
        progressBar.setString(total + " / " + maxPages);
    }
}
