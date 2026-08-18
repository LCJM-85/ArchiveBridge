package edu.scau.scauarchiveinsight.controller;



import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import edu.scau.scauarchiveinsight.dto.LoginDTO;
import edu.scau.scauarchiveinsight.pojo.SysUser;
import edu.scau.scauarchiveinsight.service.UserService;
import edu.scau.scauarchiveinsight.util.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "登录认证", description = "验证码生成、用户登录")
public class LoginController {
    private static final Logger log = LoggerFactory.getLogger(LoginController.class);
    private static final int MAX_LOGIN_ATTEMPTS = 8;
    private static final int MAX_CAPTCHA_REQUESTS = 30;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(10);
    private static final Duration CAPTCHA_WINDOW = Duration.ofMinutes(1);
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(2);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Operation(summary = "用户登录", description = "需要先获取验证码，提交用户名+密码+验证码")
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO,
                                                     HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        String clientIp = resolveClientIp(request);
        // @Valid 已经兜底空值/空白校验，这里只保留业务验证逻辑
        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();
        String inputCaptcha = loginDTO.getCaptcha();
        String loginKey = "scau:auth:login:fail:" + clientIp + ":" + username.trim().toLowerCase();

        if (isBlocked(loginKey, MAX_LOGIN_ATTEMPTS)) {
            result.put("code", 429);
            result.put("message", "登录失败次数过多，请稍后再试");
            result.put("success", false);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(result);
        }

        String captchaKey = "scau:auth:captcha:" + loginDTO.getUuid().trim();
        final String storedCaptcha;
        try {
            // 原子取出并删除，验证码只能使用一次。
            storedCaptcha = stringRedisTemplate.opsForValue().getAndDelete(captchaKey);
        } catch (RuntimeException e) {
            log.error("读取验证码 Redis 失败", e);
            result.put("code", 503);
            result.put("message", "验证码服务暂时不可用，请稍后重试");
            result.put("success", false);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
        }

        if (storedCaptcha == null || !storedCaptcha.equalsIgnoreCase(inputCaptcha)) {
            recordAttempt(loginKey, LOGIN_WINDOW);
            result.put("code", 400);
            result.put("message", storedCaptcha == null ? "验证码已过期，请刷新后重试" : "验证码错误");
            result.put("success", false);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }

        // 校验账号密码是否匹配数据库中的用户
        int verifyCode = userService.verifyUser(username.trim(), password);
        if (verifyCode == 0) {
            clearAttempts(loginKey);

            SysUser user = userService.getUserByUsername(username.trim());
            String role = user != null && user.getRole() != null ? user.getRole() : "user";

            // 登录成功后签发 JWT，含角色信息
            String token = jwtUtils.generateToken(username.trim(), role);
            result.put("code", 200);
            result.put("message", "登录成功");
            result.put("success", true);
            result.put("token", token);
            result.put("role", role);
            result.put("username", username.trim());
            return ResponseEntity.ok(result);
        } else {
            recordAttempt(loginKey, LOGIN_WINDOW);
            String msg;
            if (verifyCode == 3) {
                msg = "账号已被禁用，请联系管理员";
            } else if (verifyCode == 1) {
                msg = "用户名或密码错误";
            } else {
                msg = "用户名或密码错误";
            }
            result.put("code", 401);
            result.put("message", msg);
            result.put("success", false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
    }

    @Operation(summary = "获取验证码图片")
    @GetMapping("/captcha")
    public void captcha(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String clientIp = resolveClientIp(request);
        String requestKey = "scau:auth:captcha:req:" + clientIp;
        if (recordAndCheckCaptchaLimit(requestKey)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"验证码请求过于频繁，请稍后再试\",\"success\":false}");
            return;
        }
        // 创建验证码（宽，高，字符数，干扰线数）
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(100, 38, 4, 20);

        String uuid = UUID.randomUUID().toString().replace("-", "");
        try {
            stringRedisTemplate.opsForValue().set("scau:auth:captcha:" + uuid, captcha.getCode(), CAPTCHA_TTL);
        } catch (RuntimeException e) {
            log.error("写入验证码 Redis 失败", e);
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":503,\"message\":\"验证码服务暂时不可用，请稍后重试\",\"success\":false}");
            return;
        }

        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");// 防缓存
        response.getWriter().write("{\"code\":200,\"success\":true,\"data\":{\"uuid\":\""
                + uuid + "\",\"imageBase64\":\"" + captcha.getImageBase64Data() + "\"}}");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isBlocked(String key, int maxAttempts) {
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            return value != null && Long.parseLong(value) >= maxAttempts;
        } catch (RuntimeException e) {
            // 限流故障不阻断正常登录；验证码本身仍采用 fail-closed。
            log.warn("读取登录限流 Redis 失败，临时放行: {}", key, e);
            return false;
        }
    }

    private void recordAttempt(String key, Duration window) {
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) stringRedisTemplate.expire(key, window);
        } catch (RuntimeException e) {
            log.warn("更新登录限流 Redis 失败，临时放行: {}", key, e);
        }
    }

    private void clearAttempts(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (RuntimeException e) {
            log.warn("清理登录限流 Redis 失败: {}", key, e);
        }
    }

    private boolean recordAndCheckCaptchaLimit(String key) {
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) stringRedisTemplate.expire(key, CAPTCHA_WINDOW);
            return count != null && count > MAX_CAPTCHA_REQUESTS;
        } catch (RuntimeException e) {
            log.warn("更新验证码限流 Redis 失败，临时放行: {}", key, e);
            return false;
        }
    }
}
