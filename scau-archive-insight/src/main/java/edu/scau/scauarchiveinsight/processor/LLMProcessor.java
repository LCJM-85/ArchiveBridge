package edu.scau.scauarchiveinsight.processor;

import edu.scau.scauarchiveinsight.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Component
public class LLMProcessor {

    private static final Logger log = LoggerFactory.getLogger(LLMProcessor.class);

    private final LLMExtractionService llmExtractionService;
    private final DataPersistenceService dataPersistenceService;
    private final OCRLogService ocrLogService;
    private final QualityScoreService qualityScoreService;
    private final StorageService storageService;

    public LLMProcessor(LLMExtractionService llmExtractionService,
                        DataPersistenceService dataPersistenceService,
                        OCRLogService ocrLogService,
                        QualityScoreService qualityScoreService,
                        StorageService storageService) {
        this.llmExtractionService = llmExtractionService;
        this.dataPersistenceService = dataPersistenceService;
        this.ocrLogService = ocrLogService;
        this.qualityScoreService = qualityScoreService;
        this.storageService = storageService;
    }

    public List<Map<String, Object>> process(List<String> imagePaths, String archiveType) {
        return process(imagePaths, archiveType, null, null);
    }

    public List<Map<String, Object>> process(List<String> imagePaths, String archiveType,
                                              String provinceName, String admissionDate) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (String imagePath : imagePaths) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("originalPath", imagePath);
            String fileName = Paths.get(imagePath).getFileName().toString();
            String fileType = "picture-llm";
            List<Map<String, Object>> allErrors = new ArrayList<>();

            try {
                List<Map<String, Object>> data = llmExtractionService.extract(imagePath);

                if (!data.isEmpty()) {
                    Integer fileId = dataPersistenceService.saveArchiveFileDimData(fileName, fileType);

                    for (Map<String, Object> record : data) {
                        Map<String, String> flatRecord = new LinkedHashMap<>();
                        for (Map.Entry<String, Object> entry : record.entrySet()) {
                            if (entry.getValue() != null) {
                                flatRecord.put(entry.getKey(), entry.getValue().toString());
                            }
                        }
                        if (provinceName != null && !provinceName.isBlank()) {
                            flatRecord.putIfAbsent("province_name", provinceName);
                        }
                        if (admissionDate != null && !admissionDate.isBlank()) {
                            flatRecord.putIfAbsent("admission_date", admissionDate);
                        }
                        dataPersistenceService.saveExtractedData(archiveType, flatRecord, fileId);
                    }

                    List<Map<String, String>> stringData = new ArrayList<>();
                    for (Map<String, Object> record : data) {
                        Map<String, String> flat = new LinkedHashMap<>();
                        for (Map.Entry<String, Object> e : record.entrySet()) {
                            if (e.getValue() != null) flat.put(e.getKey(), e.getValue().toString());
                        }
                        stringData.add(flat);
                    }
                    qualityScoreService.scoreFile(fileId, archiveType, stringData, allErrors.size());
                    storageService.moveArchiveFile(fileName);

                    if (!allErrors.isEmpty()) {
                        StringBuilder sb = new StringBuilder("LLM 提取警告: ");
                        for (Map<String, Object> e : allErrors) {
                            sb.append(e.get("message")).append("; ");
                        }
                        ocrLogService.addLog(fileId, fileName, fileType, "warning", sb.toString());
                    }
                } else {
                    storageService.failedFile(fileName, "LLM 未提取到有效数据");
                }

                item.put("data", data);
                item.put("errors", allErrors);
            } catch (Exception e) {
                item.put("data", List.of());
                item.put("errors", List.of(Map.of("msg", e.getMessage())));
                try {
                    storageService.failedFile(fileName, "LLM 提取异常: " + e.getMessage());
                } catch (Exception ignored) {}
            }

            results.add(item);
        }

        return results;
    }

    /**
     * 处理 PDF 的所有页面图片，合并为一条记录归档（避免每张图独立计数和日志）
     */
    public List<Map<String, Object>> processPdfPages(String pdfPath, List<String> pagePaths,
                                                      String archiveType, String provinceName, String admissionDate) {
        List<Map<String, Object>> results = new ArrayList<>();
        String pdfFileName = Paths.get(pdfPath).getFileName().toString();

        try {
            // 1. 提取所有页面的数据
            List<Map<String, String>> allData = new ArrayList<>();
            for (String pagePath : pagePaths) {
                try {
                    List<Map<String, Object>> pageRecords = llmExtractionService.extract(pagePath);
                    for (Map<String, Object> record : pageRecords) {
                        Map<String, String> flat = new LinkedHashMap<>();
                        for (Map.Entry<String, Object> e : record.entrySet()) {
                            if (e.getValue() != null) flat.put(e.getKey(), e.getValue().toString());
                        }
                        if (provinceName != null && !provinceName.isBlank()) {
                            flat.putIfAbsent("province_name", provinceName);
                        }
                        if (admissionDate != null && !admissionDate.isBlank()) {
                            flat.putIfAbsent("admission_date", admissionDate);
                        }
                        if (!flat.isEmpty()) allData.add(flat);
                    }
                } catch (Exception e) {
                    log.warn("PDF 页面处理失败: {} - {}", pagePath, e.getMessage());
                }
            }

            if (allData.isEmpty()) {
                storageService.failedFile(pdfFileName, "LLM 未提取到有效数据");
                results.add(Map.of("originalPath", pdfPath, "data", List.of(), "errors", List.of(Map.of("msg", "LLM 未提取到有效数据"))));
                return results;
            }

            // 2. 以 PDF 名义创建一条归档记录
            Integer fileId = dataPersistenceService.saveArchiveFileDimData(pdfFileName, "pdf-llm");
            for (Map<String, String> record : allData) {
                dataPersistenceService.saveExtractedData(archiveType, record, fileId);
            }

            // 3. 一次质量评分
            qualityScoreService.scoreFile(fileId, archiveType, allData, 0);

            // 4. 归 PDF 原始文件（只减一次计数）
            storageService.moveArchiveFile(pdfFileName);

            // 5. 删除临时页面图片（不影响计数）
            for (String pagePath : pagePaths) {
                try { Files.deleteIfExists(Paths.get(pagePath)); } catch (Exception ignored) {}
            }
            // 清理页面目录
            for (String pagePath : pagePaths) {
                Path dir = Paths.get(pagePath).getParent();
                if (dir != null && dir.toString().endsWith("_pages")) {
                    try { Files.deleteIfExists(dir); } catch (Exception ignored) {}
                }
            }

            log.info("PDF LLM 处理完成: {} ({} 条数据, {} 页)", pdfFileName, allData.size(), pagePaths.size());
            results.add(Map.of("originalPath", pdfPath, "data", allData));
        } catch (Exception e) {
            log.error("PDF LLM 处理失败: {}", pdfFileName, e);
            try {
                storageService.failedFile(pdfFileName, "LLM 提取异常: " + e.getMessage());
            } catch (Exception ignored) {}
            results.add(Map.of("originalPath", pdfPath, "data", List.of(), "errors", List.of(Map.of("msg", e.getMessage()))));
        }

        return results;
    }
}
