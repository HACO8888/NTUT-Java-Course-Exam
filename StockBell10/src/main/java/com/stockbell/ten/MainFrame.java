package com.stockbell.ten;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MainFrame extends JFrame {
    private static final String CONFIG_PATH = "config.properties";
    private static final Color BG = new Color(245, 245, 250);
    private static final Color RED = new Color(220, 60, 60);
    private static final Color GREEN = new Color(40, 160, 80);
    private static final Color HIGHLIGHT = new Color(255, 245, 200);

    private static final String[][] PEERS = {
            {Config.SYMBOL,  Config.NAME},
            {"2881.TW",      "富邦金"},
            {"2882.TW",      "國泰金"},
            {"2412.TW",      "中華電"},
            {"2303.TW",      "聯電"}
    };

    private final YahooClient client = new YahooClient();
    private final NotificationDispatcher dispatcher = new NotificationDispatcher();
    private final RankModel model = new RankModel();
    private final JTable table = new JTable(model);
    private final JLabel titleLabel = new JLabel(String.format(
            "%s %s 漲跌幅排行榜（追蹤主標的：%s）", Config.SYMBOL, Config.NAME, Config.NAME));
    private final JSpinner highSpinner = new JSpinner(new SpinnerNumberModel(Config.DEFAULT_HIGH, 0.0, 1_000_000.0, 1.0));
    private final JSpinner lowSpinner = new JSpinner(new SpinnerNumberModel(Config.DEFAULT_LOW, 0.0, 1_000_000.0, 1.0));
    private final JCheckBox trayCheck = new JCheckBox("Tray", true);
    private final JCheckBox consoleCheck = new JCheckBox("Beep", true);
    private final JCheckBox popupCheck = new JCheckBox("彈窗", true);
    private final JCheckBox discordCheck = new JCheckBox("Discord", true);
    private final JTextArea logArea = new JTextArea(4, 30);
    private int intervalSec = 15;

    private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "stockbell-10-poller"); t.setDaemon(true); return t;
    });

    public MainFrame() {
        super("StockBell 10 [排行榜] - " + Config.NAME);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(620, 480);
        setLocationRelativeTo(null);
        loadConfig();
        initUI();
        dispatcher.setLogSink(this::appendLog);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { shutdown(); }
        });
        poller.scheduleAtFixedRate(this::pollOnce, 0, intervalSec, TimeUnit.SECONDS);
    }

    private void loadConfig() {
        Path p = Paths.get(CONFIG_PATH);
        if (!Files.exists(p)) return;
        Properties props = new Properties();
        try { props.load(Files.newBufferedReader(p)); } catch (IOException e) { return; }
        dispatcher.setWebhookUrl(props.getProperty("discord.webhook.url", "").trim());
        dispatcher.setUsername(props.getProperty("discord.username", Config.DEFAULT_USERNAME).trim());
        try { intervalSec = Integer.parseInt(props.getProperty("poll.interval.seconds", "15").trim()); } catch (Exception ignored) {}
        String ch = props.getProperty("notify.channels", "TRAY,CONSOLE,POPUP,DISCORD");
        java.util.Set<String> set = new java.util.HashSet<>();
        for (String s : ch.split(",")) set.add(s.trim().toUpperCase());
        trayCheck.setSelected(set.contains("TRAY"));
        consoleCheck.setSelected(set.contains("CONSOLE"));
        popupCheck.setSelected(set.contains("POPUP"));
        discordCheck.setSelected(set.contains("DISCORD"));
        syncChannels();
    }

    private void syncChannels() {
        dispatcher.setChannelEnabled(NotificationDispatcher.Channel.TRAY, trayCheck.isSelected());
        dispatcher.setChannelEnabled(NotificationDispatcher.Channel.CONSOLE, consoleCheck.isSelected());
        dispatcher.setChannelEnabled(NotificationDispatcher.Channel.POPUP, popupCheck.isSelected());
        dispatcher.setChannelEnabled(NotificationDispatcher.Channel.DISCORD, discordCheck.isSelected());
    }

    private void initUI() {
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(0, 6));

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 14, 4, 14));

        table.setRowHeight(28);
        table.setFont(table.getFont().deriveFont(13f));
        DefaultTableCellRenderer right = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean f, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, f, row, col);
                setHorizontalAlignment(SwingConstants.RIGHT);
                Row r = model.rows.get(row);
                boolean main = r.symbol.equalsIgnoreCase(Config.SYMBOL);
                if (!sel) c.setBackground(main ? HIGHLIGHT : Color.WHITE);
                if (col == 3 || col == 4) {
                    c.setForeground(r.changePct > 0 ? RED : (r.changePct < 0 ? GREEN : Color.DARK_GRAY));
                } else {
                    c.setForeground(Color.DARK_GRAY);
                }
                setFont(getFont().deriveFont(main ? Font.BOLD : Font.PLAIN));
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(right);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));

        JPanel form = new JPanel(new GridLayout(0, 4, 6, 4));
        form.setBackground(BG);
        form.setBorder(BorderFactory.createEmptyBorder(6, 14, 4, 14));
        form.add(new JLabel("主標的 高點"));
        form.add(highSpinner);
        form.add(new JLabel("主標的 低點"));
        form.add(lowSpinner);
        JButton applyBtn = new JButton("套用");
        JButton testBtn = new JButton("測試通知");
        form.add(applyBtn); form.add(testBtn);

        JPanel checks = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        checks.setBackground(BG);
        checks.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        checks.add(new JLabel("通知:"));
        checks.add(trayCheck); checks.add(consoleCheck);
        checks.add(popupCheck); checks.add(discordCheck);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("記錄"));

        applyBtn.addActionListener(e -> appendLog(String.format("套用 高=%.2f 低=%.2f",
                ((Number) highSpinner.getValue()).doubleValue(),
                ((Number) lowSpinner.getValue()).doubleValue())));
        testBtn.addActionListener(e -> {
            double hi = ((Number) highSpinner.getValue()).doubleValue();
            Quote q = new Quote(Config.SYMBOL, Config.NAME, hi, hi, hi, hi, 0, System.currentTimeMillis() / 1000);
            dispatcher.fire(q, NotificationDispatcher.AlertType.HIGH, hi);
        });
        java.awt.event.ActionListener sync = e -> syncChannels();
        trayCheck.addActionListener(sync); consoleCheck.addActionListener(sync);
        popupCheck.addActionListener(sync); discordCheck.addActionListener(sync);

        add(titleLabel, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.setBackground(BG);
        south.add(form, BorderLayout.NORTH);
        south.add(checks, BorderLayout.CENTER);
        south.add(logScroll, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);
    }

    private void pollOnce() {
        List<Row> rows = new ArrayList<>();
        Quote mainQuote = null;
        for (String[] pair : PEERS) {
            Quote q = client.fetchQuote(pair[0]).orElse(null);
            if (q == null) continue;
            if (pair[0].equalsIgnoreCase(Config.SYMBOL)) mainQuote = q;
            rows.add(new Row(pair[0], pair[1], q.price(), q.change(), q.changePercent()));
        }
        rows.sort(Comparator.comparingDouble((Row r) -> r.changePct).reversed());
        if (mainQuote != null) {
            double hi = ((Number) highSpinner.getValue()).doubleValue();
            double lo = ((Number) lowSpinner.getValue()).doubleValue();
            dispatcher.check(mainQuote, hi, lo);
        }
        SwingUtilities.invokeLater(() -> {
            model.setRows(rows);
        });
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void shutdown() {
        poller.shutdownNow();
        dispatcher.shutdown();
        dispose();
        System.exit(0);
    }

    private static final class Row {
        final String symbol, name;
        final double price, change, changePct;
        Row(String s, String n, double p, double c, double cp) {
            symbol = s; name = n; price = p; change = c; changePct = cp;
        }
    }

    private static final class RankModel extends AbstractTableModel {
        private static final String[] COLS = {"名次", "代號", "名稱", "現價", "漲跌%"};
        private final List<Row> rows = new ArrayList<>();
        void setRows(List<Row> r) { rows.clear(); rows.addAll(r); fireTableDataChanged(); }
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override
        public Object getValueAt(int r, int c) {
            Row row = rows.get(r);
            return switch (c) {
                case 0 -> r + 1;
                case 1 -> row.symbol;
                case 2 -> row.name;
                case 3 -> String.format("%.2f", row.price);
                case 4 -> String.format("%+.2f%%", row.changePct);
                default -> "";
            };
        }
    }
}
