package edu.scau.scauarchiveinsight.controller;



import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import edu.scau.scauarchiveinsight.dto.LoginDTO;
import edu.scau.scauarchiveinsight.service.UserService;
import edu.scau.scauarchiveinsight.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class LoginController {
    private static final int MAX_LOGIN_ATTEMPTS = 8;
    private static final long LOGIN_WINDOW_MILLIS = 10 * 60 * 1000L;
    private static final int MAX_CAPTCHA_REQUESTS = 30;
    private static final long CAPTCHA_WINDOW_MILLIS = 60 * 1000L;
    private static final long CAPTCHA_TTL_MILLIS = 2 * 60 * 1000L;

    private final Map<String, AttemptWindow> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, AttemptWindow> captchaAttempts = new ConcurrentHashMap<>();

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO,
                                                     HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        String clientIp = resolveClientIp(request);
        // @Valid 已经兜底空值/空白校验，这里只保留业务验证逻辑
        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();
        String inputCaptcha = loginDTO.getCaptcha();
        String loginKey = (clientIp + ":" + username.trim().toLowerCase());

        if (isBlocked(loginAttempts, loginKey, MAX_LOGIN_ATTEMPTS, LOGIN_WINDOW_MILLIS)) {
            result.put("code", 429);
            result.put("message", "登录失败次数过多，请稍后再试");
            result.put("success", false);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(result);
        }

        // 取 session 中的验证码
        String sessionCaptcha = (String) request.getSession().getAttribute("captcha");
        Long captchaExpireAt = (Long) request.getSession().getAttribute("captchaExpireAt");
        boolean captchaExpired = (captchaExpireAt == null || Instant.now().toEpochMilli() > captchaExpireAt);

        // 验证验证码
        if (sessionCaptcha == null || captchaExpired || !sessionCaptcha.equalsIgnoreCase(inputCaptcha)) {
            recordAttempt(loginAttempts, loginKey, LOGIN_WINDOW_MILLIS);
            result.put("code", 400);
            result.put("message", captchaExpired ? "验证码已过期，请刷新后重试" : "验证码错误");
            result.put("success", false);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }

        // 3. 验证完就清掉（防止复用）
        request.getSession().removeAttribute("captcha");
        request.getSession().removeAttribute("captchaExpireAt");

        // 校验账号密码是否匹配数据库中的用户
        boolean isValid = userService.verifyUser(username.trim(), password);
        if (isValid) {
            clearAttempts(loginAttempts, loginKey);
            // 登录成功后签发 JWT，前端后续请求放到 Authorization 头中
            String token = jwtUtils.generateToken(username.trim());
            result.put("code", 200);
            result.put("message", "登录成功");
            result.put("success", true);
            result.put("token", token);
            return ResponseEntity.ok(result);
        } else {
            recordAttempt(loginAttempts, loginKey, LOGIN_WINDOW_MILLIS);
            // 认证失败统一返回 401，便于前端分支处理
            result.put("code", 401);
            result.put("message", "用户名或密码错误");
            result.put("success", false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
    }

    @GetMapping("/captcha")
    public void captcha(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String clientIp = resolveClientIp(request);
        if (isBlocked(captchaAttempts, clientIp, MAX_CAPTCHA_REQUESTS, CAPTCHA_WINDOW_MILLIS)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"验证码请求过于频繁，请稍后再试\",\"success\":false}");
            return;
        }
        recordAttempt(captchaAttempts, clientIp, CAPTCHA_WINDOW_MILLIS);

        // 创建验证码（宽，高，字符数，干扰线数）
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(100, 38, 4, 20);

        String code = captcha.getCode();
        // 存入 session
        request.getSession().setAttribute("captcha", code);
        request.getSession().setAttribute("captchaExpireAt", Instant.now().toEpochMilli() + CAPTCHA_TTL_MILLIS);

        // 输出图片
        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-store");// 防缓存
        captcha.write(response.getOutputStream());
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

    private boolean isBlocked(Map<String, AttemptWindow> store, String key, int maxAttempts, long windowMillis) {
        AttemptWindow window = store.get(key);
        if (window == null) return false;
        long now = Instant.now().toEpochMilli();
        if (now - window.windowStart >= windowMillis) {
            store.remove(key);
            return false;
        }
        return window.count >= maxAttempts;
    }

    private void recordAttempt(Map<String, AttemptWindow> store, String key, long windowMillis) {
        long now = Instant.now().toEpochMilli();
        store.compute(key, (k, oldWindow) -> {
            if (oldWindow == null || now - oldWindow.windowStart >= windowMillis) {
                return new AttemptWindow(1, now);
            }
            oldWindow.count++;
            return oldWindow;
        });
    }

    private void clearAttempts(Map<String, AttemptWindow> store, String key) {
        store.remove(key);
    }

    private static class AttemptWindow {
        int count;
        final long windowStart;

        AttemptWindow(int count, long windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }
    }
}
