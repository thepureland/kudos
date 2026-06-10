# kudos-ability-web-guest

匿名访客（guest）识别 + 在线统计模块。给业务工程提供：

1. **基于 AES 加密 Cookie 的两段式访客标识**：第一次请求种 Cookie，第二次开始按 hash 入 Redis
2. **可插拔的 4 个 SPI**：
   - `IGuestAccessStore` —— 存储后端（默认 `RedisGuestAccessStore`）
   - `IGuestAccessUniqueKey` —— 访客指纹算法（默认 `MD5(UA + IP, cipherKey)`）
   - `IGuestAccessIgnore` —— 请求跳过规则（无默认实现，业务按需注册）
   - `IGuestAccessListener` —— 访客新激活事件钩子（可选）
3. **`OncePerRequestFilter` 入口**：异常一律 swallow + log，访客模块永远不能让正常请求挂掉
4. **总开关 + 运行时可改**：`kudos.ability.web.guest.enabled` 默认 false；改成 true 后过滤器开始处理

## 设计要点

### 为什么两段式？

```
第 1 次请求 (无 Cookie)
  → 生成 AES(UUID, cipherKey) → Set-Cookie: gi=...  → 不入 Redis
第 2 次请求 (带 Cookie)
  → 解密 Cookie 校验合法 → 算 hash(UA + IP) → 入 Redis (TTL)  → 触发 active 事件
第 N 次请求 (带 Cookie)
  → 同上；命中已有 key 时只续 TTL，不再触发 active 事件
```

好处：
- 一次性脚本攻击 / 不接受 Cookie 的爬虫不会进访客统计
- 第一次请求成本只有 Cookie 编解码 + AES，没有 Redis IO
- "访客在线"语义清晰：TTL 内有过请求 = 在线，否则即下线

### 为什么 enabled 默认 false？

挂在所有请求上的 servlet filter 是 footgun——一旦默认开启，依赖本模块的业务工程升级时
会突然多出 Redis IO 与 Cookie 行为。必须业务侧显式 `enabled: true` 才装配。

### 为什么 cipherKey 同时用作 MD5 salt？

`IGuestAccessUniqueKey` 默认实现用 `cipherKey` 作为 MD5 salt。即使攻击者枚举 UA + IP 组合，
没有部署侧的 cipherKey 也无法复现 Redis 中的 hash。**默认 cipherKey 是 soul 沿用的开发值，
生产必须覆盖**。

### 为什么不移植 `GuestAccessAuthedIgnore`？

soul 的默认 ignore 实现读 Spring Security 的 `SecurityContextHolder` 来跳过已登录用户。
kudos 当前没把 spring-security 拉进 web 栈，硬加这个默认实现会强制每个使用本模块的工程
背上 spring-security 依赖。**接入 spring-security 的业务工程自行注册：**

```kotlin
@Bean
fun authedIgnore() = IGuestAccessIgnore { request ->
    val auth = SecurityContextHolder.getContext().authentication
    auth is UsernamePasswordAuthenticationToken && auth.isAuthenticated
}
```

### Cookie 编解码格式

为什么不直接把 token 塞 Cookie 而要做 URL-encode + Base64 双包？
- URL-encode 防 `:` 分隔符出现在 token 自身（token 是 AES Base64 输出，可能含 `:`）
- 外层 Base64 把整个串变成 Cookie attribute 安全字符；末尾 `=` padding 去掉省 4 字节

格式：`Base64( URLEncode(token1):URLEncode(token2):... )`，去掉尾部 `=`。
当前只用一段（token 本身），多段格式从 soul 沿用，留给未来 token + meta 场景。

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/GuestAutoConfiguration` | 装配入口；所有默认 bean 走 `@ConditionalOnMissingBean` |
| `init/properties/GuestProperties` | yml 绑定根；`kudos.ability.web.guest.*` |
| `init/properties/GuestCookieProperties` | Cookie 属性子节点 |
| `init/properties/GuestRepository` | 存储侧子节点（Redis groupName / prefix / TTL / payload 字段） |
| `provider/IGuestAccessService` | 主服务接口（cookie 读 / token 生成 / hash） |
| `provider/IGuestAccessStore` | 存储接口（store / count / getByHash） |
| `provider/IGuestAccessUniqueKey` | 指纹算法接口 |
| `provider/IGuestAccessIgnore` | 跳过规则接口（无默认 bean） |
| `provider/IGuestAccessListener` | 激活事件接口（可选） |
| `provider/GuestAccessService` | 默认服务实现 |
| `provider/GuestAccessUniqueKey` | 默认 MD5(UA+IP) 实现 |
| `provider/RedisGuestAccessStore` | 默认 Redis 存储 |
| `filter/GuestAccessFilter` | OncePerRequestFilter 入口 |

## 配置示例

```yaml
kudos:
  ability:
    web:
      guest:
        enabled: true
        cookie:
          name: gi
          max-age: 14d
          cipher-key: ${GUEST_COOKIE_CIPHER_KEY}     # 生产请用环境变量注入
        repository:
          group-name: data                            # 路由到 redis-map.data
          prefix: my-app-guest
          timeout: 10m                                # 在线时长 = 10 分钟未请求即下线
          payload:
            param-names: [ utm_source, ref ]          # 采集 query 参数
            client-infos: [ domain, requestReferer ]  # 采集 ClientInfo 属性

