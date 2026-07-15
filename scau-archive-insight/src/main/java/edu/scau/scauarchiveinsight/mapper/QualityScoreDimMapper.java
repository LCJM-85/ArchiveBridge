package edu.scau.scauarchiveinsight.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.scau.scauarchiveinsight.pojo.QualityScoreDim;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QualityScoreDimMapper extends BaseMapper<QualityScoreDim> {

    @Select("SELECT AVG(total_score) FROM quality_score_dim WHERE total_score IS NOT NULL")
    Double selectAvgTotalScore();
}
