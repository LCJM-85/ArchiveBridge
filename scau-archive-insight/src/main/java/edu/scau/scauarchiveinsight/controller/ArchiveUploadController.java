package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.processor.CSVProcessor;
import edu.scau.scauarchiveinsight.processor.ExcelProcessor;
import edu.scau.scauarchiveinsight.processor.ImageProcessor;
import edu.scau.scauarchiveinsight.processor.LLMProcessor;
import edu.scau.scauarchiveinsight.processor.PDFProcessor;
import edu.scau.scauarchiveinsight.service.StorageService;
import edu.scau.scauarchiveinsight.service.OCRLogService;
import edu.scau.scauarchiveinsight.service.OCRTaskManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.*;

@Tag(name = "档案上传", description = "档案文件上传与处理")
@RestController
@RequestMapping("/api")
public class ArchiveUploadController {

    private final StorageService storageService;
    private final CSVProcessor csvProcessor;
    private final ExcelProcessor excelProcessor;
    private final PDFProcessor pdfProcessor;
    private final ImageProcessor imageProcessor;
    private final LLMProcessor llmProcessor;
    private final OCRLogService ocrLogService;
    private final OCRTaskManager ocrTaskManager;

    public ArchiveUploadController(StorageService storageService, CSVProcessor csvProcessor,
                                   ExcelProcessor excelProcessor, PDFProcessor pdfProcessor,
                                   ImageProcessor imageProcessor,
                                   LLMProcessor llmProcessor, OCRLogService ocrLogService,
                                   OCRTaskManager ocrTaskManager) {
        this.storageService = storageService;
        this.csvProcessor = csvProcessor;
        this.excelProcessor = excelProcessor;
        this.pdfProcessor = pdfProcessor;
        this.imageProcessor = imageProcessor;
        this.llmProcessor = llmProcessor;
        this.ocrLogService = ocrLogService;
        this.ocrTaskManager = ocrTaskManager;
    }

    @Operation(summary = "上传档案文件并自动处理")
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("type") String type,
            @RequestParam("archiveType") String archiveType,
            @RequestParam(value = "provinceName", required = false) String provinceName,
            @RequestParam(value = "admissionDate", required = false) String admissionDate,
            @RequestParam(value = "degreeName", required = false) String degreeName,
            @RequestParam(value = "useLlm", defaultValue = "false") boolean useLlm) {

        Map<String, Object> result = storageService.saveFiles(files, type);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> uploaded = (List<Map<String, String>>) result.get("uploaded");

        if (uploaded == null || uploaded.isEmpty()) {
            return ResponseEntity.ok(result);
        }

        List<Map<String, Object>> tasks = new ArrayList<>();
        for (Map<String, String> fileInfo : uploaded) {
            String originalName = fileInfo.get("name");
            String path = fileInfo.get("path");
            String storedName = Paths.get(path).getFileName().toString();
            String ext = getExtension(originalName).toLowerCase();
            Integer logId = ocrLogService.createProcessingLog(storedName, ext);
            tasks.add(Map.of("taskId", logId, "fileName", originalName));
            ocrTaskManager.submit(logId, () -> processFile(logId, storedName, path, ext, archiveType,
                    provinceName, admissionDate, degreeName, useLlm));
        }
        result.put("tasks", tasks);
        result.put("message", "文件已上传，正在后台处理");
        return ResponseEntity.ok(result);
    }

    private void processFile(Integer logId, String storedName, String path, String ext, String archiveType,
                             String provinceName, String admissionDate, String degreeName, boolean useLlm) {
        try {
            switch (ext) {
                case "csv" -> {
                    ocrLogService.updateMessage(logId, "解析 CSV 文件");
                    csvProcessor.process(path, archiveType, provinceName, admissionDate, degreeName);
                }
                case "xls", "xlsx" -> {
                    ocrLogService.updateMessage(logId, "解析 Excel 文件");
                    excelProcessor.process(path, archiveType, provinceName, admissionDate, degreeName);
                }
                case "pdf" -> {
                    if (useLlm) {
                        ocrLogService.updateMessage(logId, "PDF 转换为图片");
                        List<String> pages = pdfToImage(path);
                        ocrLogService.updateMessage(logId, "LLM：处理 1/" + pages.size() + " 页");
                        llmProcessor.processPdfPages(path, pages, archiveType, provinceName, admissionDate, degreeName);
                    } else {
                        ocrLogService.updateMessage(logId, "OCR：识别 PDF");
                        pdfProcessor.process(path, archiveType, provinceName, admissionDate, degreeName);
                    }
                }
                case "jpg", "jpeg", "png", "bmp", "gif", "tiff", "webp" -> {
                    ocrLogService.updateMessage(logId, useLlm ? "LLM：等待模型响应" : "OCR：识别图片");
                    if (useLlm) llmProcessor.process(List.of(path), archiveType, provinceName, admissionDate, degreeName);
                    else imageProcessor.process(List.of(path), archiveType, provinceName, admissionDate, degreeName);
                }
                default -> {
                    storageService.failedFile(storedName, "不支持的文件类型: " + ext);
                    ocrLogService.markFailed(logId, "不支持的文件类型: " + ext);
                    return;
                }
            }
            ocrLogService.updateMessage(logId, "确认处理结果");
            ocrLogService.syncTodayLogs();
        } catch (Exception e) {
            ocrLogService.markFailed(logId, e.getMessage());
            try { storageService.failedFile(storedName, e.getMessage()); } catch (Exception ignored) {}
        }
    }

    private List<String> pdfToImage(String pdfPath) throws Exception {
        String python = Path.of("", "src/main/python/.venv/Scripts/python.exe")
                .toAbsolutePath().normalize().toString();
        String script = Path.of("", "src/main/python/pdf2image/pdf2image.py")
                .toAbsolutePath().normalize().toString();
        Path pdfFile = Paths.get(pdfPath);
        String baseName = pdfFile.getFileName().toString().replace('.', '_');
        Path outputDir = pdfFile.getParent().resolve(baseName + "_pages");
        Files.createDirectories(outputDir);

        ProcessBuilder pb = new ProcessBuilder(python, script, pdfPath, outputDir.toString());
        pb.environment().put("PYTHONIOENCODING", "utf-8");
        Process process = pb.start();
        ocrTaskManager.registerProcess(process);
        String output = new String(process.getInputStream().readAllBytes(), "UTF-8").trim();
        process.waitFor();
        ocrTaskManager.unregisterProcess(process);

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
