package edu.scau.scauarchiveinsight.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.scau.scauarchiveinsight.mapper.AdmissionFactMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PredictionService {

    @Autowired
    private AdmissionFactMapper admissionFactMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> predictNextYears(int yearsAhead) throws Exception {
        // 1. 获取历史数据
        List<Map<String, Object>> raw = admissionFactMapper.yearlyAdmissionCounts();
        if (raw.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("historical", Collections.emptyList());
            empty.put("predictions", Collections.emptyList());
            empty.put("metrics", Collections.emptyMap());
            return empty;
        }

        String years = raw.stream().map(r -> String.valueOf(r.get("year"))).collect(Collectors.joining(","));
        String counts = raw.stream().map(r -> String.valueOf(r.get("count"))).collect(Collectors.joining(","));

        // 2. 调用 Python 脚本
        String pythonScript = Path.of("", "src/main/python/predict/predict_admission.py")
                .toAbsolutePath().normalize().toString();

        String venvPython = Path.of("", "src/main/python/.venv/Scripts/python.exe")
                .toAbsolutePath().normalize().toString();

        ProcessBuilder pb = new ProcessBuilder(
                venvPython, pythonScript,
                "--yearly", years,
                "--counts", counts,
                "--years_ahead", String.valueOf(yearsAhead)
        );

        // 设置 HOME/USERPROFILE 避免中文路径问题（与 PPStructureService 一致）
        String modelsDir = Path.of("", "models").toAbsolutePath().normalize().toString();
        pb.environment().put("HOME", modelsDir);
        pb.environment().put("USERPROFILE", modelsDir);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        String errorOutput = "";
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                errorOutput += line + "\n";
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Python prediction failed: " + errorOutput);
        }

        // 3. 解析返回 JSON
        return objectMapper.readValue(output.toString(),
                new TypeReference<Map<String, Object>>() {});
    }
}
