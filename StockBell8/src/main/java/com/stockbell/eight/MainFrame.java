package com.stockbell.eight;

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
    private static final Color BG = new Color(28, 30, 36);
    private static final Color SHELL = new Color(48, 50, 58);
    private static final Color FG = new Color(220, 222, 230);
    private static final Color FG_DIM = new Color(150, 152, 160);

    public enum LightState { OFF, GREEN, YELLOW, RED }

    private final YahooClient client = new YahooClient();
    private final NotificationDispatcher dispatcher = new NotificationDispatcher();

    private final TrafficLight light = new TrafficLight();
    private final JLabel titleLabel = new JLabel(Config.SYMBOL + "  " + Config.NAME);
    private final JLabel priceLabel = new JLabel("---");
    private final JLabel changeLabel = new JLabel("---");
    private final JLabel stateLabel = new JLabel("待機");
    private final JSpinner highSpinner = new JSpinner(new SpinnerNumberModel(Config.DEFAULT_HIGH, 0.0, 1_000_000.0, 1.0));
    private final JSpinner lowSpinner = new JSpinner(new SpinnerNumberModel(Config.DEFAULT_LOW, 0.0, 1_000_000.0, 1.0));
    private final JCheckBox trayCheck = new JCheckBox("Tray", true);
    private final JCheckBox consoleCheck = new JCheckBox("Beep", true);
    private final JCheckBox popupCheck = new JCheckBox("彈窗", true);
    private final JCheckBox discordCheck = new JCheckBox("Discord", true);
    private final JTextArea logArea = new JTextArea(4, 30);
    private int intervalSec = 15;

    private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "stockbell-8-poller"); t.setDaemon(true); return t;
    });

    public MainFrame() {
        super("StockBell 8 [紅綠燈] - " + Config.NAME);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(520, 580);
        setLocationRelativeTo(null);
        loadConfig();
        initUI();
        dispatcher.setLogSink(this::appendLog);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { shutdown(); }
        });
        poller.scheduleAtFixedRate(this::pollOnce, 0, intervalSec, TimeUnit.SECONDS);
        Timer blink = new Timer(500, e -> light.tickBlink());
        blink.start();
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

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(BG);
        info.setBorder(BorderFactory.createEmptyBorder(0, 16, 4, 16));
        priceLabel.setForeground(FG);
        priceLabel.setFont(priceLabel.getFont().deriveFont(Font.BOLD, 32f));
        changeLabel.setForeground(FG_DIM);
        stateLabel.setForeground(FG_DIM);
        info.add(priceLabel);
        info.add(changeLabel);
        info.add(Box.createVerticalStrut(4));
        info.add(stateLabel);

        JPanel north = new JPanel(new BorderLayout(0, 4));
        north.setBackground(BG);
        north.add(titleLabel, BorderLayout.NORTH);
        north.add(info, BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(0, 4, 6, 4));
        form.setBackground(BG);
        form.setBorder(BorderFactory.createEmptyBorder(8, 16, 4, 16));
        JLabel l1 = new JLabel("高點"); l1.setForeground(FG_DIM);
        JLabel l2 = new JLabel("低點"); l2.setForeground(FG_DIM);
        form.add(l1); form.add(highSpinner); form.add(l2); form.add(lowSpinner);
        JButton applyBtn = new JButton("套用");
        JButton testBtn = new JButton("測試通知");
        form.add(applyBtn); form.add(testBtn);

        JPanel checks = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        checks.setBackground(BG);
        for (JCheckBox cb : new JCheckBox[]{trayCheck, consoleCheck, popupCheck, discordCheck}) {
            cb.setBackground(BG); cb.setForeground(FG_DIM);
        }
        JLabel nLbl = new JLabel("通知:"); nLbl.setForeground(FG_DIM);
        checks.add(nLbl); checks.add(trayCheck); checks.add(consoleCheck);
        checks.add(popupCheck); checks.add(discordCheck);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        logArea.setBackground(SHELL);
        logArea.setForeground(FG_DIM);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 16));

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

        add(north, BorderLayout.NORTH);
        add(light, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.setBackground(BG);
        south.add(form, BorderLayout.NORTH);
        south.add(checks, BorderLayout.CENTER);
        south.add(logScroll, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);
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
        changeLabel.setText(String.format("%s %+.2f (%+.2f%%)", ch > 0 ? "▲" : (ch < 0 ? "▼" : "—"), ch, q.changePercent()));
        LightState s;
        if (q.price() >= hi) { s = LightState.RED; stateLabel.setText("⚠ 突破高點 " + hi); stateLabel.setForeground(new Color(255, 100, 100)); }
        else if (q.price() <= lo) { s = LightState.GREEN; stateLabel.setText("⚠ 跌破低點 " + lo); stateLabel.setForeground(new Color(60, 220, 130)); }
        else { s = LightState.YELLOW; stateLabel.setText("● 正常區間"); stateLabel.setForeground(FG_DIM); }
        light.setState(s);
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

    private static final class TrafficLight extends JPanel {
        private LightState state = LightState.OFF;
        private boolean blinkOn = true;
        TrafficLight() { setBackground(BG); }
        void setState(LightState s) { this.state = s; repaint(); }
        void tickBlink() { blinkOn = !blinkOn; if (state == LightState.RED) repaint(); }
        @Override
        protected void paintComponent(Graphics gg) {
            super.paintComponent(gg);
            Graphics2D g = (Graphics2D) gg.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int d = Math.min(h / 4, 100);
            int shellW = d + 30;
            int shellH = d * 3 + 50;
            int sx = (w - shellW) / 2;
            int sy = (h - shellH) / 2;
            g.setColor(SHELL);
            g.fillRoundRect(sx, sy, shellW, shellH, 18, 18);

            int cx = w / 2;
            int[] cys = { sy + 18 + d / 2, sy + 18 + d + 10 + d / 2, sy + 18 + 2 * d + 20 + d / 2 };
            Color[] activeColors = {
                    new Color(255, 70, 70),
                    new Color(255, 200, 60),
                    new Color(60, 220, 130)
            };
            Color[] dimColors = {
                    new Color(80, 30, 30),
                    new Color(80, 70, 30),
                    new Color(30, 70, 50)
            };
            for (int i = 0; i < 3; i++) {
                boolean on = false;
                if (i == 0 && state == LightState.RED) on = blinkOn;
                else if (i == 1 && state == LightState.YELLOW) on = true;
                else if (i == 2 && state == LightState.GREEN) on = true;
                g.setColor(on ? activeColors[i] : dimColors[i]);
                g.fillOval(cx - d / 2, cys[i] - d / 2, d, d);
                if (on) {
                    g.setColor(new Color(activeColors[i].getRed(), activeColors[i].getGreen(), activeColors[i].getBlue(), 80));
                    g.fillOval(cx - d / 2 - 8, cys[i] - d / 2 - 8, d + 16, d + 16);
                }
            }
            g.dispose();
        }
    }
}
