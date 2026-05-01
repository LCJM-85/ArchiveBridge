package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("metadata_standard") // 绑定数据库表名
public class MetaDataStandard {

    // 主键：字段编码
    @TableId(value = "field_code", type = IdType.INPUT)
    private String fieldCode;

    // 字段名称
    private String fieldName;

    // 字段类型
    private String fieldType;

    // 来源字段
    private String sourceField;

    // 转换类型
    private String transformType;

    // 转换规则
    private String transformRule;

    // 是否必填
    private Boolean isRequired;
}
