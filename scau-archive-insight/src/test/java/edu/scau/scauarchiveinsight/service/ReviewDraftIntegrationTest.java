package edu.scau.scauarchiveinsight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.scau.scauarchiveinsight.config.PythonProcessManager;
import edu.scau.scauarchiveinsight.processor.CSVProcessor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 真实数据库事务结束后回滚诊断数据；文件均在测试临时目录。 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named="RUN_RUNTIME_INTEGRATION",matches="true")
@Transactional
class ReviewDraftIntegrationTest {
    @MockBean PythonProcessManager python;
    @MockBean OCRLogService logs;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired PlatformTransactionManager transactions;
    @Autowired DataPersistenceService persistence;
    @Autowired QualityScoreService quality;
    @Autowired MetaDataService metadata;
    @Autowired MetaDataMappingService mapping;
    @Autowired CacheService cache;
    @TempDir Path directory;
    ReviewDraftService reviews;
    StorageService storage;
    OCRTaskManager tasks;
    String student;
    long draftId;
    int logId;
    String storedName;

    @BeforeEach void setup() {
        tasks=mock(OCRTaskManager.class);
        storage=new StorageService(directory);
        ReflectionTestUtils.setField(storage,"cacheService",mock(CacheService.class));
        reviews=new ReviewDraftService(jdbc,json,transactions,persistence,quality,metadata,storage,tasks);
        student="RV"+System.nanoTime();
    }
    @SuppressWarnings("unchecked")
    void parseCsv() {
        parseCsv("admission");
    }
    @SuppressWarnings("unchecked")
    void parseCsv(String archiveType) {
        var upload=storage.saveFiles(List.of(new MockMultipartFile("files","测试审查.csv","text/csv",
            ("姓名,学号\n审查测试,"+student+"\n").getBytes(StandardCharsets.UTF_8))),"csv");
        var file=((List<Map<String,String>>)upload.get("uploaded")).get(0);
        storedName=Path.of(file.get("path")).getFileName().toString();
        logId=jdbc.queryForObject("""
            INSERT INTO ocr_log_dim(file_name,file_type,recognize_status,recognize_time)
            VALUES (?,'csv','processing',CURRENT_TIMESTAMP) RETURNING log_id
            """,Integer.class,storedName);
        when(tasks.getCurrentTaskId()).thenReturn(logId);
        when(tasks.getCurrentOriginalName()).thenReturn("测试审查.csv");
        new CSVProcessor(storage,mapping,logs,quality,reviews).process(file.get("path"),archiveType);
        draftId=jdbc.queryForObject("SELECT draft_id FROM archive_review_draft WHERE log_id=?",Long.class,logId);
    }
    @SuppressWarnings("unchecked")
    List<Map<String,String>> rows(Map<String,Object> detail) {
        return (List<Map<String,String>>)detail.get("records");
    }
    int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM "+table+" WHERE student_no=?",Integer.class,student);
    }
    int version(Map<String,Object> detail) { return ((Number)detail.get("version")).intValue(); }

    @Test void parsingOnlyCreatesPersistentDraftAndReleasesProcessingCount() {
        parseCsv();
        assertEquals(0,count("admission_fact"));assertEquals(0,count("student_fact"));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM archive_file_dim WHERE file_name=?",Integer.class,storedName));
        assertEquals(0,storage.getProcessingCount());
        assertEquals("pending_review",jdbc.queryForObject("SELECT recognize_status FROM ocr_log_dim WHERE log_id=?",String.class,logId));
        var detail=reviews.detail(draftId);
        assertEquals("测试审查.csv",detail.get("original_file_name"));
        assertEquals(1,rows(detail).size());
        assertTrue(storage.isReviewTemporary(reviews.source(draftId)));
    }

    @Test void editedRowsCanBeDeletedAndAddedWhileOriginalIsImmutable() {
        parseCsv();
        var original=reviews.detail(draftId).get("originalRecords");
        var saved=reviews.save(draftId,1,List.of(Map.of("name","补充记录","student_no",student)));
        assertEquals(2,version(saved));
        assertEquals(original,saved.get("originalRecords"));
        assertEquals("补充记录",rows(saved).get(0).get("name"));
        assertThrows(IllegalStateException.class,()->reviews.save(draftId,1,List.of(Map.of("name","旧页面"))));
        assertEquals("补充记录",rows(reviews.detail(draftId)).get(0).get("name"));
    }

    @Test void validationFailurePreservesEditsAndNeverImports() {
        parseCsv();
        assertThrows(IllegalArgumentException.class,()->reviews.confirm(draftId,1,
            List.of(Map.of("name","修正后姓名","student_no",student,"gender","错误"))));
        var detail=reviews.detail(draftId);
        assertEquals("pending_review",detail.get("status"));
        assertEquals("修正后姓名",rows(detail).get(0).get("name"));
        assertEquals(2,version(detail));assertNotNull(detail.get("last_error"));
        assertEquals(0,count("admission_fact"));
    }

    @Test void confirmationImportsOnceAndRepeatOnlyFinalizesArchive() {
        parseCsv();
        var result=reviews.confirm(draftId,1,List.of(Map.of("student_no",student,"name","已修正",
            "gender","男","admission_date","2024-09-01","admission_score","600")));
        assertEquals("imported",result.get("status"));
        assertEquals(1,count("admission_fact"));assertEquals(1,count("student_fact"));
        assertEquals("已修正",jdbc.queryForObject("SELECT name FROM admission_fact WHERE student_no=?",String.class,student));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM quality_score_dim WHERE file_id=?",Integer.class,result.get("file_id")));
        assertFalse(storage.isReviewTemporary(reviews.source(draftId)));
        var repeated=reviews.confirm(draftId,1,List.of(Map.of("name","重复请求不应覆盖")));
        assertEquals(result.get("file_id"),repeated.get("file_id"));
        assertEquals(1,count("admission_fact"));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM quality_score_dim WHERE file_id=?",Integer.class,result.get("file_id")));
    }

    @Test void graduationAlsoRequiresReviewAndPreservesCorrectedDate() {
        parseCsv("graduation");
        assertEquals(0,count("graduation_fact"));
        var result=reviews.confirm(draftId,1,List.of(Map.of("student_no",student,"name","毕业审查",
            "gender","女","graduation_date","20240630")));
        assertEquals("imported",result.get("status"));
        assertEquals(1,count("graduation_fact"));
        assertEquals(java.time.LocalDate.of(2024,6,30),
            jdbc.queryForObject("SELECT graduation_date FROM graduation_fact WHERE student_no=?",
                java.sql.Date.class,student).toLocalDate());
        assertEquals(0,count("admission_fact"));
    }

    @Test void discardNeverImportsAndCannotBeConfirmed() {
        parseCsv();
        assertEquals("discarded",reviews.discard(draftId,1).get("status"));
        assertThrows(IllegalStateException.class,()->reviews.confirm(draftId,2,List.of(Map.of("name","不得入库"))));
        assertEquals(0,count("admission_fact"));
        assertTrue(Files.isRegularFile(reviews.source(draftId)));
    }

    @Test void listAndControllerExposePersistentReviewRecords() throws Exception {
        parseCsv();
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
            new edu.scau.scauarchiveinsight.controller.ReviewDraftController(reviews)).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/review")
            .param("logId",String.valueOf(logId)))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.total").value(1))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.records[0].status").value("pending_review"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/review/"+draftId+"/source"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void failedBusinessTransactionRollsBackButKeepsSavedEdits() {
        try {
            parseCsv();
            var failingQuality=mock(QualityScoreService.class);
            doThrow(new IllegalStateException("模拟评分失败")).when(failingQuality)
                .scoreFile(org.mockito.ArgumentMatchers.anyInt(),org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyList(),org.mockito.ArgumentMatchers.anyInt());
            var failing=new ReviewDraftService(jdbc,json,transactions,persistence,failingQuality,metadata,storage,tasks);
            assertThrows(IllegalStateException.class,()->failing.confirm(draftId,1,
                List.of(Map.of("student_no",student,"name","失败后保留的修正"))));
            assertEquals(0,count("admission_fact"));assertEquals(0,count("student_fact"));
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM archive_file_dim WHERE file_name=?",Integer.class,storedName));
            var detail=reviews.detail(draftId);
            assertEquals("pending_review",detail.get("status"));
            assertEquals(2,version(detail));
            assertEquals("失败后保留的修正",rows(detail).get(0).get("name"));
            assertTrue(detail.get("last_error").toString().contains("模拟评分失败"));
        } finally { cleanCommittedFixture(); }
    }

    @Test
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void concurrentConfirmNeverDuplicatesBusinessRecordsOrLosesSource() throws Exception {
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            parseCsv();
            var start=new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Boolean> request=()->{
                start.await();
                try {
                    var detail=reviews.confirm(draftId,1,List.of(Map.of("student_no",student,"name","并发确认")));
                    return "imported".equals(detail.get("status"));
                } catch (ReviewDraftService.VersionConflictException conflict) { return false; }
            };
            var first=executor.submit(request);var second=executor.submit(request);start.countDown();
            boolean one=first.get(15,java.util.concurrent.TimeUnit.SECONDS);
            boolean two=second.get(15,java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(one||two);
            assertEquals(1,count("admission_fact"));assertEquals(1,count("student_fact"));
            var detail=reviews.detail(draftId);
            assertEquals("imported",detail.get("status"));
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM quality_score_dim WHERE file_id=?",Integer.class,detail.get("file_id")));
            assertTrue(Files.isRegularFile(reviews.source(draftId)));
            assertFalse(storage.isReviewTemporary(reviews.source(draftId)));
        } finally { executor.shutdownNow();cleanCommittedFixture(); }
    }

    private void cleanCommittedFixture() {
        if (storedName==null) return;
        // 只清理本测试创建的随机文件、随机学号及其关联记录，不碰已有数据。
        jdbc.update("DELETE FROM quality_score_dim WHERE file_id IN (SELECT file_id FROM archive_file_dim WHERE file_name=?)",storedName);
        jdbc.update("DELETE FROM admission_fact WHERE student_no=?",student);
        jdbc.update("DELETE FROM student_fact WHERE student_no=?",student);
        jdbc.update("DELETE FROM archive_review_draft WHERE log_id=?",logId);
        jdbc.update("DELETE FROM ocr_log_dim WHERE log_id=?",logId);
        jdbc.update("DELETE FROM archive_file_dim WHERE file_name=?",storedName);
        cache.evictAllDimensions();cache.evictDashboard();
    }
}
