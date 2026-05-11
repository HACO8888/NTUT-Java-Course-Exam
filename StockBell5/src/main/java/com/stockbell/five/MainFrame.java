package com.stockbell.five;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MainFrame extends JFrame {
    private static final String CONFIG_PATH = "config.properties";
    private static final Color BG = new Color(8, 12, 8);
    private static final Color FG = new Color(80, 230, 100);
    private static final Color FG_DIM = new Color(40, 140, 60);
    private static final Color WARN = new Color(255, 100, 80);
    private static final Font MONO = new Font(Font.MONOSPACED, Font.BOLD, 13);

    private final YahooClient client = new YahooClient();
    private final NotificationDispatcher dispatcher = new NotificationDispatcher();

    private final JTextArea terminal = new JTextArea();
    private final JSpinner highSpinner = new JSpinner(new SpinnerNumberModel(Config.DEFAULT_HIGH, 0.0, 1_000_000.0, 1.0));
    private final JSpinner lowSpinner = new JSpinner(new SpinnerNumberModel(Config.DEFAULT_LOW, 0.0, 1_000_000.0, 1.0));
    private final JCheckBox trayCheck = new JCheckBox("TRAY", true);
    private final JCheckBox consoleCheck = new JCheckBox("BEEP", true);
    private final JCheckBox popupCheck = new JCheckBox("DIALOG", true);
    private final JCheckBox discordCheck = new JCheckBox("DISCORD", true);
    private int intervalSec = 15;
    private int tickCount = 0;

    private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "stockbell-5-poller"); t.setDaemon(true); return t;
    });

    public MainFrame() {
        super("StockBell 5 [TERMINAL] - " + Config.NAME);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(580, 540);
        setLocationRelativeTo(null);
        loadConfig();
        initUI();
        dispatcher.setLogSink(this::appendTerminal);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { shutdown(); }
        });
        appendTerminal("StockBell5 TERMINAL v1.0 booting...");
        appendTerminal("symbol: " + Config.SYMBOL + " (" + Config.NAME + ")");
        appendTerminal("config: high=" + Config.DEFAULT_HIGH + " low=" + Config.DEFAULT_LOW);
        appendTerminal("interval: " + intervalSec + "s");
        appendTerminal("connected to query1.finance.yahoo.com");
        appendTerminal("=== ready ===");
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
        setLayout(new BorderLayout(0, 0));

        terminal.setBackground(BG);
        terminal.setForeground(FG);
        terminal.setCaretColor(FG);
        terminal.setFont(MONO);
        terminal.setEditable(false);
        terminal.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        DefaultCaret caret = (DefaultCaret) terminal.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane scroll = new JScrollPane(terminal);
        scroll.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, FG_DIM));
        scroll.getViewport().setBackground(BG);

        JPanel ctrl = new JPanel(new GridLayout(0, 6, 4, 4));
        ctrl.setBackground(BG);
        ctrl.setBorder(BorderFactory.createEmptyBorder(6, 10, 4, 10));
        ctrl.add(termLabel("HIGH"));
        ctrl.add(highSpinner);
        ctrl.add(termLabel("LOW"));
        ctrl.add(lowSpinner);
        JButton applyBtn = new JButton("APPLY");
        JButton testBtn = new JButton("TEST");
        styleBtn(applyBtn); styleBtn(testBtn);
        ctrl.add(applyBtn); ctrl.add(testBtn);

        JPanel checks = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        checks.setBackground(BG);
        checks.setBorder(BorderFactory.createEmptyBorder(0, 10, 8, 10));
        for (JCheckBox cb : new JCheckBox[]{trayCheck, consoleCheck, popupCheck, discordCheck}) {
            cb.setBackground(BG); cb.setForeground(FG_DIM); cb.setFont(MONO);
        }
        JLabel nLbl = termLabel("> chan:");
        checks.add(nLbl);
        checks.add(trayCheck); checks.add(consoleCheck);
        checks.add(popupCheck); checks.add(discordCheck);

        applyBtn.addActionListener(e -> appendTerminal(String.format("> apply thresholds high=%.2f low=%.2f",
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

        add(scroll, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout(0, 0));
        south.setBackground(BG);
        south.add(ctrl, BorderLayout.NORTH);
        south.add(checks, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);
    }

    private JLabel termLabel(String s) {
        JLabel l = new JLabel(s);
        l.setForeground(FG_DIM);
        l.setFont(MONO);
        return l;
    }

    private void styleBtn(JButton b) {
        b.setBackground(BG);
        b.setForeground(FG);
        b.setFont(MONO);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(FG_DIM));
    }

    private void pollOnce() {
        tickCount++;
        double hi = ((Number) highSpinner.getValue()).doubleValue();
        double lo = ((Number) lowSpinner.getValue()).doubleValue();
        client.fetchQuote(Config.SYMBOL).ifPresentOrElse(q -> {
            dispatcher.check(q, hi, lo);
            SwingUtilities.invokeLater(() -> renderTick(q, hi, lo));
        }, () -> appendTerminal("[ERR] fetch failed"));
    }

    private void renderTick(Quote q, double hi, double lo) {
        String t = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String arrow = q.change() > 0 ? "▲" : (q.change() < 0 ? "▼" : "—");
        String line1 = String.format("[%s] tick #%04d  %s %s", t, tickCount, q.symbol(), q.name());
        String line2 = String.format("   price=%.2f  chg=%+.2f (%+.2f%%) %s  hi=%.2f  lo=%.2f",
                q.price(), q.change(), q.changePercent(), arrow, q.dayHigh(), q.dayLow());
        String bar = barString(q.price(), lo, hi, 40);
        String line3 = String.format("   [%s]  range %.0f-%.0f", bar, lo, hi);
        appendTerminal(line1);
        appendTerminal(line2);
        appendTerminal(line3);
        NotificationDispatcher.Direction d = dispatcher.lastDirection();
        if (d != null) {
            String msg = d == NotificationDispatcher.Direction.HIGH ? "*** HIGH BREACH ***" : "*** LOW BREACH ***";
            appendTerminalColor(msg, WARN);
        }
    }

    private String barString(double price, double lo, double hi, int width) {
        if (hi <= lo) return ".".repeat(width);
        double pct = Math.max(0, Math.min(1, (price - lo) / (hi - lo)));
        int pos = (int) Math.round(pct * (width - 1));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width; i++) sb.append(i == pos ? '#' : (i < pos ? '=' : '-'));
        return sb.toString();
    }

    private void appendTerminal(String msg) {
        SwingUtilities.invokeLater(() -> {
            terminal.append(msg + "\n");
            terminal.setCaretPosition(terminal.getDocument().getLength());
        });
    }

    private void appendTerminalColor(String msg, Color c) { appendTerminal(msg); }

    private void shutdown() {
        poller.shutdownNow();
        dispatcher.shutdown();
        dispose();
        System.exit(0);
    }
}
