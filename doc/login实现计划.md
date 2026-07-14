# 园区运营一体化管理平台 - 登录模块实现计划

## 当前项目状态

| 项目 | Spring Boot 3.3.0 | Java 17 | Maven 多模块 |
|---|---|---|---|
| **已有模块** | `campus-common`（公共模块）| `campus-login`（登录模块） | |
| **已配置** | 父POM依赖管理、MyBatis-Plus、Redis、MySQL、Lombok | |
| **缺失** | `campus-login` 的 `src/main/resources/` 目录不存在 | 所有业务代码未编写 | |

---

## 实施路线图（8步）

### 第1步：项目基础完善 — 预计 1~2h

**① 重建 `campus-login/src/main/resources/` 目录及其配置文件**

目前配置文件只存在于 `target/classes/`，`mvn clean` 后会丢失。需要创建：

```
campus-login/src/main/resources/
├── application.yml               # 端口8081、JWT参数、短信参数、引入common配置
├── application-dev.yml           # MySQL、Redis本地连接地址
├── application-prod.yml          # 环境变量占位符
├── jwt/
│   ├── .gitkeep                  # 占位（后续放 private_key.pem / public_key.pem）
│   └── (生成RSA密钥对脚本)
└── mapper/
    └── UserRoleMapper.xml        # 根据userId查角色编码列表
```

**② 生成 RSA 密钥对**

```
jwt/
├── private_key.pem   # 私钥，用于签发JWT
└── public_key.pem    # 公钥，用于验签JWT
```

使用 `openssl` 或 Java KeyPairGenerator 生成。

**③ 清理 `campus-common` 多余文件**

- 删除空文件 `App.java`、`AppTest.java`

---

### 第2步：账号密码登录 + JWT 签发 — 预计 3~4h

这是核心功能，涉及全栈穿透。

**① 数据模型层（model/entity/）**

| 类 | 对应表 | 关键字段 |
|---|---|---|
| `User.java` | `user` | id, username, password(BCrypt), phone, totpSecret, totpEnabled, status, isDeleted |
| `Role.java` | `role` | id, roleCode, roleName, description, isDeleted |
| `UserRole.java` | `user_role` | id, userId, roleId |
| `LoginLog.java` | `login_log` | userId, username, loginType, loginWay, ip, userAgent, result, failReason |

使用 MyBatis-Plus 的 `@TableLogic` 标记逻辑删除字段 `isDeleted`。

**② 数据访问层（repository/mapper/）**

| 类 | 说明 |
|---|---|
| `UserMapper.java` | 继承 `BaseMapper<User>`，按 username 查询、按 phone 查询 |
| `RoleMapper.java` | 继承 `BaseMapper<Role>` |
| `UserRoleMapper.java` | 继承 `BaseMapper<UserRole>` + XML 联查角色编码 |
| `LoginLogMapper.java` | 继承 `BaseMapper<LoginLog>` |

`UserRoleMapper.xml` 中的 SQL：
```xml
SELECT r.role_code FROM role r
INNER JOIN user_role ur ON r.id = ur.role_id
WHERE ur.user_id = #{userId} AND r.is_deleted = 0
```

**③ 安全组件（security/）**

| 类 | 说明 |
|---|---|
| `JwtProperties.java` | `@ConfigurationProperties("jwt")`，读取 private-key-path、public-key-path、过期时间 |
| `JwtUtil.java` | 生成 RSA 签名的 Access Token（15min）、解析 Token、校验有效性 |

Access Token Payload：`{ sub: userId, username, roles: ["ADMIN"], iat, exp, jti }`

**④ 业务层（service/）**

| 类 | 说明 |
|---|---|
| `AuthService.java` | 核心认证逻辑：账号密码验证 → 角色校验 → 判断2FA → 签发Token |

**⑤ 接口层（controller/）**

