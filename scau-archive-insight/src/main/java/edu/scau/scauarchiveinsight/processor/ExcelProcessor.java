package edu.scau.scauarchiveinsight.processor;

import edu.scau.scauarchiveinsight.service.DataPersistenceService;
import edu.scau.scauarchiveinsight.service.MetaDataMappingService;
import edu.scau.scauarchiveinsight.service.StorageService;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.*;

@Component
public class ExcelProcessor {

    private final StorageService storageService;
    private final MetaDataMappingService metaDataMappingService;
    private final DataPersistenceService dataPersistenceService;

    public ExcelProcessor(StorageService storageService, MetaDataMappingService metaDataMappingService,
                          DataPersistenceService dataPersistenceService) {
        this.storageService = storageService;
        this.metaDataMappingService = metaDataMappingService;
        this.dataPersistenceService = dataPersistenceService;
    }

    public Map<String, Object> process(String filePath, String archiveType) {
        List<Map<String, String>> rows = new ArrayList<>();
        String fileType = "excel";

        try (InputStream is = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                storageService.failedFile(Paths.get(filePath).getFileName().toString());
                return Map.of("data", List.of(), "errors", List.of());
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                storageService.failedFile(Paths.get(filePath).getFileName().toString());
                return Map.of("data", List.of(), "errors", List.of());
            }

            int colCount = headerRow.getLastCellNum();
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < colCount; i++) {
                headers.add(getCellValue(headerRow.getCell(i)));
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Map<String, String> rowData = new LinkedHashMap<>();
                boolean hasValue = false;
                for (int c = 0; c < colCount; c++) {
                    String value = getCellValue(row.getCell(c));
                    rowData.put(headers.get(c), value);
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
                Integer fileId = dataPersistenceService.saveArchiveFileDimData(fileName, fileType);//保存文件识别日志

                for (Map<String, String> record : mappedData) {
                    dataPersistenceService.saveExtractedData(archiveType, record, fileId);
                }
            }
            try {
                storageService.moveArchiveFile(fileName);
            } catch (Exception ignored) {
            }
        }

        return mapped;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf((long) cell.getNumericCellValue());
                } catch (Exception e) {
                    try {
                        yield cell.getStringCellValue();
                    } catch (Exception e2) {
                        yield cell.getCellFormula();
                    }
                }
            }
            case BLANK -> "";
            default -> "";
        };
    }
}