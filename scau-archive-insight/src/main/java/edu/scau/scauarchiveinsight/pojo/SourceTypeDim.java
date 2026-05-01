package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("source_type_dim") // 绑定数据库表名
public class SourceTypeDim {

    // 主键，自增策略
    @TableId(value = "source_id", type = IdType.AUTO)
    private Integer sourceId;

    // 数据源名称（驼峰命名自动映射到 source_name）
    private String sourceName;
}
