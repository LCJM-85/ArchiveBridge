package edu.scau.scauarchiveinsight.pojo;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("admission_fact")
public class AdmissionFact {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String studentNo;

    private String examNo;

    private Integer provinceId;

    private Integer majorId;

    @TableField("admission_date")
    private LocalDate admissionDate;

    private Integer fileId;
}
