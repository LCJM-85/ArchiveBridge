package edu.scau.scauarchiveinsight.pojo;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("admission_fact")
public class AdmissionFact {

    @TableId(type = IdType.AUTO)
    private Long id; // 主键

    private String studentNo; // 学号（对应 student_no）

    private String examNo; // 考生号（对应 exam_no）

    private Integer provinceId; // 省份ID（对应 province_id）

    private Integer majorId; // 专业ID（对应 major_id）

    private Integer admissionDateId; // 入学年份ID（对应 admission_date_id）

    private Integer fileId; // 文件ID（对应 file_id）
}
