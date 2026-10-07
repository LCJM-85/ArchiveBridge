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
    private final ReviewDraftService reviewDraftService;
    private final OCRLogService ocrLogService;
    private final QualityScoreService qualityScoreService;
    private final StorageService storageService;
    private final OCRTaskManager ocrTaskManager;

    public LLMProcessor(LLMExtractionService llmExtractionService,
                        ReviewDraftService reviewDraftService,
                        OCRLogService ocrLogService,
                        QualityScoreService qualityScoreService,
                        StorageService storageService,
                        OCRTaskManager ocrTaskManager) {
        this.llmExtractionService = llmExtractionService;
        this.reviewDraftService = reviewDraftService;
        this.ocrLogService = ocrLogService;
        this.qualityScoreService = qualityScoreService;
        this.storageService = storageService;
        this.ocrTaskManager = ocrTaskManager;
    }

    public List<Map<String, Object>> process(List<String> imagePaths, String archiveType) {
        return process(imagePaths, archiveType, null, null);
    }

    public List<Map<String, Object>> process(List<String> imagePaths, String archiveType,
                                              String provinceName, String admissionDate) {
        return process(imagePaths, archiveType, provinceName, admissionDate, null);
    }

    public List<Map<String, Object>> process(List<String> imagePaths, String archiveType,
                                              String provinceName, String admissionDate, String degreeName) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (String imagePath : imagePaths) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("originalPath", imagePath);
            String fileName = Paths.get(imagePath).getFileName().toString();
            String fileType = "picture-llm";
            List<Map<String, Object>> allErrors = new ArrayList<>();

            try {
                LLMExtractionService.ExtractionResult extraction = llmExtractionService.extractWithIssues(imagePath);
                List<Map<String, Object>> data = extraction.data();
                allErrors.addAll(extraction.errors());

                if (!data.isEmpty()) {
                    List<Map<String, String>> stringData = new ArrayList<>();
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
                        if (degreeName != null && !degreeName.isBlank()) {
                            flatRecord.putIfAbsent("degree_name", degreeName);
                        }
                        stringData.add(flatRecord);
                    }

                    reviewDraftService.stage(fileName, fileType, archiveType, stringData, allErrors);
                } else {
                    log.warn("LLM 提取结果为空 (图片: {})", imagePath);
                    storageService.failedFile(fileName,
                            summarizeIssues(allErrors, "LLM 未提取到有效数据"));
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
                                                      String archiveType, String provinceName, String admissionDate,
                                                      String degreeName) {
        List<Map<String, Object>> results = new ArrayList<>();
        String pdfFileName = Paths.get(pdfPath).getFileName().toString();

        try {
            // 1. 提取所有页面的数据
            List<Map<String, String>> allData = new ArrayList<>();
            List<Map<String, Object>> pageIssues = new ArrayList<>();
            for (int pageIndex = 0; pageIndex < pagePaths.size(); pageIndex++) {
                String pagePath = pagePaths.get(pageIndex);
                Integer taskId = ocrTaskManager.getCurrentTaskId();
                if (taskId != null) {
                    ocrLogService.updateMessage(taskId,
                            "LLM：处理第 " + (pageIndex + 1) + "/" + pagePaths.size() + " 页");
                }
                try {
                    LLMExtractionService.ExtractionResult extraction = llmExtractionService.extractWithIssues(pagePath);
                    List<Map<String, Object>> pageRecords = extraction.data();
                    for (Map<String, Object> issue : extraction.errors()) {
                        Map<String, Object> pageIssue = new LinkedHashMap<>(issue);
                        pageIssue.putIfAbsent("row", pageIndex + 1);
                        pageIssues.add(pageIssue);
                    }
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
                        if (degreeName != null && !degreeName.isBlank()) {
                            flat.putIfAbsent("degree_name", degreeName);
                        }
                        if (!flat.isEmpty()) allData.add(flat);
                    }
                } catch (Exception e) {
                    log.warn("PDF 页面处理失败: {} - {}", pagePath, e.getMessage());
                    pageIssues.add(Map.of(
                            "row", pageIndex + 1,
                            "message", "第 " + (pageIndex + 1) + " 页提取失败: " + e.getMessage()));
                }
            }

            if (allData.isEmpty()) {
                String reason = summarizeIssues(pageIssues, "LLM 未提取到有效数据");
                storageService.failedFile(pdfFileName, reason);
                results.add(Map.of("originalPath", pdfPath, "data", List.of(), "errors", List.of(Map.of("msg", reason))));
                return results;
            }

            reviewDraftService.stage(pdfFileName, "pdf-llm", archiveType, allData, pageIssues);

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

    private String summarizeIssues(List<Map<String, Object>> issues, String fallback) {
        List<String> messages = new ArrayList<>();
        for (Map<String, Object> issue : issues) {
            Object raw = issue.containsKey("message") ? issue.get("message") : issue.get("msg");
            if (raw != null && !raw.toString().isBlank()) messages.add(raw.toString());
        }
        return messages.isEmpty() ? fallback : String.join("; ", messages);
    }
}
