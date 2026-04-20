package com.crawler.view;

import com.crawler.engine.AgentService;
import com.crawler.engine.CrawlerEngine;
import com.crawler.engine.ResultExporter;
import com.crawler.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;

public class MainWindow extends JFrame {
    private final CrawlerEngine engine = new CrawlerEngine();
    private final AgentService agentService = new AgentService();
    private final CrawlToolBar toolBar;
    private final DashboardPanel dashboardPanel;
    private final ResultsPanel resultsPanel;
    private final TreePanel treePanel;
    private final LogPanel logPanel;
    private final ContentPanel contentPanel;
    private final AgentPanel agentPanel;
    private final StatusBar statusBar;
    private final JTabbedPane tabbedPane;
    private final Timer statsTimer;

    private static final int TAB_DASHBOARD = 0;
    private static final int TAB_RESULTS = 1;
    private static final int TAB_TREE = 2;
    private static final int TAB_LOG = 3;
    private static final int TAB_CONTENT = 4;
    private static final int TAB_AGENT = 5;

    public MainWindow() {
        setTitle("\u2746 Java 網站爬蟲");
        setSize(1280, 800);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (engine.getState() == CrawlState.RUNNING || engine.getState() == CrawlState.PAUSED) {
                    int choice = JOptionPane.showConfirmDialog(MainWindow.this,
                            "爬蟲正在執行中，確定要關閉嗎？", "確認關閉",
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (choice != JOptionPane.YES_OPTION) return;
                    engine.stop();
                }
                statsTimer.stop();
                dispose();
                System.exit(0);
            }
        });

        toolBar = new CrawlToolBar();
        dashboardPanel = new DashboardPanel();
        resultsPanel = new ResultsPanel();
        treePanel = new TreePanel();
        logPanel = new LogPanel();
        contentPanel = new ContentPanel();
        agentPanel = new AgentPanel(agentService);
        statusBar = new StatusBar();

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(tabbedPane.getFont().deriveFont(14f));
        tabbedPane.addTab("\u2302 儀表板", dashboardPanel);
        tabbedPane.addTab("\u2637 結果", resultsPanel);
        tabbedPane.addTab("\u2442 URL 樹", treePanel);
        tabbedPane.addTab("\u2263 日誌", logPanel);
        tabbedPane.addTab("\u2398 內容預覽", contentPanel);
        tabbedPane.addTab("\u2728 AI 助理", agentPanel);

        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        setupMenuBar();
        wireEngine();
        wireToolBar();
        wireSelectionListeners();

        statsTimer = new Timer(200, e -> updateStats());
        statsTimer.start();
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("檔案");
        JMenuItem exportCsv = new JMenuItem("匯出 CSV...");
        JMenuItem exportJson = new JMenuItem("匯出 JSON...");
        JMenuItem exit = new JMenuItem("離開");
        exportCsv.addActionListener(e -> exportResults("csv"));
        exportJson.addActionListener(e -> exportResults("json"));
        exit.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        fileMenu.add(exportCsv);
        fileMenu.add(exportJson);
        fileMenu.addSeparator();
        fileMenu.add(exit);

        JMenu crawlMenu = new JMenu("爬取");
        JMenuItem startItem = new JMenuItem("開始");
        JMenuItem pauseItem = new JMenuItem("暫停");
        JMenuItem resumeItem = new JMenuItem("繼續");
        JMenuItem stopItem = new JMenuItem("停止");
        startItem.setAccelerator(KeyStroke.getKeyStroke("ctrl ENTER"));
        pauseItem.setAccelerator(KeyStroke.getKeyStroke("ctrl P"));
        stopItem.setAccelerator(KeyStroke.getKeyStroke("ctrl shift S"));
        startItem.addActionListener(e -> startCrawl());
        pauseItem.addActionListener(e -> engine.pause());
        resumeItem.addActionListener(e -> engine.resume());
        stopItem.addActionListener(e -> engine.stop());
        crawlMenu.add(startItem);
        crawlMenu.add(pauseItem);
        crawlMenu.add(resumeItem);
        crawlMenu.add(stopItem);

        JMenu settingsMenu = new JMenu("設定");
        JMenuItem configItem = new JMenuItem("進階設定...");
        configItem.setAccelerator(KeyStroke.getKeyStroke("ctrl COMMA"));
        configItem.addActionListener(e -> showSettings());
        settingsMenu.add(configItem);

        JMenu helpMenu = new JMenu("說明");
        JMenuItem aboutItem = new JMenuItem("關於");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Java 網站爬蟲 v1.0\n\n"
                + "功能：多執行緒爬取、robots.txt 遵循、\n"
                + "即時統計、內容提取、CSV/JSON 匯出、\n"
                + "AI 助理分析\n\n"
                + "使用 Java Swing + FlatLaf 暗色主題\n"
                + "AI 由 OpenAI GPT 驅動",
                "關於", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(crawlMenu);
        menuBar.add(settingsMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }

    private void wireEngine() {
        engine.setOnResult(result -> resultsPanel.addResult(result));
        engine.setOnLog(entry -> logPanel.appendLog(entry));
        engine.setOnStateChange(() -> {
            CrawlState state = engine.getState();
            toolBar.setStateControls(state);
        });
        engine.setOnNodeDiscovered((node, parentUrl) -> treePanel.addNode(node, parentUrl));
    }

    private void wireToolBar() {
        toolBar.getStartBtn().addActionListener(e -> startCrawl());
        toolBar.getPauseBtn().addActionListener(e -> engine.pause());
        toolBar.getResumeBtn().addActionListener(e -> engine.resume());
        toolBar.getStopBtn().addActionListener(e -> engine.stop());

        toolBar.getStartBtn().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ENTER"), "start");
        toolBar.getStartBtn().getActionMap().put("start", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (toolBar.getStartBtn().isEnabled()) startCrawl();
            }
        });
    }

    private void wireSelectionListeners() {
        resultsPanel.setOnResultSelected(result -> contentPanel.showContent(result));

        resultsPanel.setOnResultDoubleClicked(result -> {
            contentPanel.showContent(result);
            tabbedPane.setSelectedIndex(TAB_CONTENT);
        });

        agentPanel.setResultsSupplier(() -> resultsPanel.getAllResults());

        agentPanel.setOnNavigateToUrl(url -> {
            tabbedPane.setSelectedIndex(TAB_CONTENT);
        });
    }

    private void startCrawl() {
        String url = toolBar.getUrl();
        if (url.isEmpty() || url.equals("https://")) {
            JOptionPane.showMessageDialog(this, "請輸入要爬取的 URL",
                    "缺少 URL", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        try {
            new java.net.URL(url);
        } catch (java.net.MalformedURLException ex) {
            JOptionPane.showMessageDialog(this, "無效的 URL 格式: " + url,
                    "URL 錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }

        resultsPanel.clear();
        treePanel.clear();
        logPanel.clear();
        contentPanel.clear();

        CrawlConfig config = new CrawlConfig();
        config.setSeedUrl(url);
        config.setMaxDepth(toolBar.getDepth());
        config.setMaxPages(toolBar.getMaxPages());

        dashboardPanel.setMaxPages(config.getMaxPages());
        treePanel.setRootNode(new SiteNode(config.getSeedUrl()));

        engine.start(config);
    }

    private void showSettings() {
        SettingsDialog dialog = new SettingsDialog(this);
        if (engine.getConfig() != null) {
            dialog.setConfig(engine.getConfig());
        }
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            CrawlConfig config = dialog.getConfig();
            if (engine.getState() == CrawlState.IDLE || engine.getState() == CrawlState.STOPPED
                    || engine.getState() == CrawlState.COMPLETED) {
                resultsPanel.clear();
                treePanel.clear();
                logPanel.clear();
                contentPanel.clear();
                dashboardPanel.setMaxPages(config.getMaxPages());
                treePanel.setRootNode(new SiteNode(config.getSeedUrl()));
                engine.start(config);
            }
        }
    }

    private void updateStats() {
        if (engine.getState() == CrawlState.RUNNING || engine.getState() == CrawlState.PAUSED) {
            CrawlStatistics stats = engine.getStatistics();
            dashboardPanel.updateStats(stats);
            statusBar.update(stats, engine.getState());
        } else if (engine.getState() == CrawlState.COMPLETED || engine.getState() == CrawlState.STOPPED) {
            CrawlStatistics stats = engine.getStatistics();
            dashboardPanel.updateStats(stats);
            statusBar.update(stats, engine.getState());
        }
    }

    private void exportResults(String format) {
        List<CrawlResult> results = resultsPanel.getAllResults();
        if (results.isEmpty()) {
            JOptionPane.showMessageDialog(this, "沒有可匯出的結果",
                    "匯出", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("crawl_results." + format));
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                if (format.equals("csv")) {
                    ResultExporter.exportCsv(results, file);
                } else {
                    ResultExporter.exportJson(results, file);
                }
                JOptionPane.showMessageDialog(this,
                        "已匯出 " + results.size() + " 筆結果到\n" + file.getAbsolutePath(),
                        "匯出成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "匯出失敗: " + ex.getMessage(),
                        "錯誤", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
