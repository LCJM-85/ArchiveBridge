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
        String fileType = "picture";

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

                        Object rawData = parsed.get("data");
                        @SuppressWarnings("unchecked")
                        List<Map<String, String>> dataList = new ArrayList<>();
                        if (rawData instanceof List) {
                            for (Object rec : (List<?>) rawData) {
                                if (rec instanceof Map) {
                                    Map<String, String> row = new LinkedHashMap<>();
                                    for (Map.Entry<?, ?> e : ((Map<?, ?>) rec).entrySet()) {
                                        if (e.getValue() != null && !e.getValue().toString().isEmpty()) {
                                            row.put(e.getKey().toString(), e.getValue().toString());
                                        }
                                    }
                                    if (!row.isEmpty()) {
                                        dataList.add(row);
                                    }
                                }
                            }
                        } else if (rawData instanceof Map) {
                            Map<String, String> single = new LinkedHashMap<>();
                            for (Map.Entry<?, ?> e : ((Map<?, ?>) rawData).entrySet()) {
                                if (e.getValue() != null && !e.getValue().toString().isEmpty()) {
                                    single.put(e.getKey().toString(), e.getValue().toString());
                                }
                            }
                            if (!single.isEmpty()) {
                                dataList.add(single);
                            }
                        }
                        item.put("data", dataList);
                        item.put("errors", errs);

                        if (!errs.isEmpty()) {
                            StringBuilder sb = new StringBuilder("字段校验警告: ");
                            for (Map<String, Object> e : errs) {
                                sb.append(e.get("message")).append("; ");
                            }
                            storageService.failedFile(fileName, sb.toString());
                        } else if (dataList.isEmpty()) {
                            storageService.failedFile(fileName, "未匹配到任何元数据字段");
                        } else {
                            Integer fileId = dataPersistenceService.saveArchiveFileDimData(fileName, fileType);

                            for (Map<String, String> record : dataList) {
                                dataPersistenceService.saveExtractedData(archiveType, record, fileId);
                            }

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
