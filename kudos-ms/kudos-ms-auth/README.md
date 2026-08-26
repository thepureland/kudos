# kudos-ms-auth

**定位**：**认证与鉴权（`auth`）原子服务**的 Gradle 聚合模块。认证侧承载统一认证事务、
认证方式 SPI 与第三方身份提供方目录；鉴权侧承载角色 / 用户组 / 角色-资源关联 /
角色-用户关联 / 组-用户关联等领域能力。模块与 `kudos-ms-user` 的用户主数据协作，
但不把密码、第三方 `client_secret` 等长期凭证保存到 auth 业务表。

**在工程中的角色**：`SysConsts.ATOMIC_SERVICE_NAME` 取值为 `"auth"`；管理端通过 HTTP，
其他微服务通过 Feign / `client` 调用。

---

## 子模块文档索引

| 子模块 | 说明 |
|--------|------|
| [kudos-ms-auth-common](kudos-ms-auth-common/README.md) | 跨模块共享契约（`IAuthRoleApi` / `IPermittedResource` / VO / 错误码） |
| [kudos-ms-auth-sql](kudos-ms-auth-sql/README.md) | Flyway 迁移脚本（`V1.0.0.20+` 为 `auth_*` 表 DDL；前段为 `sys_*` 种子） |
| [kudos-ms-auth-core](kudos-ms-auth-core/README.md) | DAO / Service / 多级缓存 / 事件订阅 / `IAuth*Api` 实现 |
| [kudos-ms-auth-api-admin](kudos-ms-auth-api-admin/README.md) | 管理端 REST：`/api/admin/auth/role/**`、`/api/admin/auth/group/**` |
| [kudos-ms-auth-api-public](kudos-ms-auth-api-public/README.md) | 对外 Web 启动入口 + `PermittedResourceController`（当前用户视图） |
| [kudos-ms-auth-api-internal](kudos-ms-auth-api-internal/README.md) | 对内 Provider 启动入口 + `AuthRoleInternalController`（Nacos / interservice 缓存） |
| [kudos-ms-auth-client](kudos-ms-auth-client/README.md) | `IAuthRoleProxy` Feign 代理 + `AuthRoleFallback` 降级 |
| [kudos-ms-auth-provider-oauth2](kudos-ms-auth-provider-oauth2/README.md) | 可选的 Spring Security OAuth2/OIDC 动态客户端注册适配层 |
| [kudos-ms-auth-provider-webauthn](kudos-ms-auth-provider-webauthn/README.md) | 可选的 Yubico WebAuthn/Passkey ceremony 及 FIDO MDS trust source 适配层 |
| [kudos-ms-auth-token-jwt](kudos-ms-auth-token-jwt/README.md) | 可选的短期 JWT、Refresh Token Rotation 与 Bearer Filter 适配层 |
| [kudos-ms-auth-notification-msg](kudos-ms-auth-notification-msg/README.md) | 可选的安全事件 outbox 到 `kudos-ms-msg` 模板/渠道发送适配层 |
| [kudos-ms-auth-notification-eventbus](kudos-ms-auth-notification-eventbus/README.md) | 可选的安全事件 outbox `EVENT_BUS` 渠道到 `kudos-ability-distributed-stream` 适配层 |
| [kudos-ms-auth-session-spring](kudos-ms-auth-session-spring/README.md) | 可选的会话撤销瞬间物理删除 Spring Session 容器记录的适配层 |
| [kudos-ms-auth-secret-vault](kudos-ms-auth-secret-vault/README.md) | 可选的 HashiCorp Vault KV `client_secret` 解析器 |
| [kudos-ms-auth-secret-aws-secrets-manager](kudos-ms-auth-secret-aws-secrets-manager/README.md) | 可选的 AWS Secrets Manager `client_secret` 解析器 |
| [kudos-ms-auth-secret-google-secret-manager](kudos-ms-auth-secret-google-secret-manager/README.md) | 可选的 Google Secret Manager `client_secret` 解析器 |
| [kudos-ms-auth-secret-azure-key-vault](kudos-ms-auth-secret-azure-key-vault/README.md) | 可选的 Azure Key Vault `client_secret` 解析器 |

---

## 依赖关系（概念）

