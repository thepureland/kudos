# Kudos 统一认证与全球第三方登录设计

> 状态：已评审，分阶段落地中
> 日期：2026-08-24
> 适用范围：`kudos-ms-auth`、`kudos-ms-user`、`kudos-ability-security` 及各 API/Client 模块

## 落地状态（2026-08-25）

首批代码已完成并保持旧 Passport API 兼容：

- Passport 公共边界已增加服务端采集的 IP/终端/浏览器/OS/User-Agent、Session ID 轮换、
  当前主体归属校验，以及成功和终态失败的登录审计；
- `kudos-ms-auth` 已建立无凭证快照的认证事务状态机、认证方法 SPI、密码/TOTP 适配器、
  乐观版本事务存储接口和公共事务 API；
- 已增加 Provider 模板与租户实例模型、Google/LINE/GENERIC_OIDC 种子、无密钥公开目录，
  以及可选的 `kudos-ms-auth-provider-oauth2` 动态 `ClientRegistration` 适配模块；
- `client_secret` 只保存引用，默认解析器支持受限环境变量/配置属性；独立的可选模块已支持
  HashiCorp Vault KV v1/v2、AWS Secrets Manager、Google Secret Manager 和 Azure Key Vault；
- 认证事务、外部登录 state 及 Spring Security 授权请求快照已支持 Redis TTL 和原子一次性消费；
  标准 OIDC 浏览器流已接通强制 PKCE S256、Spring Security state/nonce/token 校验、
  `ExternalPrincipal` 转换、已绑定账号登录和本地 Session 签发；授权发起与回调可跨节点完成；
- 已完成外部身份自助生命周期：当前本地 Session 用户在近期重新认证后，可以按 Provider 的
  `MANUAL_CONFIRM` 策略显式绑定或软解绑；回调事务固定操作者、租户和 Provider，不能由请求传入
  任意 `userId`；解绑最后一种可用登录方式会被拒绝；
- `user_account_third` 已按租户 Provider 实例增加身份唯一约束，并增加追加式绑定审计表；外部
  `subject` 在审计表中仅保存 SHA-256，不保存原始值；
- 管理员预绑定/代解绑已收敛到 Auth Admin 显式命令，按当前管理员租户和 Provider 配置确定可信
  字段，并以 `@RequiresPermission` 分别保护；操作原因、操作者及变更前后安全快照均进入审计；
- `INVITE_ONLY` 已完成面向既有本地用户的一次性邀请闭环：邀请固定租户、Provider 和用户，支持
  失效时间、吊销及可选的上游已验证邮箱约束；原始 token 只返回一次，库内 token、邮箱和消费
  subject 均仅保存 SHA-256，回调中的消费与首次绑定处于同一事务；
- `JIT_CREATE` 已完成外部身份首次登录自动开户：本地账号和首条绑定原子创建，并发回调依靠稳定
  用户名、身份唯一约束、独立事务回滚及胜出绑定重读保证最终只有一个账号；该流程不按邮箱合并
  已有账号，JIT 账号默认没有本地密码，绑定审计动作明确记录为 `JIT_BIND`；
- 已增加 Provider 一对一的类型化 JIT 配置与 Auth Admin 查询/保存接口：支持三种稳定用户名策略、
  上游已验证邮箱和域名准入、默认组织/上级/账号类型/状态/locale/时区/货币；组织和上级按当前
  租户及活动状态校验，默认组织同时写入账号主组织和组织成员关系；
- 已完成租户 Provider 管理 API：模板列表、实例列表/详情/创建/更新/启停均使用明确权限点和可信
  管理员上下文；Provider code 与模板创建后不可变，不提供破坏绑定引用的物理删除；长期密钥只接受
  `scheme:location` 引用，响应只暴露是否已配置，创建/变更操作者和原因写入领域表；
- 已完成 Provider 一对一的类型化 claim mapping：支持 subject、用户名、昵称、邮箱/验证状态、
  电话/验证状态、头像、locale、union id 的有序安全路径，支持嵌套 Map/List 且不执行表达式；OIDC
  subject 始终取协议校验后的 `sub`，OAuth2 才允许配置 subject 路径；
- Passport 已增加认证尝试防护：请求按服务端 IP 及租户+用户名双维度限流，密码和 TOTP 失败分别
  使用独立窗口；Redis Lua 保证多节点、多桶检查与计数原子性，无 Redis 时回退单机内存实现，存储
  异常默认 fail-closed，也可由部署方显式选择 fail-open；旧账号自动冻结语义保持兼容；
- Passport 公网登录与 Auth 密码适配器已统一防枚举语义：用户不存在、密码错误、账号停用、自动锁定
  和管理员冻结均只返回 `INVALID_CREDENTIALS`，不暴露错误次数或冻结标题；Passport 内部结果和登录
  审计仍保留真实原因，不存在账号及账号状态拒绝分支会执行一次 BCrypt 校验以缓解明显的时序侧信道；
- Auth 密码/TOTP 与外部身份成功路径已统一注册逻辑认证会话：会话记录携带 `amr/acr`、认证时间、
  凭证版本、客户端观测信息、闲置与绝对过期、乐观版本及撤销原因；Redis 可用时采用分布式 CAS
  存储，无 Redis 时回退单机内存；认证事务只暴露不可作为 bearer 使用的逻辑会话 ID，不再返回
  servlet 容器 Session ID；Web Filter 会校验、续期或失效 Auth 签发的浏览器会话；用户会话索引、
  多设备列表、用户主动远程撤销，以及租户受限的管理员会话查看/逐台撤销均已接通；
- Step-up 首个闭环已接入统一认证事务：创建入口只接受目标 `acr` 和方法，用户、租户及原逻辑会话
  均从当前受管 Session 固定；密码适配器忽略客户端用户名，成功主体必须与发起人一致。默认 ACR
  顺序为 password/federated < mfa < phishing-resistant，未知 ACR 只允许精确匹配，策略可替换；成功
  后使用 CAS 提升原会话、合并 `amr`、更新 `authTime` 并旋转 HttpSession ID，但不创建第二条逻辑
  会话，也不延长绝对到期时间；
- 敏感业务声明式执行点已落地：Controller 或服务入口可用 `@RequiresAuthenticationAssurance`
  声明最低 ACR，并按需设置 `maxAgeSeconds`；浏览器 Session 与 JWT Filter 都把本次请求实时验证过的
  权威逻辑会话交给统一校验器。无会话、强度不足和认证过旧均 fail-closed 为结构化 HTTP 403
  Step-up challenge；类级要求可作为默认值，方法级要求可进一步收紧；
- 外部身份自助绑定和解绑已成为首批真实 Step-up 消费端点：不再信任客户端回传的已完成认证事务，
  而是校验当前请求经 Session/JWT Filter 实时确认的权威逻辑会话；最低要求为 password/federated
  等级，认证新鲜度继续使用可配置的 `link-reauthentication-max-age-seconds`（默认 300 秒）。旧
  `reauthenticationTransactionId` 查询参数暂时作为无效兼容参数保留，客户端可直接停止发送；
- TOTP 自助注册首个安全闭环已完成：近期认证后生成短期待确认注册，待确认密钥在 Redis/内存中只以
  AES-GCM 密文保存，验证码成功才原子消费并写入正式账号；错误次数有上限，注册固定当前用户/租户，
  启用和撤销沿用认证生命周期事件使全部旧会话与 Token 失效。正式 `authentication_key` 列已改为
  透明 AES-GCM 加密并兼容读取历史明文，Flyway 将列扩为 512 字符；Ktorm 对加密列的 DEBUG 参数
  统一脱敏，并关闭可能输出解密实体的 TRACE 日志，避免凭证在诊断日志中泄露；
- MFA 恢复码闭环已完成：默认生成 10 个 80-bit 可读恢复码，原文只在轮换响应中出现一次；数据库
  仅保存带租户、用户和恢复码组作用域的 SHA-256，新组原子撤销旧组，单码通过条件更新保证并发下
  只能消费一次。密码事务在确有剩余恢复码时同时允许 TOTP/恢复码，成功会登记
  `amr=password,recovery_code` 和 MFA ACR；失败使用独立限流桶。TOTP 更换或撤销会自动撤销恢复码；
- 租户 MFA 策略基础层已完成：`OPTIONAL`、`REQUIRED`、`CONDITIONAL` 使用类型化配置和独立决策服务，
  条件可按账号类型或角色代码组合，支持 0～90 天注册宽限期、允许因子和恢复码开关；策略按租户一对一
  持久化并记录操作者、原因和时间。管理端不能传入租户，当前用户可读取不含密钥的有效决策。宽限期
  起点取账号创建时间和策略生效时间中的较晚者，避免新策略立即锁死存量账号；
- 租户 MFA 策略已接入本地密码和第三方登录事务：宽限期内允许登录，并通过无密钥的
  `postAuthenticationActions=ENROLL_MFA` 提示登录后注册；宽限期结束仍未注册时以
  `MFA_ENROLLMENT_REQUIRED` 阻断。已注册但第三方登录强度不足时，OAuth2/OIDC 回调只返回原事务的
  TOTP/恢复码挑战，不提前创建本地 Session；公开 action 接口验证成功后才签发并绑定逻辑会话。
  TOTP 与恢复码使用独立限流桶，`recoveryCodesEnabled` 已同时约束生成、状态查询、消费和挑战可见性；
- WebAuthn 凭证领域基础层已完成：`auth_webauthn_credential` 仅保存 credential ID、user handle、COSE
  公钥、transports、AAGUID、备份/可发现状态和签名计数，不保存私钥或 ceremony challenge。所有查询、
  撤销和断言状态更新均固定租户与用户；签名计数通过旧值 CAS 推进并拒绝回退/重放。账号删除会清理
  凭证，注册或撤销会使用独立生命周期原因使旧 Session/Token 失效，但不会误撤销 TOTP 恢复码；MFA
  注册判断已可同时识别 TOTP 与 WebAuthn；
- 可选 `kudos-ms-auth-provider-webauthn` 已引入 Yubico `webauthn-server-core`，完成注册 ceremony 发起。
  服务端固定租户、用户、RP ID、可信 origin、user verification 与 resident key，并将现有 credential
  放入 `excludeCredentials`；完整 library request JSON 以 256-bit 随机 ceremony id 短期保存。有 Redis
  时使用 Lua 原子创建/一次性消费和 TTL，多节点共享；无 Redis 时回退进程内一次性存储；
- registration finish 已恢复服务端保存的完整 library request，并由 Yubico 验证 challenge、type、
  RP ID hash、origin、user presence/user verification 及 attestation。协议失败的 ceremony 同样不能重放；
  只有验证结果能转换为 core 的 verified command 并持久化公开凭据材料；
- 当前受管 Session 用户的注册、凭证摘要查询、重命名与撤销公开 API 已完成；租户/用户固定取 Session，finish
  的 ceremony id 固定取路径，所有操作要求近期 password-or-stronger 认证且逻辑会话与 principal 一致；
- 管理端只读凭证审计 API 已完成，以 `auth:webauthn-credential:view` 独立授权；目标账号从可信 User
  记录核对当前管理员租户，跨租户与不存在账号统一 404。响应覆盖活动与已撤销记录，但原始 credential
  ID 仅返回 SHA-256 Base64url 指纹，且不包含 COSE 公钥、user handle 或签名计数；
- assertion begin/finish 已同时覆盖已知用户和无用户名登录：已知用户的 allow list 固定为其租户内活动
  credential，无用户名流程由 discoverable credential 的 user handle 反查活动账号。Yubico 验证 challenge、
  RP ID hash、origin、签名、UP/UV、备份与计数器后，服务层再次校验 credential、user handle、用户名、
  租户和预期主体，最后才以旧计数 CAS 更新凭证；
- App/API Token 首个闭环已落地：可选 JWT 模块签发最长 5 分钟的 Access Token，每请求校验签名、
  issuer、audience、权限版本和逻辑会话；Refresh Token 使用 256-bit 随机值、数据库只存 SHA-256，
  支持单次轮换、父子 family、旧令牌重放整族撤销和管理员 token epoch 强制失效；
- User 域已用专用事件联动登录密码变更、账号停用、冻结、TOTP 认证器变更及账号删除；Auth 在原事务
  提交后统一递增 token epoch、撤销全部 Refresh family 和逻辑会话。当前用户可调用
  `POST /api/public/auth/logout-all` 复用同一链路，管理端强制 token 失效也同步完成全端下线；
- User 域已收口本地密码持久化边界：账号创建、通用更新、管理员重置和自助改密共用可替换的
  `IPasswordPolicy`，默认长度优先且不强制字符组合；普通输入统一写成 `{bcrypt}` 版本化编码并拒绝超过
  72 UTF-8 字节，
  自助改密拒绝复用当前密码。通用更新不能跨租户或直接覆盖 TOTP/session key，登录密码和安全密码
  变化都会进入全端失效链路；JIT 空密码与可信迁移的版本化/历史 BCrypt 分别保留既有语义；
- 登录成功后会透明升级历史无前缀 BCrypt：仅在全部认证因子通过后重算，通过“旧哈希仍相等”的
  compare-and-set 防止覆盖并发改密；升级只清理账号缓存，不写密码历史、不撤销会话，失败不影响登录；
- Auth 域已建立过渡期密码历史：`auth_password_history` 只保存退役密码编码，兼容无前缀 BCrypt 和
  `{bcrypt}`，按租户、用户和登录/安全
  密码用途隔离，默认保留最近 5 个且最多 24 个；同进程装配通过 `IPasswordHistory` 自动接入 User
  写边界，在账号事务内完成复用校验、旧哈希归档和裁剪，账号删除后清理，不传输或持久化原始密码；
- 已为状态机、密码/TOTP 适配、公共控制器、登录审计、Provider 目录、端点校验和动态注册补充测试，
  Flyway 迁移已通过完整 Spring 上下文集成验证。

尚未完成、不得视为已具备生产闭环的部分：

