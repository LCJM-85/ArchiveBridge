package edu.scau.scauarchiveinsight.processor;

import edu.scau.scauarchiveinsight.service.DataPersistenceService;
import edu.scau.scauarchiveinsight.service.MetaDataMappingService;
import edu.scau.scauarchiveinsight.service.OCRLogService;
import edu.scau.scauarchiveinsight.service.QualityScoreService;
import edu.scau.scauarchiveinsight.service.StorageService;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

@Component
public class CSVProcessor {

    private final StorageService storageService;
    private final MetaDataMappingService metaDataMappingService;
    private final OCRLogService ocrLogService;
    private final QualityScoreService qualityScoreService;
    private final DataPersistenceService dataPersistenceService;

    public CSVProcessor(StorageService storageService, MetaDataMappingService metaDataMappingService,
                        OCRLogService ocrLogService, QualityScoreService qualityScoreService,
                        DataPersistenceService dataPersistenceService) {
        this.storageService = storageService;
        this.metaDataMappingService = metaDataMappingService;
        this.ocrLogService = ocrLogService;
        this.qualityScoreService = qualityScoreService;
        this.dataPersistenceService = dataPersistenceService;
    }

    public Map<String, Object> process(String filePath, String archiveType) {
        return process(filePath, archiveType, null, null);
    }

    public Map<String, Object> process(String filePath, String archiveType, String provinceName) {
        return process(filePath, archiveType, provinceName, null);
    }

    public Map<String, Object> process(String filePath, String archiveType, String provinceName, String admissionDate) {
        List<Map<String, String>> rows = new ArrayList<>();
        String fileType = "CSV";

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                storageService.failedFile(Paths.get(filePath).getFileName().toString());
                return Map.of("data", List.of(), "errors", List.of());
            }

            List<String> headers = parseCsvLine(headerLine);
            if (headers.isEmpty()) {
                storageService.failedFile(Paths.get(filePath).getFileName().toString());
                return Map.of("data", List.of(), "errors", List.of());
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                List<String> values = parseCsvLine(line);
                if (values.isEmpty()) continue;

                Map<String, String> rowData = new LinkedHashMap<>();
                boolean hasValue = false;
                for (int i = 0; i < headers.size(); i++) {
                    String value = i < values.size() ? values.get(i) : "";
                    rowData.put(headers.get(i), value);
                    if (!value.isEmpty()) hasValue = true;
                }
                if (hasValue) {
                    rows.add(rowData);
                }
            }

        } catch (Exception e) {
            try {
                storageService.failedFile(Paths.get(filePath).getFileName().toString(), e.getMessage());
            } catch (IOException ignored) {
            }
        }

        Map<String, Object> mapped = metaDataMappingService.process(rows);
        String fileName = Paths.get(filePath).getFileName().toString();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> errors = (List<Map<String, Object>>) mapped.get("errors");

        if (rows.isEmpty()) {
            try {
                storageService.failedFile(fileName, "无法解析到任何数据行");
            } catch (IOException ignored) {}
        } else {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> mappedData = (List<Map<String, String>>) mapped.get("data");
            Integer fileId = null;
            if (mappedData != null) {
                fileId = dataPersistenceService.saveArchiveFileDimData(fileName, fileType);
                if (provinceName != null && !provinceName.isBlank()) {
                    for (Map<String, String> record : mappedData) {
                        record.putIfAbsent("province_name", provinceName);
                    }
                }
                if (admissionDate != null && !admissionDate.isBlank()) {
                    for (Map<String, String> record : mappedData) {
                        record.putIfAbsent("admission_date", admissionDate);
                    }
                }
                for (Map<String, String> record : mappedData) {
                    dataPersistenceService.saveExtractedData(archiveType, record, fileId);
                }
            }
            try {
                storageService.moveArchiveFile(fileName);

                // 质量评分
                int errCount = errors != null ? errors.size() : 0;
                if (mappedData != null) {
                    qualityScoreService.scoreFile(fileId, archiveType, mappedData, errCount);
                }

                // 有校验警告仍归档，仅记录到数据库
                if (errors != null && !errors.isEmpty() && fileId != null) {
                    ocrLogService.addLog(fileId, fileName, fileType, "warning", errors.toString());
                }
            } catch (Exception ignored) {}
        }

        return mapped;
    }

    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString().trim());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString().trim());

        return fields;
    }
}