```
                    ┌──────────────────┐
                    │ kudos-ms-auth-   │
                    │ common           │
                    └────────┬─────────┘
                             │
         ┌───────────────────┼───────────────────┐
         ▼                   ▼                   │
┌─────────────────┐  ┌─────────────────┐         │
│ kudos-ms-auth-  │  │ kudos-ms-auth-  │         │
│ sql             │  │ client          │         │
└────────┬────────┘  └────────┬────────┘         │
         │                    │ only common      │
         └──────────┬─────────┘                  │
                    ▼                            │
            ┌───────────────┐                    │
            │ kudos-ms-auth-│                    │
            │ core          │◄───────────────────┘
            └───────┬───────┘
                    │
    ┌───────────────┼───────────────┐
    ▼               ▼               ▼
┌────────┐   ┌────────────┐   ┌────────────┐
│ api-   │   │ api-       │   │ api-       │
│ admin  │   │ public     │   │ internal   │
└────────┘   └────────────┘   └────────────┘
```

---

## 关键概念

- **认证事务（Authentication Transaction）**：用显式状态机统一密码、OTP 与后续第三方登录，
  对外只暴露 `create/get/act/cancel`，认证方式通过 `IAuthenticationMethodProvider` 插拔。
- **认证尝试防护**：密码适配器复用 user Passport 的 IP/主体请求限流及密码/TOTP/恢复码独立失败窗口；
  Passport `RATE_LIMITED` 会映射为可重试的 `TOO_MANY_AUTHENTICATION_ATTEMPTS` challenge。
- **防用户名枚举**：密码适配器将用户不存在、密码错误、账号停用、自动锁定及管理员冻结统一映射为
  可重试的 `INVALID_CREDENTIALS` challenge；真实原因只保留在 Passport 内部审计中。
- **统一认证会话**：密码/TOTP 和外部身份登录成功后都登记逻辑 `AuthenticationSession`；记录包含
  `authTime/amr/acr`、闲置/绝对过期、凭证版本、终端信息及撤销原因。逻辑 ID 可公开用于会话管理，
  servlet Session ID 始终留在 HttpOnly Cookie/服务端；Redis CAS 与单机内存实现自动切换。当前用户
  和管理员均可查看活动设备并逐台撤销，管理员操作强制执行租户归属、专用权限与原因审计。
- **Step-up 基础闭环**：当前受管 Session 可创建绑定原用户/租户/逻辑会话的 `STEP_UP` 事务；认证
  主体和目标 ACR 双重校验通过后，以 CAS 提升原会话并旋转 HttpSession ID，不新建逻辑会话且不延长
  绝对期限。默认 ACR 顺序可通过 `IAuthenticationAssurancePolicy` 替换，未知 ACR 只精确匹配。
- **声明式认证强度**：业务入口通过 `@RequiresAuthenticationAssurance(acr, maxAgeSeconds)` 声明最低
  ACR 与可选认证新鲜度；浏览器和 Bearer 链路共用实时验证后的逻辑会话。失败统一返回 HTTP 403
  challenge，包含失败原因、目标 ACR、最大年龄与 Step-up 创建入口。外部身份自助绑定/解绑已作为
  首批真实接入点，旧的认证事务查询参数仅作为过渡兼容参数保留且不再受信任。
- **TOTP 自助注册**：近期认证后创建短期待确认注册，Redis Lua/单机 CAS 限制错误次数并保证只消费
  一次；密钥只在创建响应出现一次，待确认和正式字段均使用 AES-GCM 密文，确认启用或撤销会触发
  既有全端失效链路。
- **MFA 恢复码**：近期认证后整组生成/轮换，原文只返回一次，库内仅保存作用域 SHA-256；单码原子
  消费，TOTP 认证器变化时整组撤销。密码事务只在存在可用码时暴露恢复码动作，成功记为 MFA ACR。
- **租户 MFA 策略**：已提供 OPTIONAL / REQUIRED / CONDITIONAL 的一对一持久化、账号类型/角色条件、
  0～90 天宽限期、允许因子/恢复码开关、租户固定的管理 API 及当前用户有效决策 API。策略已强制
  执行于密码及第三方登录：宽限期内返回登录后注册动作，到期阻断；第三方第一因素成功后可在同一
  事务继续 TOTP/恢复码/Passkey，完成前不签发会话。恢复码开关及 WebAuthn allowed method/实际注册
  状态都会约束挑战可见性；密码第一因素成功后也可在无密钥事务状态中继续 Passkey。
  V57 增加逐用户 MFA **注册**临时豁免，作为宽限期已过账号的受控救援入口，取代"放宽整个租户策略"。它只解除
  "必须先注册"这一阻断：判定顺序把"已注册 → 要求第二因素"排在豁免之前，因此豁免不可能成为管理员触发的 MFA 绕过。
  授予受三条硬边界约束——已注册账号不可豁免、不可豁免自己、窗口必须在未来且不超过配置上限（默认 7 天，收敛到
  1..30）。每次授予是新行，撤销只作用于当前生效的授予，不把"自然到期"改写成"被撤销"；三个动作使用三个独立权限点，
  租户与操作者固定取会话，自助状态接口在生效时返回 `exemptionExpiresAt`。
