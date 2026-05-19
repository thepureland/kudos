# kudos-ms-sys-client

## 定位

**其他微服务** 通过 **OpenFeign** 远程调用系统（`sys`）原子服务时的 **客户端模块**：仅依赖 **`kudos-ms-sys-common`**（及 Feign 能力模块），**不**依赖 `kudos-ms-sys-core`，避免将 ORM、缓存实现等打入调用方 classpath。

每个 **`ISys*Proxy`** 继承 `common` 中对应的 **`ISys*Api`**，保证 **本地注入 `ISys*Api` 与远程 Feign** 在方法签名上完全一致，便于测试替换与契约统一。

---

## 包结构

在 **`io.kudos.ms.sys.client.<业务模块>`** 下与 **common/core** 的模块名对齐；每个模块内再分 **`proxy`**（`ISys*Proxy`，Feign）与 **`fallback`**（`Sys*Fallback`）子包。少数模块（如 `dict`）额外提供 **`support`** 子包承载客户端侧的辅助逻辑。

典型形态：

```kotlin
@FeignClient(name = "sys-tenant", fallback = SysTenantFallback::class)
interface ISysTenantProxy : ISysTenantApi
```

`name` 为注册中心中的 **服务名**（或配置映射名），需与 **提供 sys 能力的进程** 在注册中心中的名称一致。

| Proxy | Feign `name` |
|-------|--------------|
| `ISysTenantProxy` | `sys-tenant` |
| `ISysTenantSystemProxy` | `sys-tenantsystem` |
| `ISysTenantResourceProxy` | `sys-tenantresource` |
| `ISysTenantLocaleProxy` | `sys-tenantlocale` |
| `ISysSystemProxy` | `sys-system` |
| `ISysMicroServiceProxy` | `sys-microservice` |
| `ISysSubSystemMicroServiceProxy` | `sys-subsystemmicroservice` |
| `ISysResourceProxy` | `sys-resource` |
| `ISysDictProxy` | `sys-dict` |
| `ISysDictItemProxy` | `sys-dictitem` |
| `ISysParamProxy` | `sys-param` |
| `ISysI18nProxy` | `sys-i18n` |
| `ISysDomainProxy` | `sys-domain` |
| `ISysDataSourceProxy` | `sys-datasource` |
| `ISysCacheProxy` | `sys-cache` |
| `ISysAccessRuleProxy` | `sys-accessrule` |
| `ISysAccessRuleIpProxy` | `sys-accessruleip` |
| `ISysLocaleProxy` | `sys-locale` |
| `ISysOutLineProxy` | `sys-out-line` |

> **服务名命名风格不统一**：多数模块以小写无连字符（`sys-tenantsystem`、`sys-accessruleip`），但 `outline` 用了 `sys-out-line`（含连字符）。这是历史遗留；若调整需同时改 Nacos / 网关路由映射。

**Fallback** 与对应 Proxy 分属同模块下的 **`fallback`** / **`proxy`** 包，所有 Fallback 继承自 **`io.kudos.ms.sys.client.support.SysClientFallbackSupport`**——本质是上游 `AbstractFeignFallbackSupport` 的本模块别名，保留它只是为了避免一次性修改 17+ 个已落地 Fallback 的父类；**新模块的 Fallback 可直接继承 `AbstractFeignFallbackSupport`**。

降级语义随业务而定（多为返回空集合 / null / 业务异常），目的是在 sys 不可用时避免级联故障，而不是静默吞错。

---

## Gradle 依赖

- **`kudos-ms-sys-common`**：契约与序列化类型。
- **`kudos-ability-distributed-client-feign`**：Feign 与 Spring Cloud 集成（含 `AbstractFeignFallbackSupport` 基类）。

测试依赖 `kudos-test-container`。

---

## 字典码校验：双实现 + ServiceLoader

`common.base` 的 `@DictItemCode` 校验通过 `ServiceLoader` 寻找 **`IDictItemCodeFinder`** 实现：

| 实现 | 模块 | 路径 |
|------|------|------|
| `DictItemCodeFinder` | `kudos-ms-sys-core` | 本进程查 Hash 缓存（部署 sys 的服务） |
| `FeignDictItemCodeFinder` | 本模块 `dict/support/` | 通过 `ISysDictProxy` 远程取（**不部署 sys-core 的下游服务**） |

本模块 `resources/META-INF/services/io.kudos.base.bean.validation.terminal.convert.converter.IDictItemCodeFinder` 注册了 `FeignDictItemCodeFinder`。两者在同一 deployment 通常只命中一个：sys 提供方依赖 `core`、下游服务依赖 `client`，**不会同时上线**。该实现通过 `SpringKit.getBean` 在首次校验时 lazy 取 `ISysDictProxy`，因此可以在不修改使用方代码的前提下，让没有 sys-core 的服务继续走字典码校验。

---

## 与其他子模块的关系

| 模块 | 关系 |
|------|------|
| **common** | Proxy 继承 `ISys*Api`，请求/响应体为同一套 VO |
| **core** | 在 **服务端** 实现 `ISys*Api`；客户端 **不引用** core |
| **api-*** | 暴露 HTTP 或由网关转发；Feign 通常指向 **聚合后的服务实例** |

---

## 使用注意

1. **启用 Feign 扫描**：调用方 Spring Boot 应用需能扫描到 `io.kudos.ms.sys.client`（含各模块下的 `proxy`；或通过 `@EnableFeignClients` 指定 basePackages）。
2. **服务发现**：`name` 需与 Nacos / Eureka 等中的实例一致；本地开发可用固定 URL 配置（视 Spring Cloud 版本而定）。
3. **契约变更**：修改 `common.ISys*Api` 时四处必须同步：
   - `core` 中 `Sys*Api`（同进程实现）
   - `api-internal` 中 `Sys*InternalController`（HTTP 暴露）
   - 本模块 `Sys*Proxy`（自动继承，不用手动改）
   - 本模块 `Sys*Fallback`（需手动补 override）

   前三处由编译期强制对齐，**fallback 是唯一可能漏改的地方**——新增方法时 IDE 不会报错，但运行期一旦走降级就会用到默认行为。建议每次给 `ISys*Api` 加方法时连带过一遍对应 fallback。
4. **`Pair` 入参的批量端点**：直接调 `ISysDictProxy.batchGetActiveDictItems(...)`（Pair 版）不可行——Jackson 序列化 `Pair` 后远端无法反序列化。应改调 `SysDictInternalController` 提供的 `batchGetActiveDictItemsHttp`（`List<List<String>>` 版），或在调用方做包装。当前 client 模块未为该适配端点提供 proxy 方法，使用方需要自行调用。

---

## 扩展建议

- 新增远程能力：先在 **common** 定义 `ISys*Api`，再在 **core** 实现，最后在本模块对应业务目录下增加 **`proxy/ISys*Proxy`** 与 **`fallback/Sys*Fallback`**，保持三者一一对应。
