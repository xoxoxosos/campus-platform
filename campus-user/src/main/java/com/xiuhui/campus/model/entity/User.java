package com.xiuhui.campus.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String phone;

    private String email;

    private String realName;

    /** TOTP密钥，NULL表示未绑定双因素 */
    private String totpSecret;

    /** 是否启用双因素认证：0=否, 1=是 */
    private Boolean totpEnabled;

    /** 账号状态：1=正常, 0=禁用, 2=锁定 */
    private Integer status;

    /** 锁定到期时间 */
    private LocalDateTime lockedUntil;

    private LocalDateTime lastLoginAt;

    private String lastLoginIp;

    /** 逻辑删除：0=正常, 1=已删除 */
    @TableLogic
    private Integer isDeleted;

    private LocalDateTime deletedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
