package edu.scau.scauarchiveinsight.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.scau.scauarchiveinsight.mapper.CollegeDimMapper;
import edu.scau.scauarchiveinsight.mapper.MajorDimMapper;
import edu.scau.scauarchiveinsight.pojo.CollegeDim;
import edu.scau.scauarchiveinsight.pojo.MajorDim;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CollegeService {

    @Autowired
    private CollegeDimMapper collegeDimMapper;

    @Autowired
    private MajorDimMapper majorDimMapper;

    public List<CollegeDim> list(String keyword) {
        LambdaQueryWrapper<CollegeDim> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(CollegeDim::getCollegeName, keyword);
        }
        wrapper.orderByAsc(CollegeDim::getCollegeId);
        return collegeDimMapper.selectList(wrapper);
    }

    public boolean add(CollegeDim dim) {
        return collegeDimMapper.insert(dim) > 0;
    }

    public boolean update(CollegeDim dim) {
        return collegeDimMapper.updateById(dim) > 0;
    }

    public boolean delete(Integer id) {
        Long ref = majorDimMapper.selectCount(new LambdaQueryWrapper<MajorDim>().eq(MajorDim::getCollegeId, id));
        if (ref != null && ref > 0) throw new IllegalStateException("该学院下存在专业，请先删除或迁移专业");
        return collegeDimMapper.deleteById(id) > 0;
    }
}
