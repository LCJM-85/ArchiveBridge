package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("destination_dim") // 改成你实际的表名
public class DestinationDim {

    @TableId(type = IdType.AUTO)
    private Integer destId; // 主键，去向ID

    private String destName; // 去向名称
}
