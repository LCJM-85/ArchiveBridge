package edu.scau.scauarchiveinsight.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class PPStructureService {

    @Autowired
    private OCRTaskManager ocrTaskManager;

    @Value("${ocr.device:auto}")
    private String ocrDevice = "auto";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectMapper lenientMapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS)
            .build();
    private final ReentrantLock workerLock = new ReentrantLock();
    private Process workerProcess;
    private BufferedWriter workerInput;
    private BufferedReader workerOutput;

    public String parseTable(String imagePath) {
        return runPython(imagePath);
    }

    public String parsePdf(String pdfPath) {
        return runPython(pdfPath);
    }

    private String runPython(String inputPath) {
        boolean locked = false;
        try {
            workerLock.lockInterruptibly();
            locked = true;
            ensureWorkerStarted();

            Process activeProcess = workerProcess;
            ocrTaskManager.registerProcess(activeProcess);
            try {
                String request = objectMapper.writeValueAsString(Map.of("inputPath", inputPath));
                workerInput.write(request);
                workerInput.newLine();
                workerInput.flush();

                String resultLine = readProtocolLine("RESULT\t");
                String jsonPart = resultLine.substring("RESULT\t".length());
                Map<String, Object> parsed = lenientMapper.readValue(jsonPart,
                        new TypeReference<Map<String, Object>>() {});
                return objectMapper.writeValueAsString(parsed);
            } finally {
                ocrTaskManager.unregisterProcess(activeProcess);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (locked) resetWorker();
            return errorJson("OCR 任务已取消");
        } catch (Exception e) {
            resetWorker();
            return errorJson(e.getMessage());
        } finally {
            if (locked) workerLock.unlock();
        }
    }

    private void ensureWorkerStarted() throws IOException {
        if (workerProcess != null && workerProcess.isAlive()) return;

        resetWorker();
        String python = Path.of("", "src/main/python/.venv/Scripts/python.exe")
                .toAbsolutePath().normalize().toString();
        String scriptPath = Path.of("", "src/main/python/ppstructure/ocr_worker.py")
                .toAbsolutePath().normalize().toString();

        ProcessBuilder pb = new ProcessBuilder(python, "-u", scriptPath);
        Map<String, String> env = pb.environment();
        env.put("PYTHONIOENCODING", "utf-8");
        String modelsDir = Path.of("", "models").toAbsolutePath().normalize().toString();
        env.put("HOME", modelsDir);
        env.put("USERPROFILE", modelsDir);
        env.put("OCR_DEVICE", ocrDevice);
        pb.redirectErrorStream(true);

        workerProcess = pb.start();
        workerInput = new BufferedWriter(new OutputStreamWriter(
                workerProcess.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8));
        workerOutput = new BufferedReader(new InputStreamReader(
                workerProcess.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
        readProtocolLine("READY");
    }

    private String readProtocolLine(String prefix) throws IOException {
        String line;
        while ((line = workerOutput.readLine()) != null) {
            if (line.equals(prefix) || line.startsWith(prefix)) return line;
        }
        throw new EOFException("OCR Python 工作进程意外退出");
    }

    private void resetWorker() {
        if (workerProcess != null && workerProcess.isAlive()) {
            workerProcess.descendants().forEach(ProcessHandle::destroyForcibly);
            workerProcess.destroyForcibly();
        }
        workerProcess = null;
        workerInput = null;
        workerOutput = null;
    }

    @PreDestroy
    public void shutdownWorker() {
        workerLock.lock();
        try {
            resetWorker();
        } finally {
            workerLock.unlock();
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
