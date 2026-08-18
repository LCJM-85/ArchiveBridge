package edu.scau.scauarchiveinsight.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.scau.scauarchiveinsight.mapper.ClassDimMapper;
import edu.scau.scauarchiveinsight.mapper.MajorDimMapper;
import edu.scau.scauarchiveinsight.mapper.StudentFactMapper;
import edu.scau.scauarchiveinsight.pojo.ClassDim;
import edu.scau.scauarchiveinsight.pojo.MajorDim;
import edu.scau.scauarchiveinsight.pojo.StudentFact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClassService {

    @Autowired
    private ClassDimMapper classDimMapper;

    @Autowired
    private MajorDimMapper majorDimMapper;

    @Autowired
    private StudentFactMapper studentFactMapper;

    @Autowired
    private CacheService cacheService;

    public List<Map<String, Object>> list(String keyword) {
        boolean cacheable = !StringUtils.hasText(keyword);
        if (cacheable) {
            List<Map<String, Object>> cached = cacheService.get(CacheService.CLASS_KEY, new TypeReference<>() {});
            if (cached != null) return cached;
        }
        LambdaQueryWrapper<ClassDim> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ClassDim::getClassName, keyword).or().like(ClassDim::getGrade, keyword);
        }
        wrapper.orderByAsc(ClassDim::getClassId);
        List<ClassDim> classes = classDimMapper.selectList(wrapper);
        Map<Integer, String> majorMap = majorDimMapper.selectList(null).stream()
                .collect(Collectors.toMap(MajorDim::getMajorId, MajorDim::getMajorName, (a, b) -> a));
        List<Map<String, Object>> result = classes.stream().map(c -> {
            Map<String, Object> row = new HashMap<>();
            row.put("classId", c.getClassId());
            row.put("majorId", c.getMajorId());
            row.put("majorName", majorMap.get(c.getMajorId()));
            row.put("className", c.getClassName());
            row.put("grade", c.getGrade());
            row.put("studyLength", c.getStudyLength());
            return row;
        }).collect(Collectors.toList());
        if (cacheable) cacheService.put(CacheService.CLASS_KEY, result, CacheService.DIMENSION_TTL);
        return result;
    }

    public boolean add(ClassDim dim) {
        if (dim.getMajorId() == null) return false;
        boolean success = classDimMapper.insert(dim) > 0;
        if (success) evictAfterWrite();
        return success;
    }

    public boolean update(ClassDim dim) {
        boolean success = classDimMapper.updateById(dim) > 0;
        if (success) evictAfterWrite();
        return success;
    }

    public boolean delete(Integer id) {
        Long ref = studentFactMapper.selectCount(new LambdaQueryWrapper<StudentFact>().eq(StudentFact::getClassId, id));
        if (ref != null && ref > 0) throw new IllegalStateException("该班级下存在学籍数据，请先调整学生班级");
        boolean success = classDimMapper.deleteById(id) > 0;
        if (success) evictAfterWrite();
        return success;
    }

    private void evictAfterWrite() {
        cacheService.evict(CacheService.CLASS_KEY, CacheService.CLASS_DROPDOWN_KEY);
        cacheService.evictDashboard();
    }
}
