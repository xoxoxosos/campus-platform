package com.xiuhui.campus.controller;

import com.xiuhui.campus.model.dto.LoginRequest;
import com.xiuhui.campus.model.dto.LoginResponse;
import com.xiuhui.campus.model.dto.RefreshTokenRequest;
import com.xiuhui.campus.model.dto.SmsLoginRequest;
import com.xiuhui.campus.model.dto.SmsSendRequest;
import com.xiuhui.campus.provider.UserProvider;
import com.xiuhui.campus.provider.dto.RegisterRequest;
import com.xiuhui.campus.service.AuthService;
import com.xiuhui.campus.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理后台认证接口
 */
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;
    private final UserProvider userProvider;

    /**
     * 管理员账号密码登录
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest httpRequest) {
        request.setLoginType("ADMIN");
        String ip = IpUtil.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        LoginResponse response = authService.login(request, ip, userAgent);
        return ResponseEntity.ok(response);
    }

    /**
     * 注册管理员账号（分配 ADMIN 角色）
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        userProvider.register(request, "ADMIN");
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "管理员注册成功",
                "username", request.getUsername()
        ));
    }

    /**
     * 刷新 Access Token（使用 Refresh Token 换取新 Token）
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /**
     * 发送短信验证码（管理员入口）
     */
    @PostMapping("/sms/send")
    public ResponseEntity<Map<String, Object>> sendSms(@Valid @RequestBody SmsSendRequest request) {
        authService.sendSmsCode(request.getPhone());
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "验证码已发送"
        ));
    }

    /**
     * 短信验证码登录（管理员入口）
     */
    @PostMapping("/sms/login")
    public ResponseEntity<LoginResponse> smsLogin(@Valid @RequestBody SmsLoginRequest request,
                                                   HttpServletRequest httpRequest) {
        String ip = IpUtil.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        LoginResponse response = authService.loginBySms(request, "ADMIN", ip, userAgent);
        return ResponseEntity.ok(response);
    }
}
