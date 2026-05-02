package edu.scau.scauarchiveinsight.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import edu.scau.scauarchiveinsight.pojo.MetaDataStandard;
import edu.scau.scauarchiveinsight.service.MetaDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/metadata")
public class MetaDataController {

    @Autowired
    private MetaDataService metaDataService;

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", metaDataService.list());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/page")
    public ResponseEntity<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String keyword) {
        IPage<MetaDataStandard> page = metaDataService.page(current, size, keyword);
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

    @GetMapping("/get/{fieldCode}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String fieldCode) {
        Map<String, Object> result = new HashMap<>();
        MetaDataStandard item = metaDataService.getById(fieldCode);
        if (item != null) {
            result.put("code", 200);
            result.put("data", item);
        } else {
            result.put("code", 404);
            result.put("msg", "字段编码不存在");
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> add(@RequestBody MetaDataStandard metaDataStandard) {
        Map<String, Object> result = new HashMap<>();
        boolean saved = metaDataService.add(metaDataStandard);
        if (saved) {
            result.put("code", 200);
            result.put("msg", "添加成功");
            return ResponseEntity.ok(result);
        }
        result.put("code", 500);
        result.put("msg", "添加失败");
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> delete(@RequestParam String fieldCode) {
        Map<String, Object> result = new HashMap<>();
        boolean removed = metaDataService.delete(fieldCode);
        if (removed) {
            result.put("code", 200);
            result.put("msg", "删除成功");
            return ResponseEntity.ok(result);
        }
        result.put("code", 500);
        result.put("msg", "删除失败，字段编码不存在");
        return ResponseEntity.ok(result);
    }

    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> update(@RequestBody MetaDataStandard metaDataStandard) {
        Map<String, Object> result = new HashMap<>();
        boolean updated = metaDataService.update(metaDataStandard);
        if (updated) {
            result.put("code", 200);
            result.put("msg", "更新成功");
            return ResponseEntity.ok(result);
        }
        result.put("code", 500);
        result.put("msg", "更新失败，字段编码不存在");
        return ResponseEntity.ok(result);
    }
}
