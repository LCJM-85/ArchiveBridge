package edu.scau.scauarchiveinsight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class AIAssistantClient {

    private static final Logger log = LoggerFactory.getLogger(AIAssistantClient.class);
    private static final String BASE_URL = "http://127.0.0.1:8765";
    private static final int TIMEOUT_SECONDS = 30;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String chat(String question, List<Map<String, String>> history) {
        try {
            Map<String, Object> body = Map.of("question", question, "history", history);
            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/chat"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            return (String) result.getOrDefault("answer", "抱歉，AI 助手暂时无法回答");
        } catch (Exception e) {
            log.warn("AI 助手调用失败: {}", e.getMessage());
            return null;
        }
    }

    public String analyzeReport(Map<String, Object> reportData) {
        try {
            Map<String, Object> body = Map.of("report_data", reportData);
            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/analyze-report"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            return (String) result.getOrDefault("analysis", "");
        } catch (Exception e) {
            log.warn("AI 报告分析调用失败: {}", e.getMessage());
            return "";
        }
    }

    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/docs"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
