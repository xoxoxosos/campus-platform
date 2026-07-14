package com.xiuhui.campus.service;

import com.xiuhui.campus.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token 服务：Refresh Token 的 Redis 存储与校验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtProperties jwtProperties;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /**
     * 存储 Refresh Token 到 Redis
     */
    public void storeRefreshToken(Long userId, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        long ttl = jwtProperties.getRefreshTokenExpiration();
        stringRedisTemplate.opsForValue().set(key, refreshToken, ttl, TimeUnit.MINUTES);
        log.debug("Refresh Token 已存储: userId={}", userId);
    }

    /**
     * 校验 Refresh Token 是否与 Redis 中存储的一致
     * @return true=一致，false=不一致或不存在
     */
    public boolean validateRefreshToken(Long userId, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        String stored = stringRedisTemplate.opsForValue().get(key);
        if (stored == null) {
            log.warn("Redis 中不存在 Refresh Token: userId={}", userId);
            return false;
        }
        boolean valid = refreshToken.equals(stored);
        if (!valid) {
            log.warn("Refresh Token 不匹配: userId={}", userId);
        }
        return valid;
    }

    /**
     * 删除 Refresh Token（登出、Token 被盗用等情况）
     */
    public void deleteRefreshToken(Long userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        Boolean deleted = stringRedisTemplate.delete(key);
        log.info("Refresh Token 已删除: userId={}, result={}", userId, deleted);
    }
}
