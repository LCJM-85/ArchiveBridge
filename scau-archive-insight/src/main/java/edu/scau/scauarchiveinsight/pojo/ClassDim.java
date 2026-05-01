package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("class_dim") // 改成你实际的表名
public class ClassDim {

    @TableId(type = IdType.AUTO)
    private Integer classId; // 主键，班级ID

    private Integer majorId; // 专业ID（外键）

    private String className; // 班级名称

    private String grade; // 年级

    private Integer studyLength; // 学制（年）
}
