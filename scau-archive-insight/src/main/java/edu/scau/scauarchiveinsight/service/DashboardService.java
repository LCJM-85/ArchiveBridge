package edu.scau.scauarchiveinsight.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.scau.scauarchiveinsight.mapper.AdmissionFactMapper;
import edu.scau.scauarchiveinsight.mapper.ArchiveFileDimMapper;
import edu.scau.scauarchiveinsight.mapper.OCRLogDimMapper;
import edu.scau.scauarchiveinsight.mapper.QualityScoreDimMapper;
import edu.scau.scauarchiveinsight.pojo.OCRLogDim;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DashboardService {

    @Autowired
    private AdmissionFactMapper admissionFactMapper;

    @Autowired
    private ArchiveFileDimMapper archiveFileDimMapper;

    @Autowired
    private OCRLogDimMapper ocrLogDimMapper;

    @Autowired
    private QualityScoreDimMapper qualityScoreDimMapper;

    private static final Path STORAGE_ROOT = Paths.get(System.getProperty("user.dir"), "storage");

    public Map<String, Object> getStats() {
        Map<String, Object> result = new HashMap<>();

        result.put("totalAdmissions", Optional.ofNullable(admissionFactMapper.dashboardTotalAdmissions()).orElse(0));
        result.put("totalGraduates", Optional.ofNullable(admissionFactMapper.dashboardTotalGraduates()).orElse(0));
        result.put("majorCount", Optional.ofNullable(admissionFactMapper.dashboardMajorCount()).orElse(0));
        result.put("avgScore", Optional.ofNullable(admissionFactMapper.dashboardAvgScore()).orElse(0));

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

        // 系统概览：档案总存储（统计 storage/archive/ 下实际归档的文件数）
        result.put("totalFiles", countArchiveFiles());

        // 系统概览：今日上传
        LocalDate today = LocalDate.now();
        long todayUploads = ocrLogDimMapper.selectCount(
                new LambdaQueryWrapper<OCRLogDim>().apply("recognize_time::date = {0}", today));
        result.put("todayUploads", todayUploads);

        // 系统概览：数据质量（平均总分）
        result.put("avgQuality", Optional.ofNullable(qualityScoreDimMapper.selectAvgTotalScore()).orElse(0.0));

        return result;
    }

    private long countArchiveFiles() {
        Path archiveDir = STORAGE_ROOT.resolve("archive");
        if (!Files.isDirectory(archiveDir)) return 0L;

        try (Stream<Path> stream = Files.walk(archiveDir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().endsWith(".error.json"))
                    .filter(p -> !p.getFileName().toString().endsWith(".warn.json"))
                    .count();
        } catch (IOException e) {
            return 0L;
        }
    }
}
