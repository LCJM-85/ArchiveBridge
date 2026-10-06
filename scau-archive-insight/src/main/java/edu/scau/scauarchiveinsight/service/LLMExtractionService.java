package edu.scau.scauarchiveinsight.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.scau.scauarchiveinsight.pojo.MetaDataStandard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class LLMExtractionService {

    public record ExtractionResult(List<Map<String, Object>> data,
                                   List<Map<String, Object>> errors) {}

    private static final Logger log = LoggerFactory.getLogger(LLMExtractionService.class);

    @Autowired
    private MetaDataService metaDataService;

    @Autowired
    private OCRTaskManager ocrTaskManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Value("${llm.model:deepseek-chat}")
    private String model;

    public List<Map<String, Object>> extract(String imagePath) {
        return extractWithIssues(imagePath).data();
    }

    public ExtractionResult extractWithIssues(String imagePath) {
        String json = runPython(imagePath);
        try {
            Map<String, Object> parsed = objectMapper.readValue(json,
                    new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) parsed.getOrDefault("data", List.of());
            @SuppressWarnings("unchecked")
            List<Object> rawErrors = (List<Object>) parsed.getOrDefault("errors", List.of());
            List<Map<String, Object>> errors = new ArrayList<>();
            for (Object rawError : rawErrors) {
                if (rawError instanceof Map<?, ?> map) {
                    Map<String, Object> normalized = new LinkedHashMap<>();
                    map.forEach((key, value) -> normalized.put(String.valueOf(key), value));
                    if (!normalized.containsKey("message") && normalized.containsKey("msg")) {
                        normalized.put("message", normalized.get("msg"));
                    }
                    errors.add(normalized);
                } else if (rawError != null) {
                    errors.add(Map.of("message", rawError.toString()));
                }
            }
            if (!errors.isEmpty()) {
                log.warn("LLM 提取返回错误: {} (图片: {})", errors, imagePath);
            }
            if (data.isEmpty() && errors.isEmpty()) {
                log.warn("LLM 提取结果为空且无错误信息，LLM 可能认为图片无有效数据 (图片: {})", imagePath);
            }
            return new ExtractionResult(data, errors);
        } catch (Exception e) {
            throw new RuntimeException("LLM 提取结果解析失败: " + e.getMessage(), e);
        }
    }

    private String runPython(String inputPath) {
        try {
            List<MetaDataStandard> rules = metaDataService.list();
            Path rulesFile = Files.createTempFile("llm_rules_", ".json");
            objectMapper.writeValue(rulesFile.toFile(), rules);

            String python = Path.of("", "src/main/python/.venv/Scripts/python.exe")
                    .toAbsolutePath().normalize().toString();
            String scriptPath = Path.of("", "src/main/python/ppstructure/llm_extractor.py")
                    .toAbsolutePath().normalize().toString();

            ProcessBuilder pb = new ProcessBuilder(
                    python, scriptPath, inputPath,
                    rulesFile.toAbsolutePath().toString(),
                    "--api-key", apiKey,
                    "--base-url", baseUrl,
                    "--model", model
            );
            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");
            String modelsDir = Path.of("", "models").toAbsolutePath().normalize().toString();
            env.put("HOME", modelsDir);
            env.put("USERPROFILE", modelsDir);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            ocrTaskManager.registerProcess(process);

            String output;
            try (InputStream is = process.getInputStream()) {
                output = new String(is.readAllBytes(), "UTF-8").trim();
            }

            int exitCode = process.waitFor();
            ocrTaskManager.unregisterProcess(process);
            Files.deleteIfExists(rulesFile);

            int jsonStart = output.indexOf('{');
            String jsonPart = jsonStart >= 0 ? output.substring(jsonStart) : output;

            if (exitCode == 0 && !jsonPart.isEmpty() && jsonPart.startsWith("{")) {
                return jsonPart;
            }
            throw new RuntimeException("Python 脚本失败: " + output);

        } catch (Exception e) {
            throw new RuntimeException("LLM 提取异常: " + e.getMessage(), e);
        }
    }
}
