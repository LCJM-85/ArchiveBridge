package edu.scau.scauarchiveinsight.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;

@Data
@TableName("province_dim") // 绑定数据库表名
public class ProvinceDim {

    // 主键，自增策略
    @TableId(value = "province_id", type = IdType.AUTO)
    private Integer provinceId;

    // 省份名称
    private String provinceName;

    // 省份边界（PostGIS 空间字段）
    private MultiPolygon geom;

    // 省份中心点（PostGIS 空间点）
    private Point center;
}