- WebAuthn/Passkey 的凭证持久化、签名计数 CAS、registration/assertion begin/finish、签名验证、一次性
  challenge store、注册/查询/重命名/撤销公开 API、事务绑定的 Passwordless 登录、Passkey Step-up、密码/第三方
  登录后的 Passkey 第二因素与逻辑会话签发均已完成，管理端只读凭证审计后端也已接入。租户级
  attestation format、AAGUID allow/deny 和可信证明要求的持久化、注册执行点及租户固定的管理 API 已完成；
  内置 FIDO MDS trust source、存量凭证 AAGUID 风险审计信号，以及租户显式风险阻断策略也已完成；管理员发起的
  受控凭证吊销（Passkey 与 TOTP，带独立权限、原因、可选安全事件关联和追加式记录）已完成（V59），且与 V57 的
  注册豁免构成闭环：吊销不因"这是最后一个因子"被拒绝，但会告知操作者账号是否已被挡在门外。**基于 MDS 状态或风险
  评估的自动吊销**、企业 PKI trust source、企业设备治理和凭证管理控制台 UI 仍待建设——自动吊销尤其需要先解决
  "误判即批量锁死账号"的问题，不能仅凭元数据更新直接执行。MFA 策略持久化、登录强制、
  TOTP 自助注册/确认/撤销、恢复码、Step-up、
  原会话提升与声明式执行点已完成，外部身份自助绑定/解绑已接入，其他业务端点仍需按风险逐项声明要求；
- 宽限期已过但尚未注册 MFA 的账号已有逐用户临时豁免作为受控救援入口（见 10.3），不再需要管理员调整整个租户的
  策略或生效时间。仍待设计的是管理员协助注册流程与受控救援码；普通恢复码依旧不是未注册账号的救援入口；
- Provider 管理控制台 UI、密钥写入控制面及 SAML/LDAP/JustAuth 扩展模块。

## 1. 背景

Kudos 当前已经具备用户名密码、账号冻结、错误次数、TOTP、HttpSession、第三方账号绑定、登录日志、
Remember Me、JWT 和权限管理等基础能力，但这些能力分散在多个模块中，尚未组成一套完整、可扩展的
认证体系。

当前主要实现位置如下：

- `kudos-ms-user` 的
  [`PassportService`](../kudos-ms/kudos-ms-user/kudos-ms-user-core/src/io/kudos/ms/user/core/passport/service/impl/PassportService.kt)
  负责用户名密码、账号状态、冻结和 TOTP 校验。
- `kudos-ms-user-api-public` 的
  [`PassportPublicController`](../kudos-ms/kudos-ms-user/kudos-ms-user-api-public/src/io/kudos/ms/user/api/public/controller/passport/PassportPublicController.kt)
  在登录成功后创建 `HttpSession`。
- `user_account_third` 和
  [`UserAccountThird`](../kudos-ms/kudos-ms-user/kudos-ms-user-core/src/io/kudos/ms/user/core/account/model/po/UserAccountThird.kt)
  保存本地用户与第三方身份的绑定关系。
- `kudos-ms-auth` 当前主要承担角色、用户组、资源、数据范围和授权关系管理。
- `kudos-ability-security-*` 已经提供密码编码、TOTP、JWT 签发/校验和 Resource Server 能力。

本设计的目标是将现有能力演进为一套面向多行业、多租户、多终端和全球身份提供方的统一认证与授权框架。

## 2. 设计目标

### 2.1 核心目标

1. 将 `kudos-ms-auth` 从当前偏 RBAC 的授权服务升级为统一认证与授权服务。
2. 严格区分身份主数据、认证、会话和授权职责。
3. 支持用户名密码、TOTP、短信/邮箱验证码、恢复码、Passkey/WebAuthn 等认证方法。
4. 支持 Google、LINE、Apple、Microsoft、GitHub、微信、企业微信、钉钉、飞书等全球第三方登录。
5. 支持通用 OIDC、OAuth2、SAML 2.0、LDAP/AD、CAS 等协议或接入方式。
6. 支持租户级 Provider 配置、账号绑定策略、JIT 创建、MFA 和风险策略。
7. 同时支持浏览器 Session 和 App/API Token 模式。
8. 为业务系统提供稳定的认证 SPI，避免业务代码直接依赖具体第三方 SDK。
9. 兼容现有 `/api/public/user/passport/**` 接口，并允许分阶段迁移。

### 2.2 非目标

首期不以一次性实现以下能力为目标：

- 银行交易签名、政务 CA、医保凭证等行业专有协议的完整实现。
- 自行实现 OAuth2/OIDC/SAML 的密码学和协议底层。
- 强制所有部署使用 JWT，或强制所有部署使用服务端 Session。
- 在核心模块中硬编码所有第三方厂商的业务规则。
- 用第三方 Provider 的 access token 直接充当 Kudos 的本地登录凭证。

行业专有认证应通过统一 SPI 扩展。

## 3. 术语与领域边界

### 3.1 Identity、Authentication 和 Authorization

```text
Identity（身份）
    回答：这个用户是谁？

Authentication（认证）
    回答：调用者是否证明了自己？使用了什么认证方法？认证强度多高？

Authorization（授权）
    回答：已认证用户可以访问哪些资源和数据？
```

### 3.2 服务职责

#### `kudos-ms-user`：身份目录和用户主数据

负责：

- 用户账号和基本资料。
- 联系方式。
- 组织关系。
- 用户启用、停用、注销等生命周期状态。
- 管理员冻结等账号级限制。
- 外部身份与本地用户的绑定关系。
- 向认证服务提供最小化、受控的内部身份查询能力。

#### `kudos-ms-auth`：统一安全中心

负责两类清晰分隔的子域：

```text
kudos-ms-auth
├── authentication
│   ├── 认证事务
│   ├── 认证方法
│   ├── 外部身份提供方
│   ├── MFA 与 Step-up
│   ├── 风险和登录策略
│   ├── Session
│   ├── Access Token / Refresh Token
│   └── 登录安全审计
│
└── authorization
    ├── 角色
    ├── 用户组
    ├── 资源和权限
    ├── 数据范围
    ├── 授权关系
    └── 授权决策
```

认证和授权位于同一原子服务，但包、接口、表和领域事件必须保持清晰分离。

#### `kudos-ability-security-*`：可复用安全技术能力

负责与具体业务领域无关的能力：

- `PasswordEncoder`。
- TOTP 算法。
- JWT Encoder/Decoder。
- OAuth2/OIDC/SAML 客户端封装。
- WebAuthn 基础校验。
- 安全过滤器和 Spring Boot 自动装配。

## 4. 目标架构

```text
Web / Mobile / Desktop / CLI
              │
              ▼
     kudos-ms-auth api-public
              │
              ▼
   Authentication Orchestrator
              │
     ┌────────┼─────────┬───────────────┐
     ▼        ▼         ▼               ▼
 Password   OIDC      OAuth2          SAML/LDAP
   TOTP    Google     GitHub          Enterprise IdP
  Passkey   LINE      WeChat...
     │        │         │               │
     └────────┴─────────┴───────────────┘
              │
              ▼
    External/Local Principal
              │
              ▼
      kudos-ms-user identity
     账号状态 / 外部身份绑定 / JIT
              │
              ▼
      Session / Token Issuer
              │
              ▼
      kudos-ms-auth authorization
     Role / Group / Resource / Scope
```

关键原则：

- 第三方认证成功后，Kudos 签发自己的 Session 或 Token。
- 第三方 access token 默认不进入 Kudos 本地会话。
- 外部身份解析和本地账号映射是两个独立步骤。
- 所有认证方法最终输出统一的本地认证结果。

## 5. 从单次 Login 升级为认证事务

当前 `PassportService.login()` 适合“用户名密码 + 可选 TOTP”，但无法自然表达第三方回调、多步骤 MFA、
首次登录补充资料、多租户选择和账号绑定。

新体系以 `AuthenticationTransaction` 为核心：

```text
创建认证事务
      │
      ▼
识别租户和候选账号
      │
      ▼
选择或执行认证方法
      │
      ├── 密码
      ├── TOTP / 短信 / 邮箱
      ├── Passkey
      └── 跳转第三方 IdP 并等待回调
      │
      ▼
账号绑定 / JIT / 补充资料 / Step-up
      │
      ▼
创建 Session 或签发 Token
```

### 5.1 事务状态

建议状态：

- `PENDING`
- `WAITING_FOR_ACTION`
- `WAITING_FOR_EXTERNAL_PROVIDER`
- `CHALLENGE_REQUIRED`
- `ACCOUNT_LINK_REQUIRED`
- `PROFILE_REQUIRED`
- `COMPLETED`
- `FAILED`
- `EXPIRED`
- `CANCELLED`

### 5.2 下一步动作

建议动作：

- `IDENTIFY_ACCOUNT`
- `SELECT_TENANT`
- `SELECT_METHOD`
- `VERIFY_PASSWORD`
- `VERIFY_TOTP`
- `VERIFY_SMS`
- `VERIFY_EMAIL`
- `VERIFY_PASSKEY`
- `REDIRECT_EXTERNAL_PROVIDER`
- `LINK_ACCOUNT`
- `ENROLL_MFA`
- `CHANGE_EXPIRED_PASSWORD`
- `ACCEPT_TERMS`
- `COMPLETE`

API 不应通过不断扩充 `PassportLoginStatusEnum` 来表达所有组合，而应返回当前事务状态、允许执行的动作和
安全的用户提示。

### 5.3 统一认证上下文

认证成功后生成：

```kotlin
data class AuthenticationContext(
    val userId: String,
    val tenantId: String,
    val sessionId: String,
    val authTime: Instant,
    val amr: Set<String>,
    val acr: String,
    val credentialVersion: Long,
    val riskLevel: String?,
)
```

- `amr` 表示实际使用的方法，例如 `password`、`totp`、`oidc`、`webauthn`。
- `acr` 表示最终达到的认证强度。
- 业务敏感操作可以要求特定 `acr`，触发 Step-up Authentication。

当前 Step-up 基础接口为：

```text
POST /api/public/auth/authentication/transactions/step-up
     { "requiredAcr": "urn:kudos:acr:mfa", "requestedMethod": "password" }

POST /api/public/auth/authentication/transactions/{id}/actions/{action}
```

创建端必须已有受管逻辑会话，不能从请求接受 `userId`、`tenantId` 或源 Session ID。事务以
`STEP_UP` purpose 固定这些服务端事实；认证结果若主体不一致或未达到目标 ACR 会终止且不提升会话。
默认策略只内建 `password/federated`、`mfa`、`phishing-resistant` 的保守顺序，行业自定义 ACR 可通过
`IAuthenticationAssurancePolicy` 替换。敏感入口通过统一注解声明要求：

```kotlin
@RequiresAuthenticationAssurance(
    acr = "urn:kudos:acr:mfa",
    maxAgeSeconds = 300,
)
fun transfer(...) { ... }
```

`maxAgeSeconds` 默认为 `-1`，表示只校验 ACR；需要近期重新认证的高风险操作必须显式填写。类级注解
是默认要求，方法级注解优先。浏览器 Session 和 Bearer JWT 都只使用本次请求经过权威存储校验后的
`AuthenticationSession`，不信任请求参数或 Token 内孤立的 ACR 声明。失败统一返回 HTTP 403：

```json
{
  "success": false,
  "code": "AUTHENTICATION_ASSURANCE_REQUIRED",
  "message": "Additional authentication is required.",
  "reason": "INSUFFICIENT_ACR",
  "requiredAcr": "urn:kudos:acr:mfa",
  "maxAgeSeconds": 300,
  "stepUpEndpoint": "/api/public/auth/authentication/transactions/step-up"
}
```

`reason` 为 `AUTHENTICATION_REQUIRED`、`INSUFFICIENT_ACR` 或 `AUTHENTICATION_TOO_OLD`。客户端使用
响应中的 `requiredAcr` 创建 Step-up 事务；服务端仍负责固定用户、租户和源 Session。后续需补 MFA
注册、恢复码和 WebAuthn 方法，并在各业务模块逐项标记真实敏感端点。

## 6. 认证方法 SPI

认证编排核心不得直接依赖 `GoogleAuthenticator`、JustAuth、Spring `OidcUser` 等具体实现。

建议 SPI：

```kotlin
interface AuthenticationMethodProvider {
    val method: String

    fun supports(context: AuthenticationRequestContext): Boolean

    fun begin(
        transaction: AuthenticationTransaction,
        request: BeginAuthenticationRequest,
    ): AuthenticationChallenge

    fun verify(
        transaction: AuthenticationTransaction,
        request: VerifyAuthenticationRequest,
    ): AuthenticationMethodResult
}
```

内置认证方法：

- `password`
- `totp`
- `sms_otp`
- `email_otp`
- `recovery_code`
- `webauthn`
- `oidc`
- `oauth2`
- `saml2`
- `ldap`
- `cas`

行业系统可以扩展 CA、硬件 Key、扫码 App、医保凭证等实现。

## 7. 全球第三方登录设计

### 7.1 按协议分类，不按国家分类

第三方 Provider 应按照协议能力分类：

#### 标准 OIDC

优先使用统一 OIDC 引擎：

- Google
- LINE
- Microsoft Entra ID
- Apple
- Okta
- Auth0
- Keycloak
- Amazon Cognito
- Google Workspace
- 企业自建 OIDC

Google 提供标准 OIDC Discovery；LINE Login v2.1 也基于 OAuth2/OIDC 并支持 ID Token。

#### OAuth2 + UserInfo

使用通用 OAuth2 引擎和 Provider 模板：

- GitHub
- GitLab
- Facebook
- LinkedIn
- Slack
- Discord
- X
- Spotify
- Twitch
- Reddit
- Dropbox

这类 Provider 的授权码流程通常接近标准，但用户资料端点、主键字段、邮箱获取和 scope 存在差异。

#### 专属或非标准 Provider

使用专属适配器或 JustAuth：

- 微信、企业微信
- QQ
- 钉钉
- 飞书
- 支付宝
- 微博
- 抖音
- 淘宝
- Kakao、Naver 等地区性平台
- 厂商私有 OAuth/SSO

