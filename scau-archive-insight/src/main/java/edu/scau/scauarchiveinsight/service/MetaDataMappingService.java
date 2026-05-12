package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.pojo.MetaDataStandard;
import edu.scau.scauarchiveinsight.util.TextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MetaDataMappingService {

    private static final Logger log = LoggerFactory.getLogger(MetaDataMappingService.class);

    @Autowired
    private MetaDataService metaDataService;

    /**
     * CSV/Excel 处理器入口：将按原列名索引的行数据映射为 fieldCode 索引的标准数据。
     *
     * @param rawRows 原始数据，每行为 Map<原列名, 值>
     * @return 包含 "data"（映射后数据）和 "errors"（校验错误）的 Map
     */
    public Map<String, Object> process(List<Map<String, String>> rawRows) {
        List<MetaDataStandard> rules = metaDataService.list();
        List<Map<String, String>> mappedRows = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();

        for (int rowIdx = 0; rowIdx < rawRows.size(); rowIdx++) {
            Map<String, String> raw = rawRows.get(rowIdx);
            Map<String, String> mapped = new LinkedHashMap<>();
            List<String> rowErrors = new ArrayList<>();

            for (MetaDataStandard rule : rules) {
                String rawValue = findValue(raw, rule);
                String fieldCode = rule.getFieldCode();

                // 必填校验
                if (Boolean.TRUE.equals(rule.getIsRequired()) && (rawValue == null || rawValue.isEmpty())) {
                    rowErrors.add("[" + fieldCode + "] " + rule.getFieldName() + " 为必填项");
                }

                // 类型校验
                if (rawValue != null && !rawValue.isEmpty()) {
                    String typeError = validateType(rawValue, rule.getFieldType(), rule.getFieldName());
                    if (typeError != null) {
                        rowErrors.add(typeError);
                    }
                }

                mapped.put(fieldCode, rawValue != null ? rawValue : "");
            }

            mappedRows.add(mapped);

            if (!rowErrors.isEmpty()) {
                errors.add(Map.of("row", rowIdx + 1, "messages", rowErrors));
            }
        }

        // 检测未匹配的列
        if (!rawRows.isEmpty()) {
            List<String> headers = new ArrayList<>(rawRows.get(0).keySet());
            List<String> unmatched = findUnmatchedHeaders(headers, rules);
            if (!unmatched.isEmpty()) {
                errors.add(Map.of("row", 0, "messages",
                        List.of("未匹配的列: " + String.join(", ", unmatched))));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", mappedRows);
        result.put("errors", errors);
        return result;
    }

    /**
     * 根据规则从原始行中查找匹配的值。
     * 匹配优先级：sourceField > fieldName > fieldCode，再降级到包含匹配。
     */
    private String findValue(Map<String, String> raw, MetaDataStandard rule) {
        for (String key : List.of(rule.getSourceField(), rule.getFieldName(), rule.getFieldCode())) {
            if (key != null && raw.containsKey(key)) {
                String val = raw.get(key);
                return val != null ? val.trim() : "";
            }
        }
        // 包含匹配：列名包含 key 或 key 包含列名
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            String col = entry.getKey();
            for (String key : List.of(rule.getSourceField(), rule.getFieldName(), rule.getFieldCode())) {
                if (key != null && (col.contains(key) || key.contains(col))) {
                    String val = entry.getValue();
                    return val != null ? val.trim() : "";
                }
            }
        }
        return null;
    }

    private String validateType(String value, String fieldType, String fieldName) {
        if (fieldType == null) return null;
        return switch (fieldType.toLowerCase()) {
            case "int" -> {
                try {
                    Long.parseLong(value);
                    yield null;
                } catch (NumberFormatException e) {
                    yield "[" + fieldName + "] 值 '" + value + "' 不是有效的整数";
                }
            }
            case "decimal" -> {
                try {
                    Double.parseDouble(value);
                    yield null;
                } catch (NumberFormatException e) {
                    yield "[" + fieldName + "] 值 '" + value + "' 不是有效的数字";
                }
            }
            case "boolean" -> {
                if (!Set.of("true", "false", "1", "0", "是", "否").contains(value.toLowerCase())) {
                    yield "[" + fieldName + "] 值 '" + value + "' 不是有效的布尔值";
                }
                yield null;
            }
            case "date" -> {
                if (!value.matches("\\d{4}[-/]?\\d{1,2}[-/]?\\d{1,2}")) {
                    yield "[" + fieldName + "] 值 '" + value + "' 不是有效的日期格式";
                }
                yield null;
            }
            default -> null;
        };
    }

    /**
     * 查出 headers 中哪些没匹配到任何元数据规则（用于生成 OCR 警告日志）
     */
    public List<String> findUnmatchedHeaders(List<String> headers, List<MetaDataStandard> rules) {
        List<FieldEntry> fieldEntries = buildFieldEntries(rules);
        List<String> unmatched = new ArrayList<>();
        for (String h : headers) {
            String trimmed = h.trim();
            if (!trimmed.isEmpty() && matchHeader(trimmed, fieldEntries) == null) {
                unmatched.add(h);
            }
        }
        return unmatched;
    }

    public List<Map<String, String>> mapGrid(List<String> headers, List<List<String>> rows,
                                              List<MetaDataStandard> rules) {
        List<FieldEntry> fieldEntries = buildFieldEntries(rules);
        Map<Integer, FieldEntry> headerMapping = new LinkedHashMap<>();
        List<String> unmatchedHeaders = new ArrayList<>();

        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i).trim();
            if (h.isEmpty()) {
                unmatchedHeaders.add(headers.get(i));
                continue;
            }
            FieldEntry matched = matchHeader(h, fieldEntries);
            if (matched != null) {
                headerMapping.put(i, matched);
            } else {
                unmatchedHeaders.add(h);
            }
        }

        if (!unmatchedHeaders.isEmpty()) {
            log.warn("未匹配的列: {}", String.join(", ", unmatchedHeaders));
        }

        List<Map<String, String>> result = new ArrayList<>();
        for (List<String> row : rows) {
            Map<String, String> item = new LinkedHashMap<>();
            for (int i = 0; i < row.size(); i++) {
                FieldEntry entry = headerMapping.get(i);
                if (entry == null) continue;
                String val = row.get(i).trim();
                if (!val.isEmpty()) {
                    item.put(entry.code, val);
                }
            }
            if (!item.isEmpty()) {
                result.add(item);
            }
        }

        return result;
    }

    private List<FieldEntry> buildFieldEntries(List<MetaDataStandard> rules) {
        List<FieldEntry> entries = new ArrayList<>();
        for (MetaDataStandard rule : rules) {
            String code = rule.getFieldCode();
            String name = rule.getFieldName();
            String source = rule.getSourceField();
            boolean required = rule.getIsRequired() != null && rule.getIsRequired();

            Set<String> seen = new LinkedHashSet<>();
            if (source != null && !source.isBlank()) seen.add(source.trim());
            if (name != null && !name.isBlank()) seen.add(name.trim());
            if (code != null && !code.isBlank()) seen.add(code.trim());

            if (!seen.isEmpty()) {
                entries.add(new FieldEntry(code, required, new ArrayList<>(seen)));
            }
        }
        return entries;
    }

    private FieldEntry matchHeader(String header, List<FieldEntry> entries) {
        for (FieldEntry entry : entries) {
            if (entry.candidates.contains(header)) return entry;
        }
        String compact = header.replaceAll("\s+", "");
        for (FieldEntry entry : entries) {
            for (String key : entry.candidates) {
                if (key.replaceAll("\s+", "").equals(compact)) return entry;
            }
        }
        for (FieldEntry entry : entries) {
            for (String key : entry.candidates) {
                if (key.contains(header) || header.contains(key)) return entry;
            }
        }
        FieldEntry best = null;
        int bestDist = Integer.MAX_VALUE;
        for (FieldEntry entry : entries) {
            for (String key : entry.candidates) {
                int dist = TextUtil.levenshteinDistance(header, key);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = entry;
                }
            }
        }
        if (best != null) {
            int maxLen = Math.max(header.length(), best.candidates.get(0).length());
            boolean accept = (maxLen <= 3 && bestDist <= 1) || (maxLen > 3 && (double) bestDist / maxLen <= 0.3);
            if (accept) return best;
        }
        return null;
    }

    private static class FieldEntry {
        final String code;
        final boolean required;
        final List<String> candidates;
        FieldEntry(String code, boolean required, List<String> candidates) {
            this.code = code;
            this.required = required;
            this.candidates = candidates;
        }
    }
}