- **WebAuthn/Passkey 基础层**：已持久化租户隔离的 credential ID、user handle、COSE 公钥及认证器状态，
  断言签名计数使用旧值 CAS 并拒绝回退/重放；注册/撤销会使旧 Session/Token 失效，账号删除会清理
  凭证。MFA 注册决策已同时识别 TOTP/WebAuthn。可选 provider 已完成 registration 与 assertion 的
  begin/finish、已知用户及无用户名登录、Yubico 协议验证，以及 Redis Lua/进程内两种短期一次性
  challenge store；公开断言 API、事务绑定的 Passwordless 登录、Passkey Step-up 及逻辑会话签发已
  接入；密码和第三方登录均可按租户策略在原事务继续绑定主体的 Passkey 第二因素。当前 Session 用户
  已可在近期 password-or-stronger 再认证后完成注册、查询、重命名和撤销，接口不接受客户端指定租户
  或用户；重命名只更新经过约束的显示元数据，不触发凭证生命周期失效。
  管理端另提供同租户只读凭证审计，覆盖活动及已撤销记录；只返回 credential SHA-256 指纹和认证器
  公开属性，不返回原始 credential ID、公钥、user handle 或签名计数。审计会通过通用风险评估 SPI
  聚合认证器型号风险；启用内置 FIDO MDS 时返回当前 AAGUID 的风险级别、来源及状态码。风险评估默认只做
  审计，租户可显式配置阻断 `WARNING`、`CRITICAL` 或 `NOT_EVALUATED`；断言在协议、主体和账号校验通过后、
  签名计数更新及会话签发前执行策略。策略命中只阻止新的 WebAuthn 认证，不自动撤销凭证或既有会话；
  `NORMAL` 不允许进入阻断集合，且部署没有风险评估器时不能保存非空阻断策略。每次命中同步发布
  `WebAuthnAuthenticatorRiskPolicyBlocked` 内部事件，只携带 credential SHA-256 指纹而非原始 ID；有
  Micrometer `MeterRegistry` 时自动暴露 `kudos.auth.webauthn.risk.policy.blocked` 计数器，唯一标签为固定风险
  级别，部署可据此配置告警而不会引入租户、用户或设备维度的高基数时序。同一事件还会在
  独立事务中写入 `auth_security_event`，按 5 分钟 UTC 窗口聚合重复命中；持久化故障只记录错误，
  不能使已命中的阻断失效。管理员可以使用 `auth:security-event:view` 按当前租户、用户和风险级别有界查询，
  并按状态筛选待处理队列。事件以乐观版本执行 `OPEN → ACKNOWLEDGED → CLOSED`，确认和关闭分别要求
  独立权限、原因和 Web 审计；关闭还要求标准处置分类。工作流只记录调查结论，不隐式解锁、撤销凭证或恢复会话。
  新事件还会按可替换 SLA 策略写入截止时间（默认 `CRITICAL=1h`、`WARNING=4h`、其他 `24h`）；管理员可用
  独立权限分派同租户负责人，并按负责人或 `overdueOnly=true` 查询未关闭超时队列。分派通过同一工作流版本
  做 CAS，提交后发布 `AuthSecurityEventAssigned`，部署可将其接入邮件、短信、IM 或工单，而核心不绑定渠道。
  超时事件可由外部调度有界调用升级扫描；每个候选在独立事务中以升级级别 CAS，成功后与
  `auth_security_event_notification` outbox 同事务提交。默认最多升级 3 级、每 4 小时再次升级，均可配置或替换
  策略。通知消费者通过租约批量领取，并以指数退避重试直至 `DEAD`；交付语义为至少一次，渠道适配器应以
  `notificationId` 作为幂等键。核心仍不主动启用调度器，也不内置具体通知渠道。
  V51 进一步提供 `IAuthSecurityEventNotificationPublisher` 可靠发布边界和有界交付编排：部署必须恰好提供一个
  发布器，多渠道场景由组合发布器维护自己的部分交付账本。可重试异常继续退避，明确的永久异常直接进入
  `DEAD`。管理员可凭独立权限查询当前租户死信并带原因重放；重放重置本轮尝试次数，并在追加式审计表保留
  租户、操作者、原因和 UTC 时间。
  V52 在发布前增加可替换的租户路由 SPI；默认将已分派事件路由给负责人站内信，未分派事件路由到租户安全队列
  语义。交付结果发布不含租户/用户的低基数事件，并在 Micrometer 可用时暴露按固定通知类型和结果枚举标记的
  `kudos.auth.security.event.notification.delivery`。可选 `kudos-ms-auth-notification-msg` 将 USER 路由适配到
  `kudos-ms-msg` 的站内信、邮件和短信，每个渠道使用独立幂等键。
  V53 把租户路由从"只能替换 SPI"升级为可持久化配置：`auth_security_event_notification_route` 按租户、通知类型和
  投递形态（已分派/未分派）唯一保存路由码、目标、渠道集合、固定响应人、是否并入负责人、启停状态和降级行为。
  装配的默认策略读取该配置，未配置或规则停用时与 V52 完全一致；无人可寻址时按配置降级为内置默认、安全队列，
  或显式失败进入 `DEAD`，不会替换成任何未被配置过的收件人。管理接口以独立权限点固定当前管理员租户和操作者，
  请求体不含租户与操作者字段；保存对 `configVersion` 做 CAS，陈旧版本与并发创建统一 409，并在同一事务追加带
  变更前后快照的审计。投递路径读取按租户缓存（`LOCAL_REMOTE`），保存提交后由只含租户 ID 的领域事件驱逐并跨节点
  广播失效；路由在每次重试和重放时重新解析，因此配置变更无需触碰 outbox 行即可生效。
  V54 补上了"未分派事件只能配置固定响应人"的缺口：`auth_security_event_oncall_roster` 与
  `auth_security_event_oncall_shift` 按租户保存命名轮值和显式 UTC 班次窗口，路由规则可用 `responderRosterCode`
  引用它。核心只暴露协议中立的 `IAuthSecurityEventResponderResolver`（租户 + 值班表编码 + 升级级别 + 时刻 →
  本地用户 ID），内置实现读取上述轮值，升级级别 N 追加命中 `tier <= N` 的班次；无人值班返回空集并交给规则的
  fallback，读取失败则抛出使 dispatcher 退避重试。轮值按整份替换保存、做版本 CAS 并留存变更前后排班快照；
  缓存的是排班计划而非"当前值班人"，时间过滤在读取时施加。把轮值放在外部值班/呼叫系统的部署替换该 SPI 即可，
  core 仍不依赖任何厂商。
  V55 把投递单位从"行"下沉到"渠道"：publisher 通过 `supports(destination, channel)` 声明能力，dispatcher 按渠道
  选择并逐渠道结算到 `auth_security_event_notification_channel` 账本。多渠道部署因此只需分别注册各自的 publisher，
  不再需要手写组合器和部分交付账本；重试只挑未结算的渠道，某渠道被永久拒绝不会拖住其它渠道，也不再消耗行级尝试
  次数。行级结果由渠道结果聚合，只有全部渠道被永久拒绝时整行才 `DEAD`，管理员重放会清空账本。同一 destination+channel
  被两个 publisher 声明属于部署错误并直接报错。指标新增固定基数的
  `kudos.auth.security.event.notification.channel.delivery{type,channel,outcome}`。交付语义仍是至少一次，适配器
  需按 `notificationId` + 渠道幂等。
  V56 补上默认路由的最后一块：可选 `kudos-ms-auth-notification-eventbus` 认领 `EVENT_BUS` 渠道，通过
  `kudos-ability-distributed-stream` 的 producer binding 投递，broker 由部署选择，core 仍不依赖 Kafka/RabbitMQ/
  RocketMQ。它可与 msg 适配器同时启用（两者的 Bean 条件已从"SPI 缺失"改为各自类型，否则会互相抑制）。总线载荷只含
  标识与路由决策，不含凭证材料、指纹或风险详情。需要注意的真实边界：`StreamBridge` 异步，发送成功只表示进入本地
  producer 队列，后续 flush 失败由 stream 的 `sys_mq_fail_msg` 承接而非 auth outbox，因此部署必须启用该失败持久化。
  租户 attestation 策略基础层也已持久化，并在注册协议验证成功、凭证落库前执行 format、AAGUID
  allow/deny 和可信证明准入；管理端查询/保存接口固定当前管理员租户，并暴露部署 trust source 可用性。
  provider 可显式启用 Yubico FIDO MDS 的验签缓存和定期刷新；受限租户注册请求 `DIRECT` attestation，
  普通租户仍为 `NONE`。未配置策略保持兼容性默认，不限制 format/AAGUID 且不强制可信证明。
  WebAuthn-only 强制策略默认仍拒绝；只有部署确认 provider 与自助入口覆盖全部认证节点后，才可通过
  `kudos.ms.auth.mfa.policy.allow-webauthn-only=true` 显式放开。
