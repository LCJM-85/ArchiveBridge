package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("student_fact")
public class StudentFact {

    // 主键，自增策略
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 学号
    private String studentNo;

    // 姓名
    private String name;

    // 身份证号
    private String idCard;

    // 性别
    private String gender;

    // 专业ID
    private Integer majorId;

    // 班级ID
    private Integer classId;

    // 省份ID
    private Integer provinceId;

    // 入学日期ID
    private Integer admissionDateId;
}
