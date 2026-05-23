package edu.scau.scauarchiveinsight.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "智能报告", description = "年度报告数据生成")
@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Operation(summary = "获取年度报告数据")
    @GetMapping("/data")
    public R<Map<String, Object>> reportData(@RequestParam(defaultValue = "2024") int year) {
        return R.ok(reportService.getReportData(year));
    }
}
