package edu.scau.scauarchiveinsight.processor;

import edu.scau.scauarchiveinsight.service.StorageService;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

@Component
public class CSVProcessor {

    private final StorageService storageService;

    public CSVProcessor(StorageService storageService) {
        this.storageService = storageService;
    }

    public List<Map<String, String>> process(String filePath) {
        List<Map<String, String>> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                storageService.failedFile(Paths.get(filePath).getFileName().toString());
                return rows;
            }

            List<String> headers = parseCsvLine(headerLine);
            if (headers.isEmpty()) {
                storageService.failedFile(Paths.get(filePath).getFileName().toString());
                return rows;
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
                storageService.failedFile(Paths.get(filePath).getFileName().toString());
            } catch (IOException ignored) {
            }
        }

        if (!rows.isEmpty()) {
            try {
                storageService.moveArchiveFile(Paths.get(filePath).getFileName().toString());
            } catch (Exception ignored) {
            }
        }

        return rows;
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
