package edu.scau.scauarchiveinsight.processor;

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

    public ExcelProcessor(StorageService storageService) {
        this.storageService = storageService;
    }

    public List<Map<String, String>> process(String filePath) {
        List<Map<String, String>> rows = new ArrayList<>();

        try (InputStream is = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                storageService.failedFile(Paths.get(filePath).getFileName().toString());
                return rows;
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                storageService.failedFile(Paths.get(filePath).getFileName().toString());
                return rows;
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
