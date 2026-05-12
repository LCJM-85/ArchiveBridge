package edu.scau.scauarchiveinsight.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.JsonParser;
import edu.scau.scauarchiveinsight.pojo.MetaDataStandard;
import edu.scau.scauarchiveinsight.pojo.ProvinceDim;
import edu.scau.scauarchiveinsight.service.DataPersistenceService;
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
    private final DataPersistenceService dataPersistenceService;
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
                          StorageService storageService, DataPersistenceService dataPersistenceService) {
        this.openCVService = openCVService;
        this.ppStructureService = ppStructureService;
        this.metaDataMappingService = metaDataMappingService;
        this.metaDataService = metaDataService;
        this.fieldCorrectionService = fieldCorrectionService;
        this.provinceDimMapper = provinceDimMapper;
        this.ocrLogService = ocrLogService;
        this.qualityScoreService = qualityScoreService;
        this.storageService = storageService;
        this.dataPersistenceService = dataPersistenceService;
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
                String text = ppStructureService.parseTable(ocrPath);

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

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> grids = (List<Map<String, Object>>) parsed.get("grids");
                        List<Map<String, String>> dataList = new ArrayList<>();

                        if (grids != null) {
                            List<MetaDataStandard> rules = metaDataService.list();
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
                            storageService.failedFile(fileName, "未匹配到任何元数据字段");
                        } else {
                            Integer fileId = dataPersistenceService.saveArchiveFileDimData(fileName, fileType);
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

                            for (Map<String, String> record : dataList) {
                                dataPersistenceService.saveExtractedData(archiveType, record, fileId);
                            }

                            storageService.moveArchiveFile(fileName);

                            int scoreErrors = (int) errs.stream()
                                    .filter(e -> !String.valueOf(e.get("message")).startsWith("未匹配的列"))
                                    .count();
                            qualityScoreService.scoreFile(fileId, archiveType, dataList, scoreErrors);

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
}
