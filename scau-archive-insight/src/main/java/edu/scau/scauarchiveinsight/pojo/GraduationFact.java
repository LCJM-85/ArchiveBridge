package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("graduation_fact")
public class GraduationFact {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String studentNo;

    private Integer degreeId;

    private Integer destId;

    @TableField("graduation_date")
    private LocalDate graduationDate;

    private Integer fileId;
}