kudos:
  ability:
    data:
      redis:
        redis-map:
          default:
            host: localhost
            port: 6379
          data:
            host: redis-data
            port: 6379
```

## 自定义 SPI 接入

### 自定义指纹（增加 device-id 维度）

```kotlin
@Bean
fun deviceAwareUniqueKey(properties: GuestProperties): IGuestAccessUniqueKey =
    IGuestAccessUniqueKey { request ->
        val deviceId = request.getHeader("X-Device-Id").orEmpty()
        val agent = request.getHeader("User-Agent").orEmpty()
        DigestKit.getMD5(agent + deviceId, properties.cookie.cipherKey)
    }
```

### 自定义忽略策略（跳过内部健康检查路径）

```kotlin
@Bean
fun probeIgnore() = IGuestAccessIgnore { request ->
    request.requestURI.startsWith("/_health") || request.requestURI.startsWith("/_monitor")
}
```

### 监听新增激活（推到运营埋点）

```kotlin
@Bean
fun analyticsListener(analytics: AnalyticsClient) = IGuestAccessListener { guest ->
    analytics.track("guest.active", mapOf("hash" to guest.hash, "payload" to guest.payload))
}
```

## 测试覆盖

- `GuestAccessServiceTest` —— Cookie encode/decode 往返；payload 反射读取；不存在的 ClientInfo
  字段被读空字符串；缺少 Cookie 时 `fetchGuestToken` 返回 null
- `GuestAccessUniqueKeyTest` —— 相同 UA + IP 生成相同 MD5；UA 或 IP 改变指纹改变
- `RedisGuestAccessStoreTest` —— 真实 Redis（testcontainer）；store/count/getByHash 三方法；
  新 key 触发 listener，TTL roll 不触发；group-name 路由
- `GuestAccessFilterTest` —— enabled 关 / ignore 命中 / 首次请求种 Cookie / 二次请求入 store；
  异常被 swallow

未覆盖：JS / 反向代理层 Cookie 行为；非 Redis 的自定义 store 实现。

## 已知限制 / 后续工作

- ❗ 默认 `cipherKey` 与 soul 一致，**生产必须从环境变量覆盖**
- ❗ Cookie 不自动加 `Secure` flag——需在反向代理 / TLS 层强制
- ❗ 单实例 Redis 下 `count()` 的 SCAN 是 O(N)；超大规模建议自定义 store 用计数器替代
- ❗ `IGuestAccessIgnore` 无默认 bean；不接入 spring-security 的业务工程在登录前后访客都会被记录
- ❗ `getRemoteIp()` 信任 `x-forwarded-for`——本模块继承 web-springmvc 的这条警告

## 依赖

```kotlin
api(project(":kudos-context"))
api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc"))
api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-memdb:kudos-ability-data-memdb-redis"))

testImplementation(project(":kudos-test:kudos-test-common"))
testImplementation(project(":kudos-test:kudos-test-container"))
testImplementation(libs.spring.boot.starter.webmvc.test)
testImplementation(libs.spring.boot.starter.jetty)
testImplementation(libs.spring.boot.starter.data.redis)
```

## 改进建议（自动分析 2026-06-11）

**安全性**
- `enabled=true` 且 `cipherKey` 仍为模块默认值时建议启动期打 WARN（甚至 fail-fast 由开关控制），
  防止开发密钥带上生产——目前只靠 README 提醒（`init/GuestAutoConfiguration.kt`）。
- Cookie 的 `Secure` 属性建议做成配置项（`GuestCookieProperties.secure`），与 `sameSite=None`
  组合时强制要求 Secure，而不是完全依赖反向代理层（`init/properties/GuestCookieProperties.kt`、
  `provider/GuestAccessService.kt`）。
- 指纹算法默认 MD5（加 salt）；用于非对抗性访客统计可接受，但建议默认实现升级为
  HMAC-SHA256，避免安全审计工具对 MD5 的固定告警（`provider/GuestAccessUniqueKey.kt`）。

**测试缺口**
- `sameSite` 配置此前未生效（已于本次修复：`GuestAccessService.setCookie` 通过 Servlet 6
  `setAttribute("SameSite", ...)` 写入）；建议在 `GuestAccessServiceTest.genToken_setsCookieAndReturnsToken`
  补一条 SameSite 属性断言，防止回归。
