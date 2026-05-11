package com.stockbell.three;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
    private static final Color BG_DARK = new Color(18, 18, 26);
    private static final Color BG_CARD = new Color(32, 32, 44);
    private static final Color FG = new Color(230, 232, 240);
    private static final Color FG_DIM = new Color(150, 150, 165);
    private static final Color UP = new Color(255, 80, 80);
    private static final Color DOWN = new Color(0, 220, 130);
    private static final Color ACCENT = new Color(120, 180, 255);

    private final YahooClient client = new YahooClient();
    private final NotificationDispatcher dispatcher = new NotificationDispatcher();

    private final JLabel symbolLabel = new JLabel(Config.SYMBOL);
    private final JLabel nameLabel = new JLabel(Config.NAME);
    private final JLabel priceLabel = new JLabel("---");
    private final JLabel changeLabel = new JLabel("---");
    private final JLabel hiloLabel = new JLabel("--");
    private final JLabel clockLabel = new JLabel("--:--:--");
    private final JLabel marketLabel = new JLabel("● 載入中");
    private final JSpinner highSpinner = new JSpinner(new SpinnerNumberModel(Config.DEFAULT_HIGH, 0.0, 1_000_000.0, 1.0));
    private final JSpinner lowSpinner = new JSpinner(new SpinnerNumberModel(Config.DEFAULT_LOW, 0.0, 1_000_000.0, 1.0));
    private final JCheckBox trayCheck = new JCheckBox("Tray", true);
    private final JCheckBox consoleCheck = new JCheckBox("Beep", true);
    private final JCheckBox popupCheck = new JCheckBox("彈窗", true);
    private final JCheckBox discordCheck = new JCheckBox("Discord", true);
    private final JTextArea logArea = new JTextArea(4, 30);
    private int intervalSec = 15;

    private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "stockbell-3-poller"); t.setDaemon(true); return t;
    });

    public MainFrame() {
        super("StockBell 3 [暗黑] - " + Config.NAME);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(500, 520);
        setLocationRelativeTo(null);
        loadConfig();
        initUI();
        dispatcher.setLogSink(this::appendLog);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { shutdown(); }
        });
        poller.scheduleAtFixedRate(this::pollOnce, 0, intervalSec, TimeUnit.SECONDS);
        Timer clock = new Timer(500, e -> {
            clockLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            updateMarketStatus();
        });
        clock.start();
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
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(0, 8));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_DARK);
        header.setBorder(new EmptyBorder(10, 16, 4, 16));
        symbolLabel.setForeground(FG_DIM);
        symbolLabel.setFont(symbolLabel.getFont().deriveFont(11f));
        nameLabel.setForeground(FG);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 18f));
        JPanel left = new JPanel();
        left.setBackground(BG_DARK);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(symbolLabel);
        left.add(nameLabel);
        clockLabel.setForeground(ACCENT);
        clockLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        marketLabel.setForeground(FG_DIM);
        JPanel right = new JPanel();
        right.setBackground(BG_DARK);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.add(clockLabel);
        right.add(marketLabel);
        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        JPanel card = new JPanel();
        card.setBackground(BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 80), 1, true),
                new EmptyBorder(14, 18, 14, 18)));
        priceLabel.setFont(priceLabel.getFont().deriveFont(Font.BOLD, 46f));
        priceLabel.setForeground(FG);
        changeLabel.setFont(changeLabel.getFont().deriveFont(15f));
        changeLabel.setForeground(FG_DIM);
        hiloLabel.setForeground(FG_DIM);
        hiloLabel.setFont(hiloLabel.getFont().deriveFont(12f));
        card.add(priceLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(changeLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(hiloLabel);

        JPanel cardWrap = new JPanel(new BorderLayout());
        cardWrap.setBackground(BG_DARK);
        cardWrap.setBorder(new EmptyBorder(2, 16, 2, 16));
        cardWrap.add(card, BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(0, 4, 6, 4));
        form.setBackground(BG_DARK);
        form.setBorder(new EmptyBorder(4, 16, 0, 16));
        JLabel l1 = new JLabel("高點"); l1.setForeground(FG_DIM);
        JLabel l2 = new JLabel("低點"); l2.setForeground(FG_DIM);
        form.add(l1); form.add(highSpinner); form.add(l2); form.add(lowSpinner);
        JButton applyBtn = new JButton("套用");
        JButton testBtn = new JButton("測試通知");
        form.add(applyBtn); form.add(testBtn);

        JPanel checks = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        checks.setBackground(BG_DARK);
        checks.setBorder(new EmptyBorder(0, 16, 0, 16));
        for (JCheckBox cb : new JCheckBox[]{trayCheck, consoleCheck, popupCheck, discordCheck}) {
            cb.setBackground(BG_DARK); cb.setForeground(FG_DIM);
        }
        JLabel notiLbl = new JLabel("通知:"); notiLbl.setForeground(FG_DIM);
        checks.add(notiLbl); checks.add(trayCheck); checks.add(consoleCheck);
        checks.add(popupCheck); checks.add(discordCheck);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        logArea.setBackground(BG_CARD);
        logArea.setForeground(FG_DIM);
        logArea.setCaretColor(FG);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(60,60,80)),
                "記錄", 0, 0, null, FG_DIM));

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
        center.setBackground(BG_DARK);
        center.add(cardWrap, BorderLayout.NORTH);
        center.add(form, BorderLayout.CENTER);
        center.add(checks, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(logScroll, BorderLayout.SOUTH);
    }

    private void updateMarketStatus() {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Taipei"));
        java.time.LocalTime t = now.toLocalTime();
        java.time.DayOfWeek dow = now.getDayOfWeek();
        boolean weekday = dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY;
        boolean inSession = weekday && !t.isBefore(java.time.LocalTime.of(9, 0)) && t.isBefore(java.time.LocalTime.of(13, 31));
        if (inSession) { marketLabel.setText("● 盤中"); marketLabel.setForeground(UP); }
        else { marketLabel.setText("● 收盤"); marketLabel.setForeground(FG_DIM); }
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
        Color cc = ch > 0 ? UP : (ch < 0 ? DOWN : FG_DIM);
        priceLabel.setForeground(cc);
        changeLabel.setText(String.format("%s %+.2f   (%+.2f%%)", ch > 0 ? "▲" : (ch < 0 ? "▼" : "—"), ch, q.changePercent()));
        changeLabel.setForeground(cc);
        hiloLabel.setText(String.format("今高 %.2f   今低 %.2f", q.dayHigh(), q.dayLow()));
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
}
