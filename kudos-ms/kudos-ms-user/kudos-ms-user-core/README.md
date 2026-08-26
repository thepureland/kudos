# kudos-ms-user-core

User 原子服务的**领域实现层**：账号 / 组织 / 通行证 / 联系方式 / 第三方账号绑定 / 后台调度。
**不含 HTTP 控制器**（控制器在 `user-api-admin` / `user-api-internal` / `user-api-public`）。

## 业务对象

| 表 | 角色 |
|---|---|
| `user_account` | 用户账号主表（含 loginPassword / securityPassword / authenticationKey / freeze*) |
| `user_account_protection` | 登录错误次数 / 冻结窗口等保护性字段 |
| `user_account_third` | 第三方账号绑定（微信 / GitHub / Google 等） |
| `user_contact_way` | 联系方式（手机 / 邮箱），可多条 |
| `user_org` | 组织树（path 列做祖先链） |
| `user_org_user` | 组织↔用户关系 |
| `user_login_remember_me` | 记住登录的长效 token（持久化 session） |
| `user_log_login` | 登录日志（每次登录的审计记录） |

## 业务模块

```
io.kudos.ms.user.core
├── platform/init/UserAutoConfiguration   装配入口
├── account/                              账号 + 第三方账号 + 调度
│   ├── schedule/AutoUnfreezeScheduler    每小时清过期冻结（仅 @EnableScheduling 时生效）
│   ├── cache/                            UserAccountHashCache + AccountThirdByUserIdAndProviderCodeCache
│   ├── dao/UserAccount{Protection,Third}Dao
│   └── service/                          UserAccountService + ExternalAccountProvisioningService
│                                         （JIT 账号与首条外部身份绑定的原子事务）
├── passport/                             登录通行证 - 真正"鉴权"逻辑落在这里
│   ├── service/impl/PassportService      login / logout / verify / changePassword
│   └── api/PassportApi                   IPassportApi 实现
├── login/                                记住登录 + 登录日志（与 passport 的瞬时鉴权区分）
│   ├── cache/RememberMeByTenantIdAndUsernameCache
│   ├── dao/UserLoginRememberMeDao + UserLogLoginDao
│   └── service/impl/UserLoginRememberMeService + UserLogLoginService
├── contact/                              联系方式增删改查 + by-userId 缓存
└── org/                                  组织树 + 跨组织用户聚合缓存
```

## 通行证（PassportService）登录状态机

```
login(req) ──► getUserByTenantIdAndUsername
                ├─ null            → USER_NOT_FOUND
                ├─ active != true  → INACTIVE
                ├─ 在冻结窗口内    → ACCOUNT_FROZEN
                ├─ 密码不匹配      → incrementLoginErrorTimes, WRONG_PASSWORD
                ├─ authKey 非空    ──┐
                │                    ├─ authCode == null     → OTP_REQUIRED (不动错误计数)
                │                    └─ OTP 校验失败          → incrementLoginErrorTimes, OTP_WRONG
                └─ 全通过           → resetLoginErrorTimes + updateLastLoginInfo, SUCCESS
```

冻结窗口 `[freezeStartTime, freezeEndTime)`：
- start 为 null = 立即生效
- end 为 null = 永久冻结
- freezeType 为空 = 无冻结记录

## 关键缓存敏感点

- `getOrgUserIds(orgId)` 返回该组织 + 所有后代组织的用户列表 —— 组织 path 变更要级联清缓存
- 历史 fix（`fix(user): precise cache invalidation on org-tree mutations`）：移动 / 重命名
  组织时要按"老 path + 新 path 的所有上下级"做精确失效，否则 stale data 持续到 TTL 自然过期
- `AccountThirdByUserIdAndProviderCodeCache`：第三方账号绑定缓存——一个用户在每个 provider 下
  最多绑定一个第三方账号，按 (userId, providerCode) 唯一

## 外部账号 JIT 预配

`ExternalAccountProvisioningService.provision` 使用 `REQUIRES_NEW` 创建无本地密码账号，并强制调用
`jitBindExternalIdentity` 写入首条身份绑定。两步共享事务，任何用户名或外部身份唯一约束冲突都会
回滚整个账号；成功事件在审计表中标记为 `JIT_BIND`。配置默认组织时还会在同一事务建立
`user_org_user` 成员关系，使账号主组织与数据范围一致。服务同时接受经 Auth 域校验的直属上级、
账号类型/状态、locale、IANA 时区和货币默认值。重复请求若已能看到活动绑定则直接幂等返回。

## 本地密码策略与写入边界

`IPasswordPolicy` 是可替换的密码策略 SPI；应用未提供实现时由 `DefaultPasswordPolicy` 生效。默认配置：

```yaml
kudos:
  ms:
    user:
      password-policy:
        enabled: true
        min-length: 12
        max-length: 64
        reject-username: true
        reject-repeated-character: true
        require-uppercase: false
        require-lowercase: false
        require-digit: false
        require-special: false
```

