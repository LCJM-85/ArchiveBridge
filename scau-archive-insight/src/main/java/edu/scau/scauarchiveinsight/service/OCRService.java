package edu.scau.scauarchiveinsight.service;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@Service
public class OCRService {

    public String recognizeText(String imagePath) {
        try {
            String python = ".venv/Scripts/python.exe";
            String scriptPath = "src/main/python/ocr/ocr.py";

            ProcessBuilder pb = new ProcessBuilder(
                    python,
                    scriptPath,
                    imagePath
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

            // 提取输出中的 JSON（兼容 PaddleOCR 日志混入 stdout 的情况）
            int jsonStart = output.indexOf('{');
            String jsonPart = jsonStart >= 0 ? output.substring(jsonStart) : output;

            if (exitCode == 0 && !jsonPart.isEmpty() && jsonPart.startsWith("{")) {
                return jsonPart;
            }

            // 非正常退出或输出非 JSON，说明识别异常
            String errorMsg = output.isEmpty() ? "Python 脚本无输出" : output;
            return "OCR 识别出错：" + errorMsg;

        } catch (Exception e) {
            e.printStackTrace();
            return "OCR 识别出错：" + e.getMessage();
        }
    }
}
