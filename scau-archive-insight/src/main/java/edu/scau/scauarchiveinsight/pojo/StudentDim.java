package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("student_dim") // 绑定数据库表名
public class StudentDim {

    // 主键，自增策略
    @TableId(value = "dim_id", type = IdType.AUTO)
    private Integer dimId;

    // 学号
    private String studentNo;

    // 姓名
    private String name;

    // 民族ID
    private Integer nationId;

    // 政治面貌ID
    private Integer politicalId;

    // 入学日期
    private LocalDate startDate;

    // 毕业日期
    private LocalDate endDate;

    // 是否当前有效
    private Boolean isCurrent;
}
