package edu.scau.scauarchiveinsight.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.scau.scauarchiveinsight.service.DataPersistenceService;
import edu.scau.scauarchiveinsight.service.OCRService;
import edu.scau.scauarchiveinsight.service.OpenCVService;
import edu.scau.scauarchiveinsight.service.StorageService;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Component
public class WaxProcessor {

    private final OpenCVService openCVService;
    private final OCRService ocrService;
    private final StorageService storageService;
    private final DataPersistenceService dataPersistenceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WaxProcessor(OpenCVService openCVService, OCRService ocrService,
                        StorageService storageService, DataPersistenceService dataPersistenceService) {
        this.openCVService = openCVService;
        this.ocrService = ocrService;
        this.storageService = storageService;
        this.dataPersistenceService = dataPersistenceService;
    }

    private static boolean isEnhanceFailed(String enhancedPath) {
        return enhancedPath == null || enhancedPath.startsWith("ERROR") || enhancedPath.startsWith("图片增强失败");
    }

    public List<Map<String, Object>> process(List<String> imagePaths, String archiveType) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (String imagePath : imagePaths) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("originalPath", imagePath);
            String fileName = Paths.get(imagePath).getFileName().toString();

            String enhancedPath = null;
            try {
                enhancedPath = openCVService.enhanceImage(imagePath);
                item.put("enhancedPath", enhancedPath);
            } catch (Exception e) {
                item.put("enhancedPath", null);
                item.put("enhanceError", e.getMessage());
            }

            String ocrPath = isEnhanceFailed(enhancedPath) ? imagePath : enhancedPath;

            try {
                String text = ocrService.recognizeText(ocrPath);
                // ====================== 打印 OCR 返回结果 ======================

                if (text != null && text.startsWith("OCR 识别出错")) {
                    // OCR 调用失败
                    item.put("data", Map.of());
                    item.put("errors", List.of(Maps("field", "", "message", text)));
                    try {
                        storageService.failedFile(fileName, text);
                    } catch (Exception ignored) {}
                } else {
                    // OCR 成功，解析结果
                    try {
                        Map<String, Object> parsed = objectMapper.readValue(text,
                                new TypeReference<Map<String, Object>>() {});
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> errs = (List<Map<String, Object>>) parsed.getOrDefault("errors", List.of());
                        @SuppressWarnings("unchecked")
                        Map<String, String> data;
                        Object rawData = parsed.get("data");
                        if (rawData instanceof List) {
                            // 新格式：data 是列表 [{...}, ...]，合并为单条
                            data = new LinkedHashMap<>();
                            for (Object rec : (List<?>) rawData) {
                                if (rec instanceof Map) {
                                    for (Map.Entry<?, ?> e : ((Map<?, ?>) rec).entrySet()) {
                                        if (e.getValue() != null && !e.getValue().toString().isEmpty()) {
                                            data.put(e.getKey().toString(), e.getValue().toString());
                                        }
                                    }
                                }
                            }
                        } else {
                            data = (Map<String, String>) (rawData instanceof Map ? rawData : Map.of());
                        }
                        item.put("data", data);
                        item.put("errors", errs);

                        boolean hasData = data.values().stream().anyMatch(v -> v != null && !v.isEmpty());
                        if (!errs.isEmpty()) {
                            StringBuilder sb = new StringBuilder("字段校验警告: ");
                            for (Map<String, Object> e : errs) {
                                sb.append(e.get("message")).append("; ");
                            }
                            storageService.failedFile(fileName, sb.toString());
                        } else if (!hasData) {
                            storageService.failedFile(fileName, "未匹配到任何元数据字段");
                        } else {
                            dataPersistenceService.saveExtractedData(archiveType, data);
                            storageService.moveArchiveFile(fileName);
                        }
                    } catch (Exception e) {
                        item.put("data", Map.of());
                        item.put("errors", List.of(Maps("field", "", "message", "JSON解析失败: " + e.getMessage())));
                        storageService.failedFile(fileName, "OCR结果解析失败: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                item.put("data", Map.of());
                item.put("errors", List.of());
                item.put("ocrError", e.getMessage());
                try {
                    storageService.failedFile(fileName, "OCR处理异常: " + e.getMessage());
                } catch (Exception ignored) {}
            }

            if (!isEnhanceFailed(enhancedPath)) {
                try {
                    Files.deleteIfExists(Paths.get(enhancedPath));
                } catch (Exception ignored) {}
            }

            results.add(item);
        }

        return results;
    }

    // 简单 Map.of 替代（兼容 Java 9+）
    private static Map<String, Object> Maps(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }
}
