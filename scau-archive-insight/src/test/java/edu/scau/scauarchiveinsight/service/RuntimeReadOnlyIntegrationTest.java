package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.config.PythonProcessManager;
import edu.scau.scauarchiveinsight.mapper.AdmissionFactMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 真实数据库只读冒烟检查，停用进程启动和任务重启处理，避免修改现有任务。 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_RUNTIME_INTEGRATION", matches = "true")
@Transactional(readOnly = true)
class RuntimeReadOnlyIntegrationTest {
    @MockBean PythonProcessManager python;
    @MockBean OCRLogService logs;
    @Autowired JdbcTemplate jdbc;
    @Autowired AdmissionService admission;
    @Autowired GraduationService graduation;
    @Autowired StudentService students;
    @Autowired TrendAnalysisService trends;
    @Autowired GeographicService geography;
    @Autowired TrainingPathService training;
    @Autowired ReportService reports;
    @Autowired AdmissionFactMapper mapper;
    @Autowired DataPersistenceService persistence;
    @Autowired edu.scau.scauarchiveinsight.mapper.OCRLogDimMapper logMapper;
    @Autowired edu.scau.scauarchiveinsight.mapper.ArchiveFileDimMapper archiveMapper;

    @Test
    @Transactional
    void completionPreservesWarningAndCancelledStatusesInRealDatabase() {
        OCRLogService service = new OCRLogService();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "ocrLogDimMapper", logMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "archiveFileDimMapper", archiveMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "cacheService", org.mockito.Mockito.mock(CacheService.class));
        String prefix = "terminal-test-" + UUID.randomUUID();
        Integer success = service.createProcessingLog(prefix + "-success.png", "picture");
        service.finishProcessing(success, prefix + "-success.png");
        assertEquals("success", service.getById(success).getRecognizeStatus());
        Integer warning = service.createProcessingLog(prefix + "-warning.png", "picture");
        service.addWarningMessages(null, prefix + "-warning.png", "picture", java.util.List.of("字段提示"));
        service.finishProcessing(warning, prefix + "-warning.png");
        assertEquals("warning", service.getById(warning).getRecognizeStatus());
        assertEquals("字段提示", service.getById(warning).getIssues().get(0).message());
        Integer cancelled = service.createProcessingLog(prefix + "-cancelled.png", "picture");
        assertTrue(service.markCancelled(cancelled));
        service.finishProcessing(cancelled, prefix + "-cancelled.png");
        service.markFailed(cancelled, "处理线程退出");
        assertEquals("cancelled", service.getById(cancelled).getRecognizeStatus());
        // Spring 测试事务结束后回滚所有诊断日志。
    }

    @Test
    void sourceFieldConstraintAndExistingMetadataMatchRequiredDesign() {
        assertEquals("NO", jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns " +
                "WHERE table_schema = 'public' AND table_name = 'metadata_standard' AND column_name = 'source_field'", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM metadata_standard " +
                "WHERE source_field IS NULL OR btrim(source_field) = ''", Integer.class));
    }

    @Test
    void databaseAndDataPagesExecuteAgainstRealSchema() {
        assertEquals(1, jdbc.queryForObject("SELECT 1", Integer.class));
        assertNotNull(admission.page(1, 2, null, null, null, null, null, null));
        assertNotNull(graduation.page(1, 2, null, null, null, null, null, null));
        assertNotNull(students.page(1, 2, null, null, null, null, null, null));
    }

    @Test
    void trendAndGeographicQueriesExecuteAgainstRealSchema() {
        assertNotNull(trends.yearlyTrend(null, null, null));
        assertNotNull(trends.majorTrend(null, null, null));
        assertNotNull(trends.provinceTrend(null, null, null));
        assertNotNull(trends.scoreTrend(null, null, null));
        assertNotNull(trends.genderTrend(null, null, null));
        assertNotNull(geography.provinceStats());
        assertNotNull(geography.provinceMapGeoJson());
    }

    @Test
    void reportAndTrainingQueriesExecuteAgainstRealSchema() {
        assertNotNull(reports.getReportData(LocalDate.now().getYear()));
        assertNotNull(training.sankeyData());
        assertNotNull(mapper.dashboardTotalAdmissions());
        assertNotNull(mapper.degreeDistribution());
        assertEquals(jdbc.queryForList("SELECT m.major_name AS name, COUNT(*)::int AS count " +
                "FROM admission_fact f JOIN major_dim m ON f.major_id = m.major_id GROUP BY m.major_name ORDER BY count DESC"),
                mapper.dashboardMajorDistribution());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failedFileImportRollsBackArchiveAndBusinessRows() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String fileName = "runtime-audit-" + suffix + ".csv";
        String studentNo = "AUD" + suffix;
        // 事务内第一行插入后，第二行空记录故意触发异常；所有测试数据必须回滚。
        assertThrows(NullPointerException.class, () ->
                persistence.saveFileData(fileName, "CSV", "admission",
                        Arrays.asList(Map.of("student_no", studentNo, "name", "运行测试"), null)));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM archive_file_dim WHERE file_name = ?", Integer.class, fileName));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM student_fact WHERE student_no = ?", Integer.class, studentNo));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM admission_fact WHERE student_no = ?", Integer.class, studentNo));
    }
}