账号创建、通用更新、登录密码/安全密码重置都在 `UserAccountService` 的持久化边界统一校验，并由
`DelegatingPasswordEncoder` 写成 `{bcrypt}`。无论策略的 `max-length` 如何配置，原始密码都不得超过
72 个 UTF-8 字节，以免当前默认 BCrypt
静默截断后产生等价密码。JIT 账号的空登录密码仍表示没有本地密码；仅可信的内部 `UserAccount`
迁移对象可携带已有的受支持版本化编码或历史无前缀 BCrypt，表单传入类似哈希的字符串仍按普通密码
处理。通用更新禁止改变
租户，并保留现有 TOTP secret 与 session key，相关字段只能走各自的专用维护入口。

所有改密入口都会拒绝复用当前密码，并用稳定的 `PASSWORD_POLICY_VIOLATION` / `PASSWORD_REUSED`
表达自助改密失败。`PasswordPolicyContext` 包含租户、用户和密码用途，业务可据此实现租户级字典或
泄露密码库策略。可选的 `IPasswordHistory` 列表用于历史校验与旧哈希归档；同进程引入 Auth Core
时默认接入其 `auth_password_history`（默认最近 5 个），单独部署 User Core 时列表为空，仍保留当前
密码复用保护。

### 登录密码的归属：`IAccountCredentialStore`

登录密码的最终归属是 Auth 域的 `auth_credential`，本模块通过 `IAccountCredentialStore` 端口访问它。
依赖方向是 auth → user，反向会成环，所以由本模块声明端口、Auth 侧实现、同进程部署时 Spring 组合
——与 `IPasswordHistory` 同一套模式。

- **存在实现时**：`user_account.login_password` 既不读也不写（写入空串），校验、改密、登录时哈希
  升级和退役哈希归档全部发生在 Auth 侧。
- **不存在实现时**：仍按原样读写该列，本模块可独立部署。该列已由 `V1.0.0.35` 置为可空并废弃。

端口只交换明文与布尔结论，从不返回密码哈希：校验哈希需要明文和哈希同时在场，把哈希递回本模块
比较没有任何收益，只会让存储凭证多跨一道边界。租户或用户缺失按接线错误抛出，而不是返回 false
——后者在调用方读起来等同于"密码错误"。

## 调度

`AutoUnfreezeScheduler`：每小时整点清理过期冻结（cron 可覆盖）：
- **`@Scheduled` 默认不启用**——需部署方加 `@EnableScheduling`，保持本核心库性质
- 无 ShedLock——SQL 幂等 + 频次低 + 写入量小，多实例并发跑可接受
- 高一致性场景再加 `@SchedulerLock` 即可

## 跨服务依赖

- `kudos-ms-sys-core`——拿子系统 / 租户元数据（active / accountTypeDictCode 等枚举）
  - ⚠️ 注意是 `sys-core`，**不是** `sys-client`：这是同进程**强代码耦合**，不是 Feign 远程
    调用。意味着启动本服务会把 sys 服务的 service / dao / 缓存层一起拉入 classpath。
  - 想换成 Feign 远程调用需要把 `api(project(":...:kudos-ms-sys-core"))` 替换为
    `api(project(":...:kudos-ms-sys-client"))` 并改写注入点，目前没这么做

## 装配

`UserAutoConfiguration`：
- `@ComponentScan("io.kudos.ms.user.core")`
- `@AutoConfigureAfter(KtormAutoConfiguration::class)`
- `IComponentInitializer.getComponentName() = "kudos-ms-user-core"`

## 测试覆盖

- 25 个测试类覆盖 cache / dao / service / passport（含 PassportServiceTest 登录路径全枚举）
- 用 h2 + `application.yml` + `test-resources/sql/h2/*.sql` 初始化
- PassportService 通过构造器注入 `IUserAccountService` + `UserAccountDao`，易于在测试中替换

## 已知限制 / 安全考量

- ✅ **公网用户枚举响应已收敛**：`PassportService.login` 仍保留细分状态供可信内部逻辑和审计使用，
  `PassportPublicController` 与 Auth 密码适配器统一对外返回 `INVALID_CREDENTIALS`，并移除错误次数、
  冻结标题等差异字段；不存在、停用和冻结账号分支执行 BCrypt 校验以缓解明显的时序快路径。
- ❗ **登录错误次数自增 ≠ 强一致**：`incrementLoginErrorTimes` 后又走 `getUserRecord` 读
  实际值——并发同账号登录场景下错误次数可能不严格累加（DB UPDATE 是原子的，但读 + 返回
  的"累计"可能不是最新）
- ❗ **`changePassword` 没有"旧密码错误次数"限制**：连续暴力试旧密码不会触发冻结。建议
  对接 `incrementLoginErrorTimes` 同款保护
- ✅ **密码与 OTP 失败窗口已分离**：错误 TOTP 只消费 TOTP 限流桶，不再累计旧密码错误次数或
  触发账号自动冻结。
- ❗ **`verifyPassword` 直查 DAO 绕过缓存**：单次开销大；高频场景（如风控查询）应增加
  短 TTL 缓存或限流
