package com.stockbell.nine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
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
    private static final Color BG = new Color(34, 38, 50);
    private static final Color BG2 = new Color(48, 54, 70);
    private static final Color FG = new Color(240, 240, 248);
    private static final Color FG_DIM = new Color(170, 175, 195);
    private static final Color UP = new Color(255, 90, 90);
    private static final Color DOWN = new Color(60, 220, 130);

    private final YahooClient client = new YahooClient();
    private final NotificationDispatcher dispatcher = new NotificationDispatcher();

    private final JLabel symbolLabel = new JLabel(Config.SYMBOL);
    private final JLabel nameLabel = new JLabel(Config.NAME);
    private final JLabel priceLabel = new JLabel("---");
    private final JLabel changeLabel = new JLabel("---");
    private final JLabel timeLabel = new JLabel("--:--");
    private double highTh = Config.DEFAULT_HIGH;
    private double lowTh = Config.DEFAULT_LOW;
    private int intervalSec = 15;
    private Point dragOrigin;

    private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "stockbell-9-poller"); t.setDaemon(true); return t;
    });

    public MainFrame() {
        super("StockBell 9 [貼紙]");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setUndecorated(true);
        setAlwaysOnTop(true);
        setSize(260, 130);
        setLocationByPlatform(true);
        try { setShape(new RoundRectangle2D.Double(0, 0, 260, 130, 22, 22)); } catch (Exception ignored) {}
        try { setBackground(new Color(0, 0, 0, 0)); } catch (Exception ignored) {}

        loadConfig();
        initUI();
        installDragAndMenu();
        dispatcher.setLogSink(s -> {});
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
        dispatcher.setChannelEnabled(NotificationDispatcher.Channel.TRAY, set.contains("TRAY"));
        dispatcher.setChannelEnabled(NotificationDispatcher.Channel.CONSOLE, set.contains("CONSOLE"));
        dispatcher.setChannelEnabled(NotificationDispatcher.Channel.POPUP, set.contains("POPUP"));
        dispatcher.setChannelEnabled(NotificationDispatcher.Channel.DISCORD, set.contains("DISCORD"));
    }

    private void initUI() {
        JPanel root = new JPanel() {
            @Override protected void paintComponent(Graphics gg) {
                super.paintComponent(gg);
                Graphics2D g = (Graphics2D) gg.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, BG, 0, h, BG2);
                g.setPaint(gp);
                g.fillRoundRect(0, 0, w, h, 22, 22);
                g.setColor(new Color(255, 255, 255, 30));
                g.drawRoundRect(0, 0, w - 1, h - 1, 22, 22);
                g.dispose();
            }
        };
        root.setOpaque(false);
        root.setLayout(new BorderLayout(0, 0));
        root.setBorder(new EmptyBorder(10, 14, 10, 14));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        symbolLabel.setForeground(FG_DIM);
        symbolLabel.setFont(symbolLabel.getFont().deriveFont(10f));
        nameLabel.setForeground(FG);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 13f));
        JPanel ln = new JPanel();
        ln.setOpaque(false);
        ln.setLayout(new BoxLayout(ln, BoxLayout.Y_AXIS));
        ln.add(symbolLabel);
        ln.add(nameLabel);
        timeLabel.setForeground(FG_DIM);
        timeLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        top.add(ln, BorderLayout.WEST);
        top.add(timeLabel, BorderLayout.EAST);

        priceLabel.setFont(priceLabel.getFont().deriveFont(Font.BOLD, 34f));
        priceLabel.setForeground(FG);
        priceLabel.setHorizontalAlignment(SwingConstants.CENTER);

        changeLabel.setFont(changeLabel.getFont().deriveFont(12f));
        changeLabel.setForeground(FG_DIM);
        changeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(priceLabel, BorderLayout.CENTER);
        center.add(changeLabel, BorderLayout.SOUTH);

        root.add(top, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void installDragAndMenu() {
        MouseAdapter drag = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { dragOrigin = e.getPoint(); }
            @Override public void mouseDragged(MouseEvent e) {
                if (dragOrigin == null) return;
                Point loc = getLocation();
                setLocation(loc.x + e.getX() - dragOrigin.x, loc.y + e.getY() - dragOrigin.y);
            }
        };
        getContentPane().addMouseListener(drag);
        getContentPane().addMouseMotionListener(drag);
        for (Component c : getContentPane().getComponents()) {
            c.addMouseListener(drag);
            c.addMouseMotionListener(drag);
        }

        JPopupMenu menu = new JPopupMenu();
        JMenuItem set = new JMenuItem("設定門檻");
        JMenuItem test = new JMenuItem("測試通知");
        JMenuItem quit = new JMenuItem("關閉");
        set.addActionListener(ev -> openThresholdDialog());
        test.addActionListener(ev -> {
            Quote q = new Quote(Config.SYMBOL, Config.NAME, highTh, highTh, highTh, highTh, 0, System.currentTimeMillis() / 1000);
            dispatcher.fire(q, NotificationDispatcher.AlertType.HIGH, highTh);
        });
        quit.addActionListener(ev -> shutdown());
        menu.add(set); menu.add(test); menu.addSeparator(); menu.add(quit);
        MouseAdapter rightClick = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger() || e.getClickCount() == 2) {
                    if (e.getClickCount() == 2) openThresholdDialog();
                    else menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        };
        getContentPane().addMouseListener(rightClick);
        for (Component c : getContentPane().getComponents()) c.addMouseListener(rightClick);
    }

    private void openThresholdDialog() {
        JSpinner hi = new JSpinner(new SpinnerNumberModel(highTh, 0.0, 1_000_000.0, 1.0));
        JSpinner lo = new JSpinner(new SpinnerNumberModel(lowTh, 0.0, 1_000_000.0, 1.0));
        Object[] msg = {"高點門檻:", hi, "低點門檻:", lo};
        int r = JOptionPane.showConfirmDialog(this, msg, "設定", JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION) {
            highTh = ((Number) hi.getValue()).doubleValue();
            lowTh = ((Number) lo.getValue()).doubleValue();
        }
    }

    private void pollOnce() {
        client.fetchQuote(Config.SYMBOL).ifPresent(q -> {
            dispatcher.check(q, highTh, lowTh);
            SwingUtilities.invokeLater(() -> updateUI(q));
        });
    }

    private void updateUI(Quote q) {
        priceLabel.setText(String.format("%.2f", q.price()));
        double ch = q.change();
        Color cc = ch > 0 ? UP : (ch < 0 ? DOWN : FG);
        priceLabel.setForeground(cc);
        changeLabel.setText(String.format("%s %+.2f (%+.2f%%)", ch > 0 ? "▲" : (ch < 0 ? "▼" : "—"), ch, q.changePercent()));
        changeLabel.setForeground(cc);
        timeLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    private void shutdown() {
        poller.shutdownNow();
        dispatcher.shutdown();
        dispose();
        System.exit(0);
    }
}
