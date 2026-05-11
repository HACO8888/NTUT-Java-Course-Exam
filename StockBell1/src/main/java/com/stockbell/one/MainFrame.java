package com.stockbell.one;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class MainFrame extends JFrame {
    private static final String CONFIG_PATH = "config.properties";
    private static final String WATCHLIST_PATH = "watchlist.properties";

    private final AppConfig config = ConfigLoader.loadAppConfig(CONFIG_PATH);
    private final YahooClient client = new YahooClient();
    private final NotificationDispatcher dispatcher = new NotificationDispatcher(config);
    private final WatchlistTableModel tableModel = new WatchlistTableModel();
    private final JTable watchTable = new JTable(tableModel);
    private final ChartPanel chartPanel = new ChartPanel();
    private final JTextArea logArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("● 線上");
    private final JLabel countdownLabel = new JLabel("下次更新 -");

    private final JTextField symbolField = new JTextField("2330.TW", 10);
    private final JTextField nameField = new JTextField("台積電", 8);
    private final JSpinner highSpinner = new JSpinner(new SpinnerNumberModel(1000.0, 0.0, 1000000.0, 1.0));
    private final JSpinner lowSpinner = new JSpinner(new SpinnerNumberModel(900.0, 0.0, 1000000.0, 1.0));
    private final JSpinner intervalSpinner;
    private final JCheckBox trayCheck = new JCheckBox("Tray");
    private final JCheckBox consoleCheck = new JCheckBox("Beep");
    private final JCheckBox popupCheck = new JCheckBox("彈窗");
    private final JCheckBox discordCheck = new JCheckBox("Discord");

    private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "stockbell-poller");
        t.setDaemon(true);
        return t;
    });
    private final ScheduledExecutorService chartLoader = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "stockbell-chart");
        t.setDaemon(true);
        return t;
    });
    private final AtomicInteger countdown = new AtomicInteger(0);

    public MainFrame() {
        super("StockBell 1 - 股票即時看盤 + 警示");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1200, 760);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        intervalSpinner = new JSpinner(new SpinnerNumberModel(
                config.pollIntervalSeconds(), 5, 600, 1));

        trayCheck.setSelected(config.channels().contains(AppConfig.Channel.TRAY));
        consoleCheck.setSelected(config.channels().contains(AppConfig.Channel.CONSOLE));
        popupCheck.setSelected(config.channels().contains(AppConfig.Channel.POPUP));
        discordCheck.setSelected(config.channels().contains(AppConfig.Channel.DISCORD));

        initUI();
        bindActions();
        loadInitialWatchlist();

        dispatcher.setLogSink(this::appendLog);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdownAndExit();
            }
        });

        startPolling();
        startCountdown();
    }

    private void initUI() {
        // 工具列
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        toolbar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        toolbar.add(new JLabel("代號:"));
        toolbar.add(symbolField);
        toolbar.add(new JLabel("名稱:"));
        toolbar.add(nameField);
        toolbar.add(new JLabel("高:"));
        toolbar.add(highSpinner);
        toolbar.add(new JLabel("低:"));
        toolbar.add(lowSpinner);
        JButton addBtn = new JButton("加入");
        JButton removeBtn = new JButton("移除選取");
        JButton refreshBtn = new JButton("立即更新");
        JButton testBtn = new JButton("測試通知");
        JButton applyThBtn = new JButton("套用門檻到選取列");
        toolbar.add(addBtn);
        toolbar.add(removeBtn);
        toolbar.add(applyThBtn);
        toolbar.add(refreshBtn);
        toolbar.add(testBtn);
        toolbar.add(new JLabel("間隔(秒):"));
        toolbar.add(intervalSpinner);

        // 表格
        watchTable.setRowHeight(24);
        watchTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        watchTable.setAutoCreateRowSorter(false);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 2; i <= 8; i++) {
            watchTable.getColumnModel().getColumn(i).setCellRenderer(right);
        }
        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                            boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                WatchItem.Direction d = tableModel.get(row).lastDirection();
                if (!sel) {
                    if (d == WatchItem.Direction.HIGH) c.setBackground(new Color(255, 220, 220));
                    else if (d == WatchItem.Direction.LOW) c.setBackground(new Color(220, 240, 220));
                    else c.setBackground(Color.WHITE);
                }
                return c;
            }
        };
        watchTable.getColumnModel().getColumn(9).setCellRenderer(statusRenderer);
        JScrollPane tableScroll = new JScrollPane(watchTable);
        tableScroll.setPreferredSize(new Dimension(560, 400));

        // 中央分割：左 table，右 chart
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, chartPanel);
        centerSplit.setResizeWeight(0.45);
        centerSplit.setDividerLocation(560);

        // log 區
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setRows(6);
        JScrollPane logScroll = new JScrollPane(logArea);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, centerSplit, logScroll);
        mainSplit.setResizeWeight(0.78);

        // 狀態列
        JPanel statusBar = new JPanel(new BorderLayout(8, 0));
        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        statusLabel.setForeground(new Color(0, 140, 0));
        left.add(statusLabel);
        left.add(countdownLabel);
        JPanel rightP = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightP.add(new JLabel("通知:"));
        rightP.add(trayCheck);
        rightP.add(consoleCheck);
        rightP.add(popupCheck);
        rightP.add(discordCheck);
        statusBar.add(left, BorderLayout.WEST);
        statusBar.add(rightP, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.NORTH);
        add(mainSplit, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        // 連動 checkbox 至 config
        java.awt.event.ActionListener channelSync = e -> syncChannelsFromUI();
        trayCheck.addActionListener(channelSync);
        consoleCheck.addActionListener(channelSync);
        popupCheck.addActionListener(channelSync);
        discordCheck.addActionListener(channelSync);

        addBtn.addActionListener(e -> addCurrentToWatchlist());
        removeBtn.addActionListener(e -> removeSelected());
        applyThBtn.addActionListener(e -> applyThresholds());
        refreshBtn.addActionListener(e -> {
            poller.execute(this::pollAllNow);
            countdown.set((Integer) intervalSpinner.getValue());
        });
        testBtn.addActionListener(e -> testNotification());
        intervalSpinner.addChangeListener(e -> {
            config.setPollIntervalSeconds((Integer) intervalSpinner.getValue());
            countdown.set(config.pollIntervalSeconds());
        });

        watchTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = watchTable.getSelectedRow();
            if (row >= 0 && row < tableModel.getRowCount()) {
                loadChart(tableModel.get(row));
            }
        });

        chartPanel.setOnModeChange(() -> {
            int row = watchTable.getSelectedRow();
            if (row >= 0) loadChart(tableModel.get(row));
        });
    }

    private void bindActions() { /* handled in initUI */ }

    private void syncChannelsFromUI() {
        java.util.EnumSet<AppConfig.Channel> set = java.util.EnumSet.noneOf(AppConfig.Channel.class);
        if (trayCheck.isSelected()) set.add(AppConfig.Channel.TRAY);
        if (consoleCheck.isSelected()) set.add(AppConfig.Channel.CONSOLE);
        if (popupCheck.isSelected()) set.add(AppConfig.Channel.POPUP);
        if (discordCheck.isSelected()) set.add(AppConfig.Channel.DISCORD);
        config.setChannels(set);
    }

    private void loadInitialWatchlist() {
        List<WatchItem> items = ConfigLoader.loadWatchlist(WATCHLIST_PATH);
        if (items.isEmpty()) {
            items.add(new WatchItem("2330.TW", "台積電", 1080, 950));
        }
        for (WatchItem it : items) tableModel.add(it);
        if (tableModel.getRowCount() > 0) {
            watchTable.setRowSelectionInterval(0, 0);
        }
        appendLog("讀入 watchlist 共 " + items.size() + " 檔");
    }

    private void addCurrentToWatchlist() {
        String sym = symbolField.getText().trim();
        String nm = nameField.getText().trim();
        if (sym.isEmpty()) {
            JOptionPane.showMessageDialog(this, "請輸入股票代號 (例 2330.TW)", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (nm.isEmpty()) nm = sym;
        if (tableModel.indexOfSymbol(sym) >= 0) {
            JOptionPane.showMessageDialog(this, sym + " 已在 watchlist 中", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        double h = ((Number) highSpinner.getValue()).doubleValue();
        double l = ((Number) lowSpinner.getValue()).doubleValue();
        WatchItem it = new WatchItem(sym, nm, h, l);
        tableModel.add(it);
        ConfigLoader.saveWatchlist(WATCHLIST_PATH, tableModel.items());
        appendLog("加入 " + sym + " " + nm + " (高=" + h + " 低=" + l + ")");
        poller.execute(() -> pollOne(tableModel.indexOfSymbol(sym)));
    }

    private void removeSelected() {
        int row = watchTable.getSelectedRow();
        if (row < 0) return;
        WatchItem it = tableModel.get(row);
        tableModel.remove(row);
        ConfigLoader.saveWatchlist(WATCHLIST_PATH, tableModel.items());
        appendLog("移除 " + it.symbol() + " " + it.name());
    }

    private void applyThresholds() {
        int row = watchTable.getSelectedRow();
        if (row < 0) return;
        double h = ((Number) highSpinner.getValue()).doubleValue();
        double l = ((Number) lowSpinner.getValue()).doubleValue();
        tableModel.updateThresholds(row, h, l);
        ConfigLoader.saveWatchlist(WATCHLIST_PATH, tableModel.items());
        WatchItem it = tableModel.get(row);
        appendLog("更新門檻 " + it.symbol() + " 高=" + h + " 低=" + l);
    }

    private void testNotification() {
        int row = watchTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "請先選取一檔股票", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        WatchItem it = tableModel.get(row);
        Quote q = it.lastQuote();
        if (q == null) q = new Quote(it.symbol(), it.name(), it.highThreshold(),
                it.highThreshold(), it.highThreshold(), it.highThreshold(),
                0, System.currentTimeMillis() / 1000);
        dispatcher.fire(it, q, NotificationDispatcher.AlertType.HIGH);
    }

    private void startPolling() {
        int interval = config.pollIntervalSeconds();
        countdown.set(interval);
        poller.scheduleAtFixedRate(this::pollAllNow, 0, interval, TimeUnit.SECONDS);
    }

    private void startCountdown() {
        Timer timer = new Timer(1000, e -> {
            int v = countdown.decrementAndGet();
            if (v < 0) {
                countdown.set((Integer) intervalSpinner.getValue());
                v = (Integer) intervalSpinner.getValue();
            }
            countdownLabel.setText("下次更新 " + v + "s");
        });
        timer.start();
    }

    private void pollAllNow() {
        int n = tableModel.getRowCount();
        for (int i = 0; i < n; i++) pollOne(i);
        countdown.set((Integer) intervalSpinner.getValue());
    }

    private void pollOne(int row) {
        if (row < 0) return;
        WatchItem it;
        try {
            it = tableModel.get(row);
        } catch (IndexOutOfBoundsException e) { return; }
        client.fetchQuote(it.symbol()).ifPresentOrElse(q -> {
            dispatcher.check(it, q);
            SwingUtilities.invokeLater(() -> {
                int idx = tableModel.indexOfSymbol(it.symbol());
                if (idx >= 0) tableModel.updateQuote(idx, q);
            });
        }, () -> SwingUtilities.invokeLater(() ->
                appendLog("抓取失敗: " + it.symbol())));
    }

    private void loadChart(WatchItem it) {
        chartPanel.setData(it.symbol(), it.name(), new KlineSeries(java.util.List.of()));
        ChartPanel.Mode mode = chartPanel.mode();
        chartLoader.execute(() -> {
            java.util.Optional<KlineSeries> series = switch (mode) {
                case INTRADAY_LINE -> client.fetchIntraday(it.symbol());
                case DAILY_K -> client.fetchDaily(it.symbol());
                case WEEKLY_K -> client.fetchDaily(it.symbol()).map(MainFrame::aggregateWeekly);
            };
            series.ifPresent(s -> SwingUtilities.invokeLater(() ->
                    chartPanel.setData(it.symbol(), it.name(), s)));
            if (series.isEmpty()) {
                SwingUtilities.invokeLater(() -> appendLog("K 線抓取失敗: " + it.symbol()));
            }
        });
    }

    private static KlineSeries aggregateWeekly(KlineSeries daily) {
        java.util.List<Kline> bars = daily.bars();
        java.util.List<Kline> weekly = new java.util.ArrayList<>();
        int i = 0;
        while (i < bars.size()) {
            int end = Math.min(i + 5, bars.size());
            Kline first = bars.get(i);
            double open = first.open();
            double high = first.high();
            double low = first.low();
            double close = first.close();
            long vol = 0;
            long ts = first.timestamp();
            for (int j = i; j < end; j++) {
                Kline k = bars.get(j);
                if (k.high() > high) high = k.high();
                if (k.low() < low) low = k.low();
                close = k.close();
                vol += k.volume();
                ts = k.timestamp();
            }
            weekly.add(new Kline(ts, open, high, low, close, vol));
            i = end;
        }
        return new KlineSeries(weekly);
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            String t = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            logArea.append("[" + t + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void shutdownAndExit() {
        poller.shutdownNow();
        chartLoader.shutdownNow();
        dispatcher.shutdown();
        dispose();
        System.exit(0);
    }
}
