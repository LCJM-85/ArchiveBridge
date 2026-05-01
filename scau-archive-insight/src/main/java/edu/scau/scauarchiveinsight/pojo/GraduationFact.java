package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("graduation_fact") // 改成你实际的表名
public class GraduationFact {

    @TableId(type = IdType.AUTO)
    private Long id;// 主键,毕业id

    private String studentNo;// 学号

    private Integer degreeId;// 学历ID

    private Integer destId;// 去向ID

    private Integer graduationDateId;// 毕业年份ID

    private Integer fileId;// 文件ID
}
