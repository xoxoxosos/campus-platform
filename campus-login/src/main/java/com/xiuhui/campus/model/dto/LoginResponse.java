package com.xiuhui.campus.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** Access Token */
    private String accessToken;

    /** Refresh Token */
    // TODO 这个应该不能返回，为什么，我也不确定
    private String refreshToken;

    /** Token 类型，固定 Bearer */
    private String tokenType;

    /** Access Token 过期时间（秒） */
    private Long expiresIn;

    /** 是否需要 TOTP 二次验证 */
    private Boolean needTotp;

    /** 用户拥有的角色编码列表 */
    private List<String> roles;
}
