package edu.scau.scauarchiveinsight.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.scau.scauarchiveinsight.mapper.AdmissionFactMapper;
import edu.scau.scauarchiveinsight.mapper.CollegeDimMapper;
import edu.scau.scauarchiveinsight.mapper.MajorDimMapper;
import edu.scau.scauarchiveinsight.mapper.ProvinceDimMapper;
import edu.scau.scauarchiveinsight.pojo.AdmissionFact;
import edu.scau.scauarchiveinsight.pojo.MajorDim;
import edu.scau.scauarchiveinsight.pojo.ProvinceDim;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admission")
public class AdmissionController {

    @Autowired
    private AdmissionFactMapper admissionFactMapper;

    @Autowired
    private ProvinceDimMapper provinceDimMapper;

    @Autowired
    private MajorDimMapper majorDimMapper;

    @Autowired
    private CollegeDimMapper collegeDimMapper;

    @GetMapping("/page")
    public ResponseEntity<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String keyword) {
        Page<AdmissionFact> page = new Page<>(current, size);
        LambdaQueryWrapper<AdmissionFact> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(AdmissionFact::getStudentNo, keyword)
                   .or().like(AdmissionFact::getName, keyword)
                   .or().like(AdmissionFact::getIdCard, keyword)
                   .or().like(AdmissionFact::getExamNo, keyword);
        }
        wrapper.orderByDesc(AdmissionFact::getAdmissionDate);

        IPage<AdmissionFact> result = admissionFactMapper.selectPage(page, wrapper);
        List<Map<String, Object>> records = result.getRecords().stream().map(this::enrichRecord).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        data.put("pages", result.getPages());

        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 200);
        resp.put("data", data);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list() {
        List<AdmissionFact> list = admissionFactMapper.selectList(null);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 200);
        resp.put("data", list);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> add(@RequestBody AdmissionFact admissionFact) {
        admissionFact.setCreateTime(LocalDateTime.now());
        admissionFact.setUpdateTime(LocalDateTime.now());
        admissionFactMapper.insert(admissionFact);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 200);
        resp.put("msg", "添加成功");
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> update(@RequestBody AdmissionFact admissionFact) {
        admissionFact.setUpdateTime(LocalDateTime.now());
        admissionFactMapper.updateById(admissionFact);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 200);
        resp.put("msg", "更新成功");
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        admissionFactMapper.deleteById(id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 200);
        resp.put("msg", "删除成功");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/provinces")
    public ResponseEntity<Map<String, Object>> provinces() {
        List<ProvinceDim> list = provinceDimMapper.selectList(null);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 200);
        resp.put("data", list);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/majors")
    public ResponseEntity<Map<String, Object>> majors() {
        List<MajorDim> list = majorDimMapper.selectList(null);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 200);
        resp.put("data", list);
        return ResponseEntity.ok(resp);
    }

    private Map<String, Object> enrichRecord(AdmissionFact fact) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", fact.getId());
        map.put("studentNo", fact.getStudentNo());
        map.put("examNo", fact.getExamNo());
        map.put("name", fact.getName());
        map.put("idCard", fact.getIdCard());
        map.put("gender", fact.getGender());
        map.put("admissionDate", fact.getAdmissionDate());
        map.put("fileId", fact.getFileId());

        if (fact.getProvinceId() != null) {
            ProvinceDim p = provinceDimMapper.selectById(fact.getProvinceId());
            map.put("provinceId", fact.getProvinceId());
            map.put("provinceName", p != null ? p.getProvinceName() : null);
        }

        if (fact.getMajorId() != null) {
            MajorDim m = majorDimMapper.selectById(fact.getMajorId());
            map.put("majorId", fact.getMajorId());
            map.put("majorName", m != null ? m.getMajorName() : null);
        }

        return map;
    }
}
