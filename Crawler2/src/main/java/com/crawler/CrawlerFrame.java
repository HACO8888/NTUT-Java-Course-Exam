package com.crawler;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class CrawlerFrame extends JFrame {

    private final JTextField urlField = new JTextField("https://example.com");
    private final JSpinner depthSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
    private final JSpinner maxPagesSpinner = new JSpinner(new SpinnerNumberModel(50, 1, 500, 10));
    private final JSpinner timeoutSpinner = new JSpinner(new SpinnerNumberModel(5000, 1000, 30000, 1000));
    private final JButton startButton = new JButton("開始爬取");
    private final JButton stopButton = new JButton("停止");
    private final JButton clearButton = new JButton("清除");
    private final JLabel statusLabel = new JLabel("就緒");
    private final JLabel countLabel = new JLabel("已爬取: 0");
    private final DefaultTableModel tableModel;
    private final JTextArea logArea = new JTextArea();
    private final JProgressBar progressBar = new JProgressBar();

    private WebCrawler crawler;

    public CrawlerFrame() {
        super("網頁爬蟲 - Web Crawler");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(950, 650);
        setMinimumSize(new Dimension(700, 500));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (crawler != null && crawler.isRunning()) {
                    crawler.stop();
                }
                dispose();
                System.exit(0);
            }
        });

        tableModel = new DefaultTableModel(
                new String[]{"網址", "標題", "深度", "連結數", "回應時間(ms)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        initUI();
        bindActions();
    }

    private void initUI() {
        JPanel topPanel = new JPanel(new BorderLayout(8, 4));
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));

        JPanel urlPanel = new JPanel(new BorderLayout(6, 0));
        urlPanel.add(new JLabel("目標網址:"), BorderLayout.WEST);
        urlPanel.add(urlField, BorderLayout.CENTER);

        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        settingsPanel.add(new JLabel("最大深度:"));
        settingsPanel.add(depthSpinner);
        settingsPanel.add(new JLabel("最大頁數:"));
        settingsPanel.add(maxPagesSpinner);
        settingsPanel.add(new JLabel("逾時(ms):"));
        settingsPanel.add(timeoutSpinner);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        stopButton.setEnabled(false);
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(clearButton);

        JPanel configRow = new JPanel(new BorderLayout());
        configRow.add(settingsPanel, BorderLayout.CENTER);
        configRow.add(buttonPanel, BorderLayout.EAST);

        topPanel.add(urlPanel, BorderLayout.NORTH);
        topPanel.add(configRow, BorderLayout.SOUTH);

        JTable table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(250);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(60);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        JScrollPane tableScroll = new JScrollPane(table);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setRows(8);
        JScrollPane logScroll = new JScrollPane(logArea);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, logScroll);
        splitPane.setResizeWeight(0.7);
        splitPane.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JPanel bottomPanel = new JPanel(new BorderLayout(8, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        progressBar.setStringPainted(true);
        progressBar.setString("");
        bottomPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(progressBar, BorderLayout.CENTER);
        bottomPanel.add(countLabel, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void bindActions() {
        startButton.addActionListener(e -> startCrawl());
        stopButton.addActionListener(e -> stopCrawl());
        clearButton.addActionListener(e -> {
            tableModel.setRowCount(0);
            logArea.setText("");
            countLabel.setText("已爬取: 0");
            progressBar.setValue(0);
            progressBar.setString("");
        });
    }

    private void startCrawl() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "請輸入網址", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
            urlField.setText(url);
        }

        int maxDepth = (int) depthSpinner.getValue();
        int maxPages = (int) maxPagesSpinner.getValue();
        int timeout = (int) timeoutSpinner.getValue();

        tableModel.setRowCount(0);
        logArea.setText("");

        crawler = new WebCrawler(maxDepth, maxPages, timeout);

        progressBar.setMaximum(maxPages);
        progressBar.setValue(0);

        crawler.setOnResult(result -> SwingUtilities.invokeLater(() -> {
            tableModel.addRow(new Object[]{
                    result.url(),
                    result.title(),
                    result.depth(),
                    result.linkCount(),
                    result.responseTimeMs()
            });
            int count = tableModel.getRowCount();
            countLabel.setText("已爬取: " + count);
            progressBar.setValue(count);
            progressBar.setString(count + " / " + maxPages);
        }));

        crawler.setOnLog(msg -> SwingUtilities.invokeLater(() -> {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            logArea.append("[%s] %s\n".formatted(time, msg));
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }));

        crawler.setOnFinish(() -> SwingUtilities.invokeLater(() -> {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            setFieldsEnabled(true);
            statusLabel.setText("完成 - 共爬取 %d 頁".formatted(tableModel.getRowCount()));
        }));

        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        setFieldsEnabled(false);
        statusLabel.setText("爬取中...");

        crawler.start(url);
    }

    private void stopCrawl() {
        if (crawler != null) {
            crawler.stop();
            statusLabel.setText("已停止");
        }
    }

    private void setFieldsEnabled(boolean enabled) {
        urlField.setEnabled(enabled);
        depthSpinner.setEnabled(enabled);
        maxPagesSpinner.setEnabled(enabled);
        timeoutSpinner.setEnabled(enabled);
    }
}
