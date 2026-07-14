package com.xiuhui.campus.service;

import com.xiuhui.campus.config.SmsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 短信验证码服务：Redis 存储 + 频率限制
 */
// TODO 每小时五次限制 ，应使用滑动窗口
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeService {

    private final StringRedisTemplate stringRedisTemplate;
    private final SmsProperties smsProperties;

    private static final String CODE_PREFIX = "sms:code:";
    private static final String LIMIT_MINUTE_PREFIX = "sms:limit:minute:";
    private static final String LIMIT_HOUR_PREFIX = "sms:limit:hour:";

    /**
     * 生成验证码并存储到 Redis
     * <p>
     * Mock 模式使用固定验证码，方便测试；
     * 生产模式生成随机6位验证码。
     *
     * @param phone 手机号
     * @return 验证码
     */
    public String generateAndStore(String phone) {
        String code = "mock".equals(smsProperties.getMode())
                ? smsProperties.getMockCode()
                : generateCode();
        String key = CODE_PREFIX + phone;
        stringRedisTemplate.opsForValue().set(key, code,
                smsProperties.getCodeExpireSeconds(), TimeUnit.SECONDS);
        log.debug("验证码已存储: phone={}, code={}", phone, code);
        return code;
    }

    /**
     * 校验发送频率限制
     *
     * @param phone 手机号
     * @return true=未超限，false=超限
     */
    public boolean checkRateLimit(String phone) {
        // 1. 60秒间隔限制
        String minuteKey = LIMIT_MINUTE_PREFIX + phone;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(minuteKey))) {
            log.warn("发送过于频繁，请60秒后再试: phone={}", phone);
            return false;
        }

        // 2. 每小时上限5条
        String hourKey = LIMIT_HOUR_PREFIX + phone;
        String countStr = stringRedisTemplate.opsForValue().get(hourKey);
        int count = countStr == null ? 0 : Integer.parseInt(countStr);
        if (count >= smsProperties.getSendLimitPerHour()) {
            log.warn("发送次数已达上限: phone={}, count={}", phone, count);
            return false;
        }

        return true;
    }

    /**
     * 标记发送频率（限流计数器）
     *
     * @param phone 手机号
     */
    public void markRateLimit(String phone) {
        String minuteKey = LIMIT_MINUTE_PREFIX + phone;
        stringRedisTemplate.opsForValue().set(minuteKey, "1",
                smsProperties.getSendIntervalSeconds(), TimeUnit.SECONDS);

        String hourKey = LIMIT_HOUR_PREFIX + phone;
        stringRedisTemplate.opsForValue().increment(hourKey);
        stringRedisTemplate.expire(hourKey, 1, TimeUnit.HOURS);
    }

    /**
     * 校验验证码是否正确
     *
     * @param phone 手机号
     * @param code  用户输入的验证码
     * @return true=正确，false=错误
     */
    public boolean verify(String phone, String code) {
        String key = CODE_PREFIX + phone;
        String storedCode = stringRedisTemplate.opsForValue().get(key);
        if (storedCode == null) {
            log.warn("验证码不存在或已过期: phone={}", phone);
            return false;
        }
        boolean valid = storedCode.equals(code);
        if (valid) {
            // 验证成功后删除，防止重复使用
            stringRedisTemplate.delete(key);
            log.debug("验证码校验成功: phone={}", phone);
        } else {
            log.warn("验证码错误: phone={}, expected={}, actual={}", phone, storedCode, code);
        }
        return valid;
    }

    /**
     * 生成6位随机数字验证码
     */
    private String generateCode() {
        int code = (int) ((Math.random() * 9 + 1) * 100000);
        return String.valueOf(code);
    }
}
