package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("nation_dim") // 绑定数据库表名
public class NationDim {

    // 主键，自增策略
    @TableId(value = "nation_id", type = IdType.AUTO)
    private Integer nationId;

    // 民族名称，nation_name
    private String nationName;
}
