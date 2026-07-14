package com.xiuhui.campus.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiuhui.campus.mapper.RoleMapper;
import com.xiuhui.campus.mapper.UserMapper;
import com.xiuhui.campus.mapper.UserRoleMapper;
import com.xiuhui.campus.model.entity.Role;
import com.xiuhui.campus.model.entity.User;
import com.xiuhui.campus.model.entity.UserRole;
import com.xiuhui.campus.provider.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户模块内部服务（对外不可见，由 Provider 统一暴露）
 * <p>
 * 封装 User / Role / UserRole 的所有数据库操作，
 * 供 provider 层（对外接口）和本模块其他内部服务调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    // ==================== 查询 ====================

    /**
     * 根据用户名查询用户（不含已删除）
     */
    public User findByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    /**
     * 根据用户ID查询角色编码列表
     */
    public List<String> findRoleCodesByUserId(Long userId) {
        return userRoleMapper.selectRoleCodesByUserId(userId);
    }

    /**
     * 根据角色编码查询角色
     */
    public Role findRoleByCode(String roleCode) {
        return roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, roleCode));
    }

    /**
     * 判断用户名是否已存在
     */
    public boolean isUsernameExists(String username) {
        return userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0;
    }

    /**
     * 根据ID查询用户
     */
    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 根据手机号查询用户（供短信验证码登录用）
     */
    public User findByPhone(String phone) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    }

    // ==================== 更新 ====================

    /**
     * 更新用户信息
     */
    public void updateById(User user) {
        userMapper.updateById(user);
    }

    /**
     * 存储 TOTP 密钥（未启用状态，供 TOTP 绑定流程第一步使用）
     */
    public void updateTotpSecret(Long userId, String secret) {
        User user = new User();
        user.setId(userId);
        user.setTotpSecret(secret);
        user.setTotpEnabled(false);
        userMapper.updateById(user);
        log.info("TOTP密钥已生成: userId={}", userId);
    }

    /**
     * 启用 TOTP 双因素认证（绑定流程第二步）
     */
    public void enableTotp(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setTotpEnabled(true);
        userMapper.updateById(user);
        log.info("TOTP已启用: userId={}", userId);
    }

    // ==================== 注册 ====================

    /**
     * 注册用户并分配角色
     * <p>
     * 密码会进行 BCrypt 编码。
     *
     * @param request  注册请求
     * @param roleCode 角色编码（ADMIN / USER）
     * @return 注册成功的用户
     */
    @Transactional
    public User registerUser(RegisterRequest request, String roleCode) {
        // 1. 校验用户名唯一性
        if (isUsernameExists(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 2. 校验角色存在
        Role role = findRoleByCode(roleCode);
        if (role == null) {
            throw new RuntimeException("角色不存在: " + roleCode);
        }

        // 3. 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRealName(request.getRealName());
        user.setStatus(1);          // 默认正常
        user.setTotpEnabled(false); // 默认未启用双因素
        userMapper.insert(user);

        // 4. 分配角色
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);

        log.info("用户注册成功: {}，角色: {}", user.getUsername(), roleCode);
        return user;
    }
}
