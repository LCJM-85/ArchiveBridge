package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.mapper.CollegeDimMapper;
import edu.scau.scauarchiveinsight.mapper.MajorDimMapper;
import edu.scau.scauarchiveinsight.pojo.CollegeDim;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisEvictionContractTest {

    @Test
    void collegeWriteEvictsCollegeAndMajorPayloadCachesAfterSuccess() {
        CollegeService service = new CollegeService();
        CollegeDimMapper mapper = mock(CollegeDimMapper.class);
        CacheService cache = mock(CacheService.class);
        ReflectionTestUtils.setField(service, "collegeDimMapper", mapper);
        ReflectionTestUtils.setField(service, "majorDimMapper", mock(MajorDimMapper.class));
        ReflectionTestUtils.setField(service, "cacheService", cache);
        CollegeDim college = new CollegeDim();
        when(mapper.insert(college)).thenReturn(1);

        service.add(college);

        verify(cache).evict(CacheService.COLLEGE_KEY, CacheService.MAJOR_KEY, CacheService.MAJOR_DROPDOWN_KEY);
        verify(cache).evictDashboard();
    }
}
