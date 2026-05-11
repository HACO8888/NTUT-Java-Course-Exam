package com.stockbell.one;

import java.util.EnumSet;

public final class AppConfig {
    public enum Channel { TRAY, CONSOLE, POPUP, DISCORD }

    private String discordWebhookUrl = "";
    private String discordUsername = "股票通知1";
    private int pollIntervalSeconds = 15;
    private EnumSet<Channel> channels = EnumSet.allOf(Channel.class);

    public String discordWebhookUrl() { return discordWebhookUrl; }
    public String discordUsername() { return discordUsername; }
    public int pollIntervalSeconds() { return pollIntervalSeconds; }
    public EnumSet<Channel> channels() { return channels; }

    public void setDiscordWebhookUrl(String v) { this.discordWebhookUrl = v == null ? "" : v; }
    public void setDiscordUsername(String v) { this.discordUsername = v; }
    public void setPollIntervalSeconds(int v) { this.pollIntervalSeconds = Math.max(5, v); }
    public void setChannels(EnumSet<Channel> v) { this.channels = v; }
}
