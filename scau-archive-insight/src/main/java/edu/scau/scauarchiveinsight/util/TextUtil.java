package edu.scau.scauarchiveinsight.util;

import java.util.List;

/**
 * 文本模糊匹配工具
 * 用于 OCR 识别结果的纠错场景，如 "广冬" → "广东"
 */
public class TextUtil {

    /**
     * 计算两个字符串的编辑距离（Levenshtein distance）
     * 支持中文，距离 = 增/删/改一个字符各计 1
     */
    public static int levenshteinDistance(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[m][n];
    }

    /**
     * 在标准值列表中模糊匹配输入值
     *
     * @param input      OCR 识别出的原始值
     * @param standards  维度表中的标准值列表
     * @param threshold  最大允许编辑距离（≤ threshold 才视为匹配）
     * @return 最佳匹配的标准值，没有合适匹配则返回 null
     */
    public static String fuzzyMatch(String input, List<String> standards, int threshold) {
        if (input == null || input.isBlank() || standards == null || standards.isEmpty()) {
            return null;
        }
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        String trimmedInput = input.trim();
        for (String s : standards) {
            if (s == null) continue;
            // 完全匹配直接返回
            if (s.equals(trimmedInput)) {
                return s;
            }
            int dist = levenshteinDistance(trimmedInput, s.trim());
            if (dist < bestDist) {
                bestDist = dist;
                best = s;
            }
        }
        return bestDist <= threshold ? best : null;
    }

    /**
     * 自动计算合理阈值：短文本（≤3字）最大1，长文本最大2
     */
    public static int autoThreshold(String input) {
        if (input == null) return 0;
        return input.trim().length() <= 3 ? 1 : 2;
    }
}
