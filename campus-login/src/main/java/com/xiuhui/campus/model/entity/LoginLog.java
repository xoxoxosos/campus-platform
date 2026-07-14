package com.xiuhui.campus.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 登录日志实体
 */
@Data
@TableName("login_log")
public class LoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID，登录失败时可能为NULL */
    private Long userId;

    private String username;

    /** 登录入口：ADMIN / PORTAL */
    private String loginType;

    /** 登录方式：PASSWORD / SMS */
    private String loginWay;

    private String ip;

    private String userAgent;

    /** 结果：1=成功, 0=失败 */
    private Integer result;

    private String failReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
