package edu.scau.scauarchiveinsight.processor;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.scau.scauarchiveinsight.service.DataPersistenceService;
import edu.scau.scauarchiveinsight.service.OCRLogService;
import edu.scau.scauarchiveinsight.service.OpenCVService;
import edu.scau.scauarchiveinsight.service.PPStructureService;
import edu.scau.scauarchiveinsight.service.PdfToImageService;
import edu.scau.scauarchiveinsight.service.QualityScoreService;
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
    private final OpenCVService openCVService;
    private final PPStructureService ppStructureService;
    private final OCRLogService ocrLogService;
    private final QualityScoreService qualityScoreService;
    private final StorageService storageService;
    private final DataPersistenceService dataPersistenceService;

    public PDFProcessor(PdfToImageService pdfToImageService, OpenCVService openCVService,
                        PPStructureService ppStructureService, OCRLogService ocrLogService,
                        QualityScoreService qualityScoreService,
                        StorageService storageService, DataPersistenceService dataPersistenceService) {
        this.pdfToImageService = pdfToImageService;
        this.openCVService = openCVService;
        this.ppStructureService = ppStructureService;
        this.ocrLogService = ocrLogService;
        this.qualityScoreService = qualityScoreService;
        this.storageService = storageService;
        this.dataPersistenceService = dataPersistenceService;
    }

    public List<Map<String, Object>> process(String pdfPath, String archiveType) {
        List<Map<String, Object>> results = new ArrayList<>();
        String fileType = "PDF";

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

            // OpenCV 增强
            String enhancedPath = null;
            try {
                enhancedPath = openCVService.enhanceImage(imagePath);
            } catch (Exception ignored) {}
            String ocrPath = (enhancedPath != null && !enhancedPath.startsWith("ERROR")
                    && !enhancedPath.startsWith("图片增强失败")) ? enhancedPath : imagePath;
            pageResult.put("enhancedPath", ocrPath);

            try {
                String ocrResult = ppStructureService.parseTable(ocrPath);
                pageResult.put("ocrResult", ocrResult);
            } catch (Exception e) {
                pageResult.put("ocrResult", null);
                pageResult.put("ocrError", e.getMessage());
            }

            results.add(pageResult);
        }

        String fileName = Paths.get(pdfPath).getFileName().toString();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        List<String> allErrors = new ArrayList<>();
        List<Map<String, String>> allData = new ArrayList<>();

        for (Map<String, Object> page : results) {
            String ocrResult = (String) page.get("ocrResult");
            if (ocrResult == null || ocrResult.isEmpty()) {
                allErrors.add("表格识别无返回");
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
                            Map<String, String> row = new LinkedHashMap<>();
                            for (Map.Entry<?, ?> e : ((Map<?, ?>) rec).entrySet()) {
                                if (e.getValue() != null && !e.getValue().toString().isEmpty()) {
                                    row.put(e.getKey().toString(), e.getValue().toString());
                                }
                            }
                            if (!row.isEmpty()) {
                                allData.add(row);
                            }
                        }
                    }
                } else if (rawData instanceof Map) {
                    Map<String, String> single = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> e : ((Map<?, ?>) rawData).entrySet()) {
                        if (e.getValue() != null && !e.getValue().toString().isEmpty()) {
                            single.put(e.getKey().toString(), e.getValue().toString());
                        }
                    }
                    if (!single.isEmpty()) {
                        allData.add(single);
                    }
                }

            } catch (Exception ignored) {}
        }

        if (!allData.isEmpty()) {
            try {
                Integer fileId = dataPersistenceService.saveArchiveFileDimData(fileName, fileType);

                for (Map<String, String> record : allData) {
                    dataPersistenceService.saveExtractedData(archiveType, record, fileId);
                }

                storageService.moveArchiveFile(fileName);

                // 质量评分
                qualityScoreService.scoreFile(fileId, archiveType, allData, allErrors.size());

                // 有警告仍归档，仅记录到数据库
                if (!allErrors.isEmpty()) {
                    ocrLogService.addLog(fileId, fileName, fileType, "warning", String.join("; ", allErrors));
                }
            } catch (Exception ignored) {
            }
        } else {
            try {
                storageService.failedFile(fileName, "PDF 所有页面表格识别失败");
            } catch (Exception ignored) {
            }
        }

        // 删除 PDF 转换生成的临时图片及增强图片
        for (String imagePath : imagePaths) {
            try {
                Files.deleteIfExists(Paths.get(imagePath));
            } catch (Exception ignored) {}
        }
        for (Map<String, Object> page : results) {
            String enhancedPath = (String) page.get("enhancedPath");
            if (enhancedPath != null && !enhancedPath.equals(page.get("imagePath"))) {
                try {
                    Files.deleteIfExists(Paths.get(enhancedPath));
                } catch (Exception ignored) {}
            }
        }

        return results;
    }
}
