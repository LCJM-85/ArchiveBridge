package edu.scau.scauarchiveinsight.util;

public class DesensitizeUtil {

    public static String maskName(String value) {
        if (value == null || value.length() < 2) return value;
        return value.charAt(0) + "*".repeat(value.length() - 1);
    }

    public static String maskIdCard(String value) {
        if (value == null || value.length() < 10) return value;
        return value.substring(0, 6) + "******" + value.substring(value.length() - 4);
    }

    public static String maskPhone(String value) {
        if (value == null || value.length() < 7) return value;
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    public static String maskExamNo(String value) {
        if (value == null || value.length() < 10) return value;
        return value.substring(0, 6) + "***" + value.substring(value.length() - 4);
    }

    public static String mask(SensitiveType type, String value) {
        return switch (type) {
            case NAME -> maskName(value);
            case ID_CARD -> maskIdCard(value);
            case PHONE -> maskPhone(value);
            case EXAM_NO -> maskExamNo(value);
        };
    }

    public enum SensitiveType {
        NAME, ID_CARD, PHONE, EXAM_NO
    }
}
