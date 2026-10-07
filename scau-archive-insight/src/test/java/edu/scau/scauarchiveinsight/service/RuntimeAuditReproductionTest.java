package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.pojo.MetaDataStandard;
import edu.scau.scauarchiveinsight.processor.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 运行问题的回归测试。 */
class RuntimeAuditReproductionTest {
    @TempDir Path temp;

    @Test
    void nullSourceFieldReportsInvalidConfiguration() {
        MetaDataStandard rule = new MetaDataStandard();
        rule.setFieldCode("name");
        rule.setFieldName("姓名");
        MetaDataService metadata = mock(MetaDataService.class);
        when(metadata.list()).thenReturn(List.of(rule));
        MetaDataMappingService mapping = new MetaDataMappingService();
        ReflectionTestUtils.setField(mapping, "metaDataService", metadata);
        assertTrue(assertThrows(IllegalStateException.class,
                () -> mapping.process(List.of(Map.of("姓名", "测试姓名"))))
                .getMessage().contains("来源字段"));
    }

    @Test
    void tifIsRejectedBeforeSavingOrScheduling() {
        StorageService storage = new StorageService(temp);
        ReflectionTestUtils.setField(storage, "cacheService", mock(CacheService.class));
        Map<String, Object> result = storage.saveFiles(List.of(
                new org.springframework.mock.web.MockMultipartFile("files", "scan.tif", "image/tiff", new byte[]{1})), "image");
        assertTrue(((List<?>) result.get("uploaded")).isEmpty());
        assertEquals(1, ((List<?>) result.get("errors")).size());
        assertEquals(0, storage.getProcessingCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"csv", "xlsx"})
    @SuppressWarnings({"rawtypes", "unchecked"})
    void suppliedUploadMetadataReplacesMappedEmptyStringsBeforeValidation(String extension) throws Exception {
        Path input = temp.resolve("defaults." + extension);
        if ("csv".equals(extension)) Files.writeString(input, "姓名\n测试姓名\n");
        else {
            try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                 var output = Files.newOutputStream(input)) {
                var sheet = workbook.createSheet();
                sheet.createRow(0).createCell(0).setCellValue("姓名");
                sheet.createRow(1).createCell(0).setCellValue("测试姓名");
                workbook.write(output);
            }
        }
        MetaDataMappingService mapping = new MetaDataMappingService();
        MetaDataService metadata = mock(MetaDataService.class);
        List<MetaDataStandard> rules = new ArrayList<>();
        for (String code : List.of("name", "province_name", "admission_date", "degree_name")) {
            MetaDataStandard rule = new MetaDataStandard();
            rule.setFieldCode(code);
            rule.setFieldName(code.equals("name") ? "姓名" : code);
            rule.setSourceField(rule.getFieldName());
            rule.setIsRequired(true);
            rules.add(rule);
        }
        when(metadata.list()).thenReturn(rules);
        ReflectionTestUtils.setField(mapping, "metaDataService", metadata);
        ReviewDraftService persistence = mock(ReviewDraftService.class);
        CSVProcessor processor = new CSVProcessor(mock(StorageService.class), mapping,
                mock(OCRLogService.class), mock(QualityScoreService.class), persistence);
        Map<String, Object> result = "csv".equals(extension)
                ? processor.process(input.toString(), "admission", "广东省", "2024-09-01", "学士")
                : new ExcelProcessor(mock(StorageService.class), mapping, mock(OCRLogService.class),
                        mock(QualityScoreService.class), persistence)
                        .process(input.toString(), "admission", "广东省", "2024-09-01", "学士");
        ArgumentCaptor<List> records = ArgumentCaptor.forClass(List.class);
        verify(persistence).stage(eq("defaults." + extension), anyString(), eq("admission"), records.capture(), anyList());
        Map<String, String> saved = (Map<String, String>) records.getValue().get(0);
        assertEquals("广东省", saved.get("province_name"));
        assertEquals("2024-09-01", saved.get("admission_date"));
        assertEquals("学士", saved.get("degree_name"));
        assertTrue(((List<?>) result.get("errors")).isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void supplementOnlyReplacesMissingNullAndBlankValuesNotValidOriginals() {
        MetaDataStandard rule = new MetaDataStandard();
        rule.setSourceField("省份"); rule.setFieldName("省份"); rule.setFieldCode("province_name");
        MetaDataService metadata = mock(MetaDataService.class);
        when(metadata.list()).thenReturn(List.of(rule));
        MetaDataMappingService mapping = new MetaDataMappingService();
        ReflectionTestUtils.setField(mapping, "metaDataService", metadata);
        Map<String, String> nullRow = new HashMap<>(); nullRow.put("省份", null);
        var result = mapping.process(List.of(Map.of(), nullRow, Map.of("省份", "   "), Map.of("省份", "浙江省")),
                "广东省", null, null);
        List<Map<String, String>> data = (List<Map<String, String>>) result.get("data");
        assertEquals(List.of("广东省", "广东省", "广东省", "浙江省"),
                data.stream().map(row -> row.get("province_name")).toList());
    }
}
