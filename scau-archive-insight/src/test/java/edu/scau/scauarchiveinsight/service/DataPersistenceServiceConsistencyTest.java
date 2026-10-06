package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.mapper.AdmissionFactMapper;
import edu.scau.scauarchiveinsight.mapper.ArchiveFileDimMapper;
import edu.scau.scauarchiveinsight.mapper.ClassDimMapper;
import edu.scau.scauarchiveinsight.mapper.CollegeDimMapper;
import edu.scau.scauarchiveinsight.mapper.DegreeDimMapper;
import edu.scau.scauarchiveinsight.mapper.DestinationDimMapper;
import edu.scau.scauarchiveinsight.mapper.GraduationFactMapper;
import edu.scau.scauarchiveinsight.mapper.MajorDimMapper;
import edu.scau.scauarchiveinsight.mapper.ProvinceDimMapper;
import edu.scau.scauarchiveinsight.mapper.StudentFactMapper;
import edu.scau.scauarchiveinsight.pojo.AdmissionFact;
import edu.scau.scauarchiveinsight.pojo.ArchiveFileDim;
import edu.scau.scauarchiveinsight.pojo.StudentFact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

class DataPersistenceServiceConsistencyTest {

    private DataPersistenceService service;
    private StudentFactMapper studentMapper;
    private AdmissionFactMapper admissionMapper;

    @BeforeEach
    void setUp() {
        service = new DataPersistenceService();
        studentMapper = mock(StudentFactMapper.class);
        admissionMapper = mock(AdmissionFactMapper.class);
        ReflectionTestUtils.setField(service, "archiveFileDimMapper", mock(ArchiveFileDimMapper.class));
        ReflectionTestUtils.setField(service, "provinceDimMapper", mock(ProvinceDimMapper.class));
        ReflectionTestUtils.setField(service, "majorDimMapper", mock(MajorDimMapper.class));
        ReflectionTestUtils.setField(service, "admissionFactMapper", admissionMapper);
        ReflectionTestUtils.setField(service, "studentFactMapper", studentMapper);
        ReflectionTestUtils.setField(service, "classDimMapper", mock(ClassDimMapper.class));
        ReflectionTestUtils.setField(service, "collegeDimMapper", mock(CollegeDimMapper.class));
        ReflectionTestUtils.setField(service, "graduationFactMapper", mock(GraduationFactMapper.class));
        ReflectionTestUtils.setField(service, "degreeDimMapper", mock(DegreeDimMapper.class));
        ReflectionTestUtils.setField(service, "destinationDimMapper", mock(DestinationDimMapper.class));
        ReflectionTestUtils.setField(service, "cacheService", mock(CacheService.class));
        ReflectionTestUtils.setField(service, "ocrTaskManager", mock(OCRTaskManager.class));
    }

    @Test
    void emptyOcrValuesMustNotOverwriteExistingAdmissionData() {
        StudentFact existingStudent = new StudentFact();
        existingStudent.setId(1L);
        existingStudent.setStudentNo("20240001");
        existingStudent.setName("张三");
        existingStudent.setIdCard("440101200001011234");
        existingStudent.setGender("男");
        existingStudent.setCreateTime(LocalDateTime.of(2024, 1, 1, 0, 0));
        when(studentMapper.selectOne(any())).thenReturn(existingStudent);

        AdmissionFact existingAdmission = new AdmissionFact();
        existingAdmission.setId(2L);
        existingAdmission.setStudentNo("20240001");
        existingAdmission.setName("张三");
        existingAdmission.setIdCard("440101200001011234");
        existingAdmission.setGender("男");
        existingAdmission.setExamNo("EXAM001");
        existingAdmission.setCreateTime(LocalDateTime.of(2024, 1, 1, 0, 0));
        when(admissionMapper.selectOne(any())).thenReturn(existingAdmission);

        Map<String, String> ocr = new HashMap<>();
        ocr.put("student_no", "20240001");
        ocr.put("name", "   ");
        ocr.put("id_card", "");
        ocr.put("gender", "");
        ocr.put("exam_no", "");

        service.saveExtractedData("admission", ocr, 9);

        ArgumentCaptor<StudentFact> student = ArgumentCaptor.forClass(StudentFact.class);
        verify(studentMapper).updateById(student.capture());
        assertEquals("张三", student.getValue().getName());
        assertEquals("440101200001011234", student.getValue().getIdCard());
        assertEquals("男", student.getValue().getGender());

        ArgumentCaptor<AdmissionFact> admission = ArgumentCaptor.forClass(AdmissionFact.class);
        verify(admissionMapper).updateById(admission.capture());
        assertEquals("张三", admission.getValue().getName());
        assertEquals("440101200001011234", admission.getValue().getIdCard());
        assertEquals("男", admission.getValue().getGender());
        assertEquals("EXAM001", admission.getValue().getExamNo());
    }

    @Test
    void fileLevelPersistenceEntryMustBeTransactional() {
        boolean transactionalEntryExists = Arrays.stream(DataPersistenceService.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("saveFileData"))
                .map(method -> method.getAnnotation(Transactional.class))
                .anyMatch(annotation -> annotation != null && annotation.rollbackFor().length > 0);

        assertTrue(transactionalEntryExists, "同一文件必须通过带回滚规则的事务入口统一入库");
    }

    @Test
    void databaseSanitizingMustNotChangeRecordsUsedForScoring() {
        ArchiveFileDimMapper archiveMapper = (ArchiveFileDimMapper) ReflectionTestUtils.getField(
                service, "archiveFileDimMapper");
        doAnswer(invocation -> {
            ArchiveFileDim file = invocation.getArgument(0);
            file.setFileId(11);
            return 1;
        }).when(archiveMapper).insert(any(ArchiveFileDim.class));

        Map<String, String> original = new HashMap<>();
        original.put("student_no", "20240002");
        original.put("id_card", "123");
        original.put("gender", "未知");

        service.saveFileData("test.png", "picture", "admission", java.util.List.of(original));

        assertEquals("123", original.get("id_card"));
        assertEquals("未知", original.get("gender"));
    }

    @Test
    void fileTransactionMustInvalidateCachesAgainAfterCommit() {
        ArchiveFileDimMapper archive = (ArchiveFileDimMapper) ReflectionTestUtils.getField(service, "archiveFileDimMapper");
        CacheService cache = (CacheService) ReflectionTestUtils.getField(service, "cacheService");
        doAnswer(call -> { ((ArchiveFileDim) call.getArgument(0)).setFileId(12); return 1; })
                .when(archive).insert(any(ArchiveFileDim.class));
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            service.saveFileData("after-commit.csv", "CSV", "admission", java.util.List.of(Map.of("name", "测试")));
            var callbacks = org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations();
            assertTrue(!callbacks.isEmpty(), "必须注册事务提交后的缓存失效");
            callbacks.forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);
            verify(cache).evictAllDimensions();
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
            org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }
}