#### 企业协议

- SAML 2.0
- LDAP/Active Directory
- CAS
- Kerberos/SPNEGO

### 7.2 技术选型

#### Spring Security OAuth2 Client：主引擎

采用 `spring-boot-starter-oauth2-client` 处理标准 OAuth2/OIDC：

- Authorization Code。
- PKCE。
- state、nonce。
- ID Token、issuer、audience 和 JWK 签名校验。
- UserInfo。
- access token 获取和刷新。
- Spring Security 成功/失败处理链。

Kudos 需要实现数据库驱动的动态 `ClientRegistrationRepository`，而不是只依赖 YAML 中的静态配置。

#### JustAuth：非标准 Provider 兼容层

JustAuth 覆盖国内外大量平台，包括微信、钉钉、飞书、支付宝、Google、Facebook、Amazon、Slack、LINE 等。

在 Kudos 中的定位是：

```text
生成授权 URL → code 换 token → 调 Provider API 获取外部用户资料
```

它不负责：

- Kudos 认证事务。
- 本地账号绑定。
- JIT 创建。
- Session/Token。
- MFA。
- 风控、锁定和审计。

标准 OIDC Provider 应优先使用 Spring Security，JustAuth 用于非标准差异和厂商专属适配。

#### Spring Security SAML2 / LDAP

- 企业 SAML 使用 `spring-security-saml2-service-provider`。
- LDAP/AD 使用 Spring LDAP/Spring Security LDAP。
- SAML 同样需要数据库驱动的动态 `RelyingPartyRegistrationRepository`。

#### pac4j

pac4j 能统一覆盖 OIDC、OAuth、SAML、CAS、Kerberos 和 LDAP，但会与当前 Spring Security 的 Filter、
SecurityContext、Session 和认证对象形成第二套安全体系。因此本设计不选用 pac4j 作为并行核心。

#### Keycloak

Keycloak 可以作为可选的外部 Identity Broker，由 Kudos 通过标准 OIDC 接入，但不是 Kudos 核心运行时的
强制依赖。

### 7.3 Provider 模板与租户实例

Provider 模板描述平台默认协议，租户实例保存租户自己的 client 配置。

```text
auth_provider_template
  id
  code
  protocol
  discovery_uri
  authorization_uri
  token_uri
  user_info_uri
  subject_claim
  default_scopes
  adapter_type
  default_claim_mapping
  logo_uri
  active

auth_identity_provider
  id
  tenant_id
  template_id
  code
  display_name
  issuer
  client_id
  client_secret_ref
  scopes
  custom_config
  jit_policy
  link_policy
  active
```

建议首批模板：

```text
GENERIC_OIDC、GENERIC_OAUTH2、GENERIC_SAML2

GOOGLE、LINE、APPLE、MICROSOFT、GITHUB、GITLAB、
FACEBOOK、LINKEDIN、SLACK、DISCORD、X、AMAZON、
TWITCH、SPOTIFY、KAKAO、NAVER

WECHAT、WECHAT_WORK、QQ、DINGTALK、FEISHU、
ALIPAY、WEIBO、DOUYIN、GITEE
```

`client_secret_ref` 应指向 Vault/KMS/加密配置，不应在普通查询接口中返回明文 secret。

### 7.4 统一外部身份模型

所有底层实现统一转换为 Kudos 模型：

```kotlin
data class ExternalPrincipal(
    val providerId: String,
    val protocol: ExternalProtocol,
    val issuer: String?,
    val subject: String,
    val unionId: String? = null,
    val username: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val emailVerified: Boolean? = null,
    val phone: String? = null,
    val phoneVerified: Boolean? = null,
    val avatarUrl: String? = null,
    val locale: String? = null,
    val rawClaims: Map<String, Any?> = emptyMap(),
)
```

业务层不得直接依赖 `OidcUser`、JustAuth `AuthUser` 或 `Saml2AuthenticatedPrincipal`。

### 7.5 Provider 扩展钩子

即便同属 OAuth2/OIDC，不同 Provider 仍存在差异：

- Apple client secret 是签名 JWT，姓名通常只在首次授权返回。
- LINE 支持 `bot_prompt` 等自定义参数。
- Google 有 `hd`、`prompt`、`access_type` 等扩展参数。
- GitHub 私有邮箱可能需要额外请求。
- Microsoft issuer 可能按 tenant、organizations、common 等模式变化。
- Provider 对 refresh、revoke、logout 的支持不同。

建议提供：

```kotlin
interface ExternalProviderAdapter {
    fun customizeAuthorizationRequest(context: AuthorizationRequestContext)
    fun customizeTokenRequest(context: TokenRequestContext)
    fun loadPrincipal(context: ExternalTokenContext): ExternalPrincipal
    fun validatePrincipal(principal: ExternalPrincipal)
    fun revoke(context: ExternalGrantContext): Boolean
}
```

### 7.6 分布式授权请求快照

OAuth2/OIDC 发起授权时，Spring Security 生成的 `OAuth2AuthorizationRequest` 包含 PKCE verifier、
OIDC nonce、redirect URI、scope 和 Provider registration id。该快照不能只保存在发起节点的
`HttpSession`，否则回调到另一节点时无法完成 code exchange 和 nonce 校验。

当前 `kudos-ms-auth-provider-oauth2` 已实现标准 `AuthorizationRequestRepository` 扩展：

- 存在 `RedisTemplates` 时，将完整快照写入 Redis，默认 TTL 为 300 秒；回调使用 Lua 原子读取并删除，
  同一 state 只能消费一次。
- Redis 键只保存 state 的 SHA-256，不暴露原始 state；空 state、超过 512 字符的 state 和重复 state
  均拒绝处理。
- 无 Redis 的单节点部署回退到带过期清理的内存存储；此模式仍要求授权发起与回调命中同一进程。
- 快照存储不创建 Session；登录成功后的 Kudos 本地 Session 是另一生命周期，仍按浏览器会话策略管理。
- 部署方可通过实现 `IExternalAuthorizationRequestStore` 替换底层存储，但不得削弱 TTL、碰撞保护和
  一次性消费语义。

```properties
kudos.ms.auth.external-login.authorization-request-ttl-seconds=300
```

配置允许范围为 1 至 900 秒。该时间应覆盖正常 Provider 跳转，同时保持足够短以限制未完成流程的
敏感快照存活时间。

## 8. 外部身份与账号绑定

### 8.1 稳定身份键

外部身份的唯一键应为：

```text
providerInstanceId + issuer + subject
```

不能使用以下字段作为稳定身份主键：

- 邮箱。
- 手机号。
- 昵称。
- 展示用户名。
- 头像 URL。

建议将当前 `user_account_third` 演进为 `user_external_identity`：

```text
user_external_identity
  id
  user_id
  provider_id
  issuer
  subject
  union_id
  external_username
  external_display_name
  external_email
  email_verified
  external_phone
  phone_verified
  avatar_url
  claims_snapshot
  last_login_time
  active
```

当前按 `(userId, providerCode)` 的模型无法完整表达同一租户配置多个 OIDC/AD 实例，也不利于同一用户在
多个 provider instance 下建立身份，应迁移为以 `provider_id` 为中心的唯一关系。

### 8.2 绑定策略

每个租户和 Provider 可以选择：

- `BOUND_ONLY`：只允许预先绑定的身份登录，默认最安全。
- `INVITE_ONLY`：必须匹配有效邀请。
- `JIT_CREATE`：首次登录自动创建本地用户。
- `ADMIN_PREPROVISIONED`：用户必须由管理员或 SCIM 预创建。
- `MATCH_VERIFIED_EMAIL`：显式开启时允许按已验证邮箱匹配。
- `MANUAL_CONFIRM`：要求用户再次使用已有认证方法确认后绑定。

默认禁止仅凭相同邮箱自动绑定。即使第三方返回 `email_verified=true`，也需要考虑邮箱回收、租户边界和
Provider 配置可信度。

### 8.3 绑定和解绑安全要求

- 已登录用户绑定新身份前必须进行近期重新认证。
- 解绑身份前必须确认用户仍有至少一个可用登录方法，除非执行账号关闭流程。
- 管理员代绑定/解绑必须记录操作者、原因和变更前后值。
- 回调必须幂等，避免重复 JIT 创建账号。
- 外部身份唯一约束冲突必须转为明确的“身份已绑定其他账号”结果，不能静默迁移。

### 8.4 当前已落地的自助生命周期

当前实现采用 `LOGIN`、`STEP_UP` 和 `LINK_EXTERNAL_IDENTITY` 等认证事务。发起 link/unlink 时，
服务端不接受客户端用事务 ID 证明“已经重认证”，而是读取当前请求经权威存储实时校验的逻辑
`AuthenticationSession`。会话至少达到 password/federated 等级，且 `authTime` 必须处于
`kudos.ms.auth.external-login.link-reauthentication-max-age-seconds` 配置窗口内（默认 300 秒）。不满足时
统一返回结构化 HTTP 403 Step-up challenge；完成 Step-up 后，原逻辑会话会被提升，客户端重试原请求
即可。验证通过后，link 才创建新的、服务端固定操作者和 Provider 的绑定事务，再跳转 Provider。

实际接口为：

```text
GET    /api/auth/external/{providerId}/link
GET    /api/auth/external/bindings
DELETE /api/auth/external/bindings/{bindingId}
```

过渡期仍接受旧 `reauthenticationTransactionId` 查询参数，但不读取、不信任；删除该兼容参数前应经过
正式 API 废弃周期。

约束如下：

- 只有 Provider 实例的 `link_policy=MANUAL_CONFIRM` 时允许用户自助绑定；
- 外部 `issuer/subject` 只接受 OAuth2/OIDC 回调解析出的已校验结果；
- link 回调完成后轮换 Session ID，长期 Session 不保留上游 access/refresh token；
- unlink 为软删除（`active=false`），登录映射只接受活动绑定；
- 无密码账号解绑最后一个活动外部身份时返回 `LAST_AUTHENTICATION_METHOD`；
- 数据库以 `(tenant_id, identity_provider_id, subject)` 和
  `(user_id, identity_provider_id)` 唯一索引作为并发冲突的最终防线；
- bind/unbind 成功和拒绝均写入追加式审计；审计仅保存 `subject` 哈希。

历史 `/api/admin/user/accountThird/**` 已降为只读，`IUserAccountThirdService` 也不再暴露通用写接口。
管理员身份生命周期统一从 Auth Admin 进入：

```text
POST /api/admin/auth/externalIdentity/prebind
     permission: auth:external-identity:prebind

POST /api/admin/auth/externalIdentity/unbind
     permission: auth:external-identity:unbind
```

这两个命令不接受可信 `tenantId`、`actorUserId` 或 `providerCode`：租户和操作者取当前管理员主体，
Provider code/issuer 取活动 Provider 模板与租户实例。请求必须填写 1–512 字符的操作原因；代解绑仍
执行最后登录方式保护。审计动作分别为 `ADMIN_BIND`、`ADMIN_UNBIND`，保存原因和变更前后安全快照，
快照中的 `subject` 同样只保存 SHA-256。

### 8.5 当前已落地的邀请制首次绑定

`jit_policy=INVITE_ONLY` 当前用于将一个尚未知晓 `subject` 的外部身份安全绑定到租户内**已经存在**
的本地用户；该策略本身不自动创建 `user_account`，自动开户由下一节的 `JIT_CREATE` 独立处理。

管理员接口为：

```text
POST /api/admin/auth/externalInvitation/create
     permission: auth:external-invitation:create

POST /api/admin/auth/externalInvitation/revoke
     permission: auth:external-invitation:revoke
```

创建命令固定当前管理员的 `tenantId` 和 `actorUserId`，只接受同租户活动用户及
`jit_policy=INVITE_ONLY` 的活动 Provider。每个邀请同时固定 `userId`、Provider 实例、有效期和
一次使用次数；可选 `expectedEmail` 在库内只保存规范化值的 SHA-256，配置后要求回调 Principal
明确返回 `email_verified=true` 且邮箱匹配。

登录发起方式为：

```text
GET /api/public/auth/external/{providerId}/authorize
    ?transactionId=...
    &invitationToken=...
```

原始 token 是 256 bit URL-safe bearer capability，只由创建响应签发一次。发起端验证 token 后，
仅把无密的 invitation id 固定到服务端 `AuthenticationTransaction`；内部跳转和上游 OAuth 请求
不会继续携带原始 token。回调先完成 OIDC/OAuth2 Principal 校验，再通过带租户、Provider、有效期、
`active` 和剩余次数条件的单条 SQL 更新原子消费邀请；消费和 `user_account_third` 首次绑定处于
同一事务，绑定失败会回滚消费。重放、已吊销、已过期、跨租户或跨 Provider 请求统一失败，已绑定
身份则仍可不带邀请正常登录。

`auth_external_identity_invitation` 同时保留创建/吊销操作者与原因、使用时间及消费 subject 哈希；
不保存 token、预期邮箱或 subject 原文。初始版本固定 `max_uses=1`，为后续批量邀请模型保留计数字段。
承载邀请链接的接入层必须对 `invitationToken` 查询参数做日志脱敏，并避免把完整 URL 发送到分析系统。

### 8.6 当前已落地的 JIT 自动开户

当活动 Provider 配置为 `jit_policy=JIT_CREATE`，且通过 Spring Security 校验后的稳定外部身份键
`(tenantId, providerId, issuer, subject)` 尚未绑定时，OAuth 回调会创建一个活动的本地账号，并在
同一独立事务内写入首条 `user_account_third`。该账号的 `login_password` 为空，表示尚未登记本地密码；
在用户完成密码登记前，它只能通过外部身份登录，解绑时仍受“最后一种认证方式”保护。

JIT 默认不会按邮箱查找或合并已有账号，即使上游声明 `email_verified=true` 也一样。邮箱、昵称、头像
和 locale 仅作为外部资料或显示偏好，不能代替稳定身份键。当前 locale 只在可规范化为两位语言码及
可选两位国家码时写入，例如 `ja-JP` 写为 `ja_JP`。

