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
