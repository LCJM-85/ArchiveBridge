package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.processor.CSVProcessor;
import edu.scau.scauarchiveinsight.processor.ExcelProcessor;
import edu.scau.scauarchiveinsight.processor.PDFProcessor;
import edu.scau.scauarchiveinsight.processor.WaxProcessor;
import edu.scau.scauarchiveinsight.service.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ArchiveUploadController {

    private final StorageService storageService;
    private final CSVProcessor csvProcessor;
    private final ExcelProcessor excelProcessor;
    private final PDFProcessor pdfProcessor;
    private final WaxProcessor waxProcessor;

    public ArchiveUploadController(StorageService storageService, CSVProcessor csvProcessor,
                                   ExcelProcessor excelProcessor, PDFProcessor pdfProcessor,
                                   WaxProcessor waxProcessor) {
        this.storageService = storageService;
        this.csvProcessor = csvProcessor;
        this.excelProcessor = excelProcessor;
        this.pdfProcessor = pdfProcessor;
        this.waxProcessor = waxProcessor;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("type") String type) {

        Map<String, Object> result = storageService.saveFiles(files, type);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> uploaded = (List<Map<String, String>>) result.get("uploaded");

        if (uploaded == null || uploaded.isEmpty()) {
            return ResponseEntity.ok(result);
        }

        List<Map<String, Object>> allResults = new ArrayList<>();
        List<String> imageBatch = new ArrayList<>();

        for (Map<String, String> fileInfo : uploaded) {


            String name = fileInfo.get("name");
            String path = fileInfo.get("path");
            String ext = getExtension(name).toLowerCase();

            switch (ext) {
                case "csv" -> {
                    List<Map<String, String>> rows = csvProcessor.process(path);
                    allResults.add(Map.of("file", name, "type", "csv", "data", rows));
                }
                case "xls", "xlsx" -> {
                    List<Map<String, String>> rows = excelProcessor.process(path);
                    allResults.add(Map.of("file", name, "type", "excel", "data", rows));
                }
                case "pdf" -> {
                    List<Map<String, Object>> pages = pdfProcessor.process(path);
                    allResults.add(Map.of("file", name, "type", "pdf", "data", pages));
                }
                case "jpg", "jpeg", "png", "bmp", "gif", "tiff", "webp" -> {
                    imageBatch.add(path);
                }
            }
        }

        if (!imageBatch.isEmpty()) {
            List<Map<String, Object>> waxResults = waxProcessor.process(imageBatch);
            allResults.add(Map.of("type", "image", "data", waxResults));
        }

        result.put("processed", allResults);
        return ResponseEntity.ok(result);
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        return idx == -1 ? "" : filename.substring(idx + 1);
    }
}
