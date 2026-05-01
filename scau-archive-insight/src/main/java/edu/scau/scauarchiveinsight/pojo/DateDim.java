package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("date_dim") // 改成你实际的表名
public class DateDim {

    @TableId(type = IdType.AUTO)
    private Integer dateId; // 主键，日期ID

    private LocalDate fullDate; // 完整日期

    private Integer year; // 年份

    private Integer month; // 月份

    private Integer day; // 日期

    private Integer quarter; // 季度
}