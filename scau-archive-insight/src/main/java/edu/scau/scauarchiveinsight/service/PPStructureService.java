package edu.scau.scauarchiveinsight.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import edu.scau.scauarchiveinsight.pojo.MetaDataStandard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PPStructureService {

    @Autowired
    private MetaDataService metaDataService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectMapper lenientMapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS)
            .build();

    public String parseTable(String imagePath) {
        try {
            List<MetaDataStandard> rules = metaDataService.list();

            Path rulesFile = Files.createTempFile("ppstructure_rules_", ".json");
            objectMapper.writeValue(rulesFile.toFile(), rules);

            String python = "src/main/python/.venv/Scripts/python.exe";
            String scriptPath = "src/main/python/ppstructure/ocr_table.py";

            ProcessBuilder pb = new ProcessBuilder(
                    python, scriptPath, imagePath,
                    rulesFile.toAbsolutePath().toString()
            );
            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");
            env.put("HOME", "D:/Ideaworkplace/SCAU/scau-archive-insight/models");
            env.put("USERPROFILE", "D:/Ideaworkplace/SCAU/scau-archive-insight/models");
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
                try {
                    Map<String, Object> parsed = lenientMapper.readValue(jsonPart,
                            new TypeReference<Map<String, Object>>() {});
                    return objectMapper.writeValueAsString(parsed);
                } catch (Exception e) {
                    return errorJson("Python 输出不合法 JSON: " + e.getMessage());
                }
            }

            String errorMsg = output.isEmpty() ? "Python 脚本无输出" : output;
            return errorJson(errorMsg);

        } catch (Exception e) {
            e.printStackTrace();
            return errorJson(e.getMessage());
        }
    }

    private String errorJson(String msg) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("data", List.of());
            result.put("errors", List.of(Map.of("msg", msg)));
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"data\":[],\"errors\":[{\"msg\":\"unknown error\"}]}";
        }
    }
}
