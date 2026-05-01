package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("degree_dim") // 改成你实际的表名
public class DegreeDim {

    @TableId(type = IdType.AUTO)
    private Integer degreeId; // 主键，学历ID

    private String degreeName; // 学历名称
}
