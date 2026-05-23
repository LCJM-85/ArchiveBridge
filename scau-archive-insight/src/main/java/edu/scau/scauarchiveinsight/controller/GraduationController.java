package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.dto.GraduationDTO;
import edu.scau.scauarchiveinsight.dto.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import edu.scau.scauarchiveinsight.service.GraduationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "毕业数据", description = "毕业数据管理")
@RestController
@RequestMapping("/api/graduation")
public class GraduationController {

    @Autowired
    private GraduationService graduationService;

    @Operation(summary = "分页查询毕业数据")
    @GetMapping("/page")
    public R<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String createTimeStart,
            @RequestParam(required = false) String createTimeEnd,
            @RequestParam(required = false) String updateTimeStart,
            @RequestParam(required = false) String updateTimeEnd) {
        var page = graduationService.page(current, size, keyword, createTimeStart, createTimeEnd, updateTimeStart, updateTimeEnd);
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        data.put("pages", page.getPages());
        return R.ok(data);
    }

    @Operation(summary = "添加毕业记录")
    @PostMapping("/add")
    public R<Void> add(@RequestBody GraduationDTO dto) {
        graduationService.add(dto);
        return R.ok(null, "添加成功");
    }

    @Operation(summary = "更新毕业记录")
    @PutMapping("/update")
    public R<Void> update(@RequestBody GraduationDTO dto) {
        graduationService.update(dto);
        return R.ok(null, "更新成功");
    }

    @Operation(summary = "删除毕业记录")
    @DeleteMapping("/delete/{id}")
    public R<Void> delete(@PathVariable Long id) {
        graduationService.delete(id);
        return R.ok(null, "删除成功");
    }

    @Operation(summary = "获取学历列表")
    @GetMapping("/degrees")
    public R<?> degrees() {
        return R.ok(graduationService.listDegrees());
    }

    @Operation(summary = "获取毕业去向列表")
    @GetMapping("/destinations")
    public R<?> destinations() {
        return R.ok(graduationService.listDestinations());
    }
}
