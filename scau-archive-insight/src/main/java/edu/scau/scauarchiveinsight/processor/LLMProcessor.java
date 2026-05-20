package edu.scau.scauarchiveinsight.processor;

import edu.scau.scauarchiveinsight.service.*;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.*;

@Component
public class LLMProcessor {

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
}
