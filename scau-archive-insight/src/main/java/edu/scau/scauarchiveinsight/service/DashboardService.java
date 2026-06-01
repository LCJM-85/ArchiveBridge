package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.mapper.AdmissionFactMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private AdmissionFactMapper admissionFactMapper;

    public Map<String, Object> getStats() {
        Map<String, Object> result = new HashMap<>();

        result.put("totalAdmissions", Optional.ofNullable(admissionFactMapper.dashboardTotalAdmissions()).orElse(0));
        result.put("totalGraduates", Optional.ofNullable(admissionFactMapper.dashboardTotalGraduates()).orElse(0));
        result.put("majorCount", Optional.ofNullable(admissionFactMapper.dashboardMajorCount()).orElse(0));
        result.put("avgScore", Optional.ofNullable(admissionFactMapper.dashboardAvgScore()).orElse(0));
        result.put("provinceCount", Optional.ofNullable(
                admissionFactMapper.reportOverview(2024)).map(m -> ((Number) m.getOrDefault("provincecount", 0)).intValue()).orElse(0));

        // 趋势
        result.put("trend", admissionFactMapper.yearlyAdmissionCounts());

        // 专业分布
        List<Map<String, Object>> majors = new ArrayList<>();
        for (int year = 2020; year <= 2025; year++) {
            majors.addAll(admissionFactMapper.reportMajorDist(year));
        }
        Map<String, Integer> merged = new LinkedHashMap<>();
        for (Map<String, Object> m : majors) {
            String name = (String) m.get("name");
            int count = ((Number) m.get("count")).intValue();
            merged.merge(name, count, Integer::sum);
        }
        result.put("majorDistribution", merged.entrySet().stream()
                .map(e -> { Map<String, Object> item = new HashMap<>(); item.put("name", e.getKey()); item.put("count", e.getValue()); return item; })
                .collect(Collectors.toList()));

        // 今日上传（复用已有查询）
        result.put("todayUploads", 0);

        return result;
    }
}
