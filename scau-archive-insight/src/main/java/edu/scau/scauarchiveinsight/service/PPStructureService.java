package edu.scau.scauarchiveinsight.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.scau.scauarchiveinsight.pojo.MetaDataStandard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
public class PPStructureService {

    @Autowired
    private MetaDataService metaDataService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectMapper lenientMapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

    public String parseTable(String imagePath) {
        try {
            List<MetaDataStandard> rules = metaDataService.list();

            Path rulesFile = Files.createTempFile("ppstructure_rules_", ".json");
            objectMapper.writeValue(rulesFile.toFile(), rules);

            String python = "src/main/python/.venv/Scripts/python.exe";
            String scriptPath = "src/main/python/ppstructure/ppstructure.py";

            ProcessBuilder pb = new ProcessBuilder(
                    python, scriptPath, imagePath,
                    rulesFile.toAbsolutePath().toString()
            );
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.redirectErrorStream(true);

            Process process = pb.start();

            String output;
            try (InputStream is = process.getInputStream()) {
                output = new String(is.readAllBytes(), "UTF-8").trim();
            }

            int exitCode = process.waitFor();
            Files.deleteIfExists(rulesFile);

            // 调试：将原始输出写入临时文件
            Path debugFile = Files.createTempFile("ppstructure_debug_", ".txt");
            Files.writeString(debugFile, output);

            int jsonStart = output.indexOf('{');
            String jsonPart = jsonStart >= 0 ? output.substring(jsonStart) : output;

            if (exitCode == 0 && !jsonPart.isEmpty() && jsonPart.startsWith("{")) {
                // 先修复 OCR 文本中不合法转义序列（如 \I → \\I）
                jsonPart = jsonPart.replaceAll("\\\\(?![\\\"\\\\/bfnrtu])", "\\\\$0");
                // 用 lenient mapper 解析并重新序列化，输出干净的 JSON
                try {
                    Map<String, Object> parsed = lenientMapper.readValue(jsonPart,
                            new TypeReference<Map<String, Object>>() {});
                    // 解析成功，删除调试文件
                    Files.deleteIfExists(debugFile);
                    return objectMapper.writeValueAsString(parsed);
                } catch (Exception e) {
                    // 保留调试文件供排查
                    return "{\"data\":[],\"errors\":[{\"msg\":\"Python JSON 解析失败, 调试文件: "
                            + debugFile.toAbsolutePath().toString().replace("\\", "/")
                            + "\"}]}";
                }
            }

            String errorMsg = output.isEmpty() ? "Python 脚本无输出" : output;
            return "{\"data\":[],\"errors\":[{\"msg\":\"" + errorMsg.replace("\"", "'") + "\"}]}";

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"data\":[],\"errors\":[{\"msg\":\"" + e.getMessage().replace("\"", "'") + "\"}]}";
        }
    }
}
