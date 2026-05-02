package edu.scau.scauarchiveinsight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.scau.scauarchiveinsight.pojo.MetaDataStandard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OCRService {

    @Autowired
    private MetaDataService metaDataService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String recognizeText(String imagePath) {
        try {
            // 从数据库获取元数据规则
            List<MetaDataStandard> rules = metaDataService.list();

            // 写规则到临时文件
            Path rulesFile = Files.createTempFile("ocr_rules_", ".json");
            objectMapper.writeValue(rulesFile.toFile(), rules);

            String python = ".venv/Scripts/python.exe";
            String scriptPath = "src/main/python/ocr/ocr.py";

            ProcessBuilder pb = new ProcessBuilder(
                    python,
                    scriptPath,
                    imagePath,
                    rulesFile.toAbsolutePath().toString()
            );
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.redirectErrorStream(true);

            Process process = pb.start();

            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                output = reader.lines().collect(Collectors.joining("\n")).trim();
            }

            int exitCode = process.waitFor();

            // 删除临时文件
            Files.deleteIfExists(rulesFile);

            // 提取输出中的 JSON
            int jsonStart = output.indexOf('{');
            String jsonPart = jsonStart >= 0 ? output.substring(jsonStart) : output;

            if (exitCode == 0 && !jsonPart.isEmpty() && jsonPart.startsWith("{")) {
                return jsonPart;
            }

            String errorMsg = output.isEmpty() ? "Python 脚本无输出" : output;
            return "OCR 识别出错：" + errorMsg;

        } catch (Exception e) {
            e.printStackTrace();
            return "OCR 识别出错：" + e.getMessage();
        }
    }
}
