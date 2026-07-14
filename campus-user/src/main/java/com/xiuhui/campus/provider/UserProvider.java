package com.xiuhui.campus.provider;

import com.xiuhui.campus.model.entity.User;
import com.xiuhui.campus.provider.dto.RegisterRequest;
import com.xiuhui.campus.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户模块对外暴露接口（特殊文件夹）
 * <p>
 * 外部模块（如 campus-login）只能通过 provider 包使用用户能力，
 * 不可直接依赖 service / mapper 等内部包，以此控制耦合。
 */
// TODO 简单功能可以直接用 userservice， 不需要走 provider
@Component
@RequiredArgsConstructor
public class UserProvider {

    private final UserService userService;

    /**
     * 根据用户名查询用户（供登录认证用）
     */
    public User findByUsername(String username) {
        return userService.findByUsername(username);
    }

    /**
     * 查询用户角色编码列表（供权限校验用）
     */
    public List<String> findRoleCodesByUserId(Long userId) {
        return userService.findRoleCodesByUserId(userId);
    }

    /**
     * 根据ID查询用户（供 Token 刷新时校验用户状态用）
     */
    public User findById(Long id) {
        return userService.findById(id);
    }

    /**
     * 根据手机号查询用户（供短信验证码登录用）
     */
    public User findByPhone(String phone) {
        return userService.findByPhone(phone);
    }

    /**
     * 更新用户信息（供更新登录时间/IP 等用）
     */
    public void updateById(User user) {
        userService.updateById(user);
    }

    /**
     * 存储 TOTP 密钥（未启用状态，供 TOTP 绑定流程第一步使用）
     * <p>
     * 此方法会同时将 totpEnabled 设为 false，确保密钥生成后
     * 只有在验证通过后才真正启用。
     */
    public void updateTotpSecret(Long userId, String secret) {
        userService.updateTotpSecret(userId, secret);
    }

    /**
     * 启用 TOTP 双因素认证（绑定流程第二步）
     */
    public void enableTotp(Long userId) {
        userService.enableTotp(userId);
    }

    /**
     * 注册用户并分配角色
     * <p>
     * 供 campus-login 注册接口调用。
     *
     * @param request  注册请求
     * @param roleCode 角色编码（ADMIN / USER）
     * @return 注册成功的用户
     */
    public User register(RegisterRequest request, String roleCode) {
        return userService.registerUser(request, roleCode);
    }
}
