package com.xiuhui.campus.controller;

import com.xiuhui.campus.model.dto.LoginRequest;
import com.xiuhui.campus.model.dto.LoginResponse;
import com.xiuhui.campus.model.dto.RefreshTokenRequest;
import com.xiuhui.campus.model.dto.SmsLoginRequest;
import com.xiuhui.campus.model.dto.SmsSendRequest;
import com.xiuhui.campus.model.dto.TotpBindRequest;
import com.xiuhui.campus.model.dto.TotpVerifyRequest;
import com.xiuhui.campus.provider.UserProvider;
import com.xiuhui.campus.provider.dto.RegisterRequest;
import com.xiuhui.campus.service.AuthService;
import com.xiuhui.campus.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 普通用户门户认证接口
 */
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalAuthController {

    private final AuthService authService;
    private final UserProvider userProvider;

    /**
     * 普通用户账号密码登录
     */
    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest httpRequest) {
        request.setLoginType("PORTAL");
        String ip = IpUtil.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        LoginResponse response = authService.login(request, ip, userAgent);
        if (Boolean.TRUE.equals(response.getNeedTotp())) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 注册普通用户账号（分配 USER 角色）
     */
    @PostMapping("/auth/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        userProvider.register(request, "USER");
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "用户注册成功",
                "username", request.getUsername()
        ));
    }

    /**
     * 刷新 Access Token（使用 Refresh Token 换取新 Token）
     */
    @PostMapping("/auth/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /**
     * 发送短信验证码（用户入口）
     */
    @PostMapping("/auth/sms/send")
    public ResponseEntity<Map<String, Object>> sendSms(@Valid @RequestBody SmsSendRequest request) {
        authService.sendSmsCode(request.getPhone());
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "验证码已发送"
        ));
    }

    /**
     * 短信验证码登录（用户入口）
     */
    @PostMapping("/auth/sms/login")
    public ResponseEntity<LoginResponse> smsLogin(@Valid @RequestBody SmsLoginRequest request,
                                                   HttpServletRequest httpRequest) {
        String ip = IpUtil.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        LoginResponse response = authService.loginBySms(request, "PORTAL", ip, userAgent);
        return ResponseEntity.ok(response);
    }

    /**
     * TOTP 二次验证 + 完成登录
     */
    @PostMapping("/auth/verify-totp")
    public ResponseEntity<LoginResponse> verifyTotp(@Valid @RequestBody TotpVerifyRequest request,
                                                     HttpServletRequest httpRequest) {
        String ip = IpUtil.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        LoginResponse response = authService.verifyTotpAndLogin(
                request.getTempToken(), request.getCode(), "PORTAL", ip, userAgent);
        return ResponseEntity.ok(response);
    }

    /**
     * 生成 TOTP 密钥和二维码（需登录后调用）
     */
    @PostMapping("/totp/generate")
    public ResponseEntity<Map<String, Object>> generateTotp() {
        Long userId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getCredentials();
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        Map<String, String> result = authService.generateTotpSecret(userId, username);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "secret", result.get("secret"),
                "qrCodeUrl", result.get("qrCodeUrl")
        ));
    }

    /**
     * 绑定 TOTP（扫码后输入验证码完成绑定，需登录后调用）
     */
    @PostMapping("/totp/bind")
    public ResponseEntity<Map<String, Object>> bindTotp(@Valid @RequestBody TotpBindRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getCredentials();
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        authService.bindTotp(userId, username, request.getCode());
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "TOTP绑定成功"
        ));
    }
}
