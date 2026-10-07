package edu.scau.scauarchiveinsight.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.JsonParser;
import edu.scau.scauarchiveinsight.pojo.MetaDataStandard;
import edu.scau.scauarchiveinsight.pojo.ProvinceDim;
import edu.scau.scauarchiveinsight.service.ReviewDraftService;
import edu.scau.scauarchiveinsight.service.FieldCorrectionService;
import edu.scau.scauarchiveinsight.service.MetaDataMappingService;
import edu.scau.scauarchiveinsight.service.MetaDataService;
import edu.scau.scauarchiveinsight.service.OCRLogService;
import edu.scau.scauarchiveinsight.service.PPStructureService;
import edu.scau.scauarchiveinsight.service.OpenCVService;
import edu.scau.scauarchiveinsight.mapper.ProvinceDimMapper;
import edu.scau.scauarchiveinsight.service.QualityScoreService;
import edu.scau.scauarchiveinsight.service.StorageService;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ImageProcessor {

    private final OpenCVService openCVService;
    private final PPStructureService ppStructureService;
    private final MetaDataMappingService metaDataMappingService;
    private final MetaDataService metaDataService;
    private final FieldCorrectionService fieldCorrectionService;
    private final ProvinceDimMapper provinceDimMapper;
    private final OCRLogService ocrLogService;
    private final QualityScoreService qualityScoreService;
    private final StorageService storageService;
    private final ReviewDraftService reviewDraftService;
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS)
            .build();

    public ImageProcessor(OpenCVService openCVService, PPStructureService ppStructureService,
                          MetaDataMappingService metaDataMappingService,
                          MetaDataService metaDataService,
                          FieldCorrectionService fieldCorrectionService,
                          ProvinceDimMapper provinceDimMapper,
                          OCRLogService ocrLogService, QualityScoreService qualityScoreService,
                          StorageService storageService, ReviewDraftService reviewDraftService) {
        this.openCVService = openCVService;
        this.ppStructureService = ppStructureService;
        this.metaDataMappingService = metaDataMappingService;
        this.metaDataService = metaDataService;
        this.fieldCorrectionService = fieldCorrectionService;
        this.provinceDimMapper = provinceDimMapper;
        this.ocrLogService = ocrLogService;
        this.qualityScoreService = qualityScoreService;
        this.storageService = storageService;
        this.reviewDraftService = reviewDraftService;
    }

    private static boolean isEnhanceFailed(String enhancedPath) {
        return enhancedPath == null || enhancedPath.startsWith("ERROR") || enhancedPath.startsWith("图片增强失败");
    }

    public List<Map<String, Object>> process(List<String> imagePaths, String archiveType) {
        return process(imagePaths, archiveType, null, null);
    }

    public List<Map<String, Object>> process(List<String> imagePaths, String archiveType, String provinceName) {
        return process(imagePaths, archiveType, provinceName, null);
    }

    public List<Map<String, Object>> process(List<String> imagePaths, String archiveType, String provinceName, String admissionDate) {
        return process(imagePaths, archiveType, provinceName, admissionDate, null);
    }

    public List<Map<String, Object>> process(List<String> imagePaths, String archiveType, String provinceName, String admissionDate, String degreeName) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (String imagePath : imagePaths) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("originalPath", imagePath);
            String fileName = Paths.get(imagePath).getFileName().toString();
            String fileType = "picture";

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
                List<MetaDataStandard> rules = metaDataService.list();
                String text = ppStructureService.parseTable(ocrPath);
                if (!isEnhanceFailed(enhancedPath) && !hasMappableHeaders(text, rules)) {
                    text = ppStructureService.parseTable(imagePath);
                }

                if (text == null || text.isEmpty()) {
                    item.put("data", Map.of());
                    item.put("errors", List.of(Map.of("field", "", "message", "表格识别无返回")));
                    try {
                        storageService.failedFile(fileName, "表格识别无返回");
                    } catch (Exception ignored) {}
                } else {
                    try {
                        Map<String, Object> parsed = objectMapper.readValue(text,
                                new TypeReference<Map<String, Object>>() {});
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> errs = new ArrayList<>((List<Map<String, Object>>) parsed.getOrDefault("errors", List.of()));
                        errs.replaceAll(error -> {
                            if (error.containsKey("message") || !error.containsKey("msg")) return error;
                            Map<String, Object> normalized = new LinkedHashMap<>(error);
                            normalized.put("message", error.get("msg"));
                            return normalized;
                        });

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> grids = (List<Map<String, Object>>) parsed.get("grids");
                        List<Map<String, String>> dataList = new ArrayList<>();

                        if (grids != null) {
                            List<String> allProvinces = provinceDimMapper.selectList(null).stream()
                                    .map(ProvinceDim::getProvinceName).collect(Collectors.toList());

                            for (Map<String, Object> grid : grids) {
                                @SuppressWarnings("unchecked")
                                List<String> headers = (List<String>) grid.get("headers");
                                @SuppressWarnings("unchecked")
                                List<List<String>> rows = (List<List<String>>) grid.get("rows");
                                if (headers == null || rows == null) continue;

                                // 未匹配的列写入 OCR 警告日志（每个 grid 只报一次）
                                {
                                    List<String> unmatched = metaDataMappingService.findUnmatchedHeaders(headers, rules);
                                    for (String u : unmatched) {
                                        errs.add(Map.of("message", "未匹配的列: " + u));
                                    }
                                }

                                for (List<String> row : rows) {
                                    Map<String, String> rawRow = new LinkedHashMap<>();
                                    for (int i = 0; i < headers.size() && i < row.size(); i++) {
                                        String val = row.get(i).trim();
                                        if (!val.isEmpty()) {
                                            rawRow.put(headers.get(i), val);
                                        }
                                    }
                                    if (rawRow.isEmpty()) continue;

                                    fieldCorrectionService.autoCorrectFields(rawRow, allProvinces, archiveType);

                                    List<List<String>> singleRow = new ArrayList<>();
                                    singleRow.add(new ArrayList<>(row));
                                    List<Map<String, String>> mapped = metaDataMappingService.mapGrid(headers, singleRow, rules);
                                    if (!mapped.isEmpty()) {
                                        Map<String, String> merged = new LinkedHashMap<>(mapped.get(0));
                                        merged.putAll(rawRow);
                                        if (!merged.isEmpty()) {
                                            dataList.add(merged);
                                        }
                                    }
                                }
                            }

                            // 去重：多个 grid 可能报相同的未匹配列
                            Set<String> seen = new LinkedHashSet<>();
                            errs.removeIf(e -> !seen.add(String.valueOf(e.get("message"))));
                        }

                        item.put("data", dataList);
                        item.put("errors", errs);

                        if (dataList.isEmpty()) {
                            String reason = errs.stream()
                                    .map(error -> error.get("message"))
                                    .filter(Objects::nonNull)
                                    .map(Object::toString)
                                    .filter(message -> !message.isBlank())
                                    .collect(java.util.stream.Collectors.joining("; "));
                            storageService.failedFile(fileName,
                                    reason.isBlank() ? "未匹配到任何元数据字段" : reason);
                        } else {
                            if (provinceName != null && !provinceName.isBlank()) {
                                for (Map<String, String> record : dataList) {
                                    record.putIfAbsent("province_name", provinceName);
                                }
                            }
                            if (admissionDate != null && !admissionDate.isBlank()) {
                                for (Map<String, String> record : dataList) {
                                    record.putIfAbsent("admission_date", admissionDate);
                                }
                            }
                            if (degreeName != null && !degreeName.isBlank()) {
                                for (Map<String, String> record : dataList) {
                                    record.putIfAbsent("degree_name", degreeName);
                                }
                            }

                            reviewDraftService.stage(fileName, fileType, archiveType, dataList, errs);
                        }
                    } catch (Exception e) {
                        item.put("data", Map.of());
                        item.put("errors", List.of(Map.of("field", "", "message", "JSON解析失败: " + e.getMessage())));
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

    private boolean hasMappableHeaders(String text, List<MetaDataStandard> rules) {
        if (text == null || text.isBlank()) return false;
        try {
            Map<String, Object> parsed = objectMapper.readValue(text,
                    new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> grids = (List<Map<String, Object>>) parsed.get("grids");
            if (grids == null) return false;

            for (Map<String, Object> grid : grids) {
                @SuppressWarnings("unchecked")
                List<String> headers = (List<String>) grid.get("headers");
                if (headers == null) continue;
                long nonBlankCount = headers.stream()
                        .filter(Objects::nonNull)
                        .filter(header -> !header.isBlank())
                        .count();
                if (nonBlankCount == 0) continue;

                long unmatchedCount = metaDataMappingService
                        .findUnmatchedHeaders(headers, rules).stream()
                        .filter(Objects::nonNull)
                        .filter(header -> !header.isBlank())
                        .count();
                if (unmatchedCount < nonBlankCount) return true;
            }
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }
}
