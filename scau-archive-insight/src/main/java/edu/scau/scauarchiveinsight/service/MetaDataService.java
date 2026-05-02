package edu.scau.scauarchiveinsight.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.scau.scauarchiveinsight.mapper.MetaDataStandardMapper;
import edu.scau.scauarchiveinsight.pojo.MetaDataStandard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class MetaDataService {

    @Autowired
    private MetaDataStandardMapper metaDataStandardMapper;

    public List<MetaDataStandard> list() {
        return metaDataStandardMapper.selectList(null);
    }

    public IPage<MetaDataStandard> page(int current, int size, String keyword) {
        Page<MetaDataStandard> page = new Page<>(current, size);
        LambdaQueryWrapper<MetaDataStandard> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(MetaDataStandard::getFieldCode, keyword)
                   .or()
                   .like(MetaDataStandard::getFieldName, keyword)
                   .or()
                   .like(MetaDataStandard::getSourceField, keyword);
        }
        return metaDataStandardMapper.selectPage(page, wrapper);
    }

    public MetaDataStandard getById(String fieldCode) {
        return metaDataStandardMapper.selectById(fieldCode);
    }

    public boolean add(MetaDataStandard metaDataStandard) {
        return metaDataStandardMapper.insert(metaDataStandard) > 0;
    }

    public boolean delete(String fieldCode) {
        return metaDataStandardMapper.deleteById(fieldCode) > 0;
    }

    public boolean update(MetaDataStandard metaDataStandard) {
        return metaDataStandardMapper.updateById(metaDataStandard) > 0;
    }
}
