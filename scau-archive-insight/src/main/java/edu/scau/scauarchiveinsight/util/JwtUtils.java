package edu.scau.scauarchiveinsight.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类：生成、验证、解析 Token
 */
@Component
public class JwtUtils {

    // 从配置文件读取密钥
    @Value("${jwt.secret}")
    private String secret;

    // 从配置文件读取过期时间（秒）
    @Value("${jwt.expiration}")
    private long expiration;

    // 生成加密密钥（对称加密）
    private SecretKey getSecretKey() {
        // 密钥长度至少 256 位（32 字节），否则会报错
        if (secret.length() < 32) {
            throw new RuntimeException("JWT 密钥长度不能小于 32 位");
        }
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 生成 Token（包含用户名、角色、过期时间等信息）
     * @param username 用户名（作为 Token 的核心载荷）
     * @param role 用户角色
     * @return JWT Token
     */
    public String generateToken(String username, String role) {
        // 1. 构建 Token 载荷（可添加用户ID、角色等额外信息）
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("role", role);

        // 2. 生成 Token
        return Jwts.builder()
                .setClaims(claims) // 设置载荷
                .setSubject(username) // 设置主题（用户名）
                .setIssuedAt(new Date()) // 设置签发时间
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000)) // 设置过期时间
                .signWith(getSecretKey(), SignatureAlgorithm.HS256) // 签名算法 + 密钥
                .compact();
    }

    /**
     * 从 Token 中解析用户名
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("username", String.class);
    }

    /**
     * 从 Token 中解析角色
     * @param token JWT Token
     * @return 角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("role", String.class);
    }

    /**
     * 验证 Token 是否有效（未过期 + 签名正确）
     * @param token JWT Token
     * @param username 待验证的用户名
     * @return true=有效，false=无效
     */
    public boolean validateToken(String token, String username) {
        String tokenUsername = getUsernameFromToken(token);
        // 用户名匹配 + Token 未过期
        return username.equals(tokenUsername) && !isTokenExpired(token);
    }

    /**
     * 检查 Token 是否过期
     */
    private boolean isTokenExpired(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expiration.before(new Date());
    }
}
