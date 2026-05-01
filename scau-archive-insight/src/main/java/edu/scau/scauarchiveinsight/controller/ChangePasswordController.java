package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.dto.ChangePasswordDTO;
import edu.scau.scauarchiveinsight.service.UserService;
import edu.scau.scauarchiveinsight.util.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChangePasswordController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @Valid @RequestBody ChangePasswordDTO dto,
            @RequestHeader("Authorization") String authHeader) {
        Map<String, Object> result = new HashMap<>();
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtils.getUsernameFromToken(token);

        int code = userService.changePassword(username, dto.getOldPassword(), dto.getNewPassword());
        if (code == 0) {
            result.put("code", 200);
            result.put("message", "密码修改成功");
            result.put("success", true);
            return ResponseEntity.ok(result);
        } else if (code == 1) {
            result.put("code", 404);
            result.put("message", "用户不存在");
            result.put("success", false);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        } else {
            result.put("code", 400);
            result.put("message", "旧密码错误");
            result.put("success", false);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }
}
