package edu.scau.scauarchiveinsight.processor;

import edu.scau.scauarchiveinsight.service.DataPersistenceService;
import edu.scau.scauarchiveinsight.service.MetaDataMappingService;
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
    private final DataPersistenceService dataPersistenceService;

    public CSVProcessor(StorageService storageService, MetaDataMappingService metaDataMappingService,
                        DataPersistenceService dataPersistenceService) {
        this.storageService = storageService;
        this.metaDataMappingService = metaDataMappingService;
        this.dataPersistenceService = dataPersistenceService;
    }

    public Map<String, Object> process(String filePath, String archiveType) {
        List<Map<String, String>> rows = new ArrayList<>();

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
        if (errors != null && !errors.isEmpty()) {
            try {
                storageService.failedFile(fileName, errors.toString());
            } catch (IOException ignored) {
            }
        } else if (!rows.isEmpty()) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> mappedData = (List<Map<String, String>>) mapped.get("data");
            if (mappedData != null) {
                for (Map<String, String> record : mappedData) {
                    dataPersistenceService.saveExtractedData(archiveType, record);
                }
            }
            try {
                storageService.moveArchiveFile(fileName);
            } catch (Exception ignored) {
            }
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
