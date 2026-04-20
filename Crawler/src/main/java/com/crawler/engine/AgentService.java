package com.crawler.engine;

import com.crawler.model.CrawlResult;
import com.google.gson.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AgentService {
    public enum Model {
        GPT4O("gpt-4o", "GPT-4o"),
        GPT4O_MINI("gpt-4o-mini", "GPT-4o Mini"),
        GPT4_1("gpt-4.1", "GPT-4.1"),
        GPT4_1_MINI("gpt-4.1-mini", "GPT-4.1 Mini"),
        GPT4_1_NANO("gpt-4.1-nano", "GPT-4.1 Nano");

        private final String id;
        private final String label;

        Model(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() { return id; }

        @Override
        public String toString() { return label; }
    }

    private String apiKey = "";
    private Model model = Model.GPT4O_MINI;
    private final HttpClient httpClient;
    private final List<JsonObject> conversationHistory = new ArrayList<>();
    private final Gson gson = new Gson();

    public AgentService() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public Model getModel() {
        return model;
    }

    public void clearHistory() {
        conversationHistory.clear();
    }

    public void askAsync(String question, List<CrawlResult> results,
                         Consumer<String> onResponse, Consumer<String> onError) {
        new Thread(() -> {
            try {
                String response = ask(question, results);
                onResponse.accept(response);
            } catch (Exception e) {
                onError.accept(e.getMessage());
            }
        }, "agent-request").start();
    }

    private String ask(String question, List<CrawlResult> results) throws Exception {
        if (apiKey.isEmpty()) {
            throw new Exception("請先設定 API Key（點擊上方的設定按鈕）");
        }

        String systemPrompt = buildSystemPrompt(results);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", question);
        conversationHistory.add(userMsg);

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);

        for (JsonObject msg : conversationHistory) {
            messages.add(msg);
        }

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model.getId());
        requestBody.addProperty("max_tokens", 2048);
        requestBody.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            JsonObject errorBody = JsonParser.parseString(response.body()).getAsJsonObject();
            String errorMsg = errorBody.has("error")
                    ? errorBody.getAsJsonObject("error").get("message").getAsString()
                    : response.body();
            throw new Exception("API 錯誤 (" + response.statusCode() + "): " + errorMsg);
        }

        JsonObject responseBody = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray choices = responseBody.getAsJsonArray("choices");
        String assistantText = choices.get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();

        JsonObject assistantMsg = new JsonObject();
        assistantMsg.addProperty("role", "assistant");
        assistantMsg.addProperty("content", assistantText);
        conversationHistory.add(assistantMsg);

        return assistantText;
    }

    private String buildSystemPrompt(List<CrawlResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一個網站爬蟲的 AI 助理。使用者已經爬取了一個網站，以下是爬取結果的摘要。\n");
        sb.append("請根據這些資料回答使用者的問題。回答請使用繁體中文。\n\n");

        if (results.isEmpty()) {
            sb.append("目前尚未爬取任何頁面。\n");
            return sb.toString();
        }

        sb.append(String.format("共爬取 %d 個頁面：\n\n", results.size()));

        int limit = Math.min(results.size(), 50);
        for (int i = 0; i < limit; i++) {
            CrawlResult r = results.get(i);
            sb.append(String.format("【頁面 %d】\n", i + 1));
            sb.append(String.format("  URL: %s\n", r.getUrl()));
            sb.append(String.format("  標題: %s\n", r.getTitle() != null ? r.getTitle() : "(無)"));
            sb.append(String.format("  狀態碼: %d\n", r.getStatusCode()));
            sb.append(String.format("  大小: %d bytes\n", r.getContentLength()));
            sb.append(String.format("  連結數: %d | 圖片數: %d\n", r.getLinkCount(), r.getImageCount()));

            String text = r.getTextContent();
            if (text != null && !text.isEmpty()) {
                String preview = text.length() > 200 ? text.substring(0, 200) + "..." : text;
                sb.append(String.format("  內容摘要: %s\n", preview));
            }
            sb.append("\n");
        }

        if (results.size() > limit) {
            sb.append(String.format("（還有 %d 個頁面未列出）\n", results.size() - limit));
        }

        return sb.toString();
    }
}
