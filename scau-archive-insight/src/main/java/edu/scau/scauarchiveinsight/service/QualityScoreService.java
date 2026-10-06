package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.mapper.QualityScoreDimMapper;
import edu.scau.scauarchiveinsight.pojo.MetaDataStandard;
import edu.scau.scauarchiveinsight.pojo.QualityScoreDim;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据质量评分：每个文件归档后自动评分
 * 完整性 50% + 有效性（沿用 accuracy 字段）30% + 一致性 20%
 */
@Service
public class QualityScoreService {

    private static final List<String> ADMISSION_FIELDS = List.of(
            "student_no", "exam_no", "name", "id_card", "gender",
            "province_name", "major_name", "class_name", "degree_name",
            "admission_date", "admission_score"
    );

    private static final List<String> GRADUATION_FIELDS = List.of(
            "student_no", "name", "id_card", "gender", "degree_name",
            "dest_name", "graduation_date"
    );

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuu/MM/dd").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuu.MM.dd").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuu年MM月dd日").withResolverStyle(ResolverStyle.STRICT)
    );

    private static final int[] ID_CARD_WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] ID_CARD_CHECKS = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    @Autowired
    private QualityScoreDimMapper qualityScoreDimMapper;

    @Autowired
    private MetaDataService metaDataService;

    @Autowired
    private CacheService cacheService;

    /**
     * 对已处理的文件进行质量评分并入库
     *
     * @param fileId      归档文件 ID
     * @param archiveType admission / graduation
     * @param records     该文件解析出的所有数据行
     * @param errorCount  保留的兼容参数；评分改为直接检查记录内容
     */
    public void scoreFile(Integer fileId, String archiveType,
                          List<Map<String, String>> records, int errorCount) {
        List<String> fields = fieldsFor(archiveType);
        int completeness = calcCompleteness(records, fields);
        int accuracy = calcValidity(records, fields);
        int consistency = calcConsistency(records, archiveType);
        int timeliness = 0;
        int total = clamp((int) Math.round(
                completeness * 0.50 + accuracy * 0.30 + consistency * 0.20));

        QualityScoreDim score = new QualityScoreDim();
        score.setFileId(fileId);
        score.setCompleteness(completeness);
        score.setConsistency(consistency);
        score.setAccuracy(accuracy);
        score.setTimeliness(timeliness);
        score.setTotalScore(total);
        score.setCheckTime(LocalDateTime.now());

        qualityScoreDimMapper.insert(score);
        cacheService.evictDashboard();
    }

    /**
     * 完整性：本档案类型适用字段中的非空比例。原始中文列和其他额外列不参与计算。
     */
    private int calcCompleteness(List<Map<String, String>> records, List<String> fields) {
        if (records == null || records.isEmpty() || fields.isEmpty()) return 0;
        int nonEmpty = 0;
        int totalCells = records.size() * fields.size();
        for (Map<String, String> rec : records) {
            if (rec == null) continue;
            for (String field : fields) {
                if (hasText(rec.get(field))) nonEmpty++;
            }
        }
        return clamp((int) Math.round((double) nonEmpty / totalCells * 100));
    }

    /**
     * 有效性：只检查已经提取出的值，缺失字段由完整性负责，避免重复扣分。
     * 数据库字段名保持 accuracy，以兼容现有接口和表结构。
     */
    private int calcValidity(List<Map<String, String>> records, List<String> fields) {
        if (records == null || records.isEmpty() || fields.isEmpty()) return 0;
        int populated = 0;
        int valid = 0;
        for (Map<String, String> rec : records) {
            if (rec == null) continue;
            for (String field : fields) {
                String value = rec.get(field);
                if (!hasText(value)) continue;
                populated++;
                if (isValid(field, value.trim())) valid++;
            }
        }
        if (populated == 0) return 0;
        return clamp((int) Math.round((double) valid / populated * 100));
    }

    /**
     * 一致性：按行检查文件内身份标识重复，以及身份证性别与性别字段是否冲突。
     */
    private int calcConsistency(List<Map<String, String>> records, String archiveType) {
        if (records == null || records.isEmpty()) return 0;
        List<String> identifiers = "admission".equals(archiveType)
                ? List.of("student_no", "exam_no", "id_card")
                : List.of("student_no", "id_card");
        Map<String, Set<String>> seen = new HashMap<>();
        for (String field : identifiers) seen.put(field, new HashSet<>());

        int consistentRows = 0;
        for (Map<String, String> rec : records) {
            if (rec == null) continue;
            boolean consistent = true;
            for (String field : identifiers) {
                String value = normalized(rec.get(field));
                if (!value.isEmpty() && !seen.get(field).add(value)) consistent = false;
            }
            String idCard = normalized(rec.get("id_card"));
            String gender = normalized(rec.get("gender"));
            if (isValidIdCard(idCard) && ("男".equals(gender) || "女".equals(gender))) {
                String genderFromId = (idCard.charAt(16) - '0') % 2 == 0 ? "女" : "男";
                if (!genderFromId.equals(gender)) consistent = false;
            }
            if (consistent) consistentRows++;
        }
        return clamp((int) Math.round((double) consistentRows / records.size() * 100));
    }

    private List<String> fieldsFor(String archiveType) {
        if ("admission".equals(archiveType)) return ADMISSION_FIELDS;
        if ("graduation".equals(archiveType)) return GRADUATION_FIELDS;

        List<MetaDataStandard> rules = metaDataService.list();
        if (rules == null || rules.isEmpty()) return List.of();
        Set<String> fields = new LinkedHashSet<>();
        for (MetaDataStandard rule : rules) {
            if (rule != null && hasText(rule.getFieldCode())) fields.add(rule.getFieldCode().trim());
        }
        return new ArrayList<>(fields);
    }

    private boolean isValid(String field, String value) {
        return switch (field) {
            case "student_no" -> value.matches("[A-Za-z0-9-]{6,32}");
            case "exam_no" -> value.matches("\\d{9,15}");
            case "name" -> value.length() >= 2 && value.length() <= 50 && !value.matches(".*\\d.*");
            case "id_card" -> isValidIdCard(value);
            case "gender" -> "男".equals(value) || "女".equals(value);
            case "admission_date", "graduation_date" -> isValidDate(value);
            case "admission_score" -> isValidAdmissionScore(value);
            default -> true;
        };
    }

    private boolean isValidDate(String value) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate.parse(value, formatter);
                return true;
            } catch (DateTimeException ignored) {
            }
        }
        return false;
    }

    private boolean isValidAdmissionScore(String value) {
        try {
            int score = Integer.parseInt(value);
            return score >= 0 && score <= 750;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private boolean isValidIdCard(String value) {
        if (value == null || !value.matches("\\d{17}[\\dXx]")) return false;
        if (!isValidDate(value.substring(6, 14))) return false;
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (value.charAt(i) - '0') * ID_CARD_WEIGHTS[i];
        }
        return Character.toUpperCase(value.charAt(17)) == ID_CARD_CHECKS[sum % 11];
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
