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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class PredictionService {

    @Autowired
    private AdmissionFactMapper admissionFactMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> predictNextYears(int yearsAhead, String degreeName) throws Exception {
        // 1. 获取历史数据（可按培养层次模糊筛选）
        List<Map<String, Object>> raw = degreeName != null && !degreeName.isBlank()
                ? admissionFactMapper.yearlyAdmissionCountsByDegreeName(degreeName)
                : admissionFactMapper.yearlyAdmissionCounts();
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

        // 先等待（带超时）再读输出：进程结束前读 stdout 会隐式阻塞且无超时，
        // Python 预测脚本卡住时会永久占住 Tomcat 线程，导致其它接口 502。
        boolean finished = process.waitFor(45, TimeUnit.SECONDS);
        if (!finished) {
            killProcessTree(process);
            throw new RuntimeException("Python prediction timed out after 45 seconds");
        }

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

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Python prediction failed: " + errorOutput);
        }

        // 3. 解析返回 JSON
        return objectMapper.readValue(output.toString(),
                new TypeReference<Map<String, Object>>() {});
    }

    /**
     * 强杀 Python 进程树。Windows 上 venv 的 python.exe 是 stub，会派生 base python 子进程，
     * 只 destroyForcibly 父进程会导致子进程残留，故用 taskkill /T 连根拔起。
     */
    private void killProcessTree(Process p) {
        try {
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                Process k = new ProcessBuilder("taskkill", "/PID", String.valueOf(p.pid()), "/T", "/F")
                        .redirectErrorStream(true).start();
                k.waitFor(5, TimeUnit.SECONDS);
            } else {
                p.destroyForcibly();
            }
        } catch (Exception ignored) {
            p.destroyForcibly();
        }
    }
}
