package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.pojo.MetaDataStandard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MetaDataMappingService {

    @Autowired
    private MetaDataService metaDataService;

    /**
     * 对原始行数据进行字段映射和校验
     * @return { "data": 映射后的行, "errors": 校验错误列表 }
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

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", mappedRows);
        result.put("errors", errors);
        return result;
    }

    private String findValue(Map<String, String> raw, MetaDataStandard rule) {
        // 匹配优先级：fieldName > sourceField > fieldCode
        for (String key : List.of(rule.getFieldName(), rule.getSourceField(), rule.getFieldCode())) {
            if (key != null && raw.containsKey(key)) {
                String val = raw.get(key);
                return val != null ? val.trim() : "";
            }
        }
        // 包含匹配：列名包含 key 或 key 包含列名
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            String col = entry.getKey();
            for (String key : List.of(rule.getFieldName(), rule.getSourceField(), rule.getFieldCode())) {
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
                // 简单日期格式校验 YYYY-MM-DD 或 YYYY/MM/DD
                if (!value.matches("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}")) {
                    yield "[" + fieldName + "] 值 '" + value + "' 不是有效的日期格式";
                }
                yield null;
            }
            default -> null; // varchar 等不做校验
        };
    }
}
