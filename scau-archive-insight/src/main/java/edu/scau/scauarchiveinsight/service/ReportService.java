package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.mapper.AdmissionFactMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private AdmissionFactMapper admissionFactMapper;

    public Map<String, Object> getReportData(int year) {
        Map<String, Object> result = new HashMap<>();
        result.put("year", year);

        // 概览
        Map<String, Object> overview = Optional.ofNullable(admissionFactMapper.reportOverview(year))
                .orElse(new HashMap<>());
        int total = ((Number) overview.getOrDefault("total", 0)).intValue();

        // 性别
        List<Map<String, Object>> genderRaw = admissionFactMapper.reportGender(year);
        int maleCount = 0, femaleCount = 0;
        for (Map<String, Object> g : genderRaw) {
            String gender = (String) g.get("gender");
            int cnt = ((Number) g.get("count")).intValue();
            if ("男".equals(gender)) maleCount = cnt;
            else if ("女".equals(gender)) femaleCount = cnt;
        }
        overview.put("male", maleCount);
        overview.put("female", femaleCount);
        overview.put("malePct", total > 0 ? Math.round(maleCount * 1000.0 / total) / 10.0 : 0);
        overview.put("femalePct", total > 0 ? Math.round(femaleCount * 1000.0 / total) / 10.0 : 0);

        // 分数
        Map<String, Object> score = Optional.ofNullable(admissionFactMapper.reportScore(year))
                .orElse(new HashMap<>());

        // 专业分布
        List<Map<String, Object>> majorDist = admissionFactMapper.reportMajorDist(year);
        for (Map<String, Object> m : majorDist) {
            int cnt = ((Number) m.get("count")).intValue();
            m.put("pct", total > 0 ? Math.round(cnt * 1000.0 / total) / 10.0 : 0);
        }

        // 省份分布
        List<Map<String, Object>> provinceDist = admissionFactMapper.reportProvinceDist(year);
        for (Map<String, Object> p : provinceDist) {
            int cnt = ((Number) p.get("count")).intValue();
            p.put("pct", total > 0 ? Math.round(cnt * 1000.0 / total) / 10.0 : 0);
        }

        // 毕业去向（毕业年份 = 报告年份）
        List<Map<String, Object>> destination = admissionFactMapper.reportDestination(year);
        for (Map<String, Object> d : destination) {
            int cnt = ((Number) d.get("count")).intValue();
            d.put("pct", Math.round(cnt * 1000.0 / destination.stream().mapToInt(x -> ((Number) x.get("count")).intValue()).sum()) / 10.0);
        }

        result.put("overview", overview);
        result.put("score", score);
        result.put("majorDistribution", majorDist);
        result.put("provinceDistribution", provinceDist);
        result.put("destination", destination);
        return result;
    }
}