| 类 | 路径 | 说明 |
|---|---|---|
| `AdminAuthController.java` | `POST /api/admin/auth/login` | 管理员登录入口 |
| `PortalAuthController.java` | `POST /api/portal/auth/login` | 普通用户登录入口 |

**⑥ 安全配置（config/）**

| 类 | 说明 |
|---|---|
| `SecurityConfig.java` | 配置 SecurityFilterChain：放行 `/api/**/auth/**`，其他需认证 |
| `JwtAuthenticationFilter.java` | OncePerRequestFilter，解析 JWT → 加载用户权限 → 设置 SecurityContext |

**⑦ DTO 层（model/dto/）**

| 类 | 字段 | 校验 |
|---|---|---|
| `LoginRequest.java` | username, password, loginType(ADMIN/PORTAL) | `@NotBlank` |
| `LoginResponse.java` | accessToken, refreshToken, tokenType, expiresIn, needTotp(boolean) | - |

---

### 第3步：Refresh Token + Redis + Token 刷新 — 预计 2~3h

**① 配置 Redis（config/）**

| 类 | 说明 |
|---|---|
| `RedisConfig.java` | RedisTemplate 序列化配置（Jackson2JsonRedisSerializer） |

**② Token 服务**

| 类 | 说明 |
|---|---|
| `TokenService.java` | `createRefreshToken(userId)` → 生成UUID存入Redis（7天TTL）；`validateRefreshToken()` → 校验+轮换（旧Token立即删除，签发新Token） |

**③ Redis Key 设计**

| Key | TTL | 说明 |
|---|---|---|
| `refresh_token:{userId}:{token}` | 7天 | Refresh Token存储 |
| `login:fail:{username}` | 30分钟 | 失败计数 |
| `sms:code:{phone}` | 5分钟 | 验证码 |
| `sms:limit:{phone}:minute` | 60秒 | 发送频率 |
| `sms:limit:{phone}:hour` | 1小时 | 每小时上限 |

**④ 刷新接口**

```
POST /api/auth/refresh
Body: { refreshToken: "xxx" }
Response: { accessToken, refreshToken }  ← 新Token，旧Token已失效
```

---

### 第4步：角色校验 + 登录入口隔离 — 预计 2~3h

**① 入口隔离逻辑（AuthService中）**

```
/admin 请求 → 用户角色包含 SUPER_ADMIN 或 ADMIN → 允许
            → 否则 → 返回 403 "无管理员权限"
/portal 请求 → 所有角色均可
```

**② Spring Security 配置调整**

```java
// SecurityConfig.java
.requestMatchers("/api/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
.requestMatchers("/api/portal/**").authenticated()
```

**③ 创建初始化数据**

用 SQL 或 `CommandLineRunner` 插入初始用户：
```sql
-- 测试账号（BCrypt 密码：123456）
INSERT INTO user (username, password, phone, real_name) VALUES
('admin', '$2a$10$...', '13800000001', '系统管理员'),
('staff1', '$2a$10$...', '13800000002', '物业员工');

INSERT INTO user_role (user_id, role_id) VALUES
(1, 1), -- admin → SUPER_ADMIN
(2, 4); -- staff1 → STAFF
```

---

### 第5步：手机验证码登录 + Mock 模式 — 预计 2~3h

**① 短信服务接口与实现（service/sms/）**

```java
public interface SmsService {
    void send(String phone, String code);
}

@Service @Profile({"dev", "test"})
public class SmsServiceMock implements SmsService {
    // 控制台打印验证码，实际验证码固定为配置的 mock-code
}

@Service @Profile("prod")
public class SmsServiceAliyun implements SmsService {
    // 调用阿里云短信API
}
```

**② 短信配置类**

| 类 | 说明 |
|---|---|
| `SmsProperties.java` | `@ConfigurationProperties("sms")`，读取 mode、mockCode、aliyun 参数 |

**③ 验证码发送接口**

