package com.stockbell.one;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

public final class ConfigLoader {
    private ConfigLoader() {}

    public static AppConfig loadAppConfig(String path) {
        AppConfig cfg = new AppConfig();
        Path p = Paths.get(path);
        if (!Files.exists(p)) return cfg;
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(p)) {
            props.load(in);
        } catch (IOException e) {
            return cfg;
        }
        cfg.setDiscordWebhookUrl(props.getProperty("discord.webhook.url", "").trim());
        cfg.setDiscordUsername(props.getProperty("discord.username", "股票通知1").trim());
        try {
            cfg.setPollIntervalSeconds(Integer.parseInt(
                    props.getProperty("poll.interval.seconds", "15").trim()));
        } catch (NumberFormatException ignored) {}
        String channels = props.getProperty("notify.channels", "TRAY,CONSOLE,POPUP,DISCORD");
        EnumSet<AppConfig.Channel> set = EnumSet.noneOf(AppConfig.Channel.class);
        for (String s : channels.split(",")) {
            try { set.add(AppConfig.Channel.valueOf(s.trim().toUpperCase())); }
            catch (IllegalArgumentException ignored) {}
        }
        if (!set.isEmpty()) cfg.setChannels(set);
        return cfg;
    }

    public static List<WatchItem> loadWatchlist(String path) {
        List<WatchItem> list = new ArrayList<>();
        Path p = Paths.get(path);
        if (!Files.exists(p)) return list;
        try {
            for (String line : Files.readAllLines(p)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String symbol = line.substring(0, eq).trim();
                String[] parts = line.substring(eq + 1).split(",");
                if (parts.length < 3) continue;
                try {
                    list.add(new WatchItem(
                            symbol,
                            parts[0].trim(),
                            Double.parseDouble(parts[1].trim()),
                            Double.parseDouble(parts[2].trim())
                    ));
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException ignored) {}
        return list;
    }

    public static void saveWatchlist(String path, List<WatchItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 格式：股票代號=名稱,高點門檻,低點門檻\n");
        for (WatchItem it : items) {
            sb.append(it.symbol()).append('=')
                    .append(it.name()).append(',')
                    .append(it.highThreshold()).append(',')
                    .append(it.lowThreshold()).append('\n');
        }
        try (OutputStream out = Files.newOutputStream(Paths.get(path))) {
            out.write(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException ignored) {}
    }
}
