package edu.scau.scauarchiveinsight.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.scau.scauarchiveinsight.pojo.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper（MyBatis-Plus 自动实现 CRUD）
 */
@Mapper // 标记为 MyBatis Mapper 组件
public interface UserMapper extends BaseMapper<SysUser> {
    // 无需写任何方法，BaseMapper 已包含：selectById、selectOne、insert 等
    // 自定义查询：根据用户名查用户（MyBatis-Plus 支持 Lambda 写法，无需 XML）
}
