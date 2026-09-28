package edu.scau.scauarchiveinsight.processor;

import edu.scau.scauarchiveinsight.mapper.ProvinceDimMapper;
import edu.scau.scauarchiveinsight.pojo.MetaDataStandard;
import edu.scau.scauarchiveinsight.service.DataPersistenceService;
import edu.scau.scauarchiveinsight.service.FieldCorrectionService;
import edu.scau.scauarchiveinsight.service.MetaDataMappingService;
import edu.scau.scauarchiveinsight.service.MetaDataService;
import edu.scau.scauarchiveinsight.service.OCRLogService;
import edu.scau.scauarchiveinsight.service.OpenCVService;
import edu.scau.scauarchiveinsight.service.PPStructureService;
import edu.scau.scauarchiveinsight.service.QualityScoreService;
import edu.scau.scauarchiveinsight.service.StorageService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageProcessorFallbackTest {

    @Test
    void retriesOriginalImageWhenEnhancedHeadersCannotMatchMetadata() throws Exception {
        OpenCVService openCV = mock(OpenCVService.class);
        PPStructureService ocr = mock(PPStructureService.class);
        MetaDataMappingService mapping = mock(MetaDataMappingService.class);
        MetaDataService metadata = mock(MetaDataService.class);
        FieldCorrectionService correction = mock(FieldCorrectionService.class);
        ProvinceDimMapper provinces = mock(ProvinceDimMapper.class);
        OCRLogService logs = mock(OCRLogService.class);
        QualityScoreService quality = mock(QualityScoreService.class);
        StorageService storage = mock(StorageService.class);
        DataPersistenceService persistence = mock(DataPersistenceService.class);
        ImageProcessor processor = new ImageProcessor(
                openCV, ocr, mapping, metadata, correction, provinces,
                logs, quality, storage, persistence);

        String original = "original.png";
        String enhanced = "enhanced.jpg";
        when(openCV.enhanceImage(original)).thenReturn(enhanced);
        when(ocr.parseTable(enhanced)).thenReturn("""
                {"grids":[{"headers":["批次"],"rows":[["2本科一批"]]}],"errors":[]}
                """);
        when(ocr.parseTable(original)).thenReturn("""
                {"grids":[{"headers":["姓名"],"rows":[["张三"]]}],"errors":[]}
                """);

        MetaDataStandard nameRule = new MetaDataStandard();
        nameRule.setFieldCode("name");
        nameRule.setFieldName("姓名");
        nameRule.setSourceField("姓名");
        when(metadata.list()).thenReturn(List.of(nameRule));
        when(provinces.selectList(null)).thenReturn(List.of());
        when(mapping.findUnmatchedHeaders(anyList(), anyList())).thenAnswer(invocation -> {
            List<String> headers = invocation.getArgument(0);
            return headers.contains("姓名") ? List.of() : List.copyOf(headers);
        });
        when(mapping.mapGrid(anyList(), anyList(), anyList())).thenAnswer(invocation -> {
            List<String> headers = invocation.getArgument(0);
            return headers.contains("姓名")
                    ? List.of(Map.of("name", "张三"))
                    : List.of();
        });
        when(persistence.saveArchiveFileDimData("original.png", "picture")).thenReturn(1);

        List<Map<String, Object>> result = processor.process(List.of(original), "admission");

        verify(ocr).parseTable(enhanced);
        verify(ocr).parseTable(original);
        verify(storage, never()).failedFile(eq("original.png"), eq("未匹配到任何元数据字段"));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> data = (List<Map<String, String>>) result.get(0).get("data");
        assertFalse(data.isEmpty());
    }
}
