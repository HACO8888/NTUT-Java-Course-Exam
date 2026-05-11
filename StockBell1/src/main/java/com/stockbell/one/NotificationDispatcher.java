package com.stockbell.one;

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
import java.util.Map;
import java.util.function.Consumer;

public final class NotificationDispatcher {
    public enum AlertType { HIGH, LOW }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AppConfig config;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = createMapper();

    private static ObjectMapper createMapper() {
        ObjectMapper m = new ObjectMapper();
        m.getFactory().configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
        return m;
    }
    private TrayIcon trayIcon;
    private Consumer<String> logSink = s -> {};

    public NotificationDispatcher(AppConfig config) {
        this.config = config;
        initTray();
    }

    public void setLogSink(Consumer<String> sink) {
        this.logSink = sink == null ? s -> {} : sink;
    }

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
            trayIcon = new TrayIcon(img, "StockBell");
            trayIcon.setImageAutoSize(true);
            SystemTray.getSystemTray().add(trayIcon);
        } catch (Exception e) {
            trayIcon = null;
        }
    }

    public void check(WatchItem item, Quote quote) {
        double price = quote.price();
        WatchItem.Direction dir = item.lastDirection();
        if (price >= item.highThreshold() && dir != WatchItem.Direction.HIGH) {
            item.setLastDirection(WatchItem.Direction.HIGH);
            fire(item, quote, AlertType.HIGH);
        } else if (price <= item.lowThreshold() && dir != WatchItem.Direction.LOW) {
            item.setLastDirection(WatchItem.Direction.LOW);
            fire(item, quote, AlertType.LOW);
        } else if (price < item.highThreshold() && price > item.lowThreshold()) {
            item.setLastDirection(null);
        }
    }

    public void fire(WatchItem item, Quote quote, AlertType type) {
        String typeText = type == AlertType.HIGH ? "高點警示" : "低點警示";
        double th = type == AlertType.HIGH ? item.highThreshold() : item.lowThreshold();
        String time = LocalDateTime.now().format(FMT);
        String shortMsg = String.format("觸發%s 當前: %.2f / 設定: %.2f", typeText, quote.price(), th);
        String discordMsg = String.format(
                "🔔 **%s %s** 觸發 **%s**%n當前: `%.2f`  /  設定: `%.2f`%n今高: %.2f / 今低: %.2f%n時間: %s",
                item.symbol(), item.name(), typeText, quote.price(), th,
                quote.dayHigh(), quote.dayLow(), time);
        String title = String.format("%s - %s", config.discordUsername(), item.name());

        logSink.accept(String.format("[%s] %s %s %s", time, item.symbol(), item.name(), shortMsg));

        if (config.channels().contains(AppConfig.Channel.CONSOLE)) {
            System.out.println("[" + time + "] [WARN] " + item.symbol() + " " + item.name() + " " + shortMsg
                    + (type == AlertType.HIGH ? " ▲" : " ▼"));
            Toolkit.getDefaultToolkit().beep();
        }
        if (config.channels().contains(AppConfig.Channel.TRAY) && trayIcon != null) {
            trayIcon.displayMessage(title, shortMsg,
                    type == AlertType.HIGH ? TrayIcon.MessageType.WARNING : TrayIcon.MessageType.INFO);
        }
        if (config.channels().contains(AppConfig.Channel.POPUP)) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null, shortMsg, title,
                        type == AlertType.HIGH ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
            });
        }
        if (config.channels().contains(AppConfig.Channel.DISCORD)) {
            sendDiscord(discordMsg);
        }
    }

    private void sendDiscord(String content) {
        String url = config.discordWebhookUrl();
        if (url == null || url.isBlank()) return;
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "username", config.discordUsername(),
                    "content", content
            ));
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .timeout(Duration.ofSeconds(8))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.getBytes(StandardCharsets.UTF_8)))
                    .build();
            http.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(ex -> {
                        logSink.accept("Discord 發送失敗: " + ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            logSink.accept("Discord 編碼失敗: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
    }
}
