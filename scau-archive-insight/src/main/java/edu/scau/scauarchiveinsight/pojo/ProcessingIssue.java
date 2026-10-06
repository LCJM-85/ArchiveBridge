package edu.scau.scauarchiveinsight.pojo;

/**
 * OCR/导入过程中产生的结构化问题。问题只用于说明处理结果，不表示需要人工复核。
 */
public record ProcessingIssue(
        String level,
        String code,
        Integer row,
        String field,
        String message,
        String suggestion) {

    public static ProcessingIssue warning(String message) {
        return new ProcessingIssue("warning", "DATA_QUALITY", null, null, message, null);
    }

    public static ProcessingIssue error(String message) {
        return new ProcessingIssue("error", "PROCESSING_FAILED", null, null, message, null);
    }
}
