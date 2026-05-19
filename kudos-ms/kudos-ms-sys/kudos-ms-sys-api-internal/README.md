# kudos-ms-sys-api-internal

## 定位

系统（`sys`）原子服务的 **对内 RPC HTTP 暴露层**（历史命名 **sys-api-provider**）：在 `kudos-ms-sys-core` 之上，把 `common` 中所有 `ISys*Api` 接口以 `@RestController` 形态暴露为 **`/api/internal/sys/**`** 路径，专供 **`kudos-ms-sys-client` 的 Feign 代理** 调用，同时承载注册中心、配置中心、跨服务缓存等"集群内部"基础设施。

> 与历史 README 不同：本模块 **包含 18 个内部控制器**，并不是"无 Controller 的空壳"。下方"控制器一览"列出全部实现。

---

## 入口与自动配置

| 类型 | 类 | 说明 |
|------|-----|------|
| 启动类 | `SysApiProviderApplication` | `@EnableKudos`，`main` 启动 Spring Boot |
| 自动配置 | `SysApiProviderAutoConfiguration` | `@ComponentScan("io.kudos.ms.sys.api.internal")`，组件名 **`kudos-ms-sys-api-internal`**，由 `IComponentInitializer` 机制装配（非 Spring Boot 原生 SPI） |

`@ComponentScan` 同时扫描 `init` 与 `controller` 子包；`core` 中各 `Sys*Api`（`@Component`）由 `core` 自身的 `SysAutoConfiguration` 提供，本模块只负责把它们暴露为 HTTP。

---

## 控制器实现模式

`api-internal` 中每个控制器：

1. 标 `@RestController`；
2. **实现 `common.<module>.api.ISys*Api`** 接口；
3. 不在类上写 `@RequestMapping` —— 所有路径来自 **接口方法上的 `@GetMapping` / `@PostMapping("/api/internal/sys/...")`**；
4. 仅做 `core.<module>.api.Sys*Api` 的薄委托，不放业务逻辑。

```kotlin
// common/.../tenant/api/ISysTenantApi.kt —— 契约（路径在这里）
interface ISysTenantApi {
    @GetMapping("/api/internal/sys/tenant/getTenant")
    fun getTenantFromCache(id: String): SysTenantCacheEntry?
    // ...
}

// api-internal/.../tenant/SysTenantInternalController.kt —— HTTP 实现
@RestController
class SysTenantInternalController(
    private val sysTenantApi: SysTenantApi,   // core 的 @Component
) : ISysTenantApi {
    override fun getTenantFromCache(id: String) = sysTenantApi.getTenantFromCache(id)
}

// client/.../tenant/proxy/ISysTenantProxy.kt —— Feign 代理
@FeignClient(name = "...", fallback = SysTenantFallback::class)
interface ISysTenantProxy : ISysTenantApi   // 沿用同一份方法 + 注解
```

**好处**：契约（`common`）、本地实现（`core`）、HTTP 实现（`api-internal`）、远程代理（`client`）四者方法签名物理一致；任一处签名漂移会立即编译失败。

**例外**：批量端点中入参 / 出参含 Kotlin `Pair` 时，Jackson 序列化为对象、Feign 反序列化无法还原 `Pair`，因此本模块的 `SysDictInternalController` 额外提供 `List<List<String>>` 形态的 HTTP 适配端点（路径 `/api/internal/sys/dict/batchGetActiveDictItems` 等），不在 `ISysDictApi` 接口上。其他模块如有同样问题需照此模式补适配端点。

---

## 控制器一览

包结构：`io.kudos.ms.sys.api.internal.controller.<module>.Sys*InternalController`，模块名与 `common` / `core` 一致。

| 模块 | 控制器 | 端点路径前缀 |
|------|--------|-------------|
| `tenant` | `SysTenantInternalController`、`SysTenantSystemInternalController`、`SysTenantResourceInternalController`、`SysTenantLocaleInternalController` | `/api/internal/sys/tenant`、`.../tenantSystem`、`.../tenantResource`、`.../tenantLocale` |
| `system` | `SysSystemInternalController` | `/api/internal/sys/system` |
| `microservice` | `SysMicroServiceInternalController`、`SysSubSystemMicroServiceInternalController` | `/api/internal/sys/microService`、`.../subSystemMicroService` |
| `resource` | `SysResourceInternalController` | `/api/internal/sys/resource` |
| `dict` | `SysDictInternalController` | `/api/internal/sys/dict`（含 Pair 适配端点） |
| `param` | `SysParamInternalController` | `/api/internal/sys/param` |
| `i18n` | `SysI18nInternalController` | `/api/internal/sys/i18n` |
| `domain` | `SysDomainInternalController` | `/api/internal/sys/domain` |
| `datasource` | `SysDataSourceInternalController` | `/api/internal/sys/dataSource` |
| `accessrule` | `SysAccessRuleInternalController`、`SysAccessRuleIpInternalController` | `/api/internal/sys/accessRule`、`.../accessRuleIp` |
| `locale` | `SysLocaleInternalController` | `/api/internal/sys/locale` |
| `outline` | `SysOutLineInternalController` | `/api/internal/sys/outLine` |

