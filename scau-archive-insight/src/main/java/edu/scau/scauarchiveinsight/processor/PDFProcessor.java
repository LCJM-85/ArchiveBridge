package edu.scau.scauarchiveinsight.processor;

import edu.scau.scauarchiveinsight.service.OCRService;
import edu.scau.scauarchiveinsight.service.PdfToImageService;
import edu.scau.scauarchiveinsight.service.StorageService;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PDFProcessor {

    private final PdfToImageService pdfToImageService;
    private final OCRService ocrService;
    private final StorageService storageService;

    public PDFProcessor(PdfToImageService pdfToImageService, OCRService ocrService, StorageService storageService) {
        this.pdfToImageService = pdfToImageService;
        this.ocrService = ocrService;
        this.storageService = storageService;
    }

    public List<Map<String, Object>> process(String pdfPath) {
        List<Map<String, Object>> results = new ArrayList<>();

        List<String> imagePaths = pdfToImageService.convertPdfToImages(pdfPath);

        if (imagePaths.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("pdfPath", pdfPath);
            error.put("error", "PDF 转图片失败，无图片输出");
            results.add(error);
            return results;
        }

        for (String imagePath : imagePaths) {
            Map<String, Object> pageResult = new HashMap<>();
            pageResult.put("imagePath", imagePath);

            try {
                String ocrResult = ocrService.recognizeText(imagePath);
                pageResult.put("ocrResult", ocrResult);
            } catch (Exception e) {
                pageResult.put("ocrResult", null);
                pageResult.put("ocrError", e.getMessage());
            }

            results.add(pageResult);
        }

        boolean hasSuccess = results.stream()
                .anyMatch(r -> r.get("ocrResult") != null
                        && !((String) r.get("ocrResult")).startsWith("OCR 识别出错"));
        if (hasSuccess) {
            try {
                storageService.moveArchiveFile(Paths.get(pdfPath).getFileName().toString());
            } catch (Exception ignored) {
            }
            // 删除 PDF 转换生成的临时图片
            for (String imagePath : imagePaths) {
                try {
                    Files.deleteIfExists(Paths.get(imagePath));
                } catch (Exception ignored) {
                }
            }
        }

        return results;
    }
}