- **App/API Token**：独立 API Session 承载短期 JWT；Access Token 每请求校验权限版本和会话状态。
  Refresh Token 仅存 SHA-256，使用固定绝对期限、单次轮换和 family 重放检测；管理员 token epoch
  递增后既有 Access/Refresh Token 均不能恢复有效。签名密钥完全由部署提供，不存在弱默认值。
- **密码历史**：`auth_password_history` 只保存被替换的密码编码，兼容历史 BCrypt 与 `{bcrypt}`；
  Auth 与 User 同进程装配时自动参与
  登录密码和安全密码重置，默认拒绝最近 5 个历史值，账号删除后同步清理，不保存或记录原始密码。
- **账号生命周期统一失效**：登录密码、安全密码、停用、冻结、TOTP 认证器变更和账号删除在 User 事务提交后
  触发 Auth 编排，统一递增 token epoch、撤销全部 Refresh family 与逻辑会话；用户“退出全部设备”
  和管理端强制下线复用同一安全边界。
- **身份提供方目录（Identity Provider Catalog）**：`auth_provider_template` 保存 Google、LINE、
  通用 OIDC 等无密钥模板，`auth_identity_provider` 保存租户实例；长期密钥只保存引用
  `client_secret_ref`，由可替换的密钥解析器读取。