本地 username 的生成规则为：优先使用规范化后的上游 username 作为最多 15 个 Unicode 字母/数字的
可读前缀；上游没有 username 时使用 Provider code；随后追加稳定身份键 SHA-256 的 16 位十六进制
后缀，总长不超过 `user_account.username` 的 32 字符限制。后缀使可变 username claim 不会成为账号
唯一依据，也避免普通同名冲突；同一身份的重复回调始终落到同一绑定。

开户通过 `REQUIRES_NEW` 事务执行。两个节点并发首次登录时，`user_account` 的租户用户名唯一约束和
`user_account_third` 的租户 Provider 身份唯一约束决定唯一胜者；失败事务连同临时用户整体回滚，
认证编排层随后重读已经提交的胜出绑定。成功绑定写入追加式 `JIT_BIND` 审计，subject 仍只以
SHA-256 出现在审计表中。

### 8.7 Provider JIT 配置与准入规则

`auth_identity_provider_jit_config` 与 `auth_identity_provider` 一对一，只有当前租户内活动且
`jit_policy=JIT_CREATE` 的 Provider 才能读取或保存。未建配置行时使用安全默认值：
`EXTERNAL_USERNAME_HASHED`、不强制邮箱，其余业务默认值为空。管理端通过以下显式接口维护：

```text
GET  /api/admin/auth/identityProviderJitConfig/get?providerId=...
     auth:identity-provider-jit:view
POST /api/admin/auth/identityProviderJitConfig/save
     auth:identity-provider-jit:update
```

保存操作从当前登录上下文取得租户和操作者，要求 1–512 字符原因，并进入 Web 审计。请求端不能覆盖
租户或操作者。可配置字段如下：

- 用户名策略：`EXTERNAL_USERNAME_HASHED` 使用上游 username，`EMAIL_LOCAL_PART_HASHED` 使用邮箱
  local-part，`OPAQUE_HASHED` 只暴露 `ext_` 加稳定身份哈希；三者的唯一性均来自同一稳定身份键，
  不把可变 claim 当作账号键；
- 邮箱准入：`requireVerifiedEmail` 或非空域名白名单都会强制要求 `email_verified=true`。域名先经
  IDN ASCII 规范化，支持精确 `example.com` 和仅匹配子域的 `*.example.com`；邮箱只用于准入和
  外部资料，仍不会查找、合并或覆盖已有本地账号；
- 默认组织与直属上级：保存时验证实体活动且属于当前租户。开户时默认组织同时写入
  `user_account.org_id` 和 `user_org_user`，保证组织数据范围可见；未配置上级时使用历史根占位 ID；
- 默认 locale、IANA timezone 和 ISO 4217 currency 均做格式/标准库校验；账号类型和状态保持行业
  字典扩展性，只校验非空规范化和数据库长度，不在基础认证框架硬编码某行业的字典成员。

配置保存、账号创建、组织成员关系、首条身份绑定与 `JIT_BIND` 审计分别处在清晰的领域边界内；
账号、组织关系和首条绑定共享同一个 `REQUIRES_NEW` 事务，任一步失败都会整体回滚。

### 8.8 Provider 管理与类型化 claim mapping

租户 Provider 实例通过显式 Auth Admin 接口维护，不复用无边界的通用 CRUD：

```text
GET  /api/admin/auth/identityProvider/listTemplates
GET  /api/admin/auth/identityProvider/list
GET  /api/admin/auth/identityProvider/get?providerId=...
     auth:identity-provider:view
POST /api/admin/auth/identityProvider/create
     auth:identity-provider:create
POST /api/admin/auth/identityProvider/update
     auth:identity-provider:update
POST /api/admin/auth/identityProvider/setActive
     auth:identity-provider:set-active

GET  /api/admin/auth/identityProviderClaimMapping/get?providerId=...
     auth:identity-provider-claim:view
POST /api/admin/auth/identityProviderClaimMapping/save
     auth:identity-provider-claim:update
```

所有写请求从当前管理员主体取得 `tenantId` 和 `actorUserId`，并要求 1–512 字符的操作原因。模板与
Provider code 是身份键的一部分，创建后不可修改；当前不提供物理删除，以免破坏认证事务、邀请、
JIT 配置和既有外部绑定的引用。停用是正常退役方式，重新启用前会重新校验协议、issuer、scope、
client 和密钥引用。OIDC 的有效 scope 必须包含 `openid`。

`clientSecretRef` 只接受小写 scheme 的非空引用（例如 `env:KUDOS_AUTH_GOOGLE_CLIENT_SECRET`、
`property:kudos.ms.auth.external-secrets.google`、`vault:oauth/tenant-a/google`、
`aws-sm:kudos/auth/tenant-a/google`、`gcp-sm:company-prod/kudos-auth-google`、
`azure-kv:company-prod/kudos-auth-google`），不接受裸 secret。更新时 `null` 表示
保留原引用，`clearClientSecretRef=true` 表示明确清空；响应永不返回引用内容，只返回
`clientSecretConfigured`。含引用的创建/更新请求不进入普通 Web 审计表单正文，领域表仍保存操作者和
原因。基础模块只定义引用和解析 SPI，Vault/AWS/GCP/Azure 等读取适配器由可选模块提供，密钥写入和轮换控制面
仍归属外部密钥平台。

`auth_identity_provider_claim_mapping` 与 Provider 一对一。每个字段是最多 8 个、有序、逗号落库的
dot-separated path，第一个非 null 值胜出，例如 `profile.username`、`profiles.0.email`；解析器只遍历
Map 和 List，不支持 JSONPath、SpEL、脚本、函数或任意代码。未配置时沿用安全兼容默认值：
`preferred_username/login`、`name`、`email/email_verified`、`phone_number`、`picture/avatar_url`、
`locale`、`union_id`。OIDC subject 固定为协议验证后的 `OidcUser.subject`，保存映射时只允许 `sub`；
OAuth2 subject 使用显式映射路径。`MATCH_VERIFIED_EMAIL` 仍被 Provider 管理服务拒绝，邮箱 claim
不会自动合并账号。

### 8.9 密钥引用运行时治理

`IClientSecretResolver` 是厂商无关 SPI。解析注册表要求一个引用只能命中一个 resolver，并将未配置、
引用非法、scheme 不支持、resolver 冲突、策略拒绝、目标不存在和后端异常归一为稳定状态；动态
`ClientRegistration` 对任一非成功状态都 fail closed。具体 Vault、AWS/GCP/Azure KMS 或企业密钥平台
只需提供 resolver bean，不改变 Provider 表和 API。

内置 `env:` 和 `property:` resolver 必须经过命名空间策略。默认只允许 `KUDOS_AUTH_` 环境变量及
`kudos.ms.auth.external-secrets.` Spring 属性；白名单可显式扩展，空列表表示完全禁用该来源。scheme
使用小写精确匹配，避免不同管理入口和运行时对引用作出不同解释。此边界防止拥有 Provider 管理权限的
操作者把数据库口令等无关进程配置改作 OAuth client secret。

```properties
kudos.ms.auth.external-login.allowed-secret-environment-prefixes=KUDOS_AUTH_
kudos.ms.auth.external-login.allowed-secret-property-prefixes=kudos.ms.auth.external-secrets.
kudos.ms.auth.external-login.secret-cache-ttl-seconds=0
```

成功值只允许本机短期缓存，默认 TTL 为 0，最大 3600 秒；因此默认配置下密钥源轮换会立即生效。非零
TTL 仅用于访问代价较高的远端密钥服务。管理员可使用以下显式接口进行实时探测或清除本机状态后探测：

```text
POST /api/admin/auth/identityProviderSecret/verify
     auth:identity-provider-secret:verify
POST /api/admin/auth/identityProviderSecret/refresh
     auth:identity-provider-secret:refresh
```

请求只能提交 `providerId` 和必填原因；租户、操作者以及实际引用均由服务端确定，停用的 Provider 也可在
上线前检测。响应只包含 Provider id、scheme、稳定状态和检测时间，禁止返回引用路径、值、长度、摘要、
异常文本或密钥后端元数据。两项操作进入 Web 审计。启用非零缓存的多节点部署发生紧急轮换时需逐节点
刷新；默认零 TTL 不依赖跨节点失效。未来可通过 Kudos 分布式通知 SPI 广播失效，但不得把引用或密钥值
放入通知载荷。

HashiCorp Vault 适配由独立的 `kudos-ms-auth-secret-vault` 提供。引用格式为
`vault:<relative-path>#<field>`，例如 `vault:oauth/tenant-a/google#client-secret`；field 可省略并使用
服务端默认值。mount、KV v1/v2 和允许的路径前缀全部由部署配置固定，引用不能选择 mount/version，
也不接受绝对路径、`.`/`..`、反斜线、百分号编码、query 或空白。KV v2 读取
`<mount>/data/<relative-path>` 并仅解包 `data` Map；字段缺失或删除版本视为 `NOT_FOUND`，非字符串值和
后端异常归一为 `RESOLVER_ERROR`。

模块只消费标准 Spring Vault `VaultOperations`，不持有第二套 Vault 认证配置。Vault address、TLS
信任、Enterprise namespace、Token/AppRole/Kubernetes/云 IAM/客户端证书认证、租约续期以及 HTTP
连接/读取超时由 Spring Vault 或 Spring Cloud Vault 管理。缺少 `VaultOperations` 时不阻断应用启动，
但 `vault:` 检测返回安全的 `RESOLVER_ERROR` 且动态 Provider 注册 fail closed；部署可设置
`kudos.ms.auth.external-login.vault.enabled=false` 将 scheme 交给其他实现。

AWS Secrets Manager 适配由 `kudos-ms-auth-secret-aws-secrets-manager` 提供，使用 AWS SDK v2
`SecretsManagerClient` 和 `aws-sm:` scheme：

```text
aws-sm:kudos/auth/tenant-a/google
aws-sm:kudos/auth/tenant-a/google#client-secret
```

无 `#field` 时读取完整非空 `SecretString`；有 field 时只读取 JSON 对象的精确顶层字符串字段，不执行
JSONPath 或嵌套表达式。二进制 secret、调用方指定 version id/stage、非字符串字段均不接受。服务端固定
`versionStage`，默认始终请求 `AWSCURRENT`，因此兼容 Secrets Manager 标准轮换。

普通 secret name 必须匹配服务端 `allowedSecretIdPrefixes` 的路径段边界。ARN 默认禁用；启用时还必须
配置 exact ARN 或以 `/` 结尾的 ARN namespace prefix，固定 partition、region、account、
`secretsmanager` service 及 secret namespace。
引用不能通过 `..`、重复/前导斜线、百分号、query 或 version selector 越界。AWS region、workload
identity/default credentials chain、代理、重试和 HTTP 超时由应用提供的标准 `SecretsManagerClient`
管理；缺少客户端或 SDK 异常统一 fail closed 为 `RESOLVER_ERROR`，不得返回 request id、ARN、secret id
或 AWS 错误正文。

Google Secret Manager 适配由 `kudos-ms-auth-secret-google-secret-manager` 提供，使用官方 Google Cloud
Java 客户端和 `gcp-sm:` scheme：

```text
gcp-sm:company-prod/kudos-auth-google
gcp-sm:company-prod/kudos-auth-google#client-secret
```

引用包含 project id/number 和扁平 secret id；project 必须精确命中服务端 `allowedProjectIds`，secret id
必须匹配 `allowedSecretIdPrefixes`。两项策略任一为空都拒绝访问。引用不能选择版本；版本由服务端固定为
`latest` 或正整数，因此 Provider 管理员不能自行回退到历史/已禁用版本。无 field 时读取完整非空 UTF-8
payload；有 field 时仅读取 JSON 对象的精确顶层字符串字段，不执行 JSONPath 或嵌套表达式。

解析器限制 payload 大小，并在 Google 返回校验值时验证 CRC32C；缺少 payload、校验失败、非法 UTF-8、
非字符串字段、客户端缺失或 API 异常均 fail closed。Application Default Credentials、Workload Identity、
服务账号模拟、endpoint、代理、重试和 RPC 超时由应用提供的标准 `SecretManagerServiceClient` 管理；状态和
异常不得泄露 resource name、project、secret id、校验值或 Google API 错误正文。

Azure Key Vault 适配由 `kudos-ms-auth-secret-azure-key-vault` 提供，使用官方 Azure SDK for Java 和
`azure-kv:` scheme：

```text
azure-kv:company-prod/kudos-auth-google
azure-kv:company-prod/kudos-auth-google#client-secret
```

引用只包含部署方定义的 vault alias 和扁平 secret name，不接受 vault URL 或版本。服务端把每个 alias
映射到精确 HTTPS vault 根 URL，并仅从应用已提供的 `SecretClient` bean 中选择 `getVaultUrl()` 唯一匹配
的客户端；不存在、重复匹配或 URL 含 userinfo、端口、路径、query、fragment 都 fail closed。该设计既可
为 Azure 公有云、主权云、私有端点或多个租户 vault 分别注入客户端，也避免 Provider 管理员构造任意网络
目标。secret name 还必须命中服务端前缀策略，两项策略任一为空均拒绝访问。

服务端版本为空时读取最新版本，或固定为 32 位十六进制 Azure version id；引用不能选择版本。secret
值可作为完整非空字符串读取，也可只读取 JSON 对象的精确顶层字符串字段；超大值、非法 JSON、非字符串
字段、返回名称不一致及 Azure API 异常归一为安全状态。`DefaultAzureCredential`、Managed Identity、
Workload Identity、服务主体联合身份、主权云 authority、私有端点、代理、重试和 HTTP 超时由应用提供的
标准 `SecretClient` 管理；不得泄露 vault URL、secret name、request id 或 Azure 错误正文。

## 9. 第三方认证事务安全

短期外部认证事务建议存放 Redis，必要的审计摘要异步落库：

```text
auth_external_transaction
  transaction_id
  tenant_id
  provider_id
  state_hash
  nonce_hash
  pkce_verifier_ciphertext
  redirect_uri
  return_to
  created_at
  expires_at
  consumed_at
```

