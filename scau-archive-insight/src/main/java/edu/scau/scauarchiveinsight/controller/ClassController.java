package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.pojo.ClassDim;
import edu.scau.scauarchiveinsight.service.ClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "班级管理", description = "班级维度管理")
@RestController
@RequestMapping("/api/class")
public class ClassController {

    @Autowired
    private ClassService classService;

    @Operation(summary = "获取班级列表")
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list(@RequestParam(required = false) String keyword) {
        return R.ok(classService.list(keyword));
    }

    @Operation(summary = "添加班级")
    @PostMapping("/add")
    public R<Void> add(@RequestBody ClassDim dim) {
        return classService.add(dim) ? R.ok(null, "添加成功") : R.error("添加失败：请选择所属专业");
    }

    @Operation(summary = "更新班级")
    @PutMapping("/update")
    public R<Void> update(@RequestBody ClassDim dim) {
        return classService.update(dim) ? R.ok(null, "更新成功") : R.error("更新失败");
    }

    @Operation(summary = "删除班级")
    @DeleteMapping("/delete/{id}")
    public R<Void> delete(@PathVariable Integer id) {
        try {
            return classService.delete(id) ? R.ok(null, "删除成功") : R.error("删除失败");
        } catch (Exception e) {
            return R.error(e.getMessage() != null ? e.getMessage() : "删除失败");
        }
    }
}
