package edu.scau.scauarchiveinsight.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CacheService {
    public static final String DASHBOARD_KEY = "scau:cache:dashboard:stats";
    public static final String COLLEGE_KEY = "scau:cache:dim:college:all";
    public static final String MAJOR_KEY = "scau:cache:dim:major:all";
    public static final String MAJOR_DROPDOWN_KEY = "scau:cache:dim:major:dropdown";
    public static final String CLASS_KEY = "scau:cache:dim:class:all";
    public static final String CLASS_DROPDOWN_KEY = "scau:cache:dim:class:dropdown";
    public static final String PROVINCE_KEY = "scau:cache:dim:province:all";
    public static final String DESTINATION_KEY = "scau:cache:dim:destination:all";
    public static final Duration DASHBOARD_TTL = Duration.ofMinutes(5);
    public static final Duration DIMENSION_TTL = Duration.ofMinutes(30);

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> T get(String key, TypeReference<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            return json == null ? null : objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("读取 Redis 缓存失败，回退数据库: {}", key, e);
            return null;
        }
    }

    public void put(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("写入 Redis 缓存失败，跳过缓存: {}", key, e);
        }
    }

    public void evict(String... keys) {
        for (String key : keys) {
            try {
                redisTemplate.delete(key);
            } catch (RuntimeException e) {
                log.warn("删除 Redis 缓存失败，等待 TTL 兜底: {}", key, e);
            }
        }
    }

    public void evictDashboard() {
        evict(DASHBOARD_KEY);
    }

    public void evictAllDimensions() {
        evict(COLLEGE_KEY, MAJOR_KEY, MAJOR_DROPDOWN_KEY,
                CLASS_KEY, CLASS_DROPDOWN_KEY, PROVINCE_KEY, DESTINATION_KEY);
    }
}
