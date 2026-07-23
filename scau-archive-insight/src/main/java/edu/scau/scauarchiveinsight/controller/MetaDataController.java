package edu.scau.scauarchiveinsight.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.metadata.IPage;
import edu.scau.scauarchiveinsight.dto.MetaDataDTO;
import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.pojo.MetaDataStandard;
import edu.scau.scauarchiveinsight.service.MetaDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "元数据管理", description = "元数据标准管理")
@RestController
@RequestMapping("/api/metadata")
public class MetaDataController {

    @Autowired
    private MetaDataService metaDataService;

    @Operation(summary = "获取元数据列表")
    @GetMapping("/list")
    public R<List<MetaDataStandard>> list() {
        return R.ok(metaDataService.list());
    }

    @Operation(summary = "分页查询元数据")
    @GetMapping("/page")
    public R<Map<String, Object>> page(
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
        return R.ok(data);
    }

    @Operation(summary = "根据字段编码获取元数据")
    @GetMapping("/get/{fieldCode}")
    public R<MetaDataStandard> getByFieldCode(@PathVariable String fieldCode) {
        MetaDataStandard item = metaDataService.getByFieldCode(fieldCode);
        if (item == null) {
            return R.error("字段编码不存在");
        }
        return R.ok(item);
    }

    @Operation(summary = "添加元数据")
    @PostMapping("/add")
    public R<Void> add(@RequestBody MetaDataDTO dto) {
        MetaDataStandard entity = new MetaDataStandard();
        entity.setFieldCode(dto.getFieldCode());
        entity.setFieldName(dto.getFieldName());
        entity.setFieldType(dto.getFieldType());
        entity.setSourceField(dto.getSourceField());
        entity.setTransformType(dto.getTransformType());
        entity.setTransformRule(dto.getTransformRule());
        entity.setIsRequired(dto.getIsRequired());
        boolean saved = metaDataService.add(entity);
        return saved ? R.ok(null, "添加成功") : R.error("添加失败");
    }

    @Operation(summary = "更新元数据")
    @PutMapping("/update")
    public R<Void> update(@RequestBody MetaDataDTO dto) {
        MetaDataStandard entity = new MetaDataStandard();
        entity.setMetadataId(dto.getMetadataId());
        entity.setFieldCode(dto.getFieldCode());
        entity.setFieldName(dto.getFieldName());
        entity.setFieldType(dto.getFieldType());
        entity.setSourceField(dto.getSourceField());
        entity.setTransformType(dto.getTransformType());
        entity.setTransformRule(dto.getTransformRule());
        entity.setIsRequired(dto.getIsRequired());
        boolean updated = metaDataService.update(entity);
        return updated ? R.ok(null, "更新成功") : R.error("更新失败");
    }

    @Operation(summary = "删除元数据")
    @DeleteMapping("/delete")
    public R<Void> delete(@RequestParam Integer metadataId) {
        boolean removed = metaDataService.delete(metadataId);
        return removed ? R.ok(null, "删除成功") : R.error("删除失败");
    }
}
