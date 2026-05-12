package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("metadata_standard")
public class MetaDataStandard {

    @TableId(value = "metadata_id", type = IdType.AUTO)
    private Integer metadataId;

    @TableField("field_code")
    private String fieldCode;

    @TableField("field_name")
    private String fieldName;

    @TableField("field_type")
    private String fieldType;

    @TableField("source_field")
    private String sourceField;

    @TableField("transform_type")
    private String transformType;

    @TableField("transform_rule")
    private String transformRule;

    @TableField("is_required")
    private Boolean isRequired;
}
