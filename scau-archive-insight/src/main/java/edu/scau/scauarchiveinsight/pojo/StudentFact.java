package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("student_fact")
public class StudentFact {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String studentNo;

    private String name;

    private String idCard;

    private String gender;

    private Integer majorId;

    private Integer classId;

    private Integer provinceId;

    @TableField("admission_date")
    private LocalDate admissionDate;

    private Integer fileId;

    private Boolean graduated;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
