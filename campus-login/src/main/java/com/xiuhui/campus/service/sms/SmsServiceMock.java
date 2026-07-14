package com.xiuhui.campus.service.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 短信服务 Mock 实现（开发/测试环境）
 * <p>
 * 验证码固定为配置的 mock-code，控制台打印。
 */
@Slf4j
@Service
@Profile({"dev"})
public class SmsServiceMock implements SmsService {

    @Override
    public void send(String phone, String code) {
        log.info("==================== 短信Mock ====================");
        log.info("  手机号: {}", phone);
        log.info("  验证码: {}", code);
        log.info("==================================================");
    }
}