- **外部身份邀请**：`INVITE_ONLY` 为既有本地用户签发租户/Provider 限定的一次性 bearer token；
  token、可选预期邮箱及消费 subject 在数据库中均只保存 SHA-256。
- **JIT 自动开户**：`JIT_CREATE` 为未绑定的稳定外部身份原子创建无本地密码账号和首条绑定；
  不按邮箱静默合并，并通过唯一约束、回滚和胜出绑定重读处理并发回调。
- **Provider JIT 配置**：每个租户 Provider 可独立配置用户名策略、已验证邮箱/域名准入及
  组织、上级、账号字典、locale、时区和货币默认值；保存操作要求权限、可信上下文与原因审计。
- **Provider 管理与 claim mapping**：租户实例支持列表、详情、创建、更新和启停；密钥只接受引用且
  响应不回显。类型化有序路径可映射嵌套 OAuth2/OIDC claims，OIDC subject 固定为协议校验后的 `sub`。
- **密钥引用治理**：可选 OAuth2 模块对 resolver 做确定性选择，内置 `env:`/`property:` 命名空间
  白名单、只返回状态的管理员在线检测和本机轮换失效；成功值缓存默认关闭且最多配置一小时。独立
  Vault 模块支持服务端固定 mount/path policy 的 KV v1/v2 读取；AWS 模块支持受命名空间/ARN 策略
  约束的 `AWSCURRENT` 原始字符串或 JSON 字段读取；Google 模块支持项目/secret id 策略约束、
  服务端固定版本及 CRC32C 校验；Azure 模块通过部署方 vault alias 精确匹配预配置 `SecretClient`，
  支持 secret name 前缀及服务端固定版本。
- **角色（`auth_role`）** + **角色-资源（`auth_role_resource`）** + **角色-用户（`auth_role_user`）**：
  RBAC 三件套
- **用户组（`auth_group`）** + **组-用户（`auth_group_user`）**：用户分组管理。可作为
  角色赋予的目标，让"组里所有人"自动持有该角色权限
- **生效权限** = `直接给用户的角色` ∪ `用户所在组的角色` ∪ `用户所在组的所有上级路径` 的角色
  （详见 `auth_group` 的层级 path 字段）
- 与 `kudos-ms-sys` 的关系：`sys.sys_resource` 是资源主数据，本模块只持有"哪个角色绑了哪些资源 id"
- 与 `kudos-ms-user` 的关系：`user.sys_user` 是用户主数据，本模块只持有"哪个用户属于哪些组 / 哪些角色"

---

## 命名与约定（跨模块）

- **原子服务名**：`SysConsts.ATOMIC_SERVICE_NAME = "auth"`——所有 Feign 服务名、缓存 namespace、
  Flyway 表前缀、日志 `service` 字段都以此为锚点。
- **领域 API**：`common` 中 `IAuth*Api` 由 `core` 中 `Auth*Api` 实现；`client` 中 `IAuth*Proxy`
  继承同一接口并通过 Feign 调用远程服务。**目前仅 `role` 对外**——`group` 域只通过 admin HTTP
  暴露，无 Feign 接口。
