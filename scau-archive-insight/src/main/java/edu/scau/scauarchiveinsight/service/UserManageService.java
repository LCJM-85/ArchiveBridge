package edu.scau.scauarchiveinsight.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.scau.scauarchiveinsight.mapper.UserMapper;
import edu.scau.scauarchiveinsight.pojo.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserManageService {

    @Autowired
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public IPage<SysUser> listUsers(int current, int size, String keyword) {
        Page<SysUser> page = new Page<>(current, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword)
                    .or().like(SysUser::getEmail, keyword));
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        IPage<SysUser> result = userMapper.selectPage(page, wrapper);
        // 清除密码，不返回给前端
        result.getRecords().forEach(u -> u.setPassword(null));
        return result;
    }

    public int createUser(String username, String password, String realName, String phone, String email, String remark) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<SysUser> check = new LambdaQueryWrapper<>();
        check.eq(SysUser::getUsername, username);
        if (userMapper.selectCount(check) > 0) return 1;

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setRemark(remark);
        user.setRole("user");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return 0;
    }

    public int updateUser(Integer id, String realName, String phone, String email, String remark) {
        SysUser user = userMapper.selectById(id);
        if (user == null) return 1;

        user.setRealName(realName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setRemark(remark);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return 0;
    }

    public int setStatus(Integer id, Integer status) {
        SysUser user = userMapper.selectById(id);
        if (user == null) return 1;
        if ("admin".equals(user.getRole())) return 2; // 禁止禁用 admin

        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return 0;
    }

    public int deleteUser(Integer id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) return 1;
        if ("admin".equals(user.getRole())) return 2; // 禁止删除 admin

        userMapper.deleteById(id);
        return 0;
    }

    public void resetPassword(Integer id, String newPassword) {
        SysUser user = userMapper.selectById(id);
        if (user == null) return;

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }
}
