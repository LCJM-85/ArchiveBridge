package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.util.TextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.ToIntFunction;

/**
 * 字段类型评分 + 最优分配：对一行中的每个值评分，识别它最可能属于哪个字段。
 * 不依赖表头名称，只看值本身的特征——放在元数据匹配之前执行。
 */
@Service
public class FieldCorrectionService {

    private static final Logger log = LoggerFactory.getLogger(FieldCorrectionService.class);

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy年MM月dd日")
    );

    /**
     * 对一行原始值评分，用贪心算法分配到最匹配的字段。
     *
     * @param data         单行原始 {headerName: value}（会被修改为 {fieldCode: value}）
     * @param allProvinces 省份维度表所有名称
     * @param archiveType  档案类型（admission/graduation），用于区分日期字段
     */
    public void autoCorrectFields(Map<String, String> data, List<String> allProvinces, String archiveType) {
        Map<String, ToIntFunction<String>> recognizers = buildRecognizers(allProvinces, archiveType);

        // 收集所有有值的条目
        List<String> values = new ArrayList<>();
        for (Map.Entry<String, String> e : data.entrySet()) {
            if (e.getValue() != null && !e.getValue().isBlank()) {
                values.add(e.getValue().trim());
            }
        }
        if (values.size() < 2) return;

        List<String> targets = new ArrayList<>(recognizers.keySet());

        // 贪心分配：每次取最高分 (valueIdx→target) 对
        Map<Integer, String> assignment = new HashMap<>();
        Set<Integer> usedValues = new HashSet<>();
        Set<String> usedTargets = new HashSet<>();

        while (usedValues.size() < values.size()) {
            int bestIdx = -1;
            String bestTarget = null;
            int bestScore = -1;
            for (int i = 0; i < values.size(); i++) {
                if (usedValues.contains(i)) continue;
                for (String t : targets) {
                    if (usedTargets.contains(t)) continue;
                    int s = recognizers.get(t).applyAsInt(values.get(i));
                    if (s > bestScore) {
                        bestScore = s;
                        bestIdx = i;
                        bestTarget = t;
                    }
                }
            }
            if (bestTarget == null || bestScore <= 0) break;
            assignment.put(bestIdx, bestTarget);
            usedValues.add(bestIdx);
            usedTargets.add(bestTarget);
        }

        if (assignment.isEmpty()) return;

        // 执行分配：按 fieldCode 重组数据
        Map<String, String> newData = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> e : assignment.entrySet()) {
            String newKey = e.getValue();
            String oldKey = null;
            int idx = e.getKey();
            // 找原始 key（只用于日志）
            for (Map.Entry<String, String> de : data.entrySet()) {
                if (de.getValue() != null && de.getValue().trim().equals(values.get(idx))) {
                    oldKey = de.getKey();
                    break;
                }
            }
            newData.put(newKey, values.get(idx));
            if (oldKey != null && !oldKey.equals(newKey)) {
                log.info("最优分配纠正: {}='{}' → 字段 {}", oldKey, values.get(idx), newKey);
            }
        }

        data.clear();
        data.putAll(newData);
    }

    private Map<String, ToIntFunction<String>> buildRecognizers(List<String> allProvinces, String archiveType) {
        Map<String, ToIntFunction<String>> recognizers = new LinkedHashMap<>();

        List<String> degreeKeywords = Arrays.asList("学士", "硕士", "博士");

        recognizers.put("gender", v -> "男".equals(v) || "女".equals(v) ? 100 : 0);
        recognizers.put("id_card", v -> {
            if (v.matches("\\d{17}[\\dXx]") || v.matches("\\d{18}")) return 100;
            return v.length() == 18 ? 40 : 0;
        });
        recognizers.put("name", v -> {
            if (v.contains("学士") || v.contains("硕士") || v.contains("博士")
                    || v.contains("毕业") || v.contains("结业")) return 0;
            if (v.matches("\\d+")) return 0;
            if (v.matches(".*\\d.*")) return 10;
            String pure = v.replaceAll("[·.\\s]", "");
            return pure.matches("[\\u4e00-\\u9fa5]{2,4}") ? 75 : 0;
        });
        recognizers.put("degree_name", v -> {
            if (v.contains("学士") || v.contains("硕士") || v.contains("博士")) return 100;
            return 0; // 不包含学位关键词的肯定不是学历
        });
        recognizers.put("province_name", v -> {
            if (allProvinces.contains(v)) return 100;
            if (allProvinces.stream().anyMatch(p -> p.startsWith(v.replace("省", "").replace("市", "")))) return 80;
            return TextUtil.fuzzyMatch(v, allProvinces, TextUtil.autoThreshold(v)) != null ? 70 : 0;
        });
        if ("admission".equals(archiveType)) {
            recognizers.put("admission_date", v -> v.matches("\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}[日]?") ? 100 : 0);
        } else if ("graduation".equals(archiveType)) {
            recognizers.put("graduation_date", v -> {
                if (v == null || v.isBlank()) return 0;
                String trimmed = v.trim();
                for (DateTimeFormatter fmt : DATE_FORMATTERS) {
                    try {
                        LocalDate date = LocalDate.parse(trimmed, fmt);
                        if (!date.isBefore(LocalDate.of(1950, 1, 1))
                                && !date.isAfter(LocalDate.now().plusYears(1))) {
                            return 100;
                        }
                        return 60;
                    } catch (DateTimeParseException ignored) {
                    }
                }
                if (trimmed.matches(".*\\d{4}.*")) return 30;
                return 0;
            });
        }
        recognizers.put("student_no", v -> {
            if (v.matches("\\d{12}")) return 85;
            if (v.matches("\\d{4}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])")) return 0;
            if (v.matches("\\d{18}")) return 0;
            return 0;
        });
        recognizers.put("exam_no", v -> v.matches("\\d{9,15}") ? 70 : 0);
        recognizers.put("class_name", v -> v.contains("班") ? 60 : 0);

        return recognizers;
    }
}
