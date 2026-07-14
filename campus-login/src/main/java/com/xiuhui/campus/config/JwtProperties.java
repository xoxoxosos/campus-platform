package com.xiuhui.campus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** RSA 私钥路径 */
    private String privateKeyPath;

    /** RSA 公钥路径 */
    private String publicKeyPath;

    /** Access Token 过期时间（分钟） */
    private Long accessTokenExpiration;

    /** Refresh Token 过期时间（分钟） */
    private Long refreshTokenExpiration;
}
