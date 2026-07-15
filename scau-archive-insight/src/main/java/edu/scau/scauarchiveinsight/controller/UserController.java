package edu.scau.scauarchiveinsight.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.pojo.SysUser;
import edu.scau.scauarchiveinsight.service.UserManageService;
import edu.scau.scauarchiveinsight.util.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "用户管理", description = "管理员管理普通用户账号")
@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final String TOKEN_PREFIX = "Bearer ";

    @Autowired
    private UserManageService userManageService;

    @Autowired
    private JwtUtils jwtUtils;

    private boolean isAdmin(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith(TOKEN_PREFIX)) return false;
        String token = auth.substring(TOKEN_PREFIX.length());
        try {
            String role = jwtUtils.getRoleFromToken(token);
            return "admin".equals(role);
        } catch (Exception e) {
            return false;
        }
    }

    @Operation(summary = "分页查询用户列表")
    @GetMapping("/list")
    public R<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        if (!isAdmin(request)) return R.error(403, "无权限");

        IPage<SysUser> page = userManageService.listUsers(current, size, keyword);
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        data.put("pages", page.getPages());
        return R.ok(data);
    }

    @Operation(summary = "新增用户")
    @PostMapping("/create")
    public R<Void> create(@RequestBody SysUser user, HttpServletRequest request) {
        if (!isAdmin(request)) return R.error(403, "无权限");

        int code = userManageService.createUser(
                user.getUsername(), user.getPassword(),
                user.getRealName(), user.getPhone(),
                user.getEmail(), user.getRemark());
        if (code == 1) return R.error(400, "用户名已存在");
        return R.ok(null, "创建成功");
    }

    @Operation(summary = "修改用户信息")
    @PutMapping("/update")
    public R<Void> update(@RequestBody SysUser user, HttpServletRequest request) {
        if (!isAdmin(request)) return R.error(403, "无权限");

        int code = userManageService.updateUser(
                Math.toIntExact(user.getId()),
                user.getRealName(), user.getPhone(),
                user.getEmail(), user.getRemark());
        if (code == 1) return R.error(404, "用户不存在");
        return R.ok(null, "修改成功");
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/status")
    public R<Void> setStatus(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        if (!isAdmin(request)) return R.error(403, "无权限");

        Integer id = (Integer) body.get("id");
        Integer status = (Integer) body.get("status");
        if (id == null || status == null) return R.error(400, "参数错误");

        int code = userManageService.setStatus(id, status);
        if (code == 1) return R.error(404, "用户不存在");
        if (code == 2) return R.error(400, "不能操作 admin 账号");
        return R.ok(null, status == 1 ? "已启用" : "已禁用");
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/delete/{id}")
    public R<Void> delete(@PathVariable Integer id, HttpServletRequest request) {
        if (!isAdmin(request)) return R.error(403, "无权限");

        int code = userManageService.deleteUser(id);
        if (code == 1) return R.error(404, "用户不存在");
        if (code == 2) return R.error(400, "不能删除 admin 账号");
        return R.ok(null, "删除成功");
    }

    @Operation(summary = "重置用户密码")
    @PutMapping("/reset-password")
    public R<Void> resetPassword(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        if (!isAdmin(request)) return R.error(403, "无权限");

        Integer id = (Integer) body.get("id");
        String newPassword = (String) body.get("password");
        if (id == null || newPassword == null || newPassword.isBlank()) {
            return R.error(400, "参数错误");
        }
        userManageService.resetPassword(id, newPassword);
        return R.ok(null, "密码已重置");
    }
}
