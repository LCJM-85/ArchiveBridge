package edu.scau.scauarchiveinsight.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import edu.scau.scauarchiveinsight.pojo.OCRLogDim;
import edu.scau.scauarchiveinsight.service.OCRLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ocr/log")
public class OCRLogController {

    @Autowired
    private OCRLogService ocrLogService;

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> sync() {
        ocrLogService.syncTodayLogs();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "同步完成");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> today() {
        ocrLogService.syncTodayLogs();
        List<OCRLogDim> logs = ocrLogService.getTodayLogs();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", logs);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> history(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "15") int size) {
        IPage<OCRLogDim> page = ocrLogService.getHistory(current, size);
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        data.put("pages", page.getPages());

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", data);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/delete/{logId}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Integer logId) {
        ocrLogService.removeById(logId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "删除成功");
        return ResponseEntity.ok(result);
    }
}
