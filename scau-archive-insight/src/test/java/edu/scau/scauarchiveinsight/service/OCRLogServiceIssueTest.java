package edu.scau.scauarchiveinsight.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.scau.scauarchiveinsight.mapper.ArchiveFileDimMapper;
import edu.scau.scauarchiveinsight.mapper.OCRLogDimMapper;
import edu.scau.scauarchiveinsight.pojo.OCRLogDim;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OCRLogServiceIssueTest {

    @Test
    void warningMustBeStoredAsStructuredIssueList() throws Exception {
        OCRLogDimMapper mapper = mock(OCRLogDimMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);

        OCRLogService service = new OCRLogService();
        ReflectionTestUtils.setField(service, "ocrLogDimMapper", mapper);
        ReflectionTestUtils.setField(service, "archiveFileDimMapper", mock(ArchiveFileDimMapper.class));
        ReflectionTestUtils.setField(service, "cacheService", mock(CacheService.class));

        service.addLog(3, "test.png", "picture", "warning", "姓名字段为空");

        ArgumentCaptor<OCRLogDim> captor = ArgumentCaptor.forClass(OCRLogDim.class);
        verify(mapper).insert(captor.capture());
        JsonNode stored = new ObjectMapper().readTree(captor.getValue().getErrorMessage());
        assertTrue(stored.isArray());
        assertEquals("warning", stored.get(0).get("level").asText());
        assertEquals("姓名字段为空", stored.get(0).get("message").asText());
    }

    @Test
    void mappingIssueMustAcceptPythonMsgField() throws Exception {
        OCRLogDimMapper mapper = mock(OCRLogDimMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        OCRLogService service = new OCRLogService();
        ReflectionTestUtils.setField(service, "ocrLogDimMapper", mapper);
        ReflectionTestUtils.setField(service, "archiveFileDimMapper", mock(ArchiveFileDimMapper.class));
        ReflectionTestUtils.setField(service, "cacheService", mock(CacheService.class));

        service.addMappingIssues(4, "llm.png", "picture-llm",
                List.of(Map.of("msg", "未配置 API Key")));

        ArgumentCaptor<OCRLogDim> captor = ArgumentCaptor.forClass(OCRLogDim.class);
        verify(mapper).insert(captor.capture());
        JsonNode stored = new ObjectMapper().readTree(captor.getValue().getErrorMessage());
        assertEquals("未配置 API Key", stored.get(0).get("message").asText());
    }
}
