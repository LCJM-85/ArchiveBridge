package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.mapper.AdmissionFactMapper;
import edu.scau.scauarchiveinsight.mapper.ArchiveFileDimMapper;
import edu.scau.scauarchiveinsight.mapper.ClassDimMapper;
import edu.scau.scauarchiveinsight.mapper.CollegeDimMapper;
import edu.scau.scauarchiveinsight.mapper.DegreeDimMapper;
import edu.scau.scauarchiveinsight.mapper.MajorDimMapper;
import edu.scau.scauarchiveinsight.mapper.OCRLogDimMapper;
import edu.scau.scauarchiveinsight.mapper.QualityScoreDimMapper;
import edu.scau.scauarchiveinsight.mapper.StudentFactMapper;
import edu.scau.scauarchiveinsight.mapper.ProvinceDimMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisCacheContractTest {

    @Test
    void dashboardSecondReadDoesNotRepeatAggregationQueries() {
        DashboardService service = new DashboardService();
        AdmissionFactMapper admission = mock(AdmissionFactMapper.class);
        ArchiveFileDimMapper archive = mock(ArchiveFileDimMapper.class);
        OCRLogDimMapper logs = mock(OCRLogDimMapper.class);
        QualityScoreDimMapper quality = mock(QualityScoreDimMapper.class);
        ReflectionTestUtils.setField(service, "admissionFactMapper", admission);
        ReflectionTestUtils.setField(service, "archiveFileDimMapper", archive);
        ReflectionTestUtils.setField(service, "ocrLogDimMapper", logs);
        ReflectionTestUtils.setField(service, "qualityScoreDimMapper", quality);
        ReflectionTestUtils.setField(service, "cacheService", memoryCache());
        when(admission.degreeDistribution()).thenReturn(List.of());
        when(admission.yearlyAdmissionCounts()).thenReturn(List.of());
        when(admission.reportMajorDist(any(Integer.class))).thenReturn(List.of());
        when(logs.selectCount(any())).thenReturn(0L);

        service.getStats();
        service.getStats();

        verify(admission, times(1)).dashboardTotalAdmissions();
    }

    @Test
    void unfilteredMajorListIsCachedButKeywordSearchIsNot() {
        MajorService service = new MajorService();
        MajorDimMapper majors = mock(MajorDimMapper.class);
        CollegeDimMapper colleges = mock(CollegeDimMapper.class);
        DegreeDimMapper degrees = mock(DegreeDimMapper.class);
        ReflectionTestUtils.setField(service, "majorDimMapper", majors);
        ReflectionTestUtils.setField(service, "collegeDimMapper", colleges);
        ReflectionTestUtils.setField(service, "degreeDimMapper", degrees);
        ReflectionTestUtils.setField(service, "classDimMapper", mock(ClassDimMapper.class));
        ReflectionTestUtils.setField(service, "studentFactMapper", mock(StudentFactMapper.class));
        ReflectionTestUtils.setField(service, "admissionFactMapper", mock(AdmissionFactMapper.class));
        ReflectionTestUtils.setField(service, "cacheService", memoryCache());
        when(majors.selectList(any())).thenReturn(List.of());
        when(colleges.selectList(any())).thenReturn(List.of());
        when(degrees.selectList(any())).thenReturn(List.of());

        service.list(null);
        service.list("");
        service.list("计算机");
        service.list("计算机");

        verify(majors, times(3)).selectList(any());
    }

    @Test
    void studentProvinceDropdownUsesSharedDimensionCache() {
        StudentService service = new StudentService();
        ProvinceDimMapper provinces = mock(ProvinceDimMapper.class);
        ReflectionTestUtils.setField(service, "provinceDimMapper", provinces);
        ReflectionTestUtils.setField(service, "cacheService", memoryCache());
        when(provinces.selectList(any())).thenReturn(List.of());

        service.listProvinces();
        service.listProvinces();

        verify(provinces, times(1)).selectList(any());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private CacheService memoryCache() {
        CacheService cache = mock(CacheService.class);
        AtomicReference<Object> value = new AtomicReference<>();
        when(cache.get(any(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenAnswer(invocation -> value.get());
        doAnswer(invocation -> {
            value.set(invocation.getArgument(1));
            return null;
        }).when(cache).put(any(), any(), any());
        return cache;
    }
}
