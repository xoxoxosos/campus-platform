package com.xiuhui.campus.service;

import com.xiuhui.campus.config.JwtProperties;
import com.xiuhui.campus.mapper.LoginLogMapper;
import com.xiuhui.campus.model.dto.LoginRequest;
import com.xiuhui.campus.model.dto.LoginResponse;
import com.xiuhui.campus.model.dto.SmsLoginRequest;
import com.xiuhui.campus.model.entity.LoginLog;
import com.xiuhui.campus.model.entity.User;
import com.xiuhui.campus.provider.UserProvider;
import com.xiuhui.campus.security.JwtUtil;
import com.xiuhui.campus.service.sms.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证服务：账号密码登录 + 短信验证码登录 + Token 刷新
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserProvider userProvider;
    private final LoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;
    private final SmsCodeService smsCodeService;
    private final SmsService smsService;

    /** 管理员角色编码 */
    private static final List<String> ADMIN_ROLES = List.of("SUPER_ADMIN", "ADMIN");

    /**
     * 账号密码登录
     */
    @Transactional
    public LoginResponse login(LoginRequest request, String ip, String userAgent) {
        String username = request.getUsername();
        String password = request.getPassword();
        String loginType = request.getLoginType();

        // 1. 查询用户
        User user = userProvider.findByUsername(username);

        // 2. 校验用户是否存在
        if (user == null) {
            log.warn("用户不存在: {}", username);
            saveLoginLog(null, username, loginType, "PASSWORD", ip, userAgent, 0, "用户名或密码错误");
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. 校验账号状态
        if (user.getStatus() == 0) {
            log.warn("账号已被禁用: {}", username);
            saveLoginLog(user.getId(), username, loginType, "PASSWORD", ip, userAgent, 0, "账号已被禁用");
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }

        // 4. 校验密码（BCrypt）
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("密码错误: {}", username);
            saveLoginLog(user.getId(), username, loginType, "PASSWORD", ip, userAgent, 0, "用户名或密码错误");
            throw new RuntimeException("用户名或密码错误");
        }

        // 5. 查询用户角色
        List<String> roles = userProvider.findRoleCodesByUserId(user.getId());

        // 6. 校验登录入口权限
        if ("ADMIN".equals(loginType)) {
            boolean isAdmin = roles.stream().anyMatch(ADMIN_ROLES::contains);
            if (!isAdmin) {
                log.warn("非管理员尝试登录管理后台: {}", username);
                saveLoginLog(user.getId(), username, loginType, "PASSWORD", ip, userAgent, 0, "无管理员权限");
                throw new RuntimeException("无管理员权限，请使用普通用户入口登录");
            }
        }

        // 7. 生成双 Token
        String accessToken = jwtUtil.generateAccessToken(user.getId(), username, roles);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        long expiresIn = jwtProperties.getAccessTokenExpiration() * 60; // 秒

        // 8. 存储 Refresh Token 到 Redis
        tokenService.storeRefreshToken(user.getId(), refreshToken);

        // 9. 更新最后登录信息
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ip);
        userProvider.updateById(user);

        // 10. 记录登录日志
        saveLoginLog(user.getId(), username, loginType, "PASSWORD", ip, userAgent, 1, null);

        log.info("登录成功: {} (入口: {})", username, loginType);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .needTotp(false)      // Step 6 实现
                .roles(roles)
                .build();
    }

    /**
     * 使用 Refresh Token 刷新 Access Token（Token 轮换）
     */
    public LoginResponse refreshAccessToken(String refreshToken) {
        // 1. 验证 Refresh Token 签名和有效期
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RuntimeException("Refresh Token 无效或已过期，请重新登录");
        }

        // 2. 解析 Refresh Token 获取 userId
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);

        // 3. 校验 Redis 中存储的是否一致（防止重放攻击）
        if (!tokenService.validateRefreshToken(userId, refreshToken)) {
            throw new RuntimeException("Refresh Token 已被使用或已失效，请重新登录");
        }

        // 4. 查询用户最新状态
        User user = userProvider.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }

        // 5. 查询最新角色
        List<String> roles = userProvider.findRoleCodesByUserId(userId);
        String username = user.getUsername();

        // 6. 生成新的双 Token（Token 轮换）
        String newAccessToken = jwtUtil.generateAccessToken(userId, username, roles);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);

        // 7. 更新 Redis，旧的 Refresh Token 立即失效
        tokenService.storeRefreshToken(userId, newRefreshToken);

        long expiresIn = jwtProperties.getAccessTokenExpiration() * 60;

        log.info("Token 刷新成功: userId={}, username={}", userId, username);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .needTotp(false)
                .roles(roles)
                .build();
    }

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     */
    public void sendSmsCode(String phone) {
        // 1. 频率限制检查
        if (!smsCodeService.checkRateLimit(phone)) {
            throw new RuntimeException("发送过于频繁，请稍后再试");
        }

        // 2. 生成验证码并存入 Redis
        String code = smsCodeService.generateAndStore(phone);

        // 3. 发送短信
        try {
            smsService.send(phone, code);
        } catch (Exception e) {
            log.error("短信发送失败: phone={}", phone, e);
            throw new RuntimeException("短信发送失败，请稍后重试");
        }

        // 4. 标记频率
        smsCodeService.markRateLimit(phone);
        log.info("短信验证码已发送: phone={}", phone);
    }

    /**
     * 短信验证码登录
     */
    @Transactional
    public LoginResponse loginBySms(SmsLoginRequest request, String loginType, String ip, String userAgent) {
        String phone = request.getPhone();
        String code = request.getCode();

        // 1. 校验验证码
        if (!smsCodeService.verify(phone, code)) {
            saveLoginLog(null, phone, loginType, "SMS", ip, userAgent, 0, "验证码错误或已过期");
            throw new RuntimeException("验证码错误或已过期");
        }

        // 2. 根据手机号查询用户
        User user = userProvider.findByPhone(phone);
        if (user == null) {
            log.warn("手机号未注册: {}", phone);
            saveLoginLog(null, phone, loginType, "SMS", ip, userAgent, 0, "手机号未注册");
            throw new RuntimeException("手机号未注册，请先注册账号");
        }

        // 3. 校验账号状态
        if (user.getStatus() == 0) {
            log.warn("账号已被禁用: {}", user.getUsername());
            saveLoginLog(user.getId(), user.getUsername(), loginType, "SMS", ip, userAgent, 0, "账号已被禁用");
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }

        // 4. 查询用户角色
        List<String> roles = userProvider.findRoleCodesByUserId(user.getId());
        String username = user.getUsername();

        // 5. 校验登录入口权限
        if ("ADMIN".equals(loginType)) {
            boolean isAdmin = roles.stream().anyMatch(ADMIN_ROLES::contains);
            if (!isAdmin) {
                log.warn("非管理员尝试登录管理后台: {}", username);
                saveLoginLog(user.getId(), username, loginType, "SMS", ip, userAgent, 0, "无管理员权限");
                throw new RuntimeException("无管理员权限，请使用普通用户入口登录");
            }
        }

        // 6. 生成双 Token
        String accessToken = jwtUtil.generateAccessToken(user.getId(), username, roles);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        long expiresIn = jwtProperties.getAccessTokenExpiration() * 60;

        // 7. 存储 Refresh Token 到 Redis
        tokenService.storeRefreshToken(user.getId(), refreshToken);

        // 8. 更新最后登录信息
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ip);
        userProvider.updateById(user);

        // 9. 记录登录日志
        saveLoginLog(user.getId(), username, loginType, "SMS", ip, userAgent, 1, null);

        log.info("短信登录成功: {} (入口: {})", username, loginType);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .needTotp(false)
                .roles(roles)
                .build();
    }

    /**
     * 记录登录日志
     */
    private void saveLoginLog(Long userId, String username, String loginType,
                               String loginWay, String ip, String userAgent,
                               Integer result, String failReason) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setLoginType(loginType);
        log.setLoginWay(loginWay);
        log.setIp(ip);
        log.setUserAgent(userAgent);
        log.setResult(result);
        log.setFailReason(failReason);
        loginLogMapper.insert(log);
    }
}
