package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        return R.ok(dashboardService.getStats());
    }
}