- **方法级 Feign 路由**：所有 `IAuth*Api` 的方法上挂 `@GetMapping("/api/internal/auth/...")`
  / `@PostMapping`，接口类型上**不**放 `@RequestMapping`；`auth-api-internal` 的 Controller 直接
  `implements IAuth*Api`，路径自动继承——签名漂移可在编译期暴露。
- **管理端 vs 内部**：`/api/admin/auth/**` 仅由 `api-admin` 承载，`/api/internal/auth/**` 仅由
  `api-internal` 与 `api-public` 内的 `PermittedResourceController` 承载；两套前缀在网关层应
  分别路由。
- **跨服务种子数据**：`auth-sql` 的 `V1.0.0.0–V1.0.0.6` 是写入 `sys_*` 表的菜单 / 字典 / 缓存
  登记 / 参数 / i18n 文案——遵循"被写入方负责 DDL，写入方负责 INSERT"。

具体类名与边界以各子模块源码为准。

## 已知限制 / 后续工作

- ❗ **无 Redis 时才回退单机存储** — 存在 `RedisTemplates` 时认证事务自动使用 Lua CAS + TTL
  的共享 Redis 实现；显式无 Redis 的轻量部署才使用 `InMemoryAuthenticationTransactionStore`。
- ✅ **OIDC 已绑定账号登录及自助 link/unlink 已闭环** — authorize/callback、PKCE、
  state/nonce/token 校验、外部主体转换、本地 Session、近期重新认证、最后登录方式保护和追加式
  绑定审计已具备。
- ✅ **OIDC 授权请求已支持跨节点回调** — PKCE verifier、OIDC nonce 和完整授权请求默认进入
  Redis TTL 快照，state 摘要作键，回调 Lua 原子取出并删除；无 Redis 时才回退单机内存。授权往返
  不再依赖 `HttpSession` 或粘性会话；登录成功后的本地 Session 仍按独立的多节点会话策略部署。
- ✅ **管理员预绑定/代解绑已闭环** — Auth Admin 显式命令从当前管理员上下文取得操作者和租户，
  Provider 字段取活动目录配置，要求具体权限点和操作原因，并记录变更前后安全快照；历史 User
  Admin `accountThird` 已降为只读。
- ✅ **邀请制首次绑定已闭环** — 管理员可为同租户既有用户签发/吊销一次性邀请，OAuth 发起时将
  无密 invitation id 固定到认证事务，回调原子消费并绑定；支持有效期和可选已验证邮箱约束。
- ✅ **JIT 自动开户与 Provider defaults 已闭环** — 三种稳定用户名策略、verified-email/域名
  准入、组织/上级/账号字典/locale/时区/货币默认值均已配置化；账号、默认组织成员关系和首条绑定
  原子创建，失败不遗留孤儿用户，并记录 `JIT_BIND` 审计。
- ✅ **Provider 管理与 claim mapping API 已闭环** — 模板/租户实例查询、创建、更新、启停、
  操作者/原因审计、密钥引用遮蔽及类型化嵌套 claim path 已落地；为保护引用完整性刻意不提供物理删除。
  密钥引用已有命名空间策略、实时状态检测及显式本机缓存刷新；HashiCorp Vault KV、AWS Secrets
  Manager、Google Secret Manager 和 Azure Key Vault resolver 均已作为可选模块落地。尚未完成管理
  控制台 UI 及 Vault/云密钥写入控制面；启用非零缓存的多节点部署需在每个节点刷新，默认零 TTL
  不存在该问题。
- ✅ **管理员会话管理已闭环** — 管理员可按同租户用户查看活动逻辑会话并逐台撤销；目标租户从
  当前管理员和可信账号记录共同解析，跨租户与不存在用户采用相同 404，接口要求
  `auth:session:view` / `auth:session:revoke` 权限，撤销原因同时进入会话注册表和 Web 审计。

- ❗ **生效权限算法分散** — "用户 → 直接角色 ∪ 组继承角色"的合并逻辑同时存在于
  `RoleIdsByUserIdCache.computeEffectiveRoleIds` / `ResourceIdsByUserIdCache.computeEffectiveRoleIds` /
  `ResourceIdsByTenantIdAndUsernameCache.computeEffectiveRoleIds` 三处，代码完全一样——
  后续应下沉到 base 层 util，避免三处漂移
- ❗ **只有 `role` 对外开 Feign** — `group` 域只暴露 admin HTTP，跨服务想查"用户所在组" /
  "组的权限"必须走 admin 路径或自建 group Proxy
- ❗ **组层级 path 字段未文档化** — `auth_group.path` 是字符串祖先链（如 `/g1/g2/g3`），
  上级路径继承权限要靠 path LIKE 查询；README 提到"详见 path 字段"但没有具体格式说明
