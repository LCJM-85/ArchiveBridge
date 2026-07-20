package edu.scau.scauarchiveinsight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);
    private static final String PYTHON_BASE = "http://127.0.0.1:8765";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Map<String, Object> processFile(String filePath, String title, String fileType) {
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "file_path", filePath,
                    "title", title,
                    "file_type", fileType,
                    "store_path", filePath
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PYTHON_BASE + "/kb/process"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(180))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            if (response.statusCode() >= 400) {
                String detail = (String) result.getOrDefault("detail", "处理失败");
                return Map.of("error", detail);
            }
            return result;
        } catch (Exception e) {
            log.error("知识库处理文件失败: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    public Map<String, Object> processUrl(String url, String title) {
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "url", url,
                    "title", title
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PYTHON_BASE + "/kb/process-url"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(180))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), Map.class);
        } catch (Exception e) {
            log.error("知识库处理网页失败: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    public List<Map<String, Object>> listDocuments() {
        return jdbcTemplate.queryForList(
                "SELECT id, title, file_type, source, url, chunk_count, status, error_msg, create_time " +
                "FROM knowledge_base ORDER BY create_time DESC"
        );
    }

    public boolean deleteDocument(int id) {
        // 查询文件路径
        List<String> paths = jdbcTemplate.queryForList(
                "SELECT file_path FROM knowledge_base WHERE id = ?", String.class, id);
        if (!paths.isEmpty() && paths.get(0) != null) {
            try {
                Files.deleteIfExists(Path.of(paths.get(0)));
            } catch (IOException ignored) {}
        }
        int affected = jdbcTemplate.update("DELETE FROM knowledge_base WHERE id = ?", id);
        return affected > 0;
    }
}
