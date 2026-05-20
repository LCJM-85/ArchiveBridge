package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.processor.CSVProcessor;
import edu.scau.scauarchiveinsight.processor.ExcelProcessor;
import edu.scau.scauarchiveinsight.processor.ImageProcessor;
import edu.scau.scauarchiveinsight.processor.LLMProcessor;
import edu.scau.scauarchiveinsight.processor.PDFProcessor;
import edu.scau.scauarchiveinsight.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ArchiveUploadController {

    private final StorageService storageService;
    private final CSVProcessor csvProcessor;
    private final ExcelProcessor excelProcessor;
    private final PDFProcessor pdfProcessor;
    private final ImageProcessor imageProcessor;
    private final LLMProcessor llmProcessor;

    public ArchiveUploadController(StorageService storageService, CSVProcessor csvProcessor,
                                   ExcelProcessor excelProcessor, PDFProcessor pdfProcessor,
                                   ImageProcessor imageProcessor,
                                   LLMProcessor llmProcessor) {
        this.storageService = storageService;
        this.csvProcessor = csvProcessor;
        this.excelProcessor = excelProcessor;
        this.pdfProcessor = pdfProcessor;
        this.imageProcessor = imageProcessor;
        this.llmProcessor = llmProcessor;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("type") String type,
            @RequestParam("archiveType") String archiveType,
            @RequestParam(value = "provinceName", required = false) String provinceName,
            @RequestParam(value = "admissionDate", required = false) String admissionDate,
            @RequestParam(value = "useLlm", defaultValue = "false") boolean useLlm) {

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
                    allResults.add(Map.of("file", name, "type", "csv", "data", csvResult.get("data"), "errors", csvResult.get("errors")));
                }
                case "xls", "xlsx" -> {
                    Map<String, Object> excelResult = excelProcessor.process(path, archiveType, provinceName, admissionDate);
                    allResults.add(Map.of("file", name, "type", "excel", "data", excelResult.get("data"), "errors", excelResult.get("errors")));
                }
                case "pdf" -> {
                    if (useLlm) {
                        // LLM 模式：PDF 先转图片，和图片一起批量处理
                        try {
                            List<String> pageImages = pdfToImage(path);
                            imageBatch.addAll(pageImages);
                        } catch (Exception e) {
                            Map<String, Object> entry = new LinkedHashMap<>();
                            entry.put("file", name);
                            entry.put("type", "pdf");
                            entry.put("data", List.of());
                            entry.put("errors", List.of(Map.of("msg", "PDF 转图片失败: " + e.getMessage())));
                            allResults.add(entry);
                        }
                    } else {
                        List<Map<String, Object>> pages = pdfProcessor.process(path, archiveType, provinceName, admissionDate);
                        allResults.add(Map.of("file", name, "type", "pdf", "data", pages));
                    }
                }
                case "jpg", "jpeg", "png", "bmp", "gif", "tiff", "webp" -> {
                    imageBatch.add(path);
                }
            }
        }

        if (!imageBatch.isEmpty()) {
            List<Map<String, Object>> allErrors = new ArrayList<>();
            List<Map<String, Object>> imageResults = new ArrayList<>();

            for (String imgPath : imageBatch) {
                String fileName = uploaded.stream()
                        .filter(f -> f.get("path").equals(imgPath))
                        .map(f -> f.get("name"))
                        .findFirst().orElse("unknown");

                try {
                    if (useLlm) {
                        List<Map<String, Object>> llmResults = llmProcessor.process(List.of(imgPath), archiveType,
                                provinceName, admissionDate);
                        imageResults.addAll(llmResults);
                    } else {
                        List<Map<String, Object>> ocrResults = imageProcessor.process(
                                List.of(imgPath), archiveType, provinceName, admissionDate);
                        imageResults.addAll(ocrResults);
                    }
                } catch (Exception e) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("originalPath", imgPath);
                    item.put("data", List.of());
                    item.put("errors", List.of(Map.of("msg", e.getMessage())));
                    imageResults.add(item);
                }
            }

            for (Map<String, Object> wr : imageResults) {
                @SuppressWarnings("unchecked")
                var errs = (List<Map<String, Object>>) wr.get("errors");
                if (errs != null && !errs.isEmpty()) {
                    allErrors.addAll(errs);
                }
            }

            // 清理 PDF 转换产生的临时图片目录
            for (String imgPath : imageBatch) {
                Path dir = Paths.get(imgPath).getParent();
                if (dir != null && dir.toString().endsWith("_pages")) {
                    try (var walk = Files.walk(dir)) {
                        walk.forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} });
                    } catch (Exception ignored) {}
                }
            }

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", useLlm ? "image-llm" : "image");
            entry.put("data", imageResults);
            if (!allErrors.isEmpty()) {
                entry.put("errors", allErrors);
            }
            allResults.add(entry);
        }

        result.put("processed", allResults);
        return ResponseEntity.ok(result);
    }

    private List<String> pdfToImage(String pdfPath) throws Exception {
        String python = "src/main/python/.venv/Scripts/python.exe";
        String script = "src/main/python/pdf2image/pdf2image.py";
        Path pdfFile = Paths.get(pdfPath);
        String baseName = pdfFile.getFileName().toString().replace('.', '_');
        Path outputDir = pdfFile.getParent().resolve(baseName + "_pages");
        Files.createDirectories(outputDir);

        ProcessBuilder pb = new ProcessBuilder(python, script, pdfPath, outputDir.toString());
        pb.environment().put("PYTHONIOENCODING", "utf-8");
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes(), "UTF-8").trim();
        process.waitFor();

        @SuppressWarnings("unchecked")
        Map<String, Object> result = new ObjectMapper().readValue(output, Map.class);
        @SuppressWarnings("unchecked")
        List<String> pages = (List<String>) result.get("pages");
        if (pages == null) {
            throw new RuntimeException("PDF 转图片失败: " + result.getOrDefault("error", ""));
        }
        return pages;
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        return idx == -1 ? "" : filename.substring(idx + 1);
    }
}