- ❗ **资源 / 用户主数据跨服务一致性** — auth 只持 id 不持快照；`sys_resource` / `user_account`
  被改名 / 删除时，auth 这边的 `auth_role_resource` / `auth_role_user` 会留死引用
- ❗ **`/api/admin/auth/**` 零 `@PreAuthorize`** — 修改角色权限的接口仅靠网关守护
- ❗ **fallback 只覆盖 `IAuthRoleApi`** — auth 客户端只有 1 个 Proxy 1 个 Fallback；group / 资源
  绑定关系等其他接口若新增 Proxy，需补对应 Fallback

## 改进建议（自动分析 2026-06-11）

> 本节为自动深度审查产出。已直接修复的三处（`AuthRoleApi.isUserHasResource` 参数错位、
> `AuthRoleUserService.batchBind` 绑定不存在角色、审批流自批）不在此列，下面是**未直接修改**的发现，
> 按维度分类，供后续排期。

### 安全性

1. **删除角色不回收其授权（权限残留，高优先级）** — ✅ 已修复（2026-06-11）：
   `AuthRoleService.deleteById`/`batchDelete` 现在在事务内级联删除 `auth_role_user` /
   `auth_role_resource` / `auth_group_role` / `auth_role_org` / `auth_role_exclusion` 关系行
   （删前先快照受影响的 userIds / resourceIds / groupIds），并发布
   `AuthRoleUserRelationsChanged` / `AuthRoleResourceRelationsChanged` /
   `AuthGroupRoleRelationsChanged`，使 `RoleIdsByUserIdCache` / `ResourceIdsByUserIdCache` /
   `UserIdsByRoleIdCache` / `ResourceIdsByRoleIdCache` 等正确失效；新增集成测试
   `AuthRoleDeleteCascadeTest`。剩余工作：指向已删角色的 `auth_role_grant_request`（含 PENDING）
   行未级联取消——approve 时 `batchBind` 会因角色不存在而拒绝，无安全风险，但会留下悬挂申请记录。
2. **时态授权绕过 SoD 与角色校验** — ✅ 已修复（2026-06-11）：`bindTemporal` 现在对齐
   `batchBind` 防线——先校验角色存在，再复用 `AuthRoleUserService.findSodViolationMessage`
   （batchBind 同一逻辑抽取的 internal 共享方法）做 SoD 互斥检查；且已存在**永久授权**
   （start/end 均 NULL）时直接拒绝、不再静默替换（须先显式 unbind），仅同对临时授权保留
   replace 语义。新增 4 个单测（不存在角色 / 永久授权冲突 / SoD 违规被拒、干净用户成功）。
3. **SoD 检查未覆盖候选角色的祖先链** —
   `AuthRoleUserService.batchBind` 调 `findViolation(tenantId, roleId, effectiveRoles)` 时只用候选
   roleId 本身比对互斥规则；但持有子角色等效继承祖先权限（见 `AuthRoleExclusionService.
   computeEffectiveUsersForRole` 的注释），若互斥规则定义在候选角色的**祖先**上即可绕过。应把
   `候选角色 + 其祖先` 全部送检。
4. **cancel 未校验申请人身份（水平越权）** —
   `kudos-ms-auth-core/src/io/kudos/ms/auth/core/role/grant/service/impl/AuthRoleGrantRequestService.kt`
   的 `cancel(id)` 不校验当前用户是否为 `requesterId`（DDL 注释声称 "requester flips their own"）。
   需产品决策是否允许管理员代撤销，再补归属校验。
5. **`built_in` 角色无保护** — `auth_role.built_in` 字段存在（`copyRole` 也刻意置 false），但
   `AuthRoleService` 的 update/delete/updateActive 均不阻止修改或删除内置角色。
6. **批量接口无大小上限** — `AuthRoleAdminController.bindUsers/batchBindUsers/getDeleteImpact`、
   `AuthGroupAdminController` 同名端点、`AuthResourcePermissionAdminController.roleNamesByResourceIds`
   均接收无界集合；`getDeleteImpact`/`roleNamesByResourceIds` 还会对每个 id 触发一次缓存/DAO 查询，
   可被用作放大攻击。建议统一加 `require(ids.size <= N)`。
7. **时态过滤方向不对称** — `AuthRoleUserDao.searchRoleIdsByUserId`（用户→角色）过滤生效窗口，
   而 `searchUserIdsByRoleId`（角色→用户）不过滤，导致 `UserIdsByRoleIdCache` /
   `AuthRoleService.getRoleUsers` / `getUsersByRoleCode` / SoD 违规扫描把过期、未生效的授权当作有效。

