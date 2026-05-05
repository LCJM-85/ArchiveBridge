package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("student_dim")
public class StudentDim {

    @TableId(value = "dim_id", type = IdType.AUTO)
    private Integer dimId;

    private String studentNo;

    private String name;

    private Integer nationId;

    private Integer politicalId;

    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean isCurrent;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
