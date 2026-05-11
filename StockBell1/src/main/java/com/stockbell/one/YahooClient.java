package com.stockbell.one;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class YahooClient {
    private static final String BASE = "https://query1.finance.yahoo.com/v8/finance/chart/";
    private static final String UA = "Mozilla/5.0 (compatible; StockBell/1.0)";

    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public YahooClient() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public Optional<Quote> fetchQuote(String symbol) {
        return fetchRoot(symbol, "1d", "1d").map(root -> parseQuote(symbol, root));
    }

    public Optional<KlineSeries> fetchIntraday(String symbol) {
        return fetchRoot(symbol, "1m", "1d").map(this::parseSeries);
    }

    public Optional<KlineSeries> fetchDaily(String symbol) {
        return fetchRoot(symbol, "1d", "3mo").map(this::parseSeries);
    }

    private Optional<JsonNode> fetchRoot(String symbol, String interval, String range) {
        String url = BASE + symbol + "?interval=" + interval + "&range=" + range;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", UA)
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) return Optional.empty();
            JsonNode root = mapper.readTree(res.body());
            JsonNode result = root.path("chart").path("result");
            if (!result.isArray() || result.isEmpty()) return Optional.empty();
            return Optional.of(result.get(0));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Quote parseQuote(String symbol, JsonNode result) {
        JsonNode meta = result.path("meta");
        String name = meta.path("longName").asText(meta.path("shortName").asText(symbol));
        double price = meta.path("regularMarketPrice").asDouble();
        double prev = meta.path("chartPreviousClose").asDouble(
                meta.path("previousClose").asDouble(price));
        double dayHigh = meta.path("regularMarketDayHigh").asDouble(price);
        double dayLow = meta.path("regularMarketDayLow").asDouble(price);
        long volume = meta.path("regularMarketVolume").asLong(0);
        long ts = meta.path("regularMarketTime").asLong(System.currentTimeMillis() / 1000);
        return new Quote(symbol, name, price, prev, dayHigh, dayLow, volume, ts);
    }

    private KlineSeries parseSeries(JsonNode result) {
        JsonNode timestamps = result.path("timestamp");
        JsonNode quote = result.path("indicators").path("quote").get(0);
        if (quote == null || !timestamps.isArray()) return new KlineSeries(List.of());
        JsonNode open = quote.path("open");
        JsonNode high = quote.path("high");
        JsonNode low = quote.path("low");
        JsonNode close = quote.path("close");
        JsonNode volume = quote.path("volume");

        int n = timestamps.size();
        List<Kline> bars = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            JsonNode c = close.get(i);
            JsonNode o = open.get(i);
            JsonNode h = high.get(i);
            JsonNode l = low.get(i);
            if (c == null || c.isNull() || o == null || o.isNull()) continue;
            bars.add(new Kline(
                    timestamps.get(i).asLong(),
                    o.asDouble(),
                    h == null || h.isNull() ? c.asDouble() : h.asDouble(),
                    l == null || l.isNull() ? c.asDouble() : l.asDouble(),
                    c.asDouble(),
                    volume.get(i) == null || volume.get(i).isNull() ? 0 : volume.get(i).asLong()
            ));
        }
        return new KlineSeries(bars);
    }
}
