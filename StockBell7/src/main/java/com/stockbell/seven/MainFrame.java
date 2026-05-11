package com.stockbell.seven;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.GeneralPath;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MainFrame extends JFrame {
    private static final String CONFIG_PATH = "config.properties";
    private static final Color BG = new Color(252, 252, 254);
    private static final Color RED = new Color(220, 60, 60);
    private static final Color GREEN = new Color(40, 160, 80);
    private static final Color LINE = new Color(70, 130, 220);
    private static final Color LINE_FILL = new Color(70, 130, 220, 60);

    private final YahooClient client = new YahooClient();
    private final NotificationDispatcher dispatcher = new NotificationDispatcher();

    private final JLabel titleLabel = new JLabel(Config.SYMBOL + "  " + Config.NAME);
    private final JLabel priceLabel = new JLabel("---");
    private final JLabel changeLabel = new JLabel("---");
    private final JLabel rangeLabel = new JLabel("30 日走勢");
    private final Sparkline spark = new Sparkline();
    private final JSpinner highSpinner = new JSpinner(new SpinnerNumberModel(Config.DEFAULT_HIGH, 0.0, 1_000_000.0, 1.0));
    private final JSpinner lowSpinner = new JSpinner(new SpinnerNumberModel(Config.DEFAULT_LOW, 0.0, 1_000_000.0, 1.0));
    private final JCheckBox trayCheck = new JCheckBox("Tray", true);
    private final JCheckBox consoleCheck = new JCheckBox("Beep", true);
    private final JCheckBox popupCheck = new JCheckBox("彈窗", true);
    private final JCheckBox discordCheck = new JCheckBox("Discord", true);
    private final JTextArea logArea = new JTextArea(4, 30);
    private int intervalSec = 15;

    private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "stockbell-7-poller"); t.setDaemon(true); return t;
    });
    private final ScheduledExecutorService chartLoader = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "stockbell-7-chart"); t.setDaemon(true); return t;
    });

    public MainFrame() {
        super("StockBell 7 [Sparkline] - " + Config.NAME);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(560, 520);
        setLocationRelativeTo(null);
        loadConfig();
        initUI();
        dispatcher.setLogSink(this::appendLog);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { shutdown(); }
        });
        poller.scheduleAtFixedRate(this::pollOnce, 0, intervalSec, TimeUnit.SECONDS);
        chartLoader.scheduleAtFixedRate(this::reloadChart, 0, 5, TimeUnit.MINUTES);
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

        JPanel head = new JPanel();
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setBorder(BorderFactory.createEmptyBorder(12, 16, 4, 16));
        head.setBackground(BG);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        priceLabel.setFont(priceLabel.getFont().deriveFont(Font.BOLD, 36f));
        changeLabel.setFont(changeLabel.getFont().deriveFont(14f));
        head.add(titleLabel);
        head.add(Box.createVerticalStrut(4));
        head.add(priceLabel);
        head.add(changeLabel);

        JPanel chartWrap = new JPanel(new BorderLayout(0, 4));
        chartWrap.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        chartWrap.setBackground(BG);
        rangeLabel.setForeground(Color.GRAY);
        chartWrap.add(rangeLabel, BorderLayout.NORTH);
        chartWrap.add(spark, BorderLayout.CENTER);
        spark.setPreferredSize(new Dimension(0, 160));

        JPanel form = new JPanel(new GridLayout(0, 4, 6, 4));
        form.setBackground(BG);
        form.setBorder(BorderFactory.createEmptyBorder(8, 16, 4, 16));
        form.add(new JLabel("高點"));
        form.add(highSpinner);
        form.add(new JLabel("低點"));
        form.add(lowSpinner);
        JButton applyBtn = new JButton("套用");
        JButton testBtn = new JButton("測試通知");
        form.add(applyBtn); form.add(testBtn);

        JPanel checks = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        checks.setBackground(BG);
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

        JPanel center = new JPanel(new BorderLayout(0, 4));
        center.setBackground(BG);
        center.add(chartWrap, BorderLayout.CENTER);
        center.add(form, BorderLayout.SOUTH);

        add(head, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.setBackground(BG);
        south.add(checks, BorderLayout.NORTH);
        south.add(logScroll, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    private void pollOnce() {
        double hi = ((Number) highSpinner.getValue()).doubleValue();
        double lo = ((Number) lowSpinner.getValue()).doubleValue();
        client.fetchQuote(Config.SYMBOL).ifPresentOrElse(q -> {
            dispatcher.check(q, hi, lo);
            SwingUtilities.invokeLater(() -> updateUI(q));
        }, () -> appendLog("抓取失敗"));
    }

    private void updateUI(Quote q) {
        priceLabel.setText(String.format("%.2f", q.price()));
        double ch = q.change();
        Color cc = ch > 0 ? RED : (ch < 0 ? GREEN : Color.DARK_GRAY);
        priceLabel.setForeground(cc);
        changeLabel.setText(String.format("%s %+.2f (%+.2f%%)", ch > 0 ? "▲" : (ch < 0 ? "▼" : "—"), ch, q.changePercent()));
        changeLabel.setForeground(cc);
    }

    private void reloadChart() {
        client.fetchDailyCloses(Config.SYMBOL, 30).ifPresentOrElse(closes -> {
            SwingUtilities.invokeLater(() -> {
                spark.setData(closes);
                rangeLabel.setText(String.format("30 日走勢   最高 %.2f   最低 %.2f",
                        java.util.Arrays.stream(closes).max().orElse(0),
                        java.util.Arrays.stream(closes).min().orElse(0)));
            });
        }, () -> appendLog("K 線抓取失敗"));
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void shutdown() {
        poller.shutdownNow();
        chartLoader.shutdownNow();
        dispatcher.shutdown();
        dispose();
        System.exit(0);
    }

    private static final class Sparkline extends JPanel {
        private double[] data = new double[0];
        Sparkline() { setBackground(BG); }
        void setData(double[] d) { this.data = d == null ? new double[0] : d; repaint(); }
        @Override
        protected void paintComponent(Graphics gg) {
            super.paintComponent(gg);
            Graphics2D g = (Graphics2D) gg.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int padL = 8, padR = 8, padT = 8, padB = 18;
            int plotW = w - padL - padR, plotH = h - padT - padB;
            if (data.length < 2 || plotW <= 0 || plotH <= 0) {
                g.setColor(Color.GRAY);
                g.drawString("(載入歷史資料中...)", w / 2 - 60, h / 2);
                g.dispose();
                return;
            }
            double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
            for (double v : data) { if (v < min) min = v; if (v > max) max = v; }
            if (max <= min) max = min + 1;
            double range = max - min;

            g.setColor(new Color(220, 222, 230));
            for (int i = 0; i <= 3; i++) {
                int y = padT + i * plotH / 3;
                g.drawLine(padL, y, padL + plotW, y);
            }

            GeneralPath line = new GeneralPath();
            GeneralPath fill = new GeneralPath();
            double dx = (double) plotW / (data.length - 1);
            for (int i = 0; i < data.length; i++) {
                float x = (float) (padL + i * dx);
                float y = (float) (padT + (max - data[i]) / range * plotH);
                if (i == 0) { line.moveTo(x, y); fill.moveTo(x, padT + plotH); fill.lineTo(x, y); }
                else { line.lineTo(x, y); fill.lineTo(x, y); }
            }
            fill.lineTo(padL + plotW, padT + plotH);
            fill.closePath();
            g.setColor(LINE_FILL);
            g.fill(fill);
            g.setColor(LINE);
            g.setStroke(new BasicStroke(2f));
            g.draw(line);

            g.setColor(Color.GRAY);
            g.setFont(g.getFont().deriveFont(10f));
            g.drawString(String.format("%.2f", max), padL + 2, padT + 10);
            g.drawString(String.format("%.2f", min), padL + 2, padT + plotH - 2);
            g.dispose();
        }
    }
}
