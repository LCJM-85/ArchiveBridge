package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.processor.CSVProcessor;
import edu.scau.scauarchiveinsight.processor.ExcelProcessor;
import edu.scau.scauarchiveinsight.processor.PDFProcessor;
import edu.scau.scauarchiveinsight.processor.ImageProcessor;
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
    private final ImageProcessor imageProcessor;

    public ArchiveUploadController(StorageService storageService, CSVProcessor csvProcessor,
                                   ExcelProcessor excelProcessor, PDFProcessor pdfProcessor,
                                   ImageProcessor imageProcessor) {
        this.storageService = storageService;
        this.csvProcessor = csvProcessor;
        this.excelProcessor = excelProcessor;
        this.pdfProcessor = pdfProcessor;
        this.imageProcessor = imageProcessor;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("type") String type,
            @RequestParam("archiveType") String archiveType,
            @RequestParam(value = "provinceName", required = false) String provinceName,
            @RequestParam(value = "admissionDate", required = false) String admissionDate) {

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
                    Map<String, Object> csvResult = csvProcessor.process(path, archiveType, provinceName, admissionDate);
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("file", name);
                    entry.put("type", "csv");
                    entry.put("data", csvResult.get("data"));
                    entry.put("errors", csvResult.get("errors"));
                    allResults.add(entry);
                }
                case "xls", "xlsx" -> {
                    Map<String, Object> excelResult = excelProcessor.process(path, archiveType, provinceName, admissionDate);
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("file", name);
                    entry.put("type", "excel");
                    entry.put("data", excelResult.get("data"));
                    entry.put("errors", excelResult.get("errors"));
                    allResults.add(entry);
                }
                case "pdf" -> {
                    List<Map<String, Object>> pages = pdfProcessor.process(path, archiveType, provinceName, admissionDate);
                    allResults.add(Map.of("file", name, "type", "pdf", "data", pages));
                }
                case "jpg", "jpeg", "png", "bmp", "gif", "tiff", "webp" -> {
                    imageBatch.add(path);
                }
            }
        }

        if (!imageBatch.isEmpty()) {
            List<Map<String, Object>> imageResults = imageProcessor.process(imageBatch, archiveType, provinceName, admissionDate);
            List<Map<String, Object>> allErrors = new ArrayList<>();
            for (Map<String, Object> wr : imageResults) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> errs = (List<Map<String, Object>>) wr.get("errors");
                if (errs != null && !errs.isEmpty()) {
                    allErrors.addAll(errs);
                }
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", "image");
            entry.put("data", imageResults);
            if (!allErrors.isEmpty()) {
                entry.put("errors", allErrors);
            }
            allResults.add(entry);
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
