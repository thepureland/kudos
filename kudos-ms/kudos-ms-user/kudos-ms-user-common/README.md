# kudos-ms-user-common

`user` 原子服务共享契约层。包结构：**`io.kudos.ms.user.common` 下先按业务模块再分 `api` / `consts` / `enums` / `vo`**；横切内容放在 **`platform`**。

## 模块与 `IUser*Api` 对应关系

| 模块包 | 内容 |
|--------|------|
| `account` | `IUserAccountApi`、`IUserAccountThirdApi`、`IUserAccountProtectionApi`；账号、第三方账号、账号保护、组织用户相关 VO/枚举（原 `user` / `protection` / `orguser` 合并） |
| `passport` | `IPassportApi`（login / logout / verify / changePassword）、`SessionUserPrincipal`、`CurrentUserKit`、`PassportLoginStatusEnum`、`ChangePasswordResultEnum` |
| `login` | `IUserLoginRememberMeApi`；记住登录与登录日志相关 VO/枚举（原 `loginremember` / `loglogin` 合并） |
| `contact` | `IUserContactWayApi` |
| `org` | `IUserOrgApi` |

路径示例：`io.kudos.ms.user.common.account.api`、`io.kudos.ms.user.common.org.vo.request`。

## 依赖

- `kudos-ms-sys-common`（共享 sys 服务的枚举 / 错误码 / 字典常量）
- `spring-web`（**compileOnly**）——只为在 `IUser*Api` 方法上挂 `@GetMapping` / `@PostMapping`
  让 Feign 代理与 Spring MVC 控制器同时识别；运行期由消费方提供 spring-web 实现，本模块
  自身不绑死版本

> 与 `kudos-ms-sys-common` 同模式：注解**只放方法级**，不在接口类型上放 `@RequestMapping`，
> 避免 Feign 自动拼路径与 MVC 行为冲突。

## 与 `kudos-ms-user-core` 的边界

- `common` 只声明 **接口签名、VO、错误码枚举** —— **无任何实现**，避免拉入持久层 / 缓存依赖。
- 任何 server / client 模块都可以单独依赖本模块拿到契约：
  - server 侧（`user-core`）`implements IUser*Api`
  - client 侧（`user-client`）`extends IUser*Api` 做 Feign 代理
- `CurrentUserKit` 是契约层提供的"取当前登录用户"工具（基于线程上下文 / Session），上层调用者无需感知 `passport` 内部 state machine。

## 已知限制 / 后续工作

- ❗ **`spring-web` 是 `compileOnly`** — `IUser*Api` 接口上的 `@GetMapping` / `@PostMapping`
  只在 server / client 模块都引入 spring-web 时才有效。如果业务侧把本模块单独打入 SDK
  使用，运行期会因为类不可见拿不到注解元数据，需自行补 spring-web 依赖
- ❗ **注解只放方法级是约定但无静态检查** — 任何人在接口类型上加 `@RequestMapping`
  都会让 Feign 拼路径出错，目前没有 lint 规则 / 编译期守卫
- ❗ **VO/枚举跨服务复用没有版本控制** — `kudos-ms-auth` / `kudos-ms-msg` 直接以源码依赖
  方式引入本模块；不兼容的 VO 改动（删字段、改字段类型）必须全工程编译跑通才能感知
- ❗ **`CurrentUserKit` 依赖 ThreadLocal** — 在异步线程 / kotlin coroutine 切换上下文时会拿不到当前用户；
  目前未提供 `CoroutineContextElement` 适配，跨线程业务需主动透传

## 改进建议（自动分析 2026-06-11）

- ✅ 已修复（2026-06-11）**响应 VO 携带敏感凭据**（`account/vo/UserAccountCacheEntry.kt`、
  `account/vo/response/UserAccountDetail.kt` / `UserAccountEdit.kt` / `UserAccountRow.kt`）：
  新增 `account/support/UserAccountCredentialsErasure.kt` 提供 `eraseCredentials()` 脱敏拷贝扩展函数
  （置空 `loginPassword` / `securityPassword` / `authenticationKey` / `sessionKey`），api-internal /
  api-admin 全部出口在返回前统一调用；VO 类结构未变（契约兼容），
  彻底拆分"鉴权专用 VO 与脱敏资料 VO"仍为后续待办。
- ❗ **表单缺少必填与格式校验**（`account/vo/request/UserAccountFormCreate.kt` / `IUserAccountFormBase.kt`）：
  `username` / `tenantId` / `loginPassword` 均可空且无 `@NotBlank`；且 create 表单暴露 `loginErrorTimes` /
  `sessionKey` / `authenticationKey` / `lastLoginTime` 等内部状态字段，调用方可在创建时直接篡改安全计数器。
  建议收窄表单字段并补 Bean Validation 注解。
- ❗ **联系方式无格式校验**（`contact/vo/request/IUserContactWayFormBase.kt`）：`contactWayValue`（手机 / 邮箱）
  无 `@Email` / 手机号 pattern 校验，脏数据会直接进入 `getActiveContactValuesByUserIds` 下游（如消息服务发送失败）。
- **登录状态枚举缺"服务不可达"语义**（`passport/enums/PassportLoginStatusEnum.kt`）：client 侧
  `PassportFallback.login` 目前借用 `LOCKED` 表示 Feign 降级，语义误导（用户会被提示"账号被锁定"）。
  建议新增 `SERVICE_UNAVAILABLE` 枚举值（注意旧版本客户端反序列化兼容，需随版本发布）。
