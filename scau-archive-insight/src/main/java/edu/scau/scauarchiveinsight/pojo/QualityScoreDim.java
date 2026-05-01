package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("quality_score_dim") // 绑定数据库表名
public class QualityScoreDim {

    // 主键，自增策略
    @TableId(value = "score_id", type = IdType.AUTO)
    private Integer scoreId;

    // 文件ID
    private Integer fileId;

    // 完整性得分
    private Integer completeness;

    // 一致性得分
    private Integer consistency;

    // 准确性得分
    private Integer accuracy;

    // 时效性得分
    private Integer timeliness;

    // 总分
    private Integer totalScore;

    // 检查时间
    private LocalDateTime checkTime;
}
