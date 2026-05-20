package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.dto.AdmissionDTO;
import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.service.AdmissionService;
import edu.scau.scauarchiveinsight.service.GeographicService;
import edu.scau.scauarchiveinsight.service.PredictionService;
import edu.scau.scauarchiveinsight.service.TrainingPathService;
import edu.scau.scauarchiveinsight.service.TrendAnalysisService;
import edu.scau.scauarchiveinsight.vo.AdmissionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admission")
public class AdmissionController {

    @Autowired
    private AdmissionService admissionService;

    @Autowired
    private TrendAnalysisService trendAnalysisService;

    @Autowired
    private GeographicService geographicService;

    @Autowired
    private TrainingPathService trainingPathService;

    @Autowired
    private PredictionService predictionService;

    @GetMapping("/page")
    public R<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String createTimeStart,
            @RequestParam(required = false) String createTimeEnd,
            @RequestParam(required = false) String updateTimeStart,
            @RequestParam(required = false) String updateTimeEnd) {
        var page = admissionService.page(current, size, keyword, createTimeStart, createTimeEnd, updateTimeStart, updateTimeEnd);
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        data.put("pages", page.getPages());
        return R.ok(data);
    }

    @PostMapping("/add")
    public R<Void> add(@RequestBody AdmissionDTO dto) {
        admissionService.add(dto);
        return R.ok(null, "添加成功");
    }

    @PutMapping("/update")
    public R<Void> update(@RequestBody AdmissionDTO dto) {
        admissionService.update(dto);
        return R.ok(null, "更新成功");
    }

    @DeleteMapping("/delete/{id}")
    public R<Void> delete(@PathVariable Long id) {
        admissionService.delete(id);
        return R.ok(null, "删除成功");
    }

    @GetMapping("/provinces")
    public R<?> provinces() {
        return R.ok(admissionService.listProvinces());
    }

    @GetMapping("/majors")
    public R<?> majors() {
        return R.ok(admissionService.listMajors());
    }

    @GetMapping("/trend/yearly")
    public R<List<Map<String, Object>>> yearlyTrend(
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear) {
        if (startYear != null && endYear != null && startYear > endYear) {
            return R.error("startYear must not be greater than endYear");
        }
        return R.ok(trendAnalysisService.yearlyTrend(startYear, endYear));
    }

    @GetMapping("/trend/major")
    public R<List<Map<String, Object>>> majorTrend(
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear) {
        if (startYear != null && endYear != null && startYear > endYear) {
            return R.error("startYear must not be greater than endYear");
        }
        return R.ok(trendAnalysisService.majorTrend(startYear, endYear));
    }

    @GetMapping("/trend/province")
    public R<List<Map<String, Object>>> provinceTrend(
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear) {
        if (startYear != null && endYear != null && startYear > endYear) {
            return R.error("startYear must not be greater than endYear");
        }
        return R.ok(trendAnalysisService.provinceTrend(startYear, endYear));
    }

    @GetMapping("/trend/score")
    public R<List<Map<String, Object>>> scoreTrend(
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear) {
        if (startYear != null && endYear != null && startYear > endYear) {
            return R.error("startYear must not be greater than endYear");
        }
        return R.ok(trendAnalysisService.scoreTrend(startYear, endYear));
    }

    @GetMapping("/trend/gender")
    public R<List<Map<String, Object>>> genderTrend(
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear) {
        if (startYear != null && endYear != null && startYear > endYear) {
            return R.error("startYear must not be greater than endYear");
        }
        return R.ok(trendAnalysisService.genderTrend(startYear, endYear));
    }

    @GetMapping("/geo/province-stats")
    public R<List<Map<String, Object>>> provinceStats() {
        return R.ok(geographicService.provinceStats());
    }

    @GetMapping("/geo/map-data")
    public R<Map<String, Object>> provinceMap() {
        return R.ok(geographicService.provinceMapGeoJson());
    }

    @GetMapping("/training-path/sankey")
    public R<Map<String, Object>> sankey() {
        return R.ok(trainingPathService.sankeyData());
    }

    @GetMapping("/predict/next-years")
    public R<Map<String, Object>> predict(@RequestParam(defaultValue = "3") int years) {
        try {
            return R.ok(predictionService.predictNextYears(years));
        } catch (Exception e) {
            return R.error("预测失败: " + e.getMessage());
        }
    }
}
