package com.xiuhui.campus.service.sms;

/**
 * 短信服务接口
 */
public interface SmsService {

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @param code  验证码
     */
    void send(String phone, String code);
}
