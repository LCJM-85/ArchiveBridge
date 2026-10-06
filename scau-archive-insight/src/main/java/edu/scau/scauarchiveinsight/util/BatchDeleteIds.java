package edu.scau.scauarchiveinsight.util;

import java.util.List;

public final class BatchDeleteIds {
    private BatchDeleteIds() {}

    public static List<Long> validate(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("请先选择要删除的记录");
        }
        if (ids.size() > 100) {
            throw new IllegalArgumentException("每次最多删除100条记录");
        }
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("记录ID必须为正整数");
        }
        return ids.stream().distinct().toList();
    }
}
