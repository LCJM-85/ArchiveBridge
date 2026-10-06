package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("ocr_log_dim")
public class OCRLogDim {

    @TableId(value = "log_id", type = IdType.AUTO)
    private Integer logId;

    @TableField("file_id")
    private Integer fileId;

    @TableField("file_name")
    private String fileName;

    @TableField("file_type")
    private String fileType;

    @TableField("recognize_status")
    private String recognizeStatus;

    @TableField("recognize_time")
    private LocalDateTime recognizeTime;

    @TableField("f1_score")
    private Double f1Score;

    @TableField("error_message")
    private String errorMessage;

    /** 由 error_message 解析出的结构化问题，不对应数据库字段。 */
    @TableField(exist = false)
    private List<ProcessingIssue> issues;

    @TableField("message")
    private String message;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
