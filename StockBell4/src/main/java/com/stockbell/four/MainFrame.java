package com.stockbell.four;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Arc2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MainFrame extends JFrame {
    private static final String CONFIG_PATH = "config.properties";

    private final YahooClient client = new YahooClient();
    private final NotificationDispatcher dispatcher = new NotificationDispatcher();

    private final JLabel titleLabel = new JLabel(Config.SYMBOL + "  " + Config.NAME);
    private final Gauge gauge = new Gauge();
    private final JSpinner highSpinner = new JSpinner(new SpinnerNumberModel(Config.DEFAULT_HIGH, 0.0, 1_000_000.0, 1.0));
    private final JSpinner lowSpinner = new JSpinner(new SpinnerNumberModel(Config.DEFAULT_LOW, 0.0, 1_000_000.0, 1.0));
    private final JCheckBox trayCheck = new JCheckBox("Tray", true);
    private final JCheckBox consoleCheck = new JCheckBox("Beep", true);
    private final JCheckBox popupCheck = new JCheckBox("彈窗", true);
    private final JCheckBox discordCheck = new JCheckBox("Discord", true);
    private final JTextArea logArea = new JTextArea(4, 30);
    private int intervalSec = 15;

    private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "stockbell-4-poller"); t.setDaemon(true); return t;
    });

    public MainFrame() {
        super("StockBell 4 [儀表板] - " + Config.NAME);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(520, 540);
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
        setLayout(new BorderLayout(0, 6));

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 16, 4, 16));

        JPanel form = new JPanel(new GridLayout(0, 4, 6, 4));
        form.setBorder(BorderFactory.createTitledBorder("設定"));
        form.add(new JLabel("高點"));
        form.add(highSpinner);
        form.add(new JLabel("低點"));
        form.add(lowSpinner);
        JButton applyBtn = new JButton("套用");
        JButton testBtn = new JButton("測試通知");
        form.add(applyBtn);
        form.add(testBtn);

        JPanel checks = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        checks.add(new JLabel("通知:"));
        checks.add(trayCheck);
        checks.add(consoleCheck);
        checks.add(popupCheck);
        checks.add(discordCheck);

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

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.add(gauge, BorderLayout.CENTER);
        center.add(form, BorderLayout.SOUTH);

        add(titleLabel, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.add(checks, BorderLayout.NORTH);
        south.add(logScroll, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    private void pollOnce() {
        double hi = ((Number) highSpinner.getValue()).doubleValue();
        double lo = ((Number) lowSpinner.getValue()).doubleValue();
        client.fetchQuote(Config.SYMBOL).ifPresentOrElse(q -> {
            dispatcher.check(q, hi, lo);
            SwingUtilities.invokeLater(() -> {
                gauge.setValues(q, lo, hi);
                gauge.repaint();
            });
        }, () -> appendLog("抓取失敗"));
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

    private static final class Gauge extends JPanel {
        private Quote quote;
        private double low = 0, high = 1;
        Gauge() { setBackground(new Color(248, 248, 252)); }
        void setValues(Quote q, double lo, double hi) { this.quote = q; this.low = lo; this.high = hi; }
        @Override
        protected void paintComponent(Graphics gg) {
            super.paintComponent(gg);
            Graphics2D g = (Graphics2D) gg.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int size = Math.min(w - 40, (h - 40) * 2);
            int cx = w / 2;
            int cy = h - 40;
            int r = size / 2;
            int sx = cx - r, sy = cy - r;

            g.setStroke(new BasicStroke(22f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(220, 222, 230));
            g.draw(new Arc2D.Double(sx, sy, size, size, 180, -180, Arc2D.OPEN));

            g.setColor(new Color(40, 160, 80));
            g.draw(new Arc2D.Double(sx, sy, size, size, 180, -60, Arc2D.OPEN));
            g.setColor(new Color(240, 180, 50));
            g.draw(new Arc2D.Double(sx, sy, size, size, 120, -60, Arc2D.OPEN));
            g.setColor(new Color(220, 60, 60));
            g.draw(new Arc2D.Double(sx, sy, size, size, 60, -60, Arc2D.OPEN));

            if (quote == null || high <= low) {
                g.setColor(Color.DARK_GRAY);
                g.drawString("尚無資料", cx - 30, cy);
                g.dispose();
                return;
            }

            double pct = Math.max(0, Math.min(1, (quote.price() - low) / (high - low)));
            double angle = Math.toRadians(180 - pct * 180);
            int needleR = r - 14;
            int nx = (int) (cx + Math.cos(angle) * needleR);
            int ny = (int) (cy - Math.sin(angle) * needleR);
            g.setColor(new Color(40, 40, 50));
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(cx, cy, nx, ny);
            g.fillOval(cx - 8, cy - 8, 16, 16);

            g.setColor(Color.DARK_GRAY);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 22f));
            String p = String.format("%.2f", quote.price());
            int pw = g.getFontMetrics().stringWidth(p);
            g.drawString(p, cx - pw / 2, cy - 40);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 12f));
            String pctStr = String.format("區間 %.1f%%", pct * 100);
            int pcw = g.getFontMetrics().stringWidth(pctStr);
            g.drawString(pctStr, cx - pcw / 2, cy - 22);

            g.drawString(String.format("%.0f", low), sx + 8, cy + 18);
            String hiStr = String.format("%.0f", high);
            g.drawString(hiStr, sx + size - g.getFontMetrics().stringWidth(hiStr) - 8, cy + 18);

            g.dispose();
        }
    }
}
