package com.stockbell.two;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
    private static final Color BG = new Color(245, 245, 250);
    private static final Color RED = new Color(220, 60, 60);
    private static final Color GREEN = new Color(40, 160, 80);

    private final YahooClient client = new YahooClient();
    private final NotificationDispatcher dispatcher = new NotificationDispatcher();

    private final JLabel titleLabel = new JLabel(Config.SYMBOL + "  " + Config.NAME);
    private final JLabel priceLabel = new JLabel("---");
    private final JLabel changeLabel = new JLabel("---");
    private final JLabel toHighLabel = new JLabel("距高: -");
    private final JLabel toLowLabel = new JLabel("距低: -");
    private final RangeBar rangeBar = new RangeBar();
    private final JSpinner highSpinner = new JSpinner(new SpinnerNumberModel(Config.DEFAULT_HIGH, 0.0, 1_000_000.0, 1.0));
    private final JSpinner lowSpinner = new JSpinner(new SpinnerNumberModel(Config.DEFAULT_LOW, 0.0, 1_000_000.0, 1.0));
    private final JCheckBox trayCheck = new JCheckBox("Tray", true);
    private final JCheckBox consoleCheck = new JCheckBox("Beep", true);
    private final JCheckBox popupCheck = new JCheckBox("彈窗", true);
    private final JCheckBox discordCheck = new JCheckBox("Discord", true);
    private final JTextArea logArea = new JTextArea(4, 30);
    private int intervalSec = 15;

    private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "stockbell-2-poller"); t.setDaemon(true); return t;
    });

    public MainFrame() {
        super("StockBell 2 [進度條] - " + Config.NAME);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(500, 480);
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
        setLayout(new BorderLayout(0, 8));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(BorderFactory.createEmptyBorder(14, 18, 6, 18));
        top.setBackground(BG);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        priceLabel.setFont(priceLabel.getFont().deriveFont(Font.BOLD, 36f));
        changeLabel.setFont(changeLabel.getFont().deriveFont(14f));
        top.add(titleLabel);
        top.add(Box.createVerticalStrut(4));
        top.add(priceLabel);
        top.add(Box.createVerticalStrut(2));
        top.add(changeLabel);

        JPanel barPanel = new JPanel();
        barPanel.setLayout(new BoxLayout(barPanel, BoxLayout.Y_AXIS));
        barPanel.setBorder(BorderFactory.createEmptyBorder(0, 18, 6, 18));
        barPanel.setBackground(BG);
        JPanel labels = new JPanel(new BorderLayout());
        labels.setBackground(BG);
        labels.add(toLowLabel, BorderLayout.WEST);
        labels.add(toHighLabel, BorderLayout.EAST);
        barPanel.add(labels);
        rangeBar.setPreferredSize(new Dimension(0, 36));
        barPanel.add(Box.createVerticalStrut(4));
        barPanel.add(rangeBar);

        JPanel form = new JPanel(new GridLayout(0, 4, 6, 4));
        form.setBorder(BorderFactory.createTitledBorder("設定"));
        form.setBackground(BG);
        form.add(new JLabel("高點"));
        form.add(highSpinner);
        form.add(new JLabel("低點"));
        form.add(lowSpinner);
        JButton applyBtn = new JButton("套用");
        JButton testBtn = new JButton("測試通知");
        form.add(applyBtn);
        form.add(testBtn);

        JPanel checks = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        checks.setBackground(BG);
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
        center.setBackground(BG);
        center.add(barPanel, BorderLayout.NORTH);
        center.add(form, BorderLayout.CENTER);
        center.add(checks, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(logScroll, BorderLayout.SOUTH);
    }

    private void pollOnce() {
        double hi = ((Number) highSpinner.getValue()).doubleValue();
        double lo = ((Number) lowSpinner.getValue()).doubleValue();
        client.fetchQuote(Config.SYMBOL).ifPresentOrElse(q -> {
            dispatcher.check(q, hi, lo);
            SwingUtilities.invokeLater(() -> updateUI(q, hi, lo));
        }, () -> appendLog("抓取失敗"));
    }

    private void updateUI(Quote q, double hi, double lo) {
        priceLabel.setText(String.format("%.2f", q.price()));
        double ch = q.change();
        Color cc = ch > 0 ? RED : (ch < 0 ? GREEN : Color.DARK_GRAY);
        priceLabel.setForeground(cc);
        changeLabel.setText(String.format("%s %+.2f (%+.2f%%)", ch > 0 ? "▲" : (ch < 0 ? "▼" : "—"), ch, q.changePercent()));
        changeLabel.setForeground(cc);
        toHighLabel.setText(String.format("距高 %.2f: %+.2f (%+.2f%%)", hi, q.price() - hi, (q.price() - hi) / hi * 100));
        toLowLabel.setText(String.format("距低 %.2f: %+.2f (%+.2f%%)", lo, q.price() - lo, (q.price() - lo) / lo * 100));
        rangeBar.setValues(q.price(), lo, hi);
        rangeBar.repaint();
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

    private static final class RangeBar extends JPanel {
        private double price = Double.NaN, low = 0, high = 1;
        RangeBar() { setBackground(BG); }
        void setValues(double price, double low, double high) {
            this.price = price; this.low = low; this.high = high;
        }
        @Override
        protected void paintComponent(Graphics gg) {
            super.paintComponent(gg);
            Graphics2D g = (Graphics2D) gg.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int barH = 22, y = (h - barH) / 2;
            g.setColor(new Color(220, 222, 230));
            g.fillRoundRect(0, y, w, barH, 12, 12);
            if (Double.isNaN(price) || high <= low) { g.dispose(); return; }
            double pct = (price - low) / (high - low);
            int fillEnd = (int) Math.round(Math.max(0, Math.min(1, pct)) * w);
            Color fill;
            if (price >= high) fill = RED;
            else if (price <= low) fill = GREEN;
            else fill = new Color(255, 170, 60);
            g.setColor(fill);
            g.fillRoundRect(0, y, fillEnd, barH, 12, 12);
            g.setColor(Color.DARK_GRAY);
            g.drawString(String.format("%.2f", low), 4, y - 4);
            String hiStr = String.format("%.2f", high);
            g.drawString(hiStr, w - g.getFontMetrics().stringWidth(hiStr) - 4, y - 4);
            String pctStr = String.format("%.1f%%", pct * 100);
            int sx = (w - g.getFontMetrics().stringWidth(pctStr)) / 2;
            g.setColor(Color.WHITE);
            g.drawString(pctStr, sx, y + barH - 6);
            g.dispose();
        }
    }
}
