package edu.scau.scauarchiveinsight.processor;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.scau.scauarchiveinsight.pojo.MetaDataStandard;
import edu.scau.scauarchiveinsight.service.DataPersistenceService;
import edu.scau.scauarchiveinsight.service.FieldCorrectionService;
import edu.scau.scauarchiveinsight.service.MetaDataMappingService;
import edu.scau.scauarchiveinsight.service.MetaDataService;
import edu.scau.scauarchiveinsight.service.OCRLogService;
import edu.scau.scauarchiveinsight.service.PPStructureService;
import edu.scau.scauarchiveinsight.mapper.ProvinceDimMapper;
import edu.scau.scauarchiveinsight.service.QualityScoreService;
import edu.scau.scauarchiveinsight.service.StorageService;
import edu.scau.scauarchiveinsight.pojo.ProvinceDim;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PDFProcessor {

    private static final Logger log = LoggerFactory.getLogger(PDFProcessor.class);

    private final PPStructureService ppStructureService;
    private final MetaDataMappingService metaDataMappingService;
    private final MetaDataService metaDataService;
    private final FieldCorrectionService fieldCorrectionService;
    private final ProvinceDimMapper provinceDimMapper;
    private final OCRLogService ocrLogService;
    private final QualityScoreService qualityScoreService;
    private final StorageService storageService;
    private final DataPersistenceService dataPersistenceService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

    public PDFProcessor(PPStructureService ppStructureService,
                        MetaDataMappingService metaDataMappingService,
                        MetaDataService metaDataService,
                        FieldCorrectionService fieldCorrectionService,
                        ProvinceDimMapper provinceDimMapper,
                        OCRLogService ocrLogService, QualityScoreService qualityScoreService,
                        StorageService storageService, DataPersistenceService dataPersistenceService) {
        this.ppStructureService = ppStructureService;
        this.metaDataMappingService = metaDataMappingService;
        this.metaDataService = metaDataService;
        this.fieldCorrectionService = fieldCorrectionService;
        this.provinceDimMapper = provinceDimMapper;
        this.ocrLogService = ocrLogService;
        this.qualityScoreService = qualityScoreService;
        this.storageService = storageService;
        this.dataPersistenceService = dataPersistenceService;
    }

    public List<Map<String, Object>> process(String pdfPath, String archiveType) {
        return process(pdfPath, archiveType, null, null);
    }

    public List<Map<String, Object>> process(String pdfPath, String archiveType, String provinceName) {
        return process(pdfPath, archiveType, provinceName, null);
    }

    public List<Map<String, Object>> process(String pdfPath, String archiveType, String provinceName, String admissionDate) {
        return process(pdfPath, archiveType, provinceName, admissionDate, null);
    }

    public List<Map<String, Object>> process(String pdfPath, String archiveType, String provinceName, String admissionDate, String degreeName) {
        List<Map<String, Object>> results = new ArrayList<>();
        String fileName = Paths.get(pdfPath).getFileName().toString();
        String fileType = "PDF";
        List<String> allErrors = new ArrayList<>();
        List<Map<String, String>> allData = new ArrayList<>();

        String ocrResult;
        try {
            ocrResult = ppStructureService.parsePdf(pdfPath);
        } catch (Exception e) {
            handleFailed(fileName, "PDF 识别异常: " + e.getMessage());
            return results;
        }

        if (ocrResult == null || ocrResult.isEmpty()) {
            handleFailed(fileName, "PDF 识别无返回");
            return results;
        }

        List<MetaDataStandard> rules;
        List<String> allProvinces;
        try {
            rules = metaDataService.list();
            allProvinces = provinceDimMapper.selectList(null).stream()
                    .map(ProvinceDim::getProvinceName).collect(Collectors.toList());
        } catch (Exception e) {
            handleFailed(fileName, "元数据/省份查询失败: " + e.getMessage());
            return results;
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

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> grids = (List<Map<String, Object>>) parsed.get("grids");
            if (grids != null) {
                for (Map<String, Object> grid : grids) {
                    @SuppressWarnings("unchecked")
                    List<String> headers = (List<String>) grid.get("headers");
                    @SuppressWarnings("unchecked")
                    List<List<String>> rows = (List<List<String>>) grid.get("rows");
                    if (headers == null || rows == null) continue;

                    // 未匹配的列写入 OCR 警告日志（每个 grid 只报一次）
                    {
                        List<String> unmatched = metaDataMappingService.findUnmatchedHeaders(headers, rules);
                        for (String u : unmatched) {
                            allErrors.add("未匹配的列: " + u);
                        }
                    }

                    for (List<String> row : rows) {
                        // 1. 构建原始 {header: value} 映射
                        Map<String, String> rawRow = new LinkedHashMap<>();
                        for (int i = 0; i < headers.size() && i < row.size(); i++) {
                            String val = row.get(i).trim();
                            if (!val.isEmpty()) {
                                rawRow.put(headers.get(i), val);
                            }
                        }
                        if (rawRow.isEmpty()) continue;

                        // 2. FieldCorrectionService 先评分纠正
                        fieldCorrectionService.autoCorrectFields(rawRow, allProvinces, archiveType);

                        // 3. MetaDataMappingService 元数据匹配（匹配未被纠正覆盖的字段）
                        List<List<String>> singleRow = new ArrayList<>();
                        singleRow.add(new ArrayList<>(row));
                        List<Map<String, String>> mapped = metaDataMappingService.mapGrid(headers, singleRow, rules);
                        if (!mapped.isEmpty()) {
                            // 合并纠正结果和匹配结果（纠正结果优先）
                            Map<String, String> merged = new LinkedHashMap<>(mapped.get(0));
                            merged.putAll(rawRow);
                            if (!merged.isEmpty()) {
                                allData.add(merged);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("OCR 结果解析异常: {}", e.getMessage());
        }

        // 去重：同一 PDF 中多个 grid 可能报相同的未匹配列
        allErrors = new ArrayList<>(new LinkedHashSet<>(allErrors));

        boolean processed = false;
        if (!allData.isEmpty()) {
            try {
                Integer fileId = dataPersistenceService.saveArchiveFileDimData(fileName, fileType);
                if (provinceName != null && !provinceName.isBlank()) {
                    for (Map<String, String> record : allData) {
                        record.putIfAbsent("province_name", provinceName);
                    }
                }
                if (admissionDate != null && !admissionDate.isBlank()) {
                    for (Map<String, String> record : allData) {
                        record.putIfAbsent("admission_date", admissionDate);
                    }
                }
                if (degreeName != null && !degreeName.isBlank()) {
                    for (Map<String, String> record : allData) {
                        record.putIfAbsent("degree_name", degreeName);
                    }
                }

                for (Map<String, String> record : allData) {
                    dataPersistenceService.saveExtractedData(archiveType, record, fileId);
                }

                int scoreErrors = (int) allErrors.stream()
                        .filter(e -> !e.startsWith("未匹配的列"))
                        .count();
                qualityScoreService.scoreFile(fileId, archiveType, allData, scoreErrors);

                if (!allErrors.isEmpty()) {
                    ocrLogService.addLog(fileId, fileName, fileType, "warning", String.join("; ", allErrors));
                }

                storageService.moveArchiveFile(fileName);
                processed = true;
                log.info("PDF 处理完成并归档: {}", fileName);
            } catch (Exception e) {
                log.error("PDF 数据处理失败: {}", fileName, e);
            }
        } else {
            log.warn("PDF 无有效数据: {}", fileName);
        }

        if (!processed) {
            String reason = allData.isEmpty() ? "PDF 所有页面表格识别失败" : "数据处理异常";
            handleFailed(fileName, reason);
        }

        return results;
    }

    private void handleFailed(String fileName, String reason) {
        try {
            storageService.failedFile(fileName, reason);
        } catch (Exception e) {
            log.error("PDF 文件移动到 failed 失败: {}", fileName, e);
        }
    }
}