要求：

- Authorization Code Flow。
- 默认 PKCE S256。
- `state`、OIDC `nonce` 必须随机、一次性、与事务和浏览器绑定。
- redirect URI 精确匹配注册值。
- `return_to` 只能使用白名单中的站内地址，禁止 open redirect。
- 回调事务只能消费一次。
- 校验 issuer、audience、nonce、时间窗口和签名。
- Discovery/JWKS 支持缓存、密钥轮换和超时控制。
- 禁止在日志中输出 code、ID Token、access token、refresh token 和 client secret。
- Provider metadata/discovery URL 必须有 SSRF 防护；内部企业 IdP 通过显式网络白名单放行。

## 10. 凭证、MFA 与账号保护

### 10.1 最终凭证模型

最终由 `kudos-ms-auth` 持有认证凭证：

```text
auth_credential
  id
  tenant_id
  user_id
  type
  secret_hash_or_ref
  status
  version
  enrolled_at
  expires_at
  last_used_at
  metadata
```

特殊凭证使用专表，例如：

- `auth_webauthn_credential`
- `auth_recovery_code`
- `auth_trusted_device`

迁移期间，密码哈希可以暂时保留在 `user_account`，由 `kudos-ms-user` 提供只返回成功/失败的内部
`verifyCredential` 能力；不得通过 API 返回密码哈希。迁移完成后，`user_account.login_password`、
`authentication_key` 等认证字段废弃或置为可空。

**登录密码迁移已完成（V61）。** `auth_credential` 已建表（`V1.0.0.58`），登录密码由
`V1.0.0.59__migrate_login_password_to_auth_credential.sql` 幂等回填，`user_account.login_password`
由 user 域 `V1.0.0.35` 置为可空并废弃。security_password 不在本次范围内，TOTP secret 迁移留待后续批次。

边界与失败语义：

- 依赖方向仍是 auth → user，反向会成环。因此 User 域声明 `IAccountCredentialStore` 端口，
  auth 侧 `AuthAccountCredentialStore` 实现，同进程部署由 Spring 组合——与既有 `IPasswordHistory`
  同一套模式。缺少该 Bean 时 user 域仍读写 `login_password` 列，模块可独立部署。
- 密文只进不出。`IAuthCredentialService` 不提供哈希 getter：需要哈希的两个操作
  （校验、重算编码）以 lambda 形式把判定送进服务内部，`verifyWith` / `rotateWith` 分别返回布尔值。
  引用型密钥（TOTP）另有恒定时间比较的 `matches`。
- 改密走 `rotateWith`，在读到的 version 上做 CAS。并发改密由先落地者获胜，落败方得到
  `AUTH_CREDENTIAL_VERSION_CONFLICT`，绝不覆盖；登录时的哈希升级同样条件更新，失败即放弃升级，
  不影响本次认证结果。
- 退役哈希改由 `AuthAccountCredentialStore` 在轮换时归档进 `auth_password_history`。这一步不能省：
  列被清空后 user 域原来的归档调用会静默失效，历史密码复用检查会变成永远放行。
- 租户或用户缺失按接线错误抛出，而不是返回 false——后者在调用方读起来等同于“密码错误”。
- 账号删除时 `AuthCredentialLifecycleListener` 物理删除其凭证（撤销记录只对存续账号有意义）。
- 用户枚举时序防护保持不变：有凭证存储时统一消耗固定哈希，不在“账号存在”分支上多做一次库读。

### 10.2 密码和 TOTP

- 采用带算法前缀的 `DelegatingPasswordEncoder`，支持 BCrypt 向 Argon2 等算法平滑升级。
- 登录成功且全部认证因子通过后，根据 `upgradeEncoding()` 自动重算旧哈希；持久化必须使用旧哈希
  条件更新，CAS 失败代表并发改密，不能覆盖。
- 密码错误、TOTP 错误、恢复码错误分别计数，避免共用单一错误次数。
- TOTP secret 加密存储，支持密钥版本和轮换。
- 密码策略、历史密码、过期策略按租户配置。

当前过渡实现已经在 User 域提供 `IPasswordPolicy` SPI，并覆盖所有本地密码写入口。默认策略为
12～64 个 Unicode 字符，拒绝常见密码、单字符重复和用户名相关密码；大小写、数字、特殊字符要求
均为可选配置。当前默认 BCrypt 的原始输入硬限制为 72 个 UTF-8 字节。新写入使用 `{bcrypt}` 算法
前缀，历史无前缀 BCrypt 可继续校验，并在完整登录成功后透明升级。Auth 已增加只保存退役编码的
`auth_password_history`，同进程部署通过 `IPasswordHistory` 自动拒绝当前及最近 5 个历史密码，成功
改密才归档旧哈希，账号删除后清理。独立进程不跨网络传原始密码。

自 V61 起，登录密码本身已收归 `auth_credential`（见 10.1）：同进程部署下校验、改密、登录时哈希
升级和退役哈希归档都发生在 auth 侧，`user_account.login_password` 不再写入。租户级策略持久化与
默认算法切换仍待后续批次。

当前 TOTP 过渡实现不再把“生成密钥”等同于“启用 MFA”。自助流程固定当前受管会话的用户和租户：

```text
GET    /api/public/auth/mfa/totp
POST   /api/public/auth/mfa/totp/enrollments
POST   /api/public/auth/mfa/totp/enrollments/{id}/confirm  { "code": 123456 }
DELETE /api/public/auth/mfa/totp/enrollments/{id}
DELETE /api/public/auth/mfa/totp
```

创建、确认、取消和撤销均要求 password/federated 或更高 ACR，并要求近期认证。待确认注册默认 300 秒
过期、最多失败 5 次；Redis 可用时使用 Lua CAS/原子消费，无 Redis 时回退单机内存。待确认密钥先经
AES-GCM 加密再存储，只在创建响应中返回一次；确认成功后写入同样透明加密的正式字段，并触发全端
失效。相关配置：

```properties
kudos.ms.auth.mfa.issuer=Kudos
kudos.ms.auth.mfa.enrollment-ttl-seconds=300
kudos.ms.auth.mfa.enrollment-max-failed-attempts=5
kudos.ms.auth.mfa.operation-reauthentication-max-age-seconds=300
```

部署必须在启动阶段为 Kudos `CryptoKey` 注入独立强随机密钥并建立轮换/备份流程；框架内置占位密钥
仅供本地开发测试。当前管理员 `resetAuthKey` 仍作为受信任的恢复/代管入口保留，自助客户端不得调用。

恢复码使用独立的整组轮换接口，列表不能再次查询：

```text
GET    /api/public/auth/mfa/recovery-codes
POST   /api/public/auth/mfa/recovery-codes
DELETE /api/public/auth/mfa/recovery-codes
```

`POST` 和 `DELETE` 固定当前受管会话主体并要求近期认证；默认数量可通过
`kudos.ms.auth.mfa.recovery-code.code-count=10`（有效范围 5～20）配置，认证新鲜度通过
`kudos.ms.auth.mfa.recovery-code.operation-reauthentication-max-age-seconds=300` 配置。恢复码采用排除
`0/O/1/I` 的 16 字符字母表并按四字符分组显示；提交时大小写和连字符可忽略。原文不得写入日志、
审计、认证事务或遥测属性。

### 10.3 租户 MFA 策略

策略表 `auth_tenant_mfa_policy` 与租户一对一，支持以下模式：

- `OPTIONAL`：用户自行决定是否注册 MFA；
- `REQUIRED`：租户内所有账号都要求 MFA；
- `CONDITIONAL`：账号类型或角色代码任一条件命中时要求 MFA。

管理接口固定当前管理员的可信租户和操作者：

```text
GET  /api/admin/auth/mfaPolicy/get    auth:mfa-policy:view
POST /api/admin/auth/mfaPolicy/save   auth:mfa-policy:update
GET  /api/public/auth/mfa/policy

GET  /api/admin/auth/mfaEnrollmentExemptions/list    auth:mfa-exemption:view
POST /api/admin/auth/mfaEnrollmentExemptions/grant   auth:mfa-exemption:grant
POST /api/admin/auth/mfaEnrollmentExemptions/revoke  auth:mfa-exemption:revoke
```

`gracePeriodDays` 允许 0～90 天；宽限期从账号创建时间与策略最近生效时间的较晚者开始。默认情况下，
非 OPTIONAL 策略仍必须包含 `TOTP`，防止只部署 core 或仅部分节点部署认证器时保存用户无法满足的
WebAuthn-only 策略。确认 WebAuthn provider 与自助注册公开 API 已覆盖全部认证节点后，可由部署显式设置
`kudos.ms.auth.mfa.policy.allow-webauthn-only=true`，此时 `WEBAUTHN` 可成为唯一允许方法。
公共状态接口只返回 required/enrolled/enrollmentRequired、宽限期和允许方法，不返回任何密钥。

策略已进入统一登录执行链路：

- 本地密码和第三方登录在宽限期内可以完成认证，事务通过
  `postAuthenticationActions={ENROLL_MFA}` 返回登录后注册提示；该字段不携带密钥，也不表示已满足
  MFA ACR；
- 宽限期结束仍未注册时，登录以 `MFA_ENROLLMENT_REQUIRED` 终止；当前 TOTP 注册接口要求已认证的
  受管 Session，因此用户应在宽限期内完成注册，过期账号由下述逐用户临时豁免救援；
- 逐用户 MFA 注册临时豁免（`auth_mfa_enrollment_exemption`）是唯一的受控救援入口。它**只解除"必须先注册"这一
  阻断**：判定顺序把"已注册 → 要求第二因素"放在豁免之前，因此豁免永远不能替代用户仍持有的因素，也就不会成为
  管理员触发的 MFA 绕过。执行点在 `AuthenticationMfaPolicyEnforcer` 而不是策略判定内部——判定回答"租户策略对该
  账号怎么说"，豁免是执行时的覆盖；这个方向同时避免了与豁免服务的循环依赖。豁免只在其他条件都已判定为拒绝时才
  查询，正常登录路径不增加查询；
- 授予被三条边界收紧：已注册账号不能被豁免；任何人不能豁免自己（能同时授予和使用的管理员一次调用就取消了自己的
  MFA）；窗口必须在未来且不超过 `kudos.ms.auth.mfa.policy.max-enrollment-exemption-days`（默认 7 天，服务层强制
  收敛到 1..30，部署无法配置出无限期救援）。租户策略并不要求 MFA 的账号也不能被豁免，因为那没有意义；
- 每次授予都是新行，历史保留；撤销只作用于**当前确实生效**的授予，已自然到期的行保持原样，避免把"到期"改写成
  "被管理员撤销"。管理 API 以 `auth:mfa-exemption:view` / `grant` / `revoke` 三个独立权限点保护，租户与操作者固定取
  会话，目标账号按 User 域可信记录校验归属，原因必填并进入 Web 审计。自助状态接口在确实生效时返回
  `exemptionExpiresAt`，用户能看到自己处于临时豁免且仍需注册；
- 豁免属于管理动作而非检测结果，因此**不**写入 `auth_security_event`——该表当前的语义是聚合去重的风险检测，混入
  管理动作会破坏其去重键、风险级别与 SLA 模型。管理动作的证据在豁免行本身与 Web 审计中；
- 已注册用户通过第三方身份完成第一因素后，原事务进入 `CHALLENGE_REQUIRED`，只暴露
  `VERIFY_TOTP`，以及策略允许且确有可用码时的 `VERIFY_RECOVERY_CODE`。OAuth2/OIDC 回调此时不创建
  HttpSession 或逻辑会话；客户端继续调用事务 action，验证成功后才以
  `amr=federated,<provider>,totp|recovery_code` 和 MFA ACR 签发会话；
- TOTP 和恢复码挑战分别限流。策略查询或执行依赖异常时默认 fail-closed；
- `recoveryCodesEnabled=false` 时禁止生成和消费新恢复码，状态按禁用返回且不再显示恢复码登录动作；
  撤销仍然允许，以便清理既有数据。

### 10.4 Passkey/WebAuthn

Passkey 作为一等认证方法：

- 支持多个 credential。
- 保存 credential id、公钥、sign count、transports、备份状态和设备描述。
- challenge 一次性且短期有效。
- 严格校验 RP ID、origin、challenge 和签名。
- 支持 passwordless 和作为第二因素两种策略。

当前已落地凭证领域基础层、迁移 `V1.0.0.44__init_auth_webauthn_credential.sql` 和可选协议模块的
registration/assertion begin/finish：

- credential ID、user handle 和 COSE 公钥统一保存为无填充 Base64url；服务端从不接收或保存私钥；
- 凭证按租户隔离，注册前校验本地账号归属，断言结果还必须匹配凭证所属用户；
- 记录 transports、AAGUID、attestation format、discoverable、backup eligible/backed up、最近使用时间；
- 对支持计数器的认证器要求新 sign count 严格递增；不支持计数器的 `0 → 0` 保持兼容。持久化更新包含
  旧计数 CAS，同一快照的并发断言最多一个成功；
- 注册/撤销会触发 `WEBAUTHN_CREDENTIAL_CHANGED`，复用全端 Session/Token 失效链路；账号删除后清理
  孤立凭证；该原因与 TOTP authenticator change 分离，不会撤销仍有效的 TOTP 恢复码；
- `IMfaEnrollmentQuery` 已把 TOTP 与 WebAuthn 注册状态统一提供给租户策略，策略只认可其
  `allowedMethods` 中实际已注册的方法；
- `kudos-ms-auth-provider-webauthn` 使用 Yubico `webauthn-server-core` 生成注册 options，账号与租户来自
  受信服务端上下文，RP ID/origin/user verification/resident key 来自部署配置，现有 credential 自动进入
  `excludeCredentials`；user handle 由租户与内部 user id 做 SHA-256 得到，不暴露邮箱或用户名；
- ceremony id 使用 256-bit 随机值，library request JSON 只在服务端短期保存。有 Redis 时通过 Lua 保证
  不覆盖与一次性消费，多实例共享；无 Redis 时使用具有相同原子语义的进程内回退实现；
