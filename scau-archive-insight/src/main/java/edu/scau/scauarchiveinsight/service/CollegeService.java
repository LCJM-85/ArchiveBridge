package edu.scau.scauarchiveinsight.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.scau.scauarchiveinsight.mapper.CollegeDimMapper;
import edu.scau.scauarchiveinsight.mapper.MajorDimMapper;
import edu.scau.scauarchiveinsight.pojo.CollegeDim;
import edu.scau.scauarchiveinsight.pojo.MajorDim;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

@Service
public class CollegeService {

    @Autowired
    private CollegeDimMapper collegeDimMapper;

    @Autowired
    private MajorDimMapper majorDimMapper;

    @Autowired
    private CacheService cacheService;

    public List<CollegeDim> list(String keyword) {
        boolean cacheable = !StringUtils.hasText(keyword);
        if (cacheable) {
            List<CollegeDim> cached = cacheService.get(CacheService.COLLEGE_KEY, new TypeReference<>() {});
            if (cached != null) return cached;
        }
        LambdaQueryWrapper<CollegeDim> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(CollegeDim::getCollegeName, keyword);
        }
        wrapper.orderByAsc(CollegeDim::getCollegeId);
        List<CollegeDim> result = collegeDimMapper.selectList(wrapper);
        if (cacheable) cacheService.put(CacheService.COLLEGE_KEY, result, CacheService.DIMENSION_TTL);
        return result;
    }

    public boolean add(CollegeDim dim) {
        boolean success = collegeDimMapper.insert(dim) > 0;
        if (success) evictAfterWrite();
        return success;
    }

    public boolean update(CollegeDim dim) {
        boolean success = collegeDimMapper.updateById(dim) > 0;
        if (success) evictAfterWrite();
        return success;
    }

    public boolean delete(Integer id) {
        Long ref = majorDimMapper.selectCount(new LambdaQueryWrapper<MajorDim>().eq(MajorDim::getCollegeId, id));
        if (ref != null && ref > 0) throw new IllegalStateException("该学院下存在专业，请先删除或迁移专业");
        boolean success = collegeDimMapper.deleteById(id) > 0;
        if (success) evictAfterWrite();
        return success;
    }

    private void evictAfterWrite() {
        // Major 列表缓存内嵌学院名称，学院变化时必须级联失效。
        cacheService.evict(CacheService.COLLEGE_KEY, CacheService.MAJOR_KEY, CacheService.MAJOR_DROPDOWN_KEY);
        cacheService.evictDashboard();
    }
}
