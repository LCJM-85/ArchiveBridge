package edu.scau.scauarchiveinsight.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import edu.scau.scauarchiveinsight.service.DataPersistenceService;
import edu.scau.scauarchiveinsight.service.OCRLogService;
import edu.scau.scauarchiveinsight.service.PPStructureService;
import edu.scau.scauarchiveinsight.service.OpenCVService;
import edu.scau.scauarchiveinsight.service.QualityScoreService;
import edu.scau.scauarchiveinsight.service.StorageService;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Component
public class ImageProcessor {

    private final OpenCVService openCVService;
    private final PPStructureService ppStructureService;
    private final OCRLogService ocrLogService;
    private final QualityScoreService qualityScoreService;
    private final StorageService storageService;
    private final DataPersistenceService dataPersistenceService;
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS)
            .build();

    public ImageProcessor(OpenCVService openCVService, PPStructureService ppStructureService,
                          OCRLogService ocrLogService, QualityScoreService qualityScoreService,
                        StorageService storageService, DataPersistenceService dataPersistenceService) {
        this.openCVService = openCVService;
        this.ppStructureService = ppStructureService;
        this.ocrLogService = ocrLogService;
        this.qualityScoreService = qualityScoreService;
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
                String text = ppStructureService.parseTable(ocrPath);

                if (text == null || text.isEmpty()) {
                    // 表格识别调用失败
                    item.put("data", Map.of());
                    item.put("errors", List.of(Maps("field", "", "message", "表格识别无返回")));
                    try {
                        storageService.failedFile(fileName, "表格识别无返回");
                    } catch (Exception ignored) {}
                } else {
                    // 表格识别成功，解析结果
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

                        if (dataList.isEmpty()) {
                            storageService.failedFile(fileName, "未匹配到任何元数据字段");
                        } else {
                            Integer fileId = dataPersistenceService.saveArchiveFileDimData(fileName, fileType);

                            for (Map<String, String> record : dataList) {
                                dataPersistenceService.saveExtractedData(archiveType, record, fileId);
                            }

                            storageService.moveArchiveFile(fileName);

                            // 质量评分
                            qualityScoreService.scoreFile(fileId, archiveType, dataList, errs.size());

                            // 有校验警告仍归档，仅记录到数据库
                            if (!errs.isEmpty()) {
                                StringBuilder sb = new StringBuilder("字段校验警告: ");
                                for (Map<String, Object> e : errs) {
                                    sb.append(e.get("message")).append("; ");
                                }
                                ocrLogService.addLog(fileId, fileName, fileType, "warning", sb.toString());
                            }
                        }
                    } catch (Exception e) {
                        item.put("data", Map.of());
                        item.put("errors", List.of(Maps("field", "", "message", "JSON解析失败: " + e.getMessage())));
                        storageService.failedFile(fileName, "表格识别结果解析失败: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                item.put("data", Map.of());
                item.put("errors", List.of());
                item.put("ocrError", e.getMessage());
                try {
                    storageService.failedFile(fileName, "表格识别异常: " + e.getMessage());
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
