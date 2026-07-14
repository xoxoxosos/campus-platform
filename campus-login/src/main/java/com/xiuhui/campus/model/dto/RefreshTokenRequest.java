package com.xiuhui.campus.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Refresh Token 刷新请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    /** Refresh Token */
    @NotBlank(message = "Refresh Token 不能为空")
    private String refreshToken;
}
