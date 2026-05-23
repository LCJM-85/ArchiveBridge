package edu.scau.scauarchiveinsight.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.mapper.QualityScoreDimMapper;
import edu.scau.scauarchiveinsight.pojo.QualityScoreDim;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "质量评分", description = "数据质量评分查询")
@RestController
@RequestMapping("/api/quality-score")
public class QualityScoreController {

    @Autowired
    private QualityScoreDimMapper qualityScoreDimMapper;

    /**
     * 批量查询质量评分
     * @param fileIds 逗号分隔的 fileId 列表
     */
    @Operation(summary = "批量查询文件质量评分")
    @GetMapping("/list")
    public R<Map<Integer, QualityScoreDim>> list(@RequestParam("fileIds") String fileIds) {
        if (fileIds == null || fileIds.isBlank()) {
            return R.ok(Map.of());
        }
        List<Integer> ids = List.of(fileIds.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return R.ok(Map.of());
        }
        List<QualityScoreDim> list = qualityScoreDimMapper.selectList(
                Wrappers.<QualityScoreDim>lambdaQuery().in(QualityScoreDim::getFileId, ids));
        Map<Integer, QualityScoreDim> map = list.stream()
                .collect(Collectors.toMap(QualityScoreDim::getFileId, s -> s));
        return R.ok(map);
    }
}
