package edu.scau.scauarchiveinsight.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.pojo.OCRLogDim;
import edu.scau.scauarchiveinsight.service.OCRLogService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public R<Void> sync() {
        ocrLogService.syncTodayLogs();
        return R.ok(null, "同步完成");
    }

    @GetMapping("/today")
    public R<List<OCRLogDim>> today() {
        ocrLogService.syncTodayLogs();
        return R.ok(ocrLogService.getTodayLogs());
    }

    @GetMapping("/history")
    public R<Map<String, Object>> history(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "15") int size) {
        IPage<OCRLogDim> page = ocrLogService.getHistory(current, size);
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        data.put("pages", page.getPages());
        return R.ok(data);
    }

    @DeleteMapping("/delete/{logId}")
    public R<Void> delete(@PathVariable Integer logId) {
        ocrLogService.removeById(logId);
        return R.ok(null, "删除成功");
    }
}
