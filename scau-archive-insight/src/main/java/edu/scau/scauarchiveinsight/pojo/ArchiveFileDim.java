package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("archive_file_dim") // 改成你实际的表名
public class ArchiveFileDim {

    @TableId(type = IdType.AUTO)
    private Integer fileId; // 文件ID（主键）

    private String fileName; // 文件名

    private String fileType; // 文件类型

    private LocalDateTime uploadTime; // 上传时间
}
