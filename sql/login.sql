-- ============================================
-- 园区运营一体化管理平台 - 数据库初始化脚本（简化权限版）
-- 版本：v2.1
-- 说明：后端只做 ADMIN/PORTAL 入口隔离，前端用动态路由控制页面访问
-- ============================================

-- ==================== 一、建库 ====================
CREATE DATABASE IF NOT EXISTS `campus-platform` 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `campus-platform`;


-- ==================== 二、建表 ====================

-- 2.1 用户表
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(200) NOT NULL COMMENT 'BCrypt加密密码',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `totp_secret` VARCHAR(100) DEFAULT NULL COMMENT 'TOTP密钥(NULL=未绑定双因素)',
    `totp_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用双因素认证(0=否,1=是)',
    `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '账号状态(1=正常,0=禁用,2=锁定)',
    `locked_until` DATETIME DEFAULT NULL COMMENT '锁定到期时间',
    `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username_deleted` (`username`, `is_deleted`),
    UNIQUE KEY `uk_phone_deleted` (`phone`, `is_deleted`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2.2 角色表
CREATE TABLE `role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码(前端路由用)',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除(0=正常,1=已删除)',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code_deleted` (`role_code`, `is_deleted`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 2.3 用户角色关联表
CREATE TABLE `user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 2.4 登录日志表
CREATE TABLE `login_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID(登录失败时可能为NULL)',
    `username` VARCHAR(50) NOT NULL COMMENT '尝试登录的用户名',
    `login_type` VARCHAR(20) NOT NULL COMMENT '登录入口(ADMIN/PORTAL)',
    `login_way` VARCHAR(20) NOT NULL COMMENT '登录方式(PASSWORD/SMS)',
    `ip` VARCHAR(50) DEFAULT NULL COMMENT '登录IP',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器User-Agent',
    `result` TINYINT(1) NOT NULL COMMENT '结果(1=成功,0=失败)',
    `fail_reason` VARCHAR(100) DEFAULT NULL COMMENT '失败原因',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_username` (`username`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';


-- ==================== 三、初始化数据 ====================

-- 3.1 基础角色（前端路由根据 role_code 决定显示哪些菜单）
INSERT INTO role (role_code, role_name, description) VALUES
('SUPER_ADMIN', '超级管理员', '所有模块可见'),
('ADMIN', '管理员', '物业管理、进出管理、用户管理'),
('FINANCE', '财务人员', '财务管理模块'),
('STAFF', '物业人员', '维修工单、设备查看、进出记录'),
('USER', '普通用户', '会议室预约、进出记录');


-- ==================== 四、验证数据 ====================
SELECT * FROM role;