```
POST /api/auth/sms/send
Body: { phone: "13800000001" }
Response: { success: true, message: "验证码已发送" }
```

**④ 验证码登录接口**

```
POST /api/auth/sms/login
Body: { phone: "13800000001", code: "123456" }
Response: { accessToken, refreshToken }
```

---

### 第6步：TOTP 双因素认证 — 预计 2~3h

**① TOTP 服务**

| 类 | 说明 |
|---|---|
| `TotpService.java` | 使用 `aerogear-otp-java`，`generateSecret()`、`getQrCodeUrl()`、`verify(code)` |

需要在 `pom.xml` 中添加依赖：
```xml
<dependency>
    <groupId>org.jboss.aerogear</groupId>
    <artifactId>aerogear-otp-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

**② TOTP 接口**

```
GET  /api/auth/totp/generate   → 生成密钥 + 返回二维码URL（用户用Google Authenticator扫码绑定）
POST /api/auth/totp/bind       → 验证首次绑定
POST /api/auth/verify-totp     → 登录时提交TOTP验证（配合临时Token）
```

**③ 双因素认证集成到登录流程**

- 账号密码验证通过后 → 判断 `totp_enabled == true` →
    - 是 → 返回临时 Token（5分钟有效）+ `needTotp: true`（HTTP 202）
    - 否 → 直接签发正式 Token（HTTP 200）

---

### 第7步：安全加固 — 预计 2~3h

**① 登录失败限制（service/）**

| 类 | 说明 |
|---|---|
| `LoginAttemptService.java` | Redis 计数器：5次失败 → 锁定30分钟 |

```java
public class LoginAttemptService {
    // checkLocked(username) → boolean（是否被锁定）
    // incrementFailCount(username) → void
    // resetFailCount(username) → void（登录成功后调用）
}
```

**② IP 限流**

在 `SecurityConfig` 中加入限流过滤器，使用 Redis + Bucket4j 或自定义拦截器。

**③ 登录日志（AOP 或显式调用）**

| 方式 | 说明 |
|---|---|
| 显式调用 | 在 `AuthService.login()` 中异步记录 `LoginLog` |

**④ 密码传输加密**

前端用 RSA 公钥加密密码后传输，后端用 RSA 私钥解密后再进行 BCrypt 比对。

---

### 第8步：集成测试 + 接口文档 — 预计 1~2h

**① 集成测试**

编写 `AuthServiceTest` 覆盖核心场景：
- 账号密码正确 → 登录成功
- 密码错误 → 失败计数增加
- 失败5次 → 被锁定
- 管理员登录 `/admin` → 成功
- 普通用户登录 `/admin` → 被拒绝
- TOTP 绑定用户 → 返回需验证
- Refresh Token 刷新 → Token 轮换

**② 接口文档**

使用 Apifox 或 Swagger/OpenAPI 生成接口文档。

---

## 实现顺序总结

```
第1步（基础）──┐
               ├──第2步（核心登录+JWT）──第3步（Token刷新）──第4步（权限隔离）
               │                    │
               │                    └──第6步（TOTP双因素）
               │
               └──第5步（短信验证码）
                                  ↓
                            第7步（安全加固）
                                  ↓
                            第8步（测试+文档）
```

- **第2步**是最核心的，完成之后第3、4、5、6步可以并行
- **第5步**和第6步相对独立，可并行开发
- **第7步**依赖前面的基础功能完成后才能做

---

## 待确认事项

1. Redis 目前开发环境配置被注释掉了（`application-dev.yml`），是否立即启用？
2. 阿里云短信服务的 AK/SK 是否已有，还是先用 Mock 模式完整跑通流程？
3. 前端是否同步开发？接口文档是否需要提前提供（用于前后端联调）？
4. `campus-common` 中的 `spring-boot-starter-data-redis` 是否需要配置密码连接？开发环境 Redis 端口确认是默认 6379 吗？
