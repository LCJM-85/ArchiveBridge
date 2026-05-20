package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/data")
    public R<Map<String, Object>> reportData(@RequestParam(defaultValue = "2024") int year) {
        return R.ok(reportService.getReportData(year));
    }
}