- ❗ **`AutoUnfreezeScheduler` 无观测**：异常吞掉到 log.error——没有 metrics / alarm。
  长时间错过执行无监控
- ❗ **`UserContactWayService` 无脱敏**：手机 / 邮箱以明文存储 + 明文返回。GDPR / 等保
  合规场景需自行加密
- ❗ **第三方账号 access_token 储存策略未明**：`UserAccountThird` 表是否存 token / refresh_token
  / token expiry，需对照 sql 模块的 schema 确认

## 依赖

- `kudos-ms-user-common` / `kudos-ms-user-sql`
- `kudos-ability-data-rdb-ktorm`（ORM）/ `kudos-ability-data-rdb-flyway`（迁移）
- `kudos-ability-cache-common` / `kudos-ability-cache-local-caffeine` / `kudos-ability-cache-remote-redis`（多层缓存）
- `kudos-ms-sys-core`（**同进程**依赖，非 Feign client；详见上文跨服务依赖小节）
- `kudos-base`（GoogleAuthenticator / 历史 BCrypt 兼容）
- `kudos-ability-security-common`（版本化 `PasswordEncoder` / 密码编码兼容工具）

## 改进建议（自动分析 2026-06-11）

- ✅ 已修复（2026-06-11）**登录失败无锁定阈值**（`passport/service/impl/PassportService.kt`）：
  实现"N 次失败→冻结窗口"简化版锁定——失败累计达阈值
  `kudos.ms.user.passport.login-lock.max-error-times`（默认 5，≤0 关闭）后，复用账号冻结机制以专用冻结类型
  `autoLoginLock` 自动冻结 `kudos.ms.user.passport.login-lock.lock-minutes`（默认 30，≤0 表示锁定至人工解冻）分钟，
  窗口内一律返回 `LOCKED`（含正确密码），窗口到期由 `AutoUnfreezeScheduler` 自动清理；登录成功重置计数；
  已有的人工冻结（manual / admin / scheduled）不会被自动锁覆盖。
  **剩余工作**：`user_account_protection` 保护策略表（按用户/租户差异化阈值）的接入仍为待办，当前为全局配置项。
- ✅ **登录审计已落库**（`passport/service/impl/PassportService.kt`）：login 成功及终态失败写入
  `user_log_login`，公共控制器使用服务端观测的 IP、终端、浏览器、OS 和 User-Agent；审计写入失败
  不改变认证结果。
- ✅ **安全状态变更使用专用事件**：登录密码、安全密码、停用、冻结和 TOTP 认证器变更发布
  `UserAuthenticationInvalidated(id, tenantId, reason)`；账号删除事件在删除前携带租户快照。普通的
  登录时间、退出时间和失败计数仍只发布 `UserAccountUpdated`，不会误触发全端下线。
- ✅ **账号创建/通用更新不会明文落库**：所有普通表单中的非空密码先执行策略再写为 `{bcrypt}`；
  可信迁移实体可保留受支持编码，JIT 空密码保持无本地密码语义。历史无前缀 BCrypt 在完整登录成功
  后通过 CAS 透明升级，且不写密码历史、不撤销会话。通用更新同时禁止跨租户移动及直接覆盖
  TOTP secret/session key，密码改变后发布专用全端失效事件。
- ❗ **remember-me token 明文存储**（`login/model/po/UserLoginRememberMe.kt` + `RememberMeByTenantIdAndUsernameCache`）：
  长效 token 明文存 DB 并整表加载进缓存，DB / Redis 泄露即可重放登录。建议参照 Spring Security 持久化 token 的
  series + hashed-token 方案。
- **`UserLogLoginService` 内存截断**（`login/service/impl/UserLogLoginService.kt`）：`getLoginsByUserId/getRecentLogins`
  先全量 `search` 再内存 `sortedByDescending().take(limit)`，登录日志表大后单次查询会拖垮内存；且 `limit` 无上限校验。
  应下推 ORDER BY + LIMIT 到 DAO 层。
- **`UserOrgService.getOrgsByTenantId` 冗余缓存往返**（`org/service/impl/UserOrgService.kt`）：
  `userOrgHashCache.getOrgsByTenantId` 已返回完整 `UserOrgCacheEntry` 列表，随后又按 id 走 `getOrgsByIds`
  再查一轮缓存——可直接返回第一次结果。
- **代码组织小项**（`account/service/impl/UserAccountService.kt`）：companion object 位于类中部、其后仍有实例方法；
  构造器注入与 `@Resource` 字段注入混用——建议统一为构造器注入并把 companion 移至类尾。
- **可扩展性**：OTP 实现仍硬绑定 `GoogleAuthenticator`（PassportService / UserAccountService 直接 new）；
  密码强度已有 `IPasswordPolicy`，同进程密码历史已有 Auth `IPasswordHistory` 实现，登录密码本身也已
  经 `IAccountCredentialStore` 收归 `auth_credential`（V61），但独立部署和租户级策略持久化仍需随改密
  命令迁往 Auth。短信 OTP / WebAuthn 仍需抽象 `IOtpVerifier` 并迁移到 Auth 凭证模型。
