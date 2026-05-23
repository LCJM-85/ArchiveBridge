package edu.scau.scauarchiveinsight.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.scau.scauarchiveinsight.pojo.MetaDataStandard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class LLMExtractionService {

    @Autowired
    private MetaDataService metaDataService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Value("${llm.model:deepseek-chat}")
    private String model;

    public List<Map<String, Object>> extract(String imagePath) {
        String json = runPython(imagePath);
        try {
            Map<String, Object> parsed = objectMapper.readValue(json,
                    new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) parsed.getOrDefault("data", List.of());
            return data;
        } catch (Exception e) {
            throw new RuntimeException("LLM 提取结果解析失败: " + e.getMessage(), e);
        }
    }

    private String runPython(String inputPath) {
        try {
            List<MetaDataStandard> rules = metaDataService.list();
            Path rulesFile = Files.createTempFile("llm_rules_", ".json");
            objectMapper.writeValue(rulesFile.toFile(), rules);

            String python = "src/main/python/.venv/Scripts/python.exe";
            String scriptPath = "src/main/python/ppstructure/llm_extractor.py";

            ProcessBuilder pb = new ProcessBuilder(
                    python, scriptPath, inputPath,
                    rulesFile.toAbsolutePath().toString(),
                    "--api-key", apiKey,
                    "--base-url", baseUrl,
                    "--model", model
            );
            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");
            String modelsDir = Path.of("models").toAbsolutePath().normalize().toString();
            env.put("HOME", modelsDir);
            env.put("USERPROFILE", modelsDir);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            String output;
            try (InputStream is = process.getInputStream()) {
                output = new String(is.readAllBytes(), "UTF-8").trim();
            }

            int exitCode = process.waitFor();
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
