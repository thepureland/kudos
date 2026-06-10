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
│   └── service/                          UserAccountService 含密码 / 登录错误次数 / 冻结管理
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

- ❗ **用户枚举漏洞**：`PassportService.login` 返回 `USER_NOT_FOUND` vs `WRONG_PASSWORD` /
  `INACTIVE` / `ACCOUNT_FROZEN` 四种不同状态——外部攻击者可凭响应区分用户是否存在 / 是否
  被冻结。OWASP 推荐合并成单一 `INVALID_CREDENTIALS` 返回。当前设计便于业务侧给出友好
  错误提示，与安全做了权衡
- ❗ **登录错误次数自增 ≠ 强一致**：`incrementLoginErrorTimes` 后又走 `getUserRecord` 读
  实际值——并发同账号登录场景下错误次数可能不严格累加（DB UPDATE 是原子的，但读 + 返回
  的"累计"可能不是最新）
- ❗ **`changePassword` 没有"旧密码错误次数"限制**：连续暴力试旧密码不会触发冻结。建议
  对接 `incrementLoginErrorTimes` 同款保护
- ❗ **OTP 校验失败也累加错误次数**：用户证明了密码正确但 OTP 输错两次仍可能被冻结。可
  考虑分开 password / OTP 两条错误计数线
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
- `kudos-base`（GoogleAuthenticator / PasswordKit / BCrypt）

## 改进建议（自动分析 2026-06-11）

- ✅ 已修复（2026-06-11）**登录失败无锁定阈值**（`passport/service/impl/PassportService.kt`）：
  实现"N 次失败→冻结窗口"简化版锁定——失败累计达阈值
  `kudos.ms.user.passport.login-lock.max-error-times`（默认 5，≤0 关闭）后，复用账号冻结机制以专用冻结类型
  `autoLoginLock` 自动冻结 `kudos.ms.user.passport.login-lock.lock-minutes`（默认 30，≤0 表示锁定至人工解冻）分钟，
  窗口内一律返回 `LOCKED`（含正确密码），窗口到期由 `AutoUnfreezeScheduler` 自动清理；登录成功重置计数；
  已有的人工冻结（manual / admin / scheduled）不会被自动锁覆盖。
  **剩余工作**：`user_account_protection` 保护策略表（按用户/租户差异化阈值）的接入仍为待办，当前为全局配置项。
- ❗ **登录审计未落库**（`passport/service/impl/PassportService.kt`）：login 成功 / 失败、logout 均不写 `user_log_login`，
  `UserLogLoginService` 成了无人投喂的只读统计层。建议在 login 各分支异步落一条审计记录（含 IP / 结果状态）。
- ❗ **admin 创建账号不哈希密码**（`account/service/impl/UserAccountService.kt#insert`）：`insert` 直接落库，
  `UserAccountFormCreate.loginPassword` 由调用方决定是否已哈希——一旦前端误传明文即明文入库。建议在 insert/update
  路径对密码字段统一做"已是 BCrypt 格式则跳过、否则 hash"的防御性处理。
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
- **可扩展性**：OTP 实现硬绑定 `GoogleAuthenticator`（PassportService / UserAccountService 直接 new），
  密码策略（强度校验、历史密码）无 SPI 扩展点；若需支持短信 OTP / WebAuthn 需先抽象 `IOtpVerifier` 接口。
