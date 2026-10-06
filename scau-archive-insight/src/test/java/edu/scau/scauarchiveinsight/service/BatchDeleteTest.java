package edu.scau.scauarchiveinsight.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.scau.scauarchiveinsight.mapper.*;
import edu.scau.scauarchiveinsight.controller.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BatchDeleteTest {
    record Fixture(Object service, BaseMapper<?> mapper, CacheService cache) {}

    private Fixture fixture(String type) {
        Object service;
        BaseMapper<?> mapper;
        String field;
        switch (type) {
            case "admission" -> { service = new AdmissionService(); mapper = mock(AdmissionFactMapper.class); field = "admissionFactMapper"; }
            case "graduation" -> { service = new GraduationService(); mapper = mock(GraduationFactMapper.class); field = "graduationFactMapper"; }
            default -> { service = new StudentService(); mapper = mock(StudentFactMapper.class); field = "studentFactMapper"; }
        }
        CacheService cache = mock(CacheService.class);
        ReflectionTestUtils.setField(service, field, mapper);
        ReflectionTestUtils.setField(service, "cacheService", cache);
        return new Fixture(service, mapper, cache);
    }

    private int invokeDelete(Fixture f, List<Long> ids) throws Throwable {
        Method method = Arrays.stream(f.service().getClass().getMethods())
                .filter(m -> m.getName().equals("deleteBatch") && m.getParameterCount() == 1)
                .findFirst().orElse(null);
        assertNotNull(method, "数据服务必须提供批量删除入口");
        try { return (int) method.invoke(f.service(), ids); }
        catch (InvocationTargetException e) { throw e.getCause(); }
    }

    @ParameterizedTest
    @ValueSource(strings = {"admission", "graduation", "student"})
    void deletesSelectedIdsOnceAndReturnsActualCount(String type) throws Throwable {
        Fixture f = fixture(type);
        when(f.mapper().deleteByIds(anyCollection())).thenReturn(1);
        assertEquals(1, invokeDelete(f, List.of(11L, 12L, 11L)));
        verify(f.mapper()).deleteByIds(List.of(11L, 12L));
        verify(f.cache()).evictDashboard();
        verifyNoMoreInteractions(f.mapper());
    }

    @ParameterizedTest
    @ValueSource(strings = {"admission", "graduation", "student"})
    void invalidSelectionsNeverReachDatabase(String type) {
        Fixture f = fixture(type);
        for (List<Long> ids : Arrays.asList(null, List.<Long>of(), List.of(0L), List.of(-1L),
                Arrays.asList(1L, null), LongStream.rangeClosed(1, 101).boxed().toList())) {
            assertThrows(IllegalArgumentException.class, () -> invokeDelete(f, ids));
        }
        verifyNoInteractions(f.mapper(), f.cache());
    }

    @ParameterizedTest
    @ValueSource(strings = {"admission", "graduation", "student"})
    void databaseFailureIsNotReportedAsSuccess(String type) {
        Fixture f = fixture(type);
        when(f.mapper().deleteByIds(anyCollection())).thenThrow(new IllegalStateException("database failed"));
        assertThrows(IllegalStateException.class, () -> invokeDelete(f, List.of(11L)));
        verifyNoInteractions(f.cache());
    }

    private MockMvc mvc(String type, Fixture f) {
        Object controller = switch (type) {
            case "admission" -> new AdmissionController();
            case "graduation" -> new GraduationController();
            default -> new StudentController();
        };
        ReflectionTestUtils.setField(controller, type + "Service", f.service());
        return MockMvcBuilders.standaloneSetup(controller).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"admission", "graduation", "student"})
    void batchRouteAcceptsJsonIdsAndReturnsActualCount(String type) throws Exception {
        Fixture f = fixture(type);
        when(f.mapper().deleteByIds(anyCollection())).thenReturn(2);
        mvc(type, f).perform(delete("/api/" + type + "/delete/batch")
                        .contentType(MediaType.APPLICATION_JSON).content("[11,12]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.deletedCount").value(2));
        verify(f.mapper()).deleteByIds(List.of(11L, 12L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"admission", "graduation", "student"})
    void emptyBatchRouteReturnsValidationErrorWithoutDeleting(String type) throws Exception {
        Fixture f = fixture(type);
        mvc(type, f).perform(delete("/api/" + type + "/delete/batch")
                        .contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(f.mapper(), f.cache());
    }
}
