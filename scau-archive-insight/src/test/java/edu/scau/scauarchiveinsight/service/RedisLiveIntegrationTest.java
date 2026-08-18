package edu.scau.scauarchiveinsight.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "RUN_REDIS_INTEGRATION", matches = "true")
class RedisLiveIntegrationTest {

    @Test
    void javaClientSupportsJsonCacheAndAtomicCaptchaConsumption() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration("127.0.0.1", 6379));
        factory.afterPropertiesSet();
        factory.start();
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();
        String suffix = UUID.randomUUID().toString();
        String cacheKey = "scau:test:cache:" + suffix;
        String captchaKey = "scau:test:captcha:" + suffix;

        try {
            CacheService cache = new CacheService(template, new ObjectMapper());
            List<Map<String, Object>> expected = List.of(Map.of("name", "测试", "count", 1));
            cache.put(cacheKey, expected, Duration.ofMinutes(1));

            assertThat(cache.<List<Map<String, Object>>>get(cacheKey, new TypeReference<>() {}))
                    .isEqualTo(expected);

            template.opsForValue().set(captchaKey, "A1B2", Duration.ofMinutes(1));
            assertThat(template.opsForValue().getAndDelete(captchaKey)).isEqualTo("A1B2");
            assertThat(template.opsForValue().get(captchaKey)).isNull();
        } finally {
            template.delete(cacheKey);
            template.delete(captchaKey);
            factory.destroy();
        }
    }
}
