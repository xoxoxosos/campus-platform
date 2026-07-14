package com.xiuhui.campus.provider.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求 DTO（provider 层对外暴露的参数）
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度3-50位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 4, max = 15, message = "密码长度6-100位")
    private String password;

    /** 手机号（可选） */
    private String phone;

    /** 邮箱（可选） */
    private String email;

    /** 真实姓名（可选） */
    private String realName;
}
