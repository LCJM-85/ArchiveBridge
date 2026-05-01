package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("major_dim")
public class MajorDim {

    // 主键：major_id，自动递增
    @TableId(type = IdType.AUTO)
    private Integer majorId;

    private Integer collegeId;  // college_id

    private Integer degreeId;   // degree_id

    private String majorName;   // major_name

    private String majorCode;   // major_code

}
