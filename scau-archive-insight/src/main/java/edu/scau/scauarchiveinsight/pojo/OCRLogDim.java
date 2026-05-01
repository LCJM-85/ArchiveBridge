package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ocr_log_dim") // 绑定数据库表名
public class OCRLogDim {

    // 主键，自增策略
    @TableId(value = "log_id", type = IdType.AUTO)
    private Integer logId;

    // 文件ID
    private Integer fileId;

    // 识别状态
    private String recognizeStatus;

    // 识别时间（timestamp 对应 LocalDateTime）
    private LocalDateTime recognizeTime;

    // F1分数（numeric(5,2) 对应 Double）
    private Double f1Score;
}