> **缓存配置（`cache` 模块）无 internal 控制器是有意为之**：`common.cache.api.ISysCacheApi` 当前是空接口（没有任何方法），它只作为"未来对外暴露"的占位契约存在——`core.SysCacheApi` / `client.ISysCacheProxy` / `client.SysCacheFallback` 都同样是空壳，仅维持 `@FeignClient` + `fallback` 装配的合法性。`/api/admin/sys/cache` 的 CRUD 才是该模块面向运维 / 控制台的真实接口。后续若要让其他微服务读取缓存配置元数据，需要先在 `ISysCacheApi` 上声明方法+`@*Mapping`，再补 `SysCacheInternalController`。

---

## Gradle 依赖（要点）

在 **`kudos-ms-sys-core`** 与 **`kudos-ability-web-springmvc`** 之外，本模块额外依赖：

| 依赖 | 作用 |
|------|------|
| `kudos-ability-cache-interservice-provider` | 跨服务缓存的 Provider 侧能力（响应远端 invalidate 事件等） |
| `kudos-ability-distributed-discovery-nacos` | 服务注册与发现（Nacos） |
| `kudos-ability-distributed-config-nacos` | 配置中心（Nacos） |

因此 **可执行 fat jar** 若使用本模块作为入口，会同时获得：注册到 Nacos、拉取远程配置、参与服务间缓存协议、暴露 `/api/internal/sys/**` 端点 这四项能力。

---

## 依赖关系（概念）

```
kudos-ms-sys-api-internal
    ├── kudos-ms-sys-core
    │       ├── kudos-ms-sys-sql
    │       └── kudos-ms-sys-common
    ├── kudos-ability-web-springmvc
    ├── kudos-ability-cache-interservice-provider
    ├── kudos-ability-distributed-discovery-nacos
    └── kudos-ability-distributed-config-nacos
```

---

## 与 api-public / api-admin 的对比

| 维度 | api-public | api-admin | api-internal |
|------|------------|-----------|--------------|
| 主类命名 | `SysApiWebApplication` | `SysApiAdminApplication` | `SysApiProviderApplication` |
| 路径前缀 | （无控制器） | `/api/admin/sys/**` | `/api/internal/sys/**` |
| Controller 来源 | 无 | 显式定义 `*AdminController` | 实现 `common.ISys*Api` 接口，路径来自接口方法注解 |
| 调用方 | 一般作为 Web 入口与 `api-admin` 同进程组合 | 控制台 / 管理网关 | **`kudos-ms-sys-client` 的 Feign 代理**；服务网格内部其他微服务 |
| 额外分布式能力 | 无 | 无 | Nacos discovery / config + interservice cache provider |

---

## ⚠️ 安全考量

- **零 `@PreAuthorize`**：`/api/internal/sys/**` 端点本身不做鉴权，依赖部署拓扑保证只在 **集群内网 / Service Mesh** 内可达。若 Provider 端口暴露到公网，所有 sys 数据可被未授权枚举（参见 sys-core README 的"已知限制 / 安全考量"段）。
- **Pair 适配端点的 key 拼接** `${first}|${second}` 是单字节分隔；若入参字段本身含 `|`，会在客户端 / 服务端两侧解出错误的 key，需要时改用 URL-safe 编码。

---

## 扩展建议

- **新增内部 RPC**：先在 `common.<module>.api.ISys*Api` 上加方法 + `@*Mapping`，再在 `core` 实现，最后在本模块对应 `Sys*InternalController` 中覆盖一个 `override fun` —— 三处签名编译期对齐，无需手写路径。
- **新增模块**：在 `controller/<新模块>/` 下新建 `Sys*InternalController`，并保证 `core` 中已有对应 `Sys*Api` Bean 注入。`@ComponentScan` 已覆盖整个 `controller` 子树，不需要改装配。
- **Pair / 不可序列化类型**：尽量避免；如必须使用，按 `SysDictInternalController` 模式提供 HTTP 适配端点，并在 `client` 侧的 fallback / proxy 帮助类（`dict/support/FeignDictItemCodeFinder`）里封装调用。
- **本地开发不需要 Nacos**：可改用 `api-public + api-admin` 组合（参考各模块 README）；但客户端走 Feign 时仍需 internal 控制器，最低限度需要本模块。
