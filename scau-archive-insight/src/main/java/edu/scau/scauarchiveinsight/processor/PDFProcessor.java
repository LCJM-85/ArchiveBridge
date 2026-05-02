package edu.scau.scauarchiveinsight.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.scau.scauarchiveinsight.service.DataPersistenceService;
import edu.scau.scauarchiveinsight.service.OCRService;
import edu.scau.scauarchiveinsight.service.PdfToImageService;
import edu.scau.scauarchiveinsight.service.StorageService;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PDFProcessor {

    private final PdfToImageService pdfToImageService;
    private final OCRService ocrService;
    private final StorageService storageService;
    private final DataPersistenceService dataPersistenceService;

    public PDFProcessor(PdfToImageService pdfToImageService, OCRService ocrService,
                        StorageService storageService, DataPersistenceService dataPersistenceService) {
        this.pdfToImageService = pdfToImageService;
        this.ocrService = ocrService;
        this.storageService = storageService;
        this.dataPersistenceService = dataPersistenceService;
    }

    public List<Map<String, Object>> process(String pdfPath, String archiveType) {
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

        String fileName = Paths.get(pdfPath).getFileName().toString();
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> allErrors = new ArrayList<>();
        boolean hasSuccess = false;
        Map<String, String> extractedData = new LinkedHashMap<>();

        for (Map<String, Object> page : results) {
            String ocrResult = (String) page.get("ocrResult");
            if (ocrResult == null || ocrResult.startsWith("OCR 识别出错")) {
                allErrors.add(ocrResult != null ? ocrResult : "OCR 识别出错");
                continue;
            }
            try {
                Map<String, Object> parsed = objectMapper.readValue(ocrResult,
                        new TypeReference<Map<String, Object>>() {});
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> errs = (List<Map<String, Object>>) parsed.get("errors");
                if (errs != null && !errs.isEmpty()) {
                    for (Map<String, Object> e : errs) {
                        allErrors.add(String.valueOf(e.get("message")));
                    }
                }
                Object rawData = parsed.get("data");
                if (rawData instanceof List) {
                    for (Object rec : (List<?>) rawData) {
                        if (rec instanceof Map) {
                            for (Map.Entry<?, ?> e : ((Map<?, ?>) rec).entrySet()) {
                                if (e.getValue() != null && !e.getValue().toString().isEmpty()) {
                                    extractedData.put(e.getKey().toString(), e.getValue().toString());
                                }
                            }
                        }
                    }
                } else if (rawData instanceof Map) {
                    for (Map.Entry<?, ?> e : ((Map<?, ?>) rawData).entrySet()) {
                        if (e.getValue() != null && !e.getValue().toString().isEmpty()) {
                            extractedData.put(e.getKey().toString(), e.getValue().toString());
                        }
                    }
                }
                boolean hasData = !extractedData.isEmpty();
                if (hasData) {
                    hasSuccess = true;
                } else if (errs == null || errs.isEmpty()) {
                    allErrors.add("未匹配到任何元数据字段");
                }

            } catch (Exception ignored) {}
        }

        if (!allErrors.isEmpty()) {
            try {
                storageService.failedFile(fileName, String.join("; ", allErrors));
            } catch (Exception ignored) {
            }
        } else if (hasSuccess) {
            try {
                dataPersistenceService.saveExtractedData(archiveType, extractedData);
                storageService.moveArchiveFile(fileName);
            } catch (Exception ignored) {
            }
        } else {
            try {
                storageService.failedFile(fileName, "PDF 所有页面 OCR 识别失败");
            } catch (Exception ignored) {
            }
        }

        // 删除 PDF 转换生成的临时图片
        for (String imagePath : imagePaths) {
            try {
                Files.deleteIfExists(Paths.get(imagePath));
            } catch (Exception ignored) {
            }
        }

        return results;
    }
}
