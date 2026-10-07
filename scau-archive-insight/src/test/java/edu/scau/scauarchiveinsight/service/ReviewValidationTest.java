package edu.scau.scauarchiveinsight.service;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ReviewValidationTest {
    final List<Map<String,Object>> fields=List.of(
        Map.of("code","name","label","姓名","required",true),
        Map.of("code","gender","label","性别","required",false),
        Map.of("code","id_card","label","身份证","required",false),
        Map.of("code","admission_date","label","日期","required",false),
        Map.of("code","admission_score","label","分数","required",false),
        Map.of("code","student_no","label","学号","required",false));
    @Test void normalizePreservesIdsAndDoesNotMutateInputOrPassExtraColumns() {
        var input=Map.of("_row_id","one","name"," 张三 ","姓名","原始");
        var rows=ReviewDraftService.normalize(List.of(input),fields);
        assertEquals("one",rows.get(0).get("_row_id"));
        assertEquals("张三",rows.get(0).get("name"));
        assertFalse(rows.get(0).containsKey("姓名"));
        assertEquals(" 张三 ",input.get("name"));
    }
    @Test void rejectsSilentFieldLossAndEmptyRecords() {
        var rows=ReviewDraftService.normalize(List.of(Map.of("gender","未知","id_card","123",
            "admission_date","不是日期","admission_score","6.1"),Map.of()),fields);
        assertEquals(5,ReviewDraftService.validate(rows,fields).stream().filter(i->i.get("level").equals("error")).count());
    }
    @Test void missingRequiredFieldsAndDuplicatesWarnWithoutHardBlocking() {
        var rows=ReviewDraftService.normalize(List.of(Map.of("student_no","1"),Map.of("student_no","1")),fields);
        var issues=ReviewDraftService.validate(rows,fields);
        assertEquals(3,issues.size());
        assertTrue(issues.stream().allMatch(i->i.get("level").equals("warning")));
    }
    @Test void duplicateRowIdsAndNullRowsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->ReviewDraftService.normalize(
            List.of(Map.of("_row_id","1"),Map.of("_row_id","1")),fields));
        assertThrows(IllegalArgumentException.class,()->ReviewDraftService.normalize(
            Arrays.asList((Map<String,String>)null),fields));
    }
    @Test void nonexistentCalendarDateIsNotSilentlyAdjusted() {
        var rows=ReviewDraftService.normalize(List.of(Map.of("name","日期测试","admission_date","2025-02-31")),fields);
        assertTrue(ReviewDraftService.validate(rows,fields).stream().anyMatch(i->"error".equals(i.get("level"))));
    }
}