- finish 先消费 ceremony 并重新校验租户、用户、账号状态、RP 配置和 user handle，再由 Yubico 完成
  challenge、type、RP ID hash、origin、UP/UV、attestation 和 credential public key 校验。失败状态不可
  重放；成功结果才转换为 `VerifiedWebAuthnCredentialRegistration`。注册请求启用 `credProps`，用于记录
  discoverable/passkey 属性；
- assertion begin 支持两种服务端模式：已知用户模式只把该账号的活动凭证放入 `allowCredentials`；
  username-less 模式不保存或下发用户提示，由认证器返回 discoverable credential 和 user handle；
- assertion finish 由 Yubico 验证 challenge、type、RP ID hash、origin、签名、UP/UV、backup flags 和 sign
  count；之后仍以租户作用域重新读取 credential，将 user handle、credential owner、活动账号用户名和可选
  预期用户逐项匹配。只有全部匹配才调用 core 的 `recordVerifiedAssertion` 执行旧计数 CAS；
- `passkey` 已作为 `IAuthenticationMethodProvider` 接入 LOGIN 与 STEP_UP。公开 assertion begin 只接受正等待
  `VERIFY_PASSKEY` 的同方法事务，ceremony 保存 transaction id；finish 同时核对该绑定，Step-up 还会核对
  源会话用户，避免跨事务或跨主体替换；
- 无用户名 Passwordless assertion 完成后由统一事务控制器创建逻辑 Session、旋转 HttpSession ID 并绑定
  主体；Step-up 则 CAS 提升原逻辑会话。UV assertion 使用 phishing-resistant ACR，非 UV assertion 使用较低的
  WebAuthn ACR；后者在策略仍要求第二因素时 fail-closed；
- core 提供协议中立的 `IAuthenticationSecondFactorProvider` 与 action registry。WebAuthn provider 仅在租户
  策略允许 `WEBAUTHN` 且主体已有活动 credential 时，为第三方登录挑战贡献 `VERIFY_PASSKEY`；公开 begin
  使用第三方回调已经固定的本地 user id，finish 再核对 transaction/user 绑定，成功后统一合并 AMR、设置
  ACR 并签发会话；
- 密码第一因素成功且策略要求继续认证时，method result 只把服务端确认的 user id、`password` AMR/ACR
  带入事务，不保存密码；若策略允许且主体已注册 WebAuthn，则挑战加入 `VERIFY_PASSKEY`。该 action 由
  通用第二因素 registry 截获，无需重传密码，成功后才创建 Session；
- 自助注册、查询、重命名和撤销公开 API 固定到当前受管 Session 的租户与用户，不接受客户端指定主体；注册
  finish 只接受路径上的 ceremony id 和浏览器响应，默认要求 300 秒内的 password-or-stronger 认证。
  凭证列表只返回公开摘要；显示名裁剪首尾空白并拒绝空值、超长和控制字符，重命名只更新元数据及
  乐观版本，不触发全端失效，注册/撤销继续触发既有 Session/Token 失效链路；
- 管理员可通过 `GET /api/admin/auth/webauthn/credentials?userId=...` 只读审计同租户账号的活动与已撤销
  凭证。接口要求 `auth:webauthn-credential:view`，跨租户与不存在账号使用相同 404；审计 DTO 返回
  credential SHA-256 指纹、认证器公开属性、生命周期时间，以及聚合后的认证器风险级别/来源/状态码，
  不返回原始 ID、公钥、user handle 或计数器；
- `auth_webauthn_attestation_policy` 按租户一对一保存 attestation format 白名单、AAGUID `NONE` /
  `ALLOW_LIST` / `DENY_LIST` 规则和可信证明要求，并记录创建/更新操作者、原因和时间。未配置时不限制
  format/AAGUID 且不要求可信证明，以兼容已有部署；AAGUID 规范化为 UUID，format 规范化为小写；
- registration finish 在 Yubico 协议验证成功后、重复凭证检查和落库前执行租户策略。策略拒绝保留稳定
  错误码、消费当前 ceremony 且不持久化凭证，不能通过重放绕开；
- provider 只在容器中存在唯一 Yubico `AttestationTrustSource` Bean 时启用信任链判断，并通过能力 SPI
  报告给 core。租户要求 trusted attestation 但部署没有唯一 trust source 时，保存阶段即 fail-closed；
- 管理员通过 `GET /api/admin/auth/webauthn/attestationPolicy/get` 和对应 `/save` 查询/保存当前会话租户
  的策略，分别要求 `auth:webauthn-attestation-policy:view` / `update`。请求不能覆盖租户或操作者，变更
  原因同时进入领域审计和 Web 审计；查询返回 `trustSourceAvailable`，能力缺失时强制可信证明保存返回 409；
- provider 可显式启用 Yubico `webauthn-server-attestation` 的 FIDO MDS loader。部署必须提供可写的绝对
  缓存目录和显式接受的 legal header；首次加载没有有效缓存/下载时 fail-fast。此后单线程周期执行
  `loadCachedBlob()`，验签并成功构建不可变 `FidoMetadataService` 后才原子替换；刷新失败保留最后一份
  已验证快照。自定义 `AttestationTrustSource` Bean 存在时内置实现自动退让；
- core 提供厂商无关的 `IWebAuthnAuthenticatorRiskEvaluator`，按最严重级别聚合多个风险源且不在请求内
  发起网络访问。内置 MDS 实现从同一份已验签快照构建不过滤的状态视图，使 `REVOKED`、密钥泄露、
  user verification bypass 等被默认信任过滤器排除的型号仍能进入审计；只采用已生效状态。当前存量表
  没有 attestation 证书链/认证器版本，因此仅按 AAGUID 保守评估；MDS 状态本身不自动撤销或锁定；
- `auth_webauthn_authenticator_risk_policy` 按租户一对一保存风险阻断级别与完整变更审计。未配置或空集合时
  默认只审计；可显式阻断 `NOT_EVALUATED`、`WARNING`、`CRITICAL`，不能阻断 `NORMAL`。没有部署风险
  评估器时不能保存非空策略，避免产生虚假的安全保证；
- 管理员通过 `GET /api/admin/auth/webauthn/riskPolicy/get` 和对应 `/save` 查询/保存当前会话租户策略，分别
  要求 `auth:webauthn-risk-policy:view` / `update`；请求不能覆盖租户或操作者，原因进入领域和 Web 审计，
  查询返回 `riskEvaluationAvailable`，能力缺失时保存非空策略返回 409；
- assertion 在真实协议签名、credential owner、user handle 与预期账号校验通过后评估风险，在签名计数、
  最近使用时间和会话签发前阻断。认证事务边界对匿名客户端只返回通用无效 Passkey，不暴露型号风险；
  策略不自动吊销凭证，也不追溯撤销既有会话；
- 每次阻断同步发布 `WebAuthnAuthenticatorRiskPolicyBlocked`，包含租户、内部用户、发生时间、风险级别、
  来源/状态码及 credential SHA-256 指纹，不包含原始 credential ID、AAGUID、断言载荷或公钥。监听器失败
  不能替换或绕过阻断；应用存在 Micrometer `MeterRegistry` 时自动暴露
  `kudos.auth.webauthn.risk.policy.blocked{level=...}`，只为三个可阻断级别建立固定时序，不把租户、用户、
  设备、来源或状态码放入指标标签。告警阈值与通知渠道由部署侧按行业要求配置；
- 同步事件由 `WebAuthnRiskSecurityEventListener` 转换为 `auth_security_event`，且以独立事务提交，
  避免外层认证因阻断异常回滚时丢失证据。事件按租户、类型、稳定去重键和 5 分钟 UTC 窗口唯一；
  首次并发插入发生唯一冲突时，败者在第二个独立事务中原子增加聚合次数并合并首末时间。
  持久化失败会记错，但不会将已阻断的认证改为成功；
- 管理员可通过 `GET /api/admin/auth/securityEvents` 以 `auth:security-event:view` 查询当前租户最近事件，
  并可选按同租户用户、风险级别和处置状态过滤；跨租户用户与不存在用户统一 404，结果硬上限 500。
  接口只返回指纹、聚合摘要和处置审计，不返回 WebAuthn 原始材料；
- 事件处置严格执行 `OPEN → ACKNOWLEDGED → CLOSED`。`/{id}/acknowledge` 与 `/{id}/close`
  分别要求 `auth:security-event:acknowledge` / `auth:security-event:close`，租户和操作人由会话固定，原因必填并进入
  领域字段和 Web 审计。确认只能发生在聚合窗口结束后；关闭分类为 `MITIGATED` / `FALSE_POSITIVE` /
  `ACCEPTED_RISK` / `DUPLICATE` / `OTHER`。客户端回传 `workflowVersion`，SQL CAS 使陈旧页面返回 409 而不覆盖他人处置；
  确认或关闭只记录调查结论，不自动撤销凭证、解锁或恢复会话；
- 新事件由可替换 `IAuthSecurityEventSlaPolicy` 计算 `dueAt`；内置风险级别期限为 CRITICAL 1 小时、WARNING
  4 小时、其他 24 小时，并可通过 `kudos.ms.auth.security-event.sla` 调整或关闭。行业部署可替换 SPI 实现
  租户、营业日或事件类型规则；
- `/{id}/assign` 要求 `auth:security-event:assign`，目标负责人必须是当前租户可信账号，分派与确认/关闭共享
  `workflowVersion` CAS，不能修改已关闭事件。列表支持 `assigneeUserId` 和 `overdueOnly=true`，后者只返回
  `dueAt < 当前 UTC 时间` 的未关闭事件。分派事务提交后发布 `AuthSecurityEventAssigned`，由部署接入邮件、
  短信、IM、工单或消息中间件；核心不绑定渠道和调度器；
- 部署侧可定时调用 `IAuthSecurityEventEscalationService.scanDue` 有界扫描超时事件。每个候选在独立事务内按
  `escalationLevel` 做 CAS，竞争胜者推进级别并与通知 outbox 原子提交，不占用人工工作流的
  `workflowVersion`。默认最多升级 3 级、每 4 小时再次升级，也可通过配置或替换 SPI 实现行业规则；
- `IAuthSecurityEventNotificationService` 使用数据库租约领取待发送通知，支持租约过期接管、指数退避和达到上限后
  进入 `DEAD`。outbox 保存升级时的负责人快照，未分派事件由渠道路由到租户默认安全队列。该机制是至少一次
  交付，邮件、短信、IM、工单等适配器必须以 `notificationId` 做幂等；框架不主动开启调度器，也不绑定渠道；
- 部署通过 `IAuthSecurityEventNotificationPublisher` 接入可靠消息总线或具体通知网关，dispatcher 在数据库事务外发布
  并逐条确认。投递单位是渠道：publisher 用 `supports(destination, channel)` 声明能力，dispatcher 按渠道选择，同一
  destination+channel 被两个 publisher 声明属于部署错误并直接报错，完全没有 publisher 时在领取前失败；可重试异常
  使该渠道保持未结算并进入行级退避，明确的永久错误把该渠道结算为 `DEAD`，未知异常只持久化稳定错误码而不泄露厂商响应；
- `auth_security_event_notification_channel` 是逐渠道终态账本（按通知+渠道唯一，只记录终态）。重试只挑未结算渠道，
  因此一个渠道故障不再重发已送达的渠道；某渠道被永久拒绝不拖住其它渠道，也不再消耗行级尝试次数。行级结果由渠道结果
  聚合：仍有可重试失败则退避，至少一个渠道交付则整行 `DELIVERED`，全部被永久拒绝才 `DEAD`。渠道结算写在独立事务，
  管理员重放会清空账本。交付语义仍是至少一次——worker 在发送成功与结算之间宕机会重试该渠道，适配器须按
  `notificationId` + 渠道幂等；
- 管理员以 `auth:security-event-notification:view` 查询当前租户 `DEAD`，并以独立
  `auth:security-event-notification:replay` 权限带原因重放。重放只允许 `DEAD → PENDING` CAS，重置本轮尝试次数；
  每次成功操作在同一事务追加租户、操作者、原因和 UTC 时间审计，跨租户与不存在通知统一 404；
- `IAuthSecurityEventNotificationRoutePolicy` 在每次 attempt 发布前解析租户路由；默认将已有负责人路由为
  `USER + SITE_MESSAGE`，无负责人路由为 `TENANT_SECURITY_QUEUE + EVENT_BUS`，不猜测安全管理员。非法路由直接
  `DEAD`，解析器临时异常退避；行业实现可接值班表、租户目录、风险级别或营业日规则；
- 租户路由已可持久化：`auth_security_event_notification_route` 按租户、通知类型和投递形态（`ASSIGNED` /
  `UNASSIGNED`）唯一保存路由码、目标、渠道集合、固定响应人、`includeAssignee`、启停开关、降级行为、乐观版本和
  创建/变更操作者与原因。装配的默认策略读取该配置，未配置或规则停用时与 V52 内置默认完全一致；目标为 `USER`
  且最终无人可寻址时按 `DEFAULT_ROUTE` / `TENANT_SECURITY_QUEUE` / `FAIL` 降级，`FAIL` 直接进入 `DEAD` 而不是
  替换成未经配置的收件人。路由仍在每次 attempt 解析，因此配置变更在重试和重放时自然生效，不修改 outbox 行；
- 路由管理 API 以 `auth:security-event-notification-route:view` / `update` 保护，租户和操作者固定取当前管理员会话，
  请求体不含这两个字段；响应人先用 User 域可信记录校验归属，跨租户与不存在账号统一 404。保存对 `configVersion`
  做 SQL CAS，陈旧版本、并发 CAS 失败和并发创建同一 scope 统一 409；保存校验与 dispatcher 发布前校验同形，且拒绝
  永远无法寻址的组合。不提供物理删除，`enabled=false` 即回到内置默认，每次保存在同一事务追加含变更前后快照的
  审计。投递路径按租户缓存整份规则集（`LOCAL_REMOTE`，空结果同样缓存），提交后由只含租户 ID 的领域事件驱逐并跨
  节点广播；管理查询直接读库，保证管理界面看到权威值；
