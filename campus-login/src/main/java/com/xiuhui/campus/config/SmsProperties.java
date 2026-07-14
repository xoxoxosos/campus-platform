package com.xiuhui.campus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 短信配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    /** 短信模式：mock（开发/测试）、aliyun（生产） */
    private String mode = "mock";

    /** Mock 模式下的固定验证码 */
    private String mockCode = "123456";

    /** 验证码有效期（秒），默认5分钟 */
    private int codeExpireSeconds = 300;

    /** 发送间隔限制（秒），默认60秒 */
    private int sendIntervalSeconds = 60;

    /** 每小时发送上限 */
    private int sendLimitPerHour = 5;
}
