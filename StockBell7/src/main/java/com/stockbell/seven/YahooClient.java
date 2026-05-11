package com.stockbell.seven;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

public final class YahooClient {
    private static final String BASE = "https://query1.finance.yahoo.com/v8/finance/chart/";
    private static final String UA = "Mozilla/5.0 (compatible; StockBell/1.0)";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public Optional<Quote> fetchQuote(String symbol) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + symbol + "?interval=1d&range=1d"))
                    .header("User-Agent", UA).timeout(Duration.ofSeconds(8)).GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) return Optional.empty();
            JsonNode root = mapper.readTree(res.body());
            JsonNode result = root.path("chart").path("result");
            if (!result.isArray() || result.isEmpty()) return Optional.empty();
            JsonNode meta = result.get(0).path("meta");
            String name = meta.path("longName").asText(meta.path("shortName").asText(symbol));
            double price = meta.path("regularMarketPrice").asDouble();
            double prev = meta.path("chartPreviousClose").asDouble(
                    meta.path("previousClose").asDouble(price));
            double high = meta.path("regularMarketDayHigh").asDouble(price);
            double low = meta.path("regularMarketDayLow").asDouble(price);
            long vol = meta.path("regularMarketVolume").asLong(0);
            long ts = meta.path("regularMarketTime").asLong(System.currentTimeMillis() / 1000);
            return Optional.of(new Quote(symbol, name, price, prev, high, low, vol, ts));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<double[]> fetchDailyCloses(String symbol, int days) {
        try {
            String range = days <= 7 ? "1mo" : (days <= 30 ? "3mo" : "6mo");
            HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + symbol + "?interval=1d&range=" + range))
                    .header("User-Agent", UA).timeout(Duration.ofSeconds(8)).GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) return Optional.empty();
            JsonNode root = mapper.readTree(res.body());
            JsonNode result = root.path("chart").path("result");
            if (!result.isArray() || result.isEmpty()) return Optional.empty();
            JsonNode closes = result.get(0).path("indicators").path("quote").get(0).path("close");
            if (!closes.isArray()) return Optional.empty();
            List<Double> list = new ArrayList<>();
            for (JsonNode n : closes) if (!n.isNull()) list.add(n.asDouble());
            int from = Math.max(0, list.size() - days);
            double[] arr = new double[list.size() - from];
            for (int i = from; i < list.size(); i++) arr[i - from] = list.get(i);
            return Optional.of(arr);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
