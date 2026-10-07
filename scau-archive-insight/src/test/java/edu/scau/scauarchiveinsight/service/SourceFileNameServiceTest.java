package edu.scau.scauarchiveinsight.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SourceFileNameServiceTest {
    @Test void usesExactOriginalNameBeforeLegacyFallback() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(), eq(7))).thenReturn(List.of(
                Map.of("file_id", 7, "original_file_name", "毕业 测试（原始）.JPG")));
        var service = new SourceFileNameService(jdbc);
        var names = service.originalNames(Arrays.asList(7, 7, null));
        assertEquals("毕业 测试（原始）.JPG", service.displayName(7, "6ea8b2d256f542d3b152513bda2bb253_毕业_测试.jpg", names));
        verify(jdbc, times(1)).queryForList(anyString(), eq(7));
    }
    @Test void legacyFallbackRemovesOnlyKnownStoragePrefix() {
        var service = new SourceFileNameService(mock(JdbcTemplate.class));
        assertEquals("毕业测试图片.jpg", service.displayName(8, "6ea8b2d256f542d3b152513bda2bb253_毕业测试图片.jpg", Map.of()));
        assertEquals("2026_毕业记录.csv", service.displayName(8, "2026_毕业记录.csv", Map.of()));
        assertNull(service.displayName(null, null, Map.of()));
        assertTrue(service.originalNames(Arrays.asList(null, null)).isEmpty());
    }
}
