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

    public List<Map<String, Object>> list(String keyword) {
        LambdaQueryWrapper<ClassDim> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ClassDim::getClassName, keyword).or().like(ClassDim::getGrade, keyword);
        }
        wrapper.orderByAsc(ClassDim::getClassId);
        List<ClassDim> classes = classDimMapper.selectList(wrapper);
        Map<Integer, String> majorMap = majorDimMapper.selectList(null).stream()
                .collect(Collectors.toMap(MajorDim::getMajorId, MajorDim::getMajorName, (a, b) -> a));
        return classes.stream().map(c -> {
            Map<String, Object> row = new HashMap<>();
            row.put("classId", c.getClassId());
            row.put("majorId", c.getMajorId());
            row.put("majorName", majorMap.get(c.getMajorId()));
            row.put("className", c.getClassName());
            row.put("grade", c.getGrade());
            row.put("studyLength", c.getStudyLength());
            return row;
        }).collect(Collectors.toList());
    }

    public boolean add(ClassDim dim) {
        if (dim.getMajorId() == null) return false;
        return classDimMapper.insert(dim) > 0;
    }

    public boolean update(ClassDim dim) {
        return classDimMapper.updateById(dim) > 0;
    }

    public boolean delete(Integer id) {
        Long ref = studentFactMapper.selectCount(new LambdaQueryWrapper<StudentFact>().eq(StudentFact::getClassId, id));
        if (ref != null && ref > 0) throw new IllegalStateException("该班级下存在学籍数据，请先调整学生班级");
        return classDimMapper.deleteById(id) > 0;
    }
}
