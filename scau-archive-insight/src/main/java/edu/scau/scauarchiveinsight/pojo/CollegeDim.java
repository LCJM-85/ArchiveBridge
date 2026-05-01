package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("college_dim") // 改成你实际的表名
public class CollegeDim {

    @TableId(type = IdType.AUTO)
    private Integer collegeId; // 主键，学院ID

    private String collegeName; // 学院名称
}