- 值班表已持久化并接入路由：`auth_security_event_oncall_roster` 按租户和值班表编码唯一，
  `auth_security_event_oncall_shift` 保存显式 UTC 时间窗与 1..5 层级的班次；不引入重复规则、时区规则或节假日
  日历，排班可由外部计划系统生成。保存为整份轮值替换 + `configVersion` CAS + 变更前后完整排班快照审计，每份轮值
  最多 100 条班次，窗口左闭右开使交接时刻只归属后一班；
- core 只暴露协议中立的 `IAuthSecurityEventResponderResolver`（租户、值班表编码、升级级别、时刻 → 本地用户 ID）。
  内置实现按 `tier <= 升级级别` 追加命中的班次——升级是追加更高层级而非移交。空集表示当前无人值班，交给路由规则的
  fallback；抛出表示轮值读不出来，由 dispatcher 退避重试，临时目录故障不会被记录成路由决策。外部值班/呼叫系统只需
  替换该 Bean，core 不引入厂商依赖；
- 路由规则新增可选 `responderRosterCode`，解析结果与固定响应人、负责人快照合并为收件人；保存时校验该值班表属于当前
  租户且已启用。值班读取按租户缓存**排班计划**而不是"当前值班人"，时间过滤在读取时施加，变更由只含租户 ID 的领域
  事件驱逐并跨节点广播。管理 API 以 `auth:security-event-oncall:view` / `update` 保护，租户与操作者固定取会话，
  班次响应人按 User 域可信记录校验归属；
- 每次状态 CAS 后发布只含固定 notification type/outcome 的低基数交付事件。Micrometer 存在时暴露
  `kudos.auth.security.event.notification.delivery{type,outcome}`，不使用租户、用户、worker、route 或厂商错误标签；
  逐渠道另有 `kudos.auth.security.event.notification.channel.delivery{type,channel,outcome}`，三个标签均为固定枚举，
  可对单一渠道持续失败告警而不引入高基数；
  部署可对 `DEAD` 和持续重试速率告警，指标监听失败不改变 durable 状态；
- 可选 `kudos-ms-auth-notification-msg` 只声明 `USER` 目标的站内信、邮件、短信，并以
  `{notificationId}:{channel}` 调用 `kudos-ms-msg` 的幂等发布。返回空或远程失败继续重试，非法/不支持路由永久失败；
  它不声明安全队列或事件总线渠道——声明即等于吞掉这些通知；
- 可选 `kudos-ms-auth-notification-eventbus` 认领 `EVENT_BUS` 渠道（两种目标都接受，但不认领 `WORK_ORDER` 与站内信类
  渠道），通过 `kudos-ability-distributed-stream` 的 producer binding 投递，broker 由部署选择，core 仍不依赖具体中间件。
  它与 msg 适配器可同时启用，因此默认的 `TENANT_SECURITY_QUEUE + EVENT_BUS` 路由终于有 publisher 认领。总线载荷只含
  标识与路由决策及 `{notificationId}:EVENT_BUS` 幂等键，不含凭证材料、subject 指纹、风险来源或收件人联系方式；需要
  证据的消费者回查租户隔离的管理 API，那里仍执行权限校验。**真实边界**：`StreamBridge` 异步，发送成功只表示进入本地
  producer 队列而非 broker 确认，后续 flush 失败由 stream 的 `sys_mq_fail_msg` 承接而不是 auth outbox（此时该渠道已
  结算为 `DELIVERED`），因此部署必须启用 stream 失败消息持久化，否则 broker 故障会成为静默丢失；
- registration begin 读取租户策略：format/AAGUID 有限制或要求可信证明时请求 `DIRECT` attestation，
  其他租户明确请求 `NONE`，避免为了少数受管设备场景扩大所有用户的认证器信息披露；
- 单元测试覆盖已知用户、无用户名、主体错配、失败后重放和写入边界；协议测试动态生成 P-256 密钥并签出
  完整 assertion，验证真实签名成功及 challenge/origin 篡改失败；Redis 容器测试覆盖 nullable user id 的
  跨节点序列化与原子消费，H2/Flyway 测试覆盖租户隔离的 user handle 反查。

core 基础层仍只接受协议库已经验证过的 registration/assertion 结果；浏览器提交数据不能直接调用内部
`registerVerified`/`recordVerifiedAssertion`。当前 provider 已完成 assertion 公开 API、Passwordless 登录、
Passkey Step-up、密码/第三方登录 Passkey 第二因素、会话接入以及注册/查询/重命名/撤销公开 API。WebAuthn-only
强制策略只有在部署显式启用能力开关后才允许保存；安全默认值仍拒绝，避免部分部署造成账号锁死。
后续重点转向工单适配、强制替代因素与受控吊销授权闭环，以及企业 PKI
trust source、企业设备治理和管理控制台 UI，而不是绕过协议验证边界或根据元数据更新直接批量锁定账号。

## 11. Session、Token 与 Remember Me

### 11.1 浏览器模式

浏览器管理系统默认推荐服务端 Session：

- Redis 共享 Session。
- Cookie 使用 `HttpOnly`、`Secure`、适当的 `SameSite`。
- 启用 CSRF 防护。
- 登录成功后旋转 Session ID，防止 session fixation。
- 同时支持闲置过期和绝对过期。

### 11.2 App/API 模式

- 短期 Access Token。
- Refresh Token Rotation。
- 数据库只保存 Refresh Token hash。
- 保存 token family 和父子关系。
- 检测旧 Refresh Token 重用时撤销整个 family。
- 修改密码、禁用账号、管理员强制下线时撤销相关会话。

当前首个闭环由 `kudos-ms-auth-token-jwt` 提供，默认关闭。启用时部署必须显式提供持久化且可轮换的
`JwtEncoder`/`JwtDecoder` Bean；框架不会生成临时签名密钥，也不会使用内置共享口令。Access Token
最长 3600 秒，默认 300 秒，包含 `sid/tenant_id/pv/auth_time/amr/acr`；Bearer Filter 除密码学验签和
时间窗口外，还会比较实时权限版本并触碰逻辑 API Session，因此角色权限变化、管理员 token epoch
递增、会话撤销或过期都不能继续使用旧 Access Token。

Refresh Token 原文为带 `krt_` 前缀的 256-bit URL-safe 随机值，只在签发/轮换响应返回一次。
数据库仅保存 SHA-256、family/父子关系和消费状态；一个父令牌只允许一次原子消费。再次提交已消费
令牌视为重放，整族与对应 API Session 一并撤销。每个 family 固定绝对到期时间，轮换不会无限延寿；
签发时还固化主体 token epoch，管理员递增 epoch 后旧 family 不能换取新 Access Token。

初次 token pair 通过当前已经认证的逻辑 Session 换取独立 API Session，不接受调用方提供 `userId`。
这使密码和第三方登录继续共用同一认证事务，同时避免把公开 transaction id 当作 bearer exchange code。

### 11.3 会话模型

```text
auth_session
  id
  tenant_id
  user_id
  client_id
  device_id
  auth_time
  amr
  acr
  risk_level
  credential_version
  created_at
  last_seen_at
  idle_expires_at
  absolute_expires_at
  revoked_at
  revoke_reason

auth_refresh_token
  id
  session_id
  family_id
  parent_id
  replaced_by_id
  token_hash
  token_epoch
  issued_at
  expires_at
  consumed_at
  revoked_at
```

现有 `user_login_remember_me` 应逐步废弃，由“持久 Session”或 Refresh Token 统一替代。

当前已落地浏览器会话的首个闭环：

- `AuthenticationSessionService` 统一登记密码/TOTP 与第三方登录产生的本地会话，逻辑会话 ID 使用
  独立 UUID；真实 servlet Session ID 仅由容器和 HttpOnly Cookie 持有，不能写入公开认证事务。
- 会话运行时记录包含认证主体、`authTime/amr/acr`、风险与凭证版本、服务端观测的终端信息、
  `lastSeenAt/idleExpiresAt/absoluteExpiresAt` 以及撤销状态；默认闲置 1800 秒、绝对 43200 秒。
- Redis 存储使用版本号 CAS 和绝对 TTL，单机部署自动回退内存实现；用户索引采用租户与用户标识的
  SHA-256 摘要键，并随其最晚会话绝对过期。Auth Web Filter 对带逻辑会话
  标记的请求执行活动续期、主体归属校验，发现过期、撤销、丢失或错配时立即使 HttpSession 失效。
- 当前用户可查询全部活动会话，并撤销自己的当前或指定远端会话；所有列表/撤销操作同时校验租户与
  用户归属。当前会话撤销同时写入 `USER_LOGOUT` 并立即使 HttpSession 失效；指定远端会话写入
  `USER_REVOKE`，该浏览器的下一次请求会被权威注册表拒绝并使其 HttpSession 失效。
- 管理员可通过 `GET /api/admin/auth/sessions?userId=...` 查看同租户用户的全部活动会话，并通过
  `POST /api/admin/auth/sessions/{sessionId}/revoke` 逐台撤销。两个端点分别要求
  `auth:session:view`、`auth:session:revoke`；目标租户从可信用户记录解析，跨租户与不存在用户使用
  相同的 404 语义。撤销原因必填并以 `ADMIN_REVOKE:` 前缀保存，同时进入 Web 审计。
- `UserAuthenticationInvalidated` 专用事件只覆盖登录密码、安全密码、停用、冻结和 TOTP 认证器等安全变更，不复用
  同时承载登录时间/失败次数的通用更新事件。账号单删/批删使用删除前保存的租户快照。Auth 的
  `AuthenticationLifecycleService` 会挂起调用方事务，使 token epoch 先在独立数据库事务中提交，再撤销
  该租户用户的全部 Refresh family 与逻辑会话；启用账号和解冻不会重复失效。定时冻结采用保守策略，在配置冻结时立即
  撤销既有认证状态，而不是等生效窗口到达。
- 当前用户的 `POST /api/public/auth/logout-all` 会执行同一完整失效链路并使当前 HttpSession 失效；
  Auth Admin 原有 `revokeAllTokens` 端点也已改为完整编排，而非只递增 epoch。
- 旧 Passport 直接创建的 Session 暂不带逻辑会话标记，继续由兼容 Filter 处理；待调用方迁移至 Auth
  认证事务后再移除该兼容路径。

```properties
kudos.ms.auth.session.idle-timeout-seconds=1800
kudos.ms.auth.session.absolute-timeout-seconds=43200
```

Spring Session repository 的主动物理删除联动已完成（V60）。core 只暴露协议中立的
`IAuthenticationContainerSessionPurger`，在每个撤销点尽力而为地调用，且绝不把清理失败抛回撤销路径——撤销是
安全动作且已落库，每个请求都会重新校验逻辑会话，用真正的保证去换装饰性的保证不划算。可选
`kudos-ms-auth-session-spring` 用 Spring Session 的 principal 索引实现它，只依赖 `spring-session-core`，具体存储
仍由部署选择；未安装该模块的部署保持原有语义：立即撤销、远端容器记录在下一次请求时失效。

```properties
kudos.ms.auth.token.jwt.enabled=true
kudos.ms.auth.token.jwt.issuer=kudos
kudos.ms.auth.token.jwt.audience=kudos-api
kudos.ms.auth.token.jwt.access-ttl-seconds=300
kudos.ms.auth.token.jwt.signing-algorithm=RS256
# 多把活动签名密钥并行轮换时必须指定
kudos.ms.auth.token.jwt.key-id=current-signing-key
kudos.ms.auth.token.refresh-ttl-seconds=2592000
```

生命周期编排依赖 User/Auth 共享的 Spring 事务事件总线；拆成独立进程部署时，必须把这些域事件桥接到
可靠消息/Outbox，不能假定进程内事件会跨服务传播。逻辑撤销立即生效；远端 Spring Session 容器记录默认在
下一次请求时由 Filter 清理，安装可选的 `kudos-ms-auth-session-spring` 后在撤销瞬间即被物理删除。

### 11.4 第三方 Token

仅为登录获取的第三方 access token 在完成身份解析后应尽快丢弃。

如果业务确实需要代表用户调用第三方 API，应使用独立模型，例如 `auth_external_grant`：

- 加密保存 access/refresh token。
- 保存 scope、expiry、provider 和授权用途。
- 与登录身份绑定表分离。
- 支持撤销、刷新和最小权限。

## 12. 风险控制和安全审计

### 12.1 登录防护

- 外部响应统一使用 `INVALID_CREDENTIALS` 等安全结果，内部审计保留真实失败原因。
- 按账号、IP、租户、设备、Provider 等维度限流。
- 登录 IP 由服务端从连接或可信代理头解析，不接受客户端 DTO 自报。
- 区分管理员冻结、账号停用和认证失败临时锁定。
- 支持异地、陌生设备、异常频率等 Risk Signal SPI。
- 高风险时可以拒绝、追加 MFA 或限制会话权限。

当前 Passport 已落地其中的基础限流层，并保持旧接口兼容：

- 每次请求同时消费 `IP` 与 `tenantId + username` 两个固定窗口；任一桶耗尽时整个多桶操作不再计数。
- 密码失败与 TOTP 失败使用独立的 `tenantId + username` 桶。密码校验成功即清除密码失败桶；TOTP
  成功才清除 TOTP 失败桶，错误 TOTP 不再累计旧的密码错误次数或触发账号自动冻结。
- 计数键只包含作用域和 SHA-256 摘要，不在 Redis 暴露租户、用户名或 IP 原文。
- 存在 `RedisTemplates` 时使用 Redis Lua 原子检查/消费，并由 TTL 自动回收；无 Redis 的轻量部署
  使用进程内实现。存储异常默认拒绝认证，availability-first 部署可显式启用 fail-open。
- 达到请求或因素窗口限制时，Passport 返回 `RATE_LIMITED` 和 `retryAfterSeconds`；认证事务适配器将其
  转换为可重试的 `TOO_MANY_AUTHENTICATION_ATTEMPTS` challenge。
