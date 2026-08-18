package edu.scau.scauarchiveinsight.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.scau.scauarchiveinsight.mapper.AdmissionFactMapper;
import edu.scau.scauarchiveinsight.mapper.ClassDimMapper;
import edu.scau.scauarchiveinsight.mapper.CollegeDimMapper;
import edu.scau.scauarchiveinsight.mapper.DegreeDimMapper;
import edu.scau.scauarchiveinsight.mapper.MajorDimMapper;
import edu.scau.scauarchiveinsight.mapper.StudentFactMapper;
import edu.scau.scauarchiveinsight.pojo.AdmissionFact;
import edu.scau.scauarchiveinsight.pojo.ClassDim;
import edu.scau.scauarchiveinsight.pojo.CollegeDim;
import edu.scau.scauarchiveinsight.pojo.DegreeDim;
import edu.scau.scauarchiveinsight.pojo.MajorDim;
import edu.scau.scauarchiveinsight.pojo.StudentFact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class MajorService {

    @Autowired
    private MajorDimMapper majorDimMapper;

    @Autowired
    private CollegeDimMapper collegeDimMapper;

    @Autowired
    private DegreeDimMapper degreeDimMapper;

    @Autowired
    private ClassDimMapper classDimMapper;

    @Autowired
    private StudentFactMapper studentFactMapper;

    @Autowired
    private AdmissionFactMapper admissionFactMapper;

    @Autowired
    private CacheService cacheService;

    public List<Map<String, Object>> list(String keyword) {
        boolean cacheable = !StringUtils.hasText(keyword);
        if (cacheable) {
            List<Map<String, Object>> cached = cacheService.get(CacheService.MAJOR_KEY, new TypeReference<>() {});
            if (cached != null) return cached;
        }
        LambdaQueryWrapper<MajorDim> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(MajorDim::getMajorName, keyword).or().like(MajorDim::getMajorCode, keyword);
        }
        wrapper.orderByAsc(MajorDim::getMajorId);
        List<MajorDim> majors = majorDimMapper.selectList(wrapper);
        Map<Integer, String> collegeMap = collegeDimMapper.selectList(null).stream()
                .collect(Collectors.toMap(CollegeDim::getCollegeId, CollegeDim::getCollegeName, (a, b) -> a));
        Map<Integer, String> degreeMap = degreeDimMapper.selectList(null).stream()
                .collect(Collectors.toMap(DegreeDim::getDegreeId, DegreeDim::getDegreeName, (a, b) -> a));
        List<Map<String, Object>> result = majors.stream().map(m -> {
            Map<String, Object> row = new HashMap<>();
            row.put("majorId", m.getMajorId());
            row.put("collegeId", m.getCollegeId());
            row.put("collegeName", collegeMap.get(m.getCollegeId()));
            row.put("degreeId", m.getDegreeId());
            row.put("degreeName", degreeMap.get(m.getDegreeId()));
            row.put("majorName", m.getMajorName());
            row.put("majorCode", m.getMajorCode());
            return row;
        }).collect(Collectors.toList());
        if (cacheable) cacheService.put(CacheService.MAJOR_KEY, result, CacheService.DIMENSION_TTL);
        return result;
    }

    public boolean add(MajorDim dim) {
        if (dim.getCollegeId() == null) return false;
        boolean success = majorDimMapper.insert(dim) > 0;
        if (success) evictAfterWrite();
        return success;
    }

    public boolean update(MajorDim dim) {
        boolean success = majorDimMapper.updateById(dim) > 0;
        if (success) evictAfterWrite();
        return success;
    }

    public boolean delete(Integer id) {
        Long classRef = classDimMapper.selectCount(new LambdaQueryWrapper<ClassDim>().eq(ClassDim::getMajorId, id));
        if (classRef != null && classRef > 0) throw new IllegalStateException("该专业下存在班级，请先删除或迁移班级");
        Long stuRef = studentFactMapper.selectCount(new LambdaQueryWrapper<StudentFact>().eq(StudentFact::getMajorId, id));
        if (stuRef != null && stuRef > 0) throw new IllegalStateException("该专业下存在学籍数据，请先调整学生专业");
        Long admRef = admissionFactMapper.selectCount(new LambdaQueryWrapper<AdmissionFact>().eq(AdmissionFact::getMajorId, id));
        if (admRef != null && admRef > 0) throw new IllegalStateException("该专业下存在招生记录，请先调整");
        boolean success = majorDimMapper.deleteById(id) > 0;
        if (success) evictAfterWrite();
        return success;
    }

    private void evictAfterWrite() {
        // Class 列表缓存内嵌专业名称，专业变化时必须级联失效。
        cacheService.evict(CacheService.MAJOR_KEY, CacheService.MAJOR_DROPDOWN_KEY,
                CacheService.CLASS_KEY, CacheService.CLASS_DROPDOWN_KEY);
        cacheService.evictDashboard();
    }
}
