package edu.scau.scauarchiveinsight.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import edu.scau.scauarchiveinsight.mapper.UserMapper;
import edu.scau.scauarchiveinsight.pojo.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务层（处理登录业务逻辑）
 */
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    // BCrypt 密码加密器（Spring 内置，无需额外依赖）
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 根据用户名查询用户
     */
    public SysUser getUserByUsername(String username) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, username); // 等价于：WHERE username = ?
        return userMapper.selectOne(queryWrapper); // 查单条数据
    }

    /**
     * 验证用户名和密码
     */
    public boolean verifyUser(String username, String password) {
        // 1. 根据用户名查用户
        SysUser user = getUserByUsername(username);
        if (user == null) {
            return false; // 用户名不存在
        }
        // 2. 验证密码（BCrypt 匹配：明文密码 vs 数据库加密密码）
        return passwordEncoder.matches(password, user.getPassword());
    }

    /**
     * 修改密码
     * @return 0=成功, 1=用户不存在, 2=旧密码错误
     */
    public int changePassword(String username, String oldPassword, String newPassword) {
        SysUser user = getUserByUsername(username);
        if (user == null) {
            return 1;
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return 2;
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        return 0;
    }
}
