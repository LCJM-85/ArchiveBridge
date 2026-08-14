package edu.scau.scauarchiveinsight.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.metadata.IPage;
import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.pojo.OCRLogDim;
import edu.scau.scauarchiveinsight.service.OCRLogService;
import edu.scau.scauarchiveinsight.service.OCRTaskManager;
import edu.scau.scauarchiveinsight.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "OCR日志", description = "OCR 处理日志管理")
@RestController
@RequestMapping("/api/ocr/log")
public class OCRLogController {

    @Autowired
    private OCRLogService ocrLogService;

    @Autowired
    private OCRTaskManager ocrTaskManager;

    @Autowired
    private StorageService storageService;

    @Operation(summary = "同步今日 OCR 日志")
    @PostMapping("/sync")
    public R<Void> sync() {
        ocrLogService.syncTodayLogs();
        return R.ok(null, "同步完成");
    }

    @Operation(summary = "获取今日 OCR 日志")
    @GetMapping("/today")
    public R<List<OCRLogDim>> today() {
        ocrLogService.syncTodayLogs();
        return R.ok(ocrLogService.getTodayLogs());
    }

    @Operation(summary = "分页查询历史日志")
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

    @Operation(summary = "删除 OCR 日志")
    @DeleteMapping("/delete/{logId}")
    public R<Void> delete(@PathVariable Integer logId) {
        ocrLogService.removeById(logId);
        return R.ok(null, "删除成功");
    }

    @Operation(summary = "取消正在处理的 OCR/LLM 任务")
    @PostMapping("/{logId}/cancel")
    public R<Void> cancel(@PathVariable Integer logId) {
        if (!ocrLogService.markCancelled(logId)) {
            return R.error(400, "任务不存在或已结束");
        }
        ocrTaskManager.cancel(logId);
        OCRLogDim log = ocrLogService.getById(logId);
        if (log != null && log.getFileName() != null) {
            try { storageService.failedFile(log.getFileName(), "用户已取消"); } catch (Exception ignored) {}
        }
        return R.ok(null, "已取消");
    }
}
