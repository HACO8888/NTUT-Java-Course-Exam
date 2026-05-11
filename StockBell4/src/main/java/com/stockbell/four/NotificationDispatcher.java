package com.stockbell.four;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Consumer;

public final class NotificationDispatcher {
    public enum Channel { TRAY, CONSOLE, POPUP, DISCORD }
    public enum AlertType { HIGH, LOW }
    public enum Direction { HIGH, LOW }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EnumSet<Channel> channels = EnumSet.allOf(Channel.class);
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = createMapper();

    private static ObjectMapper createMapper() {
        ObjectMapper m = new ObjectMapper();
        m.getFactory().configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
        return m;
    }
    private String webhookUrl = "";
    private String username = Config.DEFAULT_USERNAME;
    private TrayIcon trayIcon;
    private Consumer<String> logSink = s -> {};
    private Direction lastDirection;

    public NotificationDispatcher() { initTray(); }

    public EnumSet<Channel> channels() { return channels; }
    public void setWebhookUrl(String u) { this.webhookUrl = u == null ? "" : u; }
    public void setUsername(String u) { if (u != null && !u.isBlank()) this.username = u; }
    public void setLogSink(Consumer<String> s) { this.logSink = s == null ? x -> {} : s; }
    public void setChannelEnabled(Channel c, boolean on) { if (on) channels.add(c); else channels.remove(c); }

    private void initTray() {
        if (!SystemTray.isSupported()) return;
        try {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(220, 60, 60));
            g.fillOval(2, 2, 12, 12);
            g.setColor(Color.WHITE);
            g.fillOval(7, 5, 2, 6);
            g.dispose();
            trayIcon = new TrayIcon(img, "StockBell 4");
            trayIcon.setImageAutoSize(true);
            SystemTray.getSystemTray().add(trayIcon);
        } catch (Exception e) { trayIcon = null; }
    }

    public Direction check(Quote q, double highThreshold, double lowThreshold) {
        double price = q.price();
        Direction triggered = null;
        if (price >= highThreshold && lastDirection != Direction.HIGH) {
            lastDirection = Direction.HIGH;
            triggered = Direction.HIGH;
            fire(q, AlertType.HIGH, highThreshold);
        } else if (price <= lowThreshold && lastDirection != Direction.LOW) {
            lastDirection = Direction.LOW;
            triggered = Direction.LOW;
            fire(q, AlertType.LOW, lowThreshold);
        } else if (price < highThreshold && price > lowThreshold) {
            lastDirection = null;
        }
        return triggered;
    }

    public Direction lastDirection() { return lastDirection; }

    public void fire(Quote q, AlertType type, double threshold) {
        String typeText = type == AlertType.HIGH ? "高點警示" : "低點警示";
        String time = LocalDateTime.now().format(FMT);
        String shortMsg = String.format("觸發%s 當前: %.2f / 設定: %.2f", typeText, q.price(), threshold);
        String discordMsg = String.format(
                "🔔 **%s %s** 觸發 **%s**%n當前: `%.2f`  /  設定: `%.2f`%n今高: %.2f / 今低: %.2f%n時間: %s",
                q.symbol(), q.name(), typeText, q.price(), threshold, q.dayHigh(), q.dayLow(), time);
        String title = username + " - " + q.name();
        logSink.accept(String.format("[%s] %s", time, shortMsg));

        if (channels.contains(Channel.CONSOLE)) {
            System.out.println("[" + time + "] [WARN] " + q.symbol() + " " + q.name() + " " + shortMsg
                    + (type == AlertType.HIGH ? " ▲" : " ▼"));
            Toolkit.getDefaultToolkit().beep();
        }
        if (channels.contains(Channel.TRAY) && trayIcon != null) {
            trayIcon.displayMessage(title, shortMsg,
                    type == AlertType.HIGH ? TrayIcon.MessageType.WARNING : TrayIcon.MessageType.INFO);
        }
        if (channels.contains(Channel.POPUP)) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null, shortMsg, title,
                            type == AlertType.HIGH ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE));
        }
        if (channels.contains(Channel.DISCORD)) sendDiscord(discordMsg);
    }

    private void sendDiscord(String content) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        try {
            String body = mapper.writeValueAsString(Map.of("username", username, "content", content));
            HttpRequest req = HttpRequest.newBuilder(URI.create(webhookUrl))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .timeout(Duration.ofSeconds(8))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.getBytes(StandardCharsets.UTF_8))).build();
            http.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(ex -> { logSink.accept("Discord 發送失敗: " + ex.getMessage()); return null; });
        } catch (Exception e) {
            logSink.accept("Discord 編碼失敗: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (trayIcon != null) SystemTray.getSystemTray().remove(trayIcon);
    }
}
