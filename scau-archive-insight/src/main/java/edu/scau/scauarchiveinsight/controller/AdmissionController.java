package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.dto.AdmissionDTO;
import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.service.AdmissionService;
import edu.scau.scauarchiveinsight.service.GeographicService;
import edu.scau.scauarchiveinsight.service.PredictionService;
import edu.scau.scauarchiveinsight.service.TrainingPathService;
import edu.scau.scauarchiveinsight.service.TrendAnalysisService;
import edu.scau.scauarchiveinsight.vo.AdmissionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "招生录取", description = "招生录取管理，含趋势分析、地理分布、预测等")
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

    @Operation(summary = "分页查询招生录取数据")
    @GetMapping("/page")
    public R<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String createTimeStart,
            @RequestParam(required = false) String createTimeEnd,
            @RequestParam(required = false) String updateTimeStart,
            @RequestParam(required = false) String updateTimeEnd,
            @RequestParam(required = false) Integer degreeId) {
        var page = admissionService.page(current, size, keyword, createTimeStart, createTimeEnd, updateTimeStart, updateTimeEnd, degreeId);
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        data.put("pages", page.getPages());
        return R.ok(data);
    }

    @Operation(summary = "添加招生录取记录")
    @PostMapping("/add")
    public R<Void> add(@RequestBody AdmissionDTO dto) {
        admissionService.add(dto);
        return R.ok(null, "添加成功");
    }

    @Operation(summary = "更新招生录取记录")
    @PutMapping("/update")
    public R<Void> update(@RequestBody AdmissionDTO dto) {
        admissionService.update(dto);
        return R.ok(null, "更新成功");
    }

    @Operation(summary = "删除招生录取记录")
    @DeleteMapping("/delete/{id}")
    public R<Void> delete(@PathVariable Long id) {
        admissionService.delete(id);
        return R.ok(null, "删除成功");
    }

    @Operation(summary = "获取省份列表")
    @GetMapping("/provinces")
    public R<?> provinces() {
        return R.ok(admissionService.listProvinces());
    }

    @Operation(summary = "获取专业列表")
    @GetMapping("/majors")
    public R<?> majors() {
        return R.ok(admissionService.listMajors());
    }

    @Operation(summary = "获取学历层次列表")
    @GetMapping("/degrees")
    public R<?> degrees() {
        return R.ok(admissionService.listDegrees());
    }

    @Operation(summary = "年度录取趋势分析")
    @GetMapping("/trend/yearly")
    public R<List<Map<String, Object>>> yearlyTrend(
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear,
            @RequestParam(required = false) String degreeName) {
        if (startYear != null && endYear != null && startYear > endYear) {
            return R.error("startYear must not be greater than endYear");
        }
        return R.ok(trendAnalysisService.yearlyTrend(startYear, endYear, degreeName));
    }

    @Operation(summary = "专业录取趋势分析")
    @GetMapping("/trend/major")
    public R<List<Map<String, Object>>> majorTrend(
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear,
            @RequestParam(required = false) String degreeName) {
        if (startYear != null && endYear != null && startYear > endYear) {
            return R.error("startYear must not be greater than endYear");
        }
        return R.ok(trendAnalysisService.majorTrend(startYear, endYear, degreeName));
    }

    @Operation(summary = "省份录取趋势分析")
    @GetMapping("/trend/province")
    public R<List<Map<String, Object>>> provinceTrend(
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear,
            @RequestParam(required = false) String degreeName) {
        if (startYear != null && endYear != null && startYear > endYear) {
            return R.error("startYear must not be greater than endYear");
        }
        return R.ok(trendAnalysisService.provinceTrend(startYear, endYear, degreeName));
    }

    @Operation(summary = "分数录取趋势分析")
    @GetMapping("/trend/score")
    public R<List<Map<String, Object>>> scoreTrend(
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear,
            @RequestParam(required = false) String degreeName) {
        if (startYear != null && endYear != null && startYear > endYear) {
            return R.error("startYear must not be greater than endYear");
        }
        return R.ok(trendAnalysisService.scoreTrend(startYear, endYear, degreeName));
    }

    @Operation(summary = "性别录取趋势分析")
    @GetMapping("/trend/gender")
    public R<List<Map<String, Object>>> genderTrend(
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear,
            @RequestParam(required = false) String degreeName) {
        if (startYear != null && endYear != null && startYear > endYear) {
            return R.error("startYear must not be greater than endYear");
        }
        return R.ok(trendAnalysisService.genderTrend(startYear, endYear, degreeName));
    }

    @Operation(summary = "省份录取统计")
    @GetMapping("/geo/province-stats")
    public R<List<Map<String, Object>>> provinceStats() {
        return R.ok(geographicService.provinceStats());
    }

    @Operation(summary = "获取地理地图数据")
    @GetMapping("/geo/map-data")
    public R<Map<String, Object>> provinceMap() {
        return R.ok(geographicService.provinceMapGeoJson());
    }

    @Operation(summary = "获取升学路径桑基图数据")
    @GetMapping("/training-path/sankey")
    public R<Map<String, Object>> sankey() {
        return R.ok(trainingPathService.sankeyData());
    }

    @Operation(summary = "预测未来录取人数")
    @GetMapping("/predict/next-years")
    public R<Map<String, Object>> predict(@RequestParam(defaultValue = "3") int years,
                                          @RequestParam(required = false) String degreeName) {
        try {
            return R.ok(predictionService.predictNextYears(years, degreeName));
        } catch (Exception e) {
            return R.error("预测失败: " + e.getMessage());
        }
    }
}
