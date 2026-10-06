package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.mapper.QualityScoreDimMapper;
import edu.scau.scauarchiveinsight.pojo.QualityScoreDim;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QualityScoreServiceTest {

    @Mock
    private QualityScoreDimMapper qualityScoreDimMapper;
    @Mock
    private MetaDataService metaDataService;
    @Mock
    private CacheService cacheService;

    private QualityScoreService service;

    @BeforeEach
    void setUp() {
        service = new QualityScoreService();
        ReflectionTestUtils.setField(service, "qualityScoreDimMapper", qualityScoreDimMapper);
        ReflectionTestUtils.setField(service, "metaDataService", metaDataService);
        ReflectionTestUtils.setField(service, "cacheService", cacheService);
        when(qualityScoreDimMapper.insert(any(QualityScoreDim.class))).thenReturn(1);
    }

    @Test
    void graduationScoreIgnoresRawDuplicateColumnsAndAdmissionFields() {
        String idCard = validIdCard("11010520000101002");
        Map<String, String> record = graduationRecord("200730820110", idCard, "女");
        record.put("学号", "200730820110");
        record.put("姓名", "测试姓名");
        record.put("身份证号", idCard);
        record.put("招生分数", "620");

        QualityScoreDim score = score("graduation", List.of(record), 0);

        assertEquals(100, score.getCompleteness());
        assertEquals(100, score.getAccuracy());
        assertEquals(100, score.getConsistency());
        assertEquals(0, score.getTimeliness());
        assertEquals(100, score.getTotalScore());
    }

    @Test
    void admissionCompletenessUsesOnlyAdmissionProfileFields() {
        Map<String, String> record = new LinkedHashMap<>();
        record.put("exam_no", "14370802151298");
        record.put("name", "测试姓名");
        record.put("id_card", validIdCard("11010520000101002"));
        record.put("gender", "女");
        record.put("major_name", "城乡规划");
        record.put("degree_name", "学士");
        record.put("admission_score", "613");

        QualityScoreDim score = score("admission", List.of(record), 0);

        assertEquals(64, score.getCompleteness());
        assertEquals(100, score.getAccuracy());
        assertEquals(100, score.getConsistency());
        assertEquals(82, score.getTotalScore());
    }

    @Test
    void accuracyMeasuresValidityOfPopulatedFields() {
        Map<String, String> record = new LinkedHashMap<>();
        record.put("student_no", "202600000001");
        record.put("exam_no", "错误考号");
        record.put("name", "测试姓名");
        record.put("id_card", "12345");
        record.put("gender", "未知值");
        record.put("province_name", "山东省");
        record.put("major_name", "城乡规划");
        record.put("class_name", "规划1班");
        record.put("degree_name", "学士");
        record.put("admission_date", "2026-02-30");
        record.put("admission_score", "900");

        QualityScoreDim score = score("admission", List.of(record), 0);

        assertEquals(100, score.getCompleteness());
        assertEquals(55, score.getAccuracy());
        assertEquals(87, score.getTotalScore());
    }

    @Test
    void consistencyPenalizesDuplicateIdentifiersByRow() {
        String idCard = validIdCard("11010520000101002");
        Map<String, String> first = graduationRecord("200730820110", idCard, "女");
        Map<String, String> duplicate = graduationRecord("200730820110", idCard, "女");

        QualityScoreDim score = score("graduation", List.of(first, duplicate), 0);

        assertEquals(50, score.getConsistency());
        assertEquals(90, score.getTotalScore());
    }

    @Test
    void consistencyDetectsGenderConflictWithIdCard() {
        String femaleIdCard = validIdCard("11010520000101002");
        Map<String, String> record = graduationRecord("200730820110", femaleIdCard, "男");

        QualityScoreDim score = score("graduation", List.of(record), 0);

        assertEquals(0, score.getConsistency());
        assertEquals(80, score.getTotalScore());
    }

    @Test
    void emptyExtractionScoresZero() {
        QualityScoreDim score = score("graduation", List.of(), 0);

        assertEquals(0, score.getCompleteness());
        assertEquals(0, score.getAccuracy());
        assertEquals(0, score.getConsistency());
        assertEquals(0, score.getTimeliness());
        assertEquals(0, score.getTotalScore());
    }

    private QualityScoreDim score(String archiveType, List<Map<String, String>> records, int errorCount) {
        service.scoreFile(42, archiveType, records, errorCount);
        ArgumentCaptor<QualityScoreDim> captor = ArgumentCaptor.forClass(QualityScoreDim.class);
        verify(qualityScoreDimMapper).insert(captor.capture());
        return captor.getValue();
    }

    private Map<String, String> graduationRecord(String studentNo, String idCard, String gender) {
        Map<String, String> record = new LinkedHashMap<>();
        record.put("student_no", studentNo);
        record.put("name", "测试姓名");
        record.put("id_card", idCard);
        record.put("gender", gender);
        record.put("degree_name", "法学学士");
        record.put("dest_name", "毕业");
        record.put("graduation_date", "2011-06-30");
        return record;
    }

    private String validIdCard(String firstSeventeenDigits) {
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] checks = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
        int sum = 0;
        for (int i = 0; i < firstSeventeenDigits.length(); i++) {
            sum += (firstSeventeenDigits.charAt(i) - '0') * weights[i];
        }
        return firstSeventeenDigits + checks[sum % 11];
    }
}
