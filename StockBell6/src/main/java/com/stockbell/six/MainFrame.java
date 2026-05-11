package com.stockbell.six;

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
    private static final Color BG = new Color(20, 22, 30);
    private static final Color BAR_BG = new Color(8, 10, 18);
    private static final Color FG = new Color(255, 220, 90);
    private static final Color UP = new Color(255, 90, 90);
    private static final Color DOWN = new Color(60, 220, 130);

    private final YahooClient client = new YahooClient();
    private final NotificationDispatcher dispatcher = new NotificationDispatcher();

    private final TickerPanel ticker = new TickerPanel();
    private final JLabel titleLabel = new JLabel("StockBell 6  ★  " + Config.SYMBOL + "  " + Config.NAME);
    private final JLabel statusLabel = new JLabel("● 待機");
    private final JSpinner highSpinner = new JSpinner(new SpinnerNumberModel(Config.DEFAULT_HIGH, 0.0, 1_000_000.0, 1.0));
    private final JSpinner lowSpinner = new JSpinner(new SpinnerNumberModel(Config.DEFAULT_LOW, 0.0, 1_000_000.0, 1.0));
    private final JCheckBox trayCheck = new JCheckBox("Tray", true);
    private final JCheckBox consoleCheck = new JCheckBox("Beep", true);
    private final JCheckBox popupCheck = new JCheckBox("彈窗", true);
    private final JCheckBox discordCheck = new JCheckBox("Discord", true);
    private final JTextArea logArea = new JTextArea(4, 30);
    private int intervalSec = 15;

    private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "stockbell-6-poller"); t.setDaemon(true); return t;
    });

    public MainFrame() {
        super("StockBell 6 [跑馬燈] - " + Config.NAME);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(640, 420);
        setLocationRelativeTo(null);
        loadConfig();
        initUI();
        dispatcher.setLogSink(this::appendLog);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { shutdown(); }
        });
        poller.scheduleAtFixedRate(this::pollOnce, 0, intervalSec, TimeUnit.SECONDS);
        ticker.start();
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

        titleLabel.setForeground(FG);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 16, 4, 16));

        ticker.setPreferredSize(new Dimension(0, 72));
        JPanel form = new JPanel(new GridLayout(0, 4, 6, 4));
        form.setBackground(BG);
        form.setBorder(BorderFactory.createEmptyBorder(8, 16, 4, 16));
        JLabel l1 = new JLabel("高點"); l1.setForeground(FG);
        JLabel l2 = new JLabel("低點"); l2.setForeground(FG);
        form.add(l1); form.add(highSpinner); form.add(l2); form.add(lowSpinner);
        JButton applyBtn = new JButton("套用");
        JButton testBtn = new JButton("測試通知");
        form.add(applyBtn); form.add(testBtn);

        JPanel checks = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        checks.setBackground(BG);
        checks.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        for (JCheckBox cb : new JCheckBox[]{trayCheck, consoleCheck, popupCheck, discordCheck}) {
            cb.setBackground(BG); cb.setForeground(FG);
        }
        JLabel notiLbl = new JLabel("通知:"); notiLbl.setForeground(FG);
        checks.add(notiLbl); checks.add(trayCheck); checks.add(consoleCheck);
        checks.add(popupCheck); checks.add(discordCheck);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        logArea.setBackground(BAR_BG);
        logArea.setForeground(FG);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 16));
        logScroll.getViewport().setBackground(BAR_BG);

        statusLabel.setForeground(FG);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 16, 4, 16));

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

        JPanel north = new JPanel(new BorderLayout(0, 4));
        north.setBackground(BG);
        north.add(titleLabel, BorderLayout.NORTH);
        north.add(ticker, BorderLayout.CENTER);
        north.add(statusLabel, BorderLayout.SOUTH);

        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.setBackground(BG);
        south.add(form, BorderLayout.NORTH);
        south.add(checks, BorderLayout.CENTER);
        south.add(logScroll, BorderLayout.SOUTH);

        add(north, BorderLayout.NORTH);
        add(south, BorderLayout.CENTER);
    }

    private void pollOnce() {
        double hi = ((Number) highSpinner.getValue()).doubleValue();
        double lo = ((Number) lowSpinner.getValue()).doubleValue();
        client.fetchQuote(Config.SYMBOL).ifPresentOrElse(q -> {
            dispatcher.check(q, hi, lo);
            SwingUtilities.invokeLater(() -> {
                ticker.setQuote(q, hi, lo);
                String t = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                statusLabel.setText("● 最新 " + t + "  " + q.price());
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

    private static final class TickerPanel extends JPanel {
        private String text = "● 載入中...";
        private Color textColor = FG;
        private int offset = 0;
        private final Timer animTimer;

        TickerPanel() {
            setBackground(BAR_BG);
            animTimer = new Timer(30, e -> {
                offset -= 2;
                repaint();
            });
        }

        void start() { animTimer.start(); }

        void setQuote(Quote q, double hi, double lo) {
            String arrow = q.change() > 0 ? "▲" : (q.change() < 0 ? "▼" : "—");
            textColor = q.change() > 0 ? UP : (q.change() < 0 ? DOWN : FG);
            text = String.format("  ⌚ %s  ★ %s %s  ◆ 現價 %.2f  %s %+.2f (%+.2f%%)  ◆ 今高 %.2f  今低 %.2f  ◆ 高門檻 %.2f  低門檻 %.2f  ◆◆◆",
                    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    q.symbol(), q.name(), q.price(), arrow, q.change(), q.changePercent(),
                    q.dayHigh(), q.dayLow(), hi, lo);
        }

        @Override
        protected void paintComponent(Graphics gg) {
            super.paintComponent(gg);
            Graphics2D g = (Graphics2D) gg.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
            FontMetrics fm = g.getFontMetrics();
            int textW = fm.stringWidth(text);
            int y = (h + fm.getAscent() - fm.getDescent()) / 2;
            if (textW > 0) {
                int draws = w / textW + 2;
                int startX = offset % textW;
                for (int i = -1; i <= draws; i++) {
                    int x = startX + i * textW;
                    g.setColor(textColor);
                    g.drawString(text, x, y);
                }
                if (offset <= -textW) offset = 0;
            }
            g.dispose();
        }
    }
}
