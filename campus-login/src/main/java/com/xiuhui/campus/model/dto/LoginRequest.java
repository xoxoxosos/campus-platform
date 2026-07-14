package com.xiuhui.campus.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求 DTO
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 登录入口：ADMIN / PORTAL */
    @NotBlank(message = "登录入口不能为空")
    private String loginType;
}
