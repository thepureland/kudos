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

- `kudos-ms-sys-client`——拿子系统 / 租户元数据（active / accountTypeDictCode 等枚举）

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
- `kudos-ability-data-rdb-ktorm` / `kudos-ability-cache-common`
- `kudos-ms-sys-client`
- `kudos-base`（GoogleAuthenticator / PasswordKit / BCrypt）
