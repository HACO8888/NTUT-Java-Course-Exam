package com.crawler.view;

import com.crawler.model.CrawlState;
import com.crawler.util.CrawlerIcons;
import com.crawler.util.SwingUtils;

import javax.swing.*;
import java.awt.*;

public class CrawlToolBar extends JPanel {
    private final JTextField urlField;
    private final JButton startBtn;
    private final JButton pauseBtn;
    private final JButton resumeBtn;
    private final JButton stopBtn;
    private final JSpinner depthSpinner;
    private final JSpinner maxPagesSpinner;

    public CrawlToolBar() {
        setLayout(new BorderLayout(8, 0));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JPanel leftPanel = new JPanel(new BorderLayout(6, 0));

        JLabel urlLabel = new JLabel("URL:");
        urlLabel.setFont(urlLabel.getFont().deriveFont(Font.BOLD));
        urlField = new JTextField("https://www.ntut.edu.tw/");
        urlField.setFont(urlField.getFont().deriveFont(14f));
        urlField.setPreferredSize(new Dimension(400, 32));

        leftPanel.add(urlLabel, BorderLayout.WEST);
        leftPanel.add(urlField, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));

        startBtn = SwingUtils.createToolButton(CrawlerIcons.START + " 開始", "開始爬取");
        startBtn.setBackground(new Color(0x2ea043));
        startBtn.setForeground(Color.WHITE);

        pauseBtn = SwingUtils.createToolButton(CrawlerIcons.PAUSE + " 暫停", "暫停爬取");
        resumeBtn = SwingUtils.createToolButton(CrawlerIcons.START + " 繼續", "繼續爬取");
        stopBtn = SwingUtils.createToolButton(CrawlerIcons.STOP + " 停止", "停止爬取");
        stopBtn.setBackground(new Color(0xda3633));
        stopBtn.setForeground(Color.WHITE);

        controlPanel.add(startBtn);
        controlPanel.add(pauseBtn);
        controlPanel.add(resumeBtn);
        controlPanel.add(stopBtn);

        controlPanel.add(Box.createHorizontalStrut(16));
        controlPanel.add(new JLabel("深度:"));
        depthSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 20, 1));
        depthSpinner.setPreferredSize(new Dimension(60, 28));
        controlPanel.add(depthSpinner);

        controlPanel.add(Box.createHorizontalStrut(8));
        controlPanel.add(new JLabel("最大頁數:"));
        maxPagesSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 10000, 10));
        maxPagesSpinner.setPreferredSize(new Dimension(80, 28));
        controlPanel.add(maxPagesSpinner);

        add(leftPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.EAST);

        setStateControls(CrawlState.IDLE);
    }

    public void setStateControls(CrawlState state) {
        startBtn.setEnabled(state == CrawlState.IDLE || state == CrawlState.STOPPED || state == CrawlState.COMPLETED);
        pauseBtn.setEnabled(state == CrawlState.RUNNING);
        resumeBtn.setEnabled(state == CrawlState.PAUSED);
        stopBtn.setEnabled(state == CrawlState.RUNNING || state == CrawlState.PAUSED);
        urlField.setEnabled(state != CrawlState.RUNNING && state != CrawlState.PAUSED);
        depthSpinner.setEnabled(state != CrawlState.RUNNING && state != CrawlState.PAUSED);
        maxPagesSpinner.setEnabled(state != CrawlState.RUNNING && state != CrawlState.PAUSED);

        pauseBtn.setVisible(state != CrawlState.PAUSED);
        resumeBtn.setVisible(state == CrawlState.PAUSED);
    }

    public String getUrl() { return urlField.getText().trim(); }
    public int getDepth() { return (int) depthSpinner.getValue(); }
    public int getMaxPages() { return (int) maxPagesSpinner.getValue(); }

    public JButton getStartBtn() { return startBtn; }
    public JButton getPauseBtn() { return pauseBtn; }
    public JButton getResumeBtn() { return resumeBtn; }
    public JButton getStopBtn() { return stopBtn; }
}
