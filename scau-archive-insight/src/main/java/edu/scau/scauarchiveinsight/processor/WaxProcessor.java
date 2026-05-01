package edu.scau.scauarchiveinsight.processor;

import edu.scau.scauarchiveinsight.service.OCRService;
import edu.scau.scauarchiveinsight.service.OpenCVService;
import edu.scau.scauarchiveinsight.service.StorageService;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WaxProcessor {

    private final OpenCVService openCVService;
    private final OCRService ocrService;
    private final StorageService storageService;

    public WaxProcessor(OpenCVService openCVService, OCRService ocrService, StorageService storageService) {
        this.openCVService = openCVService;
        this.ocrService = ocrService;
        this.storageService = storageService;
    }

    private static boolean isEnhanceFailed(String enhancedPath) {
        return enhancedPath == null || enhancedPath.startsWith("ERROR") || enhancedPath.startsWith("图片增强失败");
    }

    public List<Map<String, Object>> process(List<String> imagePaths) {
        List<Map<String, Object>> results = new ArrayList<>();

        for (String imagePath : imagePaths) {
            Map<String, Object> item = new HashMap<>();
            item.put("originalPath", imagePath);

            String enhancedPath = null;
            try {
                enhancedPath = openCVService.enhanceImage(imagePath);
                item.put("enhancedPath", enhancedPath);
            } catch (Exception e) {
                item.put("enhancedPath", null);
                item.put("enhanceError", e.getMessage());
            }

            String ocrPath = isEnhanceFailed(enhancedPath) ? imagePath : enhancedPath;

            try {
                String text = ocrService.recognizeText(ocrPath);
                item.put("ocrText", text);

                if (text != null && !text.startsWith("OCR 识别出错")) {
                    try {
                        storageService.moveArchiveFile(Paths.get(imagePath).getFileName().toString());
                    } catch (Exception ignored) {
                    }
                    // 删除增强临时图片
                    if (!isEnhanceFailed(enhancedPath)) {
                        try {
                            Files.deleteIfExists(Paths.get(enhancedPath));
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception e) {
                item.put("ocrText", null);
                item.put("ocrError", e.getMessage());
            }

            results.add(item);
        }

        return results;
    }
}
