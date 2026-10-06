package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.controller.MetaDataController;
import edu.scau.scauarchiveinsight.controller.ArchiveUploadController;
import edu.scau.scauarchiveinsight.controller.OCRLogController;
import edu.scau.scauarchiveinsight.dto.MetaDataDTO;
import edu.scau.scauarchiveinsight.mapper.*;
import edu.scau.scauarchiveinsight.processor.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuntimeRepairTest {
    @TempDir Path temp;

    @Test
    void interruptedImportCannotInsertAnyArchiveRow() {
        DataPersistenceService persistence = new DataPersistenceService();
        OCRTaskManager tasks = new OCRTaskManager();
        ReflectionTestUtils.setField(persistence, "ocrTaskManager", tasks);
        for (Class<?> type : List.of(ProvinceDimMapper.class, MajorDimMapper.class, AdmissionFactMapper.class,
                StudentFactMapper.class, ClassDimMapper.class, CollegeDimMapper.class, GraduationFactMapper.class,
                DegreeDimMapper.class, DestinationDimMapper.class, CacheService.class)) {
            String name = type.getSimpleName();
            ReflectionTestUtils.setField(persistence, Character.toLowerCase(name.charAt(0)) + name.substring(1), mock(type));
        }
        ArchiveFileDimMapper archives = mock(ArchiveFileDimMapper.class);
        doAnswer(call -> {
            ((edu.scau.scauarchiveinsight.pojo.ArchiveFileDim) call.getArgument(0)).setFileId(1);
            return 1;
        }).when(archives).insert(any(edu.scau.scauarchiveinsight.pojo.ArchiveFileDim.class));
        ReflectionTestUtils.setField(persistence, "archiveFileDimMapper", archives);
        Thread.currentThread().interrupt();
        try {
            assertThrows(CancellationException.class, () -> persistence.saveFileData(
                    "cancel.csv", "CSV", "admission", List.of(Map.of("name", "测试"))));
            verifyNoInteractions(archives);
        } finally {
            Thread.interrupted();
            tasks.shutdown();
        }
    }

    @Test
    void missingLiveTaskCannotBeReportedAsCancelled() {
        OCRTaskManager tasks = new OCRTaskManager();
        try {
            OCRLogController controller = new OCRLogController();
            OCRLogService logs = mock(OCRLogService.class);
            when(logs.markCancelled(77)).thenReturn(true);
            ReflectionTestUtils.setField(controller, "ocrTaskManager", tasks);
            ReflectionTestUtils.setField(controller, "ocrLogService", logs);
            assertEquals(400, controller.cancel(77).getCode());
            verify(logs, never()).markCancelled(77);
        } finally {
            tasks.shutdown();
        }
    }

    @Test
    void previousDayArchiveCompletesWithoutTodayDirectoryScan() throws Exception {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "test"),
                edu.scau.scauarchiveinsight.pojo.OCRLogDim.class);
        StorageService storage = new StorageService(temp);
        ReflectionTestUtils.setField(storage, "cacheService", mock(CacheService.class));
        Path upload = temp.resolve("temp/20000101/csv/cross.csv");
        Files.createDirectories(upload.getParent());
        Files.writeString(upload, "测试");
        CSVProcessor csv = mock(CSVProcessor.class);
        when(csv.process(anyString(), anyString(), isNull(), isNull(), isNull())).thenAnswer(call -> {
            storage.moveArchiveFile("cross.csv");
            return Map.of();
        });
        OCRLogService logs = new OCRLogService();
        OCRLogDimMapper logMapper = mock(OCRLogDimMapper.class);
        ReflectionTestUtils.setField(logs, "ocrLogDimMapper", logMapper);
        ReflectionTestUtils.setField(logs, "archiveFileDimMapper", mock(ArchiveFileDimMapper.class));
        ReflectionTestUtils.setField(logs, "cacheService", mock(CacheService.class));
        ArchiveUploadController controller = new ArchiveUploadController(storage, csv,
                mock(ExcelProcessor.class), mock(PDFProcessor.class), mock(ImageProcessor.class),
                mock(LLMProcessor.class), logs, mock(OCRTaskManager.class));
        ReflectionTestUtils.invokeMethod(controller, "processFile", 88, "cross.csv", upload.toString(),
                "csv", "admission", null, null, null, false);
        // 一次进度更新、一次确认结果更新之外，还必须写入终态。
        verify(logMapper, times(3)).update(isNull(), any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void failedFilePreservesOriginalFailureReason() throws Exception {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "test"),
                edu.scau.scauarchiveinsight.pojo.OCRLogDim.class);
        StorageService storage = new StorageService(temp);
        ReflectionTestUtils.setField(storage, "cacheService", mock(CacheService.class));
        Path upload = temp.resolve("temp/20000101/csv/failed.csv");
        Files.createDirectories(upload.getParent());
        Files.writeString(upload, "测试");
        CSVProcessor csv = mock(CSVProcessor.class);
        when(csv.process(anyString(), anyString(), isNull(), isNull(), isNull())).thenAnswer(call -> {
            storage.failedFile("failed.csv", "原始失败原因");
            return Map.of();
        });
        OCRLogService logs = new OCRLogService();
        OCRLogDimMapper mapper = mock(OCRLogDimMapper.class);
        ReflectionTestUtils.setField(logs, "ocrLogDimMapper", mapper);
        ReflectionTestUtils.setField(logs, "cacheService", mock(CacheService.class));
        ArchiveUploadController controller = new ArchiveUploadController(storage, csv,
                mock(ExcelProcessor.class), mock(PDFProcessor.class), mock(ImageProcessor.class),
                mock(LLMProcessor.class), logs, mock(OCRTaskManager.class));
        ReflectionTestUtils.invokeMethod(controller, "processFile", 89, "failed.csv", upload.toString(),
                "csv", "admission", null, null, null, false);
        org.mockito.ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper> updates =
                org.mockito.ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(mapper, times(3)).update(isNull(), updates.capture());
        assertTrue(updates.getAllValues().stream().anyMatch(update ->
                update.getParamNameValuePairs().values().stream().anyMatch(value -> String.valueOf(value).contains("原始失败原因"))));
    }

    @Test
    void directoryScanMustNotFinalizeAnExistingActiveTask() throws Exception {
        Path archive = temp.resolve("archive/20000101/csv/active.csv");
        Files.createDirectories(archive.getParent());
        Files.writeString(archive, "测试");
        OCRLogDimMapper mapper = mock(OCRLogDimMapper.class);
        edu.scau.scauarchiveinsight.pojo.OCRLogDim processing = new edu.scau.scauarchiveinsight.pojo.OCRLogDim();
        processing.setRecognizeStatus("processing");
        when(mapper.selectOne(any())).thenReturn(processing);
        OCRLogService logs = new OCRLogService();
        ReflectionTestUtils.setField(logs, "ocrLogDimMapper", mapper);
        ReflectionTestUtils.setField(logs, "archiveFileDimMapper", mock(ArchiveFileDimMapper.class));
        ReflectionTestUtils.invokeMethod(logs, "scanDir", temp.resolve("archive"), "20000101", "success");
        verify(mapper, never()).updateById(any(edu.scau.scauarchiveinsight.pojo.OCRLogDim.class));
        verify(mapper, never()).insert(any(edu.scau.scauarchiveinsight.pojo.OCRLogDim.class));
    }
    @Test
    void metadataAddAndUpdateRejectBlankSourceBeforeDatabaseWrite() {
        MetaDataController controller = new MetaDataController();
        MetaDataService metadata = mock(MetaDataService.class);
        ReflectionTestUtils.setField(controller, "metaDataService", metadata);
        for (String source : Arrays.asList(null, "", "   ")) {
            MetaDataDTO dto = new MetaDataDTO();
            dto.setSourceField(source);
            assertEquals(400, controller.add(dto).getCode());
            assertEquals(400, controller.update(dto).getCode());
        }
        verifyNoInteractions(metadata);
    }

    @Test
    void dashboardIncludesYearsOutsideOldFixedRange() {
        DashboardService service = new DashboardService();
        AdmissionFactMapper admission = mock(AdmissionFactMapper.class);
        ReflectionTestUtils.setField(service, "admissionFactMapper", admission);
        ReflectionTestUtils.setField(service, "ocrLogDimMapper", mock(OCRLogDimMapper.class));
        ReflectionTestUtils.setField(service, "qualityScoreDimMapper", mock(QualityScoreDimMapper.class));
        ReflectionTestUtils.setField(service, "cacheService", mock(CacheService.class));
        when(admission.yearlyAdmissionCounts()).thenReturn(List.of(
                Map.of("year", 2019, "count", 2), Map.of("year", 2026, "count", 3)));
        when(admission.dashboardMajorDistribution()).thenReturn(List.of(Map.of("name", "软件工程", "count", 5)));
        assertEquals(List.of(Map.of("name", "软件工程", "count", 5)), service.getStats().get("majorDistribution"));
        verify(admission, never()).reportMajorDist(anyInt());
    }

    @Test
    void dashboardDoesNotReuseCachedOldYearRange() {
        DashboardService service = new DashboardService();
        AdmissionFactMapper admission = mock(AdmissionFactMapper.class);
        ReflectionTestUtils.setField(service, "admissionFactMapper", admission);
        ReflectionTestUtils.setField(service, "ocrLogDimMapper", mock(OCRLogDimMapper.class));
        ReflectionTestUtils.setField(service, "qualityScoreDimMapper", mock(QualityScoreDimMapper.class));
        CacheService cache = mock(CacheService.class);
        ReflectionTestUtils.setField(service, "cacheService", cache);
        when(cache.get(anyString(), any())).thenReturn(Map.of("majorDistribution", List.of()));
        assertEquals("all", service.getStats().get("majorDistributionScope"));
    }
}
