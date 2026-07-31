package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.pojo.MajorDim;
import edu.scau.scauarchiveinsight.service.MajorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "专业管理", description = "专业维度管理")
@RestController
@RequestMapping("/api/major")
public class MajorController {

    @Autowired
    private MajorService majorService;

    @Operation(summary = "获取专业列表")
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list(@RequestParam(required = false) String keyword) {
        return R.ok(majorService.list(keyword));
    }

    @Operation(summary = "添加专业")
    @PostMapping("/add")
    public R<Void> add(@RequestBody MajorDim dim) {
        return majorService.add(dim) ? R.ok(null, "添加成功") : R.error("添加失败：请选择所属学院");
    }

    @Operation(summary = "更新专业")
    @PutMapping("/update")
    public R<Void> update(@RequestBody MajorDim dim) {
        return majorService.update(dim) ? R.ok(null, "更新成功") : R.error("更新失败");
    }

    @Operation(summary = "删除专业")
    @DeleteMapping("/delete/{id}")
    public R<Void> delete(@PathVariable Integer id) {
        try {
            return majorService.delete(id) ? R.ok(null, "删除成功") : R.error("删除失败");
        } catch (Exception e) {
            return R.error(e.getMessage() != null ? e.getMessage() : "删除失败");
        }
    }
}