- 旧 `login_error_times` 与 `autoLoginLock` 仍只承担账号级密码连续失败冻结，和流量限流并存。
- Passport 核心继续生成 `USER_NOT_FOUND`、`WRONG_PASSWORD`、`INACTIVE`、`LOCKED`、
  `ACCOUNT_FROZEN` 等细分结果，供可信内部逻辑和审计使用；`PassportPublicController` 与 Auth 密码
  适配器在公网边界统一映射为 `INVALID_CREDENTIALS`，并清除错误次数、冻结标题等差异字段。
- 不存在账号、停用账号和有效冻结账号在返回前都会执行一次 BCrypt 校验；缺失或损坏的密码哈希使用
  固定 cost-10 虚拟哈希。该措施用于缓解明显快路径，不能替代公网统一响应、限流和持续时序监测。

默认配置如下；任一 `*-max-attempts` 设为非正数可关闭该维度：

```properties
kudos.ms.user.passport.attempt-limit.enabled=true
kudos.ms.user.passport.attempt-limit.fail-open=false
kudos.ms.user.passport.attempt-limit.ip-max-attempts=120
kudos.ms.user.passport.attempt-limit.ip-window-seconds=60
kudos.ms.user.passport.attempt-limit.principal-max-attempts=20
kudos.ms.user.passport.attempt-limit.principal-window-seconds=60
kudos.ms.user.passport.attempt-limit.password-failure-max-attempts=5
kudos.ms.user.passport.attempt-limit.password-failure-window-seconds=900
kudos.ms.user.passport.attempt-limit.totp-failure-max-attempts=5
kudos.ms.user.passport.attempt-limit.totp-failure-window-seconds=300
```

下一阶段将设备、Provider 和风险信号纳入同一策略编排，并完善指标、告警及自适应处置。

### 12.2 登录审计

`auth_login_event` 已落地并接通全部认证路径（V58）；`user_log_login` 仍在 User 域按原样写入，本阶段没有迁移或
废弃它。目标模型：

```text
auth_login_event
  id
  tenant_id
  user_id
  identifier_hash
  provider_id
  authentication_method
  transaction_id
  session_id
  success
  failure_code
  risk_level
  ip
  location
  device
  browser
  os
  user_agent
  occurred_at
```

不存在用户的失败尝试也要记录，但只记录规范化标识的哈希或脱敏值。

已实现的边界：

- 投喂点是 `AuthenticationTransactionService.save()`——事务状态的唯一写入口。密码、TOTP、恢复码、Passkey、
  第三方回调和 Step-up 都经由它到达终态，因此覆盖是结构性的而不是逐处添加的，后续新增路径也无法漏报；
- 只记录 `COMPLETED` 与 `FAILED`。取消或过期是有人放弃了表单，不是对身份的判定；
- 事务存储的 CAS 使终态转换只发生一次，`transaction_id` 唯一约束使一次认证只留一行，重复投喂被静默抑制；
- 标识只存规范化（trim + 小写）后的 SHA-256，明文不入库；查询时对搜索词施加同样的哈希，因此"同一名字被试了
  多少次"仍可回答，而库里不会攒出一份可迁移复用的用户名清单；
- 成功与失败的证据互不借用：成功必须有 `user_id` 且没有 `failure_code`，失败必须有 `failure_code`。服务层与表
  CHECK 都表达这一条；
- 写入在独立事务中且不外抛。认证决定此时已落库，让审计故障回滚一次成功登录、或让审计库故障拒绝所有人服务，
  都不比一条 error 日志更安全；
- 从未解析出租户的失败尝试以空 `tenant_id` 记录，不进入任何租户的管理查询——它无法归属，展示给某个租户等于把
  别家的噪声和被猜测的标识哈希摆到其面前；
- 管理端 `GET /api/admin/auth/loginEvents` 以 `auth:login-event:view` 只读查询当前租户，支持按同租户用户、明文
  尝试标识和成败筛选，硬上限 500。

尚未汇入该视图、目前各自留在领域审计表或 Web 审计中的还有：

- Provider 配置变更。
- 账号绑定/解绑。
- MFA 注册/移除。
- 密码和恢复信息变更。
- Session 撤销。
- 管理员代操作。
- 风险策略命中。

## 13. API 草案

### 13.1 Public API

```text
POST   /api/public/auth/authentication/transactions
GET    /api/public/auth/authentication/transactions/{id}
POST   /api/public/auth/authentication/transactions/{id}/actions/{action}
POST   /api/public/auth/authentication/transactions/{id}/cancel

GET    /api/public/auth/providers
GET    /api/public/auth/external/{providerId}/authorize
GET    /api/public/auth/external/{providerId}/callback
POST   /api/public/auth/external/{providerId}/callback

POST   /api/public/auth/token
POST   /api/public/auth/token/refresh
POST   /api/public/auth/token/revoke
POST   /api/public/auth/logout-all
POST   /api/public/auth/authentication/transactions/step-up

GET    /api/public/auth/sessions
DELETE /api/public/auth/sessions/{sessionId}

# 当前已实现的浏览器会话接口
GET    /api/public/auth/sessions/current
DELETE /api/public/auth/sessions/current

GET    /api/public/auth/identities
POST   /api/public/auth/identities/{providerId}/link
DELETE /api/public/auth/identities/{identityId}
```

### 13.2 Admin API

```text
/api/admin/auth/provider/**
/api/admin/auth/authenticationPolicy/**
GET  /api/admin/auth/sessions?userId={userId}
POST /api/admin/auth/sessions/{sessionId}/revoke
/api/admin/auth/loginEvent/**
/api/admin/auth/riskPolicy/**
```

### 13.3 Internal API

```text
/api/internal/auth/session/introspect
/api/internal/auth/token/introspect
/api/internal/auth/session/revoke
/api/internal/auth/authentication/context
```

Public API 中涉及“当前用户”的操作不得接受任意 `userId` 作为可信身份；必须从 Session/Token 中取得。
跨服务代操作只允许走 Internal API，并校验服务身份和权限。

## 14. 模块建议

保持现有 `kudos-ms-auth` 聚合模块，同时增加认证相关实现：

```text
kudos-ms-auth
├── kudos-ms-auth-common
├── kudos-ms-auth-core
├── kudos-ms-auth-client
├── kudos-ms-auth-sql
├── kudos-ms-auth-api-public
├── kudos-ms-auth-api-admin
├── kudos-ms-auth-api-internal
├── kudos-ms-auth-provider-oauth2
├── kudos-ms-auth-provider-justauth
├── kudos-ms-auth-provider-saml2
├── kudos-ms-auth-provider-ldap
└── kudos-ms-auth-provider-webauthn
```

是否将 provider 拆成独立 Gradle 模块可按依赖重量决定：

- OIDC/OAuth2 可以成为默认能力。
- SAML、LDAP、JustAuth、WebAuthn 建议可选，避免所有项目被迫引入全部依赖。
- 通用 SPI 和领域模型放在 common/core，不得引用可选 provider 的类型。

## 15. 兼容迁移方案

### 阶段 0：修复现有安全边界

- 为 public API 接入明确的 Spring Security FilterChain。
- 修复 public API 信任客户端 `userId` 的问题。
- 登录成功后旋转 Session ID。
- 启用适当的 CSRF、Cookie 和 CORS 策略。
- 服务端解析可信登录 IP。
- 登录成功/失败真正写入审计。
- Remember Me token 改为 hash 存储，作为过渡方案。
- 对用户名枚举、限流和错误计数进行加固。

### 阶段 1：建立认证事务和 SPI

- 实现 `AuthenticationTransaction`。
- 实现认证方法 SPI 和策略接口。
- 将现有密码/TOTP 流程接入新编排器。
- 保持旧 `/api/public/user/passport/login`，内部委托新认证服务。
- 建立统一 `AuthenticationContext`、`amr`、`acr`。

### 阶段 2：标准 OIDC 和全球 Provider Catalog

- 引入 Spring Security OAuth2 Client。
- 实现动态 `ClientRegistrationRepository`。
- 建立 Provider Template 和租户 Provider 配置。
- 首批验证 Google、LINE、Microsoft、Keycloak。
- 完成 state、nonce、PKCE、回调幂等、claim mapping 和外部身份绑定。

### 阶段 3：OAuth2/JustAuth 和企业 SSO

- 接入 GitHub、GitLab、Facebook、Slack、Discord 等 OAuth2 Provider。
- 接入微信、企业微信、钉钉、飞书、支付宝等 JustAuth Provider。
- 接入 SAML 2.0 和 LDAP/AD。
- 完善批量/多次邀请扩展、管理员预创建和账号绑定流程，并补 Provider 管理控制台 UI。

### 阶段 4：会话、凭证和 MFA 完整迁移

- 建立 `auth_credential`（已完成，`V1.0.0.58`）。
- 迁移密码和 TOTP secret（登录密码已完成，V61；security_password 与 TOTP secret 待后续批次）。
- 建立安全 Session/Refresh Token 体系（Session + JWT/Rotation + 账号生命周期/全部下线闭环已完成）。
- 收口现有 User 域密码写入口（策略 SPI、BCrypt 边界、当前/历史密码复用检测、Auth 历史哈希存储、
  版本化算法前缀、登录成功 CAS 透明升级、安全事件，以及经 `IAccountCredentialStore` 端口把登录密码
  收归 `auth_credential` 均已完成；租户级策略与默认算法切换待后续批次）。
- 废弃 `user_login_remember_me`。
- 支持恢复码、短信/邮箱 OTP、Passkey/WebAuthn。
- 支持设备管理、全部下线和 Step-up（当前用户与管理员会话列表、逐台撤销、全部下线及 Step-up
  事务/原会话 CAS 提升、ACR/认证新鲜度声明式执行点已完成，外部身份自助绑定/解绑已接入；设备
  信任、设备命名以及其余业务敏感端点的风险分级接入待完成）。

### 阶段 5：高级安全和生命周期

- 风险引擎和设备信任。
- Provider Token Vault。
- SCIM/企业预配。
- 上游 IdP Logout/Revocation。
- 安全指标、告警和合规报表。

## 16. 测试与验收标准

### 16.1 协议测试

- OIDC Discovery、JWKS 缓存和密钥轮换。
- issuer、audience、nonce、exp、iat 校验。
- PKCE S256 正常、缺失和降级攻击用例。
- state 缺失、错误、重复消费和过期。
- OAuth 回调重放和并发回调。
- SAML 签名、证书轮换、Assertion 时间窗口和加密断言。

### 16.2 账号绑定测试

- 已绑定身份登录。
- JIT 创建并绑定。
- 并发首次登录只创建一个用户。
- 同一外部身份禁止绑定多个本地用户。
- 未验证邮箱不能自动匹配。
- 跨租户身份不串联。
- 解绑最后一种登录方法被拒绝。

### 16.3 会话和 Token 测试

- Session fixation 防护。
- 闲置/绝对过期。
- 修改密码后旧会话失效。
- Refresh Token rotation 和 reuse detection。
- 管理员 token epoch 递增后旧 Refresh Token 不可换新。
- 单设备退出和全部设备退出。
- 禁用/冻结用户后 Token freshness 校验。

### 16.4 Provider 兼容矩阵

首批至少对以下 Provider 建立真实或沙箱集成测试：

| 类型 | Provider |
|---|---|
| 标准 OIDC | Keycloak、本地模拟 IdP |
| 国际 OIDC | Google、LINE、Microsoft |
| 国际 OAuth2 | GitHub、GitLab |
| 国内平台 | 微信/企业微信、钉钉或飞书 |
| 企业 SSO | SAML 测试 IdP、LDAP 测试容器 |

真实 Provider 凭证不得进入源码仓库；CI 中通过 Secret 和受控测试租户注入。

## 17. 已确定的设计决策

1. 不新增强制独立的 `kudos-ms-authn` 服务；由现有 `kudos-ms-auth` 扩展为统一认证和授权服务。
2. `kudos-ms-auth` 内部严格区分 `authentication` 与 `authorization` 子域。
3. `kudos-ms-user` 收敛为身份目录和用户主数据服务。
4. 登录从单次方法升级为可恢复、可过期的认证事务。
5. 标准 OAuth2/OIDC 以 Spring Security OAuth2 Client 为主引擎。
6. JustAuth 不是“国内 Provider 模块”，而是全球非标准和厂商差异兼容层。
7. Google、LINE 等标准 OIDC Provider 优先走统一 OIDC 引擎。
8. pac4j 不作为与 Spring Security 并行的第二套核心安全体系。
9. Keycloak 是可选外部 Identity Broker，不是核心强依赖。
10. 外部身份统一使用 `providerId + issuer + subject` 定位。
11. 默认禁止仅凭相同邮箱自动绑定账号。
12. 第三方认证成功后由 Kudos 签发自己的 Session/Token。
13. 浏览器 Session 与 App/API Token 两种模式同时支持。
14. Remember Me 最终由持久 Session 或 Refresh Token 替代。
15. Provider access token 只有在业务需要调用上游 API 时才加密保存，并与身份绑定表分离。

## 18. 参考规范与项目

- [OAuth 2.0 Security Best Current Practice, RFC 9700](https://www.rfc-editor.org/info/rfc9700/)
- [OAuth 2.0 for Browser-Based Applications, RFC 10017](https://www.rfc-editor.org/rfc/rfc10017.html)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)
- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html)
- [Spring Security SAML2](https://docs.spring.io/spring-security/reference/servlet/saml2/index.html)
- [Google OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect)
- [LINE Login](https://developers.line.biz/en/docs/line-login/)
- [GitHub OAuth](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps)
- [Sign in with Apple REST API](https://developer.apple.com/documentation/signinwithapplerestapi)
- [JustAuth](https://github.com/justauth/JustAuth)
- [pac4j](https://www.pac4j.org/docs/)
- [Keycloak Identity Brokering](https://www.keycloak.org/docs/latest/server_admin/#integrating-identity-providers)
- [Web Authentication Level 3](https://www.w3.org/TR/webauthn-3/)
