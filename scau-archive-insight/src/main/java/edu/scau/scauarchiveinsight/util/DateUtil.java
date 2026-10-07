package edu.scau.scauarchiveinsight.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;

public class DateUtil {

    // 支持所有常见日期格式
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList(
            "uuuu-MM-dd",
            "uuuu/MM/dd",
            "uuuuMMdd",
            "uuuu.MM.dd",
            "uuuu年MM月dd日",
            "MM/dd/uuuu",
            "dd-MM-uuuu"
    );

    public static LocalDate convertToLocalDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }

        for (String format : SUPPORTED_FORMATS) {
            try {
                return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern(format)
                    .withResolverStyle(ResolverStyle.STRICT));
            } catch (DateTimeParseException ignored) {
                // 格式不匹配，继续试下一个
            }
        }

        // 所有格式都不匹配，返回null，不报错
        return null;
    }
}
