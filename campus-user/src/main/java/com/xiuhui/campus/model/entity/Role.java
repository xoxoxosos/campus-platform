package com.xiuhui.campus.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 角色实体
 */
@Data
@TableName("role")
public class Role {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色编码，前端路由用 */
    private String roleCode;

    private String roleName;

    private String description;

    @TableLogic
    private Integer isDeleted;

    private LocalDateTime deletedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
