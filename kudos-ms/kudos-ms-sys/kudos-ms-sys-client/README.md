# kudos-ms-sys-client

## 定位

**其他微服务** 通过 **OpenFeign** 远程调用系统（`sys`）原子服务时的 **客户端模块**：仅依赖 **`kudos-ms-sys-common`**（及 Feign 能力模块），**不**依赖 `kudos-ms-sys-core`，避免将 ORM、缓存实现等打入调用方 classpath。

每个 **`ISys*Proxy`** 继承 `common` 中对应的 **`ISys*Api`**，保证 **本地注入 `ISys*Api` 与远程 Feign** 在方法签名上完全一致，便于测试替换与契约统一。

---

## 包结构

### `io.kudos.ms.sys.client.proxy`

**Feign 接口**，命名 `ISys*Proxy`，典型形态：

```kotlin
@FeignClient(name = "sys-tenant", fallback = SysTenantFallback::class)
interface ISysTenantProxy : ISysTenantApi
```

`name` 为注册中心中的 **服务名**（或配置映射名），需与 **提供 sys 能力的进程** 在注册中心中的名称一致。

| Proxy | Feign `name` |
|-------|----------------|
| `ISysTenantProxy` | `sys-tenant` |
| `ISysSystemProxy` | `sys-system` |
| `ISysMicroServiceProxy` | `sys-microservice` |
| `ISysSubSystemMicroServiceProxy` | `sys-subsystemmicroservice` |
| `ISysResourceProxy` | `sys-resource` |
| `ISysDictProxy` / `ISysDictItemProxy` | `sys-dict` / `sys-dictitem` |
| `ISysParamProxy` | `sys-param` |
| `ISysI18nProxy` | `sys-i18n` |
| `ISysDomainProxy` | `sys-domain` |
| `ISysDataSourceProxy` | `sys-datasource` |
| `ISysCacheProxy` | `sys-cache` |
| `ISysAccessRuleProxy` / `ISysAccessRuleIpProxy` | `sys-accessrule` / `sys-accessruleip` |
| `ISysTenantSystemProxy` / `ISysTenantResourceProxy` / `ISysTenantLocaleProxy` | `sys-tenantsystem` / `sys-tenantresource` / `sys-tenantlocale` |

> 若实际部署将多个 API 合并为单一服务名，需在 **Feign 配置或网关** 中做路径/服务映射，上表为代码中的默认 `name`。

### `io.kudos.ms.sys.client.fallback`

与每个 Proxy 对应的 **`Sys*Fallback`**：在远程调用失败时执行降级逻辑（返回值、空列表或抛业务异常等，以各实现类为准），避免级联故障。

---

## Gradle 依赖

- **`kudos-ms-sys-common`**：契约与序列化类型。
- **`kudos-ability-distributed-client-feign`**：Feign 与 Spring Cloud 集成。

测试依赖 **`kudos-test-container`**（以 `build.gradle.kts` 为准）。

---

## 与其他子模块的关系

| 模块 | 关系 |
|------|------|
| **common** | Proxy 继承 `ISys*Api`，请求/响应体为同一套 VO |
| **core** | 在 **服务端** 实现 `ISys*Api`；客户端 **不引用** core |
| **api-*** | 暴露 HTTP 或由网关转发；Feign 通常指向 **聚合后的服务实例** |

---

## 使用注意

1. **启用 Feign 扫描**：调用方 Spring Boot 应用需能扫描到 `io.kudos.ms.sys.client`（或通过 `@EnableFeignClients` 指定 basePackages）。
2. **服务发现**：`name` 需与 Nacos/Eureka 等中的实例一致；本地开发可用固定 URL 配置（视 Spring Cloud 版本而定）。
3. **契约变更**：修改 `ISys*Api` 时须同步检查 **core 实现**、**fallback** 与 **服务端 Controller**（若有 HTTP 对齐），避免 Feign 与实现脱节。

---

## 扩展建议

- 新增远程能力：先在 **common** 定义 `ISys*Api`，再在 **core** 实现，最后在本模块增加 **`ISys*Proxy` + `Sys*Fallback`**，保持三者一一对应。
