package com.crawler.engine;

import com.crawler.model.CrawlResult;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ResultExporter {

    public static void exportCsv(List<CrawlResult> results, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("URL,狀態碼,標題,大小(bytes),回應時間(ms),深度,連結數,圖片數,內容類型");
            writer.newLine();

            for (CrawlResult r : results) {
                writer.write(String.format("%s,%d,%s,%d,%d,%d,%d,%d,%s",
                        escapeCsv(r.getUrl()),
                        r.getStatusCode(),
                        escapeCsv(r.getTitle()),
                        r.getContentLength(),
                        r.getResponseTimeMs(),
                        r.getDepth(),
                        r.getLinkCount(),
                        r.getImageCount(),
                        escapeCsv(r.getContentType() != null ? r.getContentType() : "")));
                writer.newLine();
            }
        }
    }

    public static void exportJson(List<CrawlResult> results, File file) throws IOException {
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(results);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(json);
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