### 功能缺陷 / 可补充功能

8. **未来生效授权不会准点生效** — `start_time` 在未来的授权写入后，没有任何机制在 start_time
   到点时让 `RoleIdsByUserIdCache` 等失效（`ExpiredGrantPurgeScheduler` 只删过期行），实际生效时间
   取决于缓存何时被偶然失效。建议 purge 调度同时扫描"刚跨过 start_time"的授权并发失效事件。
9. **审批流无审批人路由** — `AuthRoleGrantRequestAdminController` KDoc 自述"没有 per-request
   approver 概念"，任何能访问 admin 端点的人都可审批任意租户的请求（垂直+租户越权面）。
10. **`searchRoleIdsByUserId` 全行加载后内存过滤** — `AuthRoleUserDao` 把用户全部授权行查出再用
    `isActiveAt` 过滤，行数大时应下推为 SQL 条件（与 `searchExpiredGrants` 一样用 Criteria 表达）。

### 测试覆盖

11. **`AuthRoleExclusionService` 无专属测试** — `kudos-ms-auth-core/test-src` 无 `role/exclusion`
    目录；`canonicalise` 交换序、同租户校验、重复 pair、`findViolation` 边界（候选在 A 侧/B 侧）等
    均未覆盖，而它是 SoD 的核心。
12. **`AuthRoleApi` 无测试** — 本次修复的 `isUserHasResource` 参数错位正是因缺少针对
    `kudos-ms-auth-core/src/io/kudos/ms/auth/core/role/api/AuthRoleApi.kt` 的委托正确性测试而长期潜伏。
13. **权限边界用例缺失** — "角色删除后用户不再持有其资源"已由 `AuthRoleDeleteCascadeTest`
    覆盖（2026-06-11），"临时授权的 SoD / 永久授权冲突被拒"已由 `AuthRoleUserTemporalServiceTest`
    新增用例覆盖；仍缺"临时授权过期后立即失权"、"跨租户绑定被拒"这类端到端断言；
    api-admin / api-internal / api-public / client 四个子模块零测试。

### 可扩展性

14. **SoD / 审批 / 数据范围策略硬编码** — 互斥判定、审批状态机、`DataScopeEnum` 解析都内嵌在各
    service；若需"三选一互斥"、多级审批等只能改核心代码，可抽 `PolicyChecker` 类 SPI。
15. **`ExpiredGrantPurgeScheduler` 无分布式锁** — KDoc 自述需 ShedLock；多实例部署时虽幂等但会
    重复扫表，建议直接集成。

### 可观测性

16. **授权变更无审计** — 角色/资源/组绑定与解绑只有 `log.debug` 与缓存事件，无审计落库
    （操作者、时间、变更前后值）；SoD 拒绝（`AuthRoleUserService.batchBind` 抛异常路径）与审批
    决策也只有 debug 级日志。安全敏感动作（bind/unbind/approve/reject/updateScope）建议
    `log.info` + 审计事件。

### 可维护性

17. **`AuthRoleService` 依赖 14 个注入、跨 role/group/sys/user 四域** — 代码注释已自知应拆
    "permissions facade"；`getEffectivePermissions` 等聚合方法宜下沉到独立 service。
18. **`@Resource` 与 `@Autowired` 混用** — 同一文件内两种注入注解并存（如 `AuthRoleUserService`），
    建议统一。
19. **`AuthRoleService.kt` 文件底部的私有 `typealias AuthGroupCacheEntryAlias`** — 为"让 import 区
    更清晰"引入别名反而增加间接性，直接 import `AuthGroupCacheEntry` 即可。

### API contract

20. **submit/cancel 放在 admin 层语义不符** — 申请人（普通用户）发起/撤销授权申请的动作走
    `/api/admin/auth/roleGrantRequest/**`，意味着申请人必须能访问 admin 网关段；宜在 api-public
    暴露受限的 submit/cancel（仅本人），admin 留 approve/reject。
21. **`findViolations` 对不存在的 exclusionId 返回空 VO** —
    `AuthRoleExclusionService.findViolatingUserIds` 查不到记录时返回 roleAId/roleBId 为空串的
    "成功"响应，调用方无法区分"无违规"与"id 不存在"，宜抛 404/IllegalArgumentException。

### 文档

22. **KDoc / README / SQL 注释总体充分**（各子模块 README、迁移脚本头注释质量高）；遗留点主要是
    组级 README 已列的 `auth_group.path` 格式未文档化，以及本节新增各项。
