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

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 根据用户名查询用户
     */
    public SysUser getUserByUsername(String username) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, username);
        return userMapper.selectOne(queryWrapper);
    }

    /**
     * 验证用户名和密码
     * @return 0=成功, 1=用户不存在, 2=密码错误, 3=账号已禁用
     */
    public int verifyUser(String username, String password) {
        SysUser user = getUserByUsername(username);
        if (user == null) return 1;
        if (user.getStatus() != null && user.getStatus() == 0) return 3;
        if (!passwordEncoder.matches(password, user.getPassword())) return 2;
        return 0;
    }

    /**
     * 修改密码
     * @return 0=成功, 1=用户不存在, 2=旧密码错误
     */
    public int changePassword(String username, String oldPassword, String newPassword) {
        SysUser user = getUserByUsername(username);
        if (user == null) return 1;
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) return 2;
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        return 0;
    }
}
