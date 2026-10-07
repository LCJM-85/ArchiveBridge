package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.mapper.OCRLogDimMapper;
import edu.scau.scauarchiveinsight.pojo.OCRLogDim;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ResourceLock("user.dir")
class OCRLogDeletionTest {
    @TempDir Path temp;

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void sourceFileCommitDeletesAndRollbackRestores(boolean committed) throws Exception {
        String originalDir = System.getProperty("user.dir");
        Path directory = Files.createDirectories(temp.resolve("storage/temp/2026-10-07"));
        Path source = directory.resolve("test.jpg");
        Path notice = directory.resolve("test.jpg.warn.json");
        Path other = directory.resolve("other.jpg");
        Files.writeString(source, "source"); Files.writeString(notice, "notice"); Files.writeString(other, "other");
        try {
            System.setProperty("user.dir", temp.toString());
            TransactionSynchronizationManager.initSynchronization();
            ReflectionTestUtils.invokeMethod(new OCRLogService(), "stageStorageFile", "test.jpg");
            assertFalse(Files.exists(source)); assertFalse(Files.exists(notice));
            assertTrue(Files.exists(other));
            for (var synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCompletion(committed ? TransactionSynchronization.STATUS_COMMITTED : TransactionSynchronization.STATUS_ROLLED_BACK);
            }
            assertEquals(!committed, Files.exists(source));
            assertEquals(!committed, Files.exists(notice));
            if (!committed) assertEquals("source", Files.readString(source));
            try (var paths = Files.list(temp.resolve("storage"))) {
                assertTrue(paths.noneMatch(p -> p.getFileName().toString().startsWith(".delete-")));
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            System.setProperty("user.dir", originalDir);
        }
    }
    @Test void deletesAssociatedDraftBeforeLog() {
        var service = new OCRLogService();
        var jdbc = mock(JdbcTemplate.class);
        var mapper = mock(OCRLogDimMapper.class);
        var cache = mock(CacheService.class);
        ReflectionTestUtils.setField(service,"reviewJdbc",jdbc);
        ReflectionTestUtils.setField(service,"ocrLogDimMapper",mapper);
        ReflectionTestUtils.setField(service,"cacheService",cache);
        when(jdbc.queryForObject(anyString(),eq(Integer.class),eq(7))).thenReturn(1);
        when(jdbc.queryForList(anyString(),eq(7))).thenReturn(List.of(Map.of("draft_id",1L)));
        var log = new OCRLogDim(); log.setRecognizeStatus("success");
        when(mapper.selectById(7)).thenReturn(log);
        service.removeById(7);
        var ordered = inOrder(jdbc,mapper);
        ordered.verify(jdbc).update("DELETE FROM archive_review_draft WHERE log_id=?",7);
        ordered.verify(mapper).deleteById(7);
    }
    @Test void processingTaskCannotBeDeleted() {
        var service = new OCRLogService();
        var jdbc = mock(JdbcTemplate.class);
        var mapper = mock(OCRLogDimMapper.class);
        ReflectionTestUtils.setField(service,"reviewJdbc",jdbc);
        ReflectionTestUtils.setField(service,"ocrLogDimMapper",mapper);
        var log = new OCRLogDim(); log.setRecognizeStatus("processing");
        when(mapper.selectById(7)).thenReturn(log);
        assertThrows(IllegalStateException.class,()->service.removeById(7));
        verify(mapper,never()).deleteById(7);
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
}
