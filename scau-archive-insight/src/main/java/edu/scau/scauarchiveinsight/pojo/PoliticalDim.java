package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("political_dim")
public class PoliticalDim {

    // 主键，自增策略
    @TableId(value = "political_id", type = IdType.AUTO)
    private Integer politicalId;

    // 政治面貌名称 political_name
    private String politicalName;
}
