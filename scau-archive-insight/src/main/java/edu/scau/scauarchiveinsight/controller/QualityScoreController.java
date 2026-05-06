package edu.scau.scauarchiveinsight.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.mapper.QualityScoreDimMapper;
import edu.scau.scauarchiveinsight.pojo.QualityScoreDim;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/quality-score")
public class QualityScoreController {

    @Autowired
    private QualityScoreDimMapper qualityScoreDimMapper;

    /**
     * 批量查询质量评分
     * @param fileIds 逗号分隔的 fileId 列表
     */
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
