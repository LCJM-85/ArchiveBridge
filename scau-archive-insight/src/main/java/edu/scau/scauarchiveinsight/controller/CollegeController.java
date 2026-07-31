package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.pojo.CollegeDim;
import edu.scau.scauarchiveinsight.service.CollegeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "学院管理", description = "学院维度管理")
@RestController
@RequestMapping("/api/college")
public class CollegeController {

    @Autowired
    private CollegeService collegeService;

    @Operation(summary = "获取学院列表")
    @GetMapping("/list")
    public R<List<CollegeDim>> list(@RequestParam(required = false) String keyword) {
        return R.ok(collegeService.list(keyword));
    }

    @Operation(summary = "添加学院")
    @PostMapping("/add")
    public R<Void> add(@RequestBody CollegeDim dim) {
        return collegeService.add(dim) ? R.ok(null, "添加成功") : R.error("添加失败");
    }

    @Operation(summary = "更新学院")
    @PutMapping("/update")
    public R<Void> update(@RequestBody CollegeDim dim) {
        return collegeService.update(dim) ? R.ok(null, "更新成功") : R.error("更新失败");
    }

    @Operation(summary = "删除学院")
    @DeleteMapping("/delete/{id}")
    public R<Void> delete(@PathVariable Integer id) {
        try {
            return collegeService.delete(id) ? R.ok(null, "删除成功") : R.error("删除失败");
        } catch (Exception e) {
            return R.error(e.getMessage() != null ? e.getMessage() : "删除失败");
        }
    }
}
