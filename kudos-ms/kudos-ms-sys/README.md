# kudos-ms-sys

**定位**：**系统（`sys`）原子服务**的 Gradle 聚合模块，承载平台级能力：租户与子系统关系、微服务注册、资源与菜单、字典与参数、国际化、数据源、域与缓存配置、访问规则等。通过 **契约（common）→ 数据库脚本（sql）→ 领域实现（core）→ HTTP/Feign 暴露（api-* / client）** 分层组织代码。

**在工程中的角色**：作为 `kudos-ms` 下的 **系统（`sys`）原子服务**（`SysConsts.ATOMIC_SERVICE_NAME = "sys"`），供控制台、网关及其他微服务通过管理端 HTTP 或 Feign 使用。

---

## 构建与运行

工程根使用 Gradle Wrapper（`./gradlew`），无需本地装 Gradle。以下命令均在仓库根执行。

### 单模块构建 / 测试

```bash
# 仅编译某个子模块（不跑测试）
./gradlew :kudos-ms:kudos-ms-sys:kudos-ms-sys-core:assemble

# 跑某个子模块的测试
./gradlew :kudos-ms:kudos-ms-sys:kudos-ms-sys-core:test
./gradlew :kudos-ms:kudos-ms-sys:kudos-ms-sys-api-admin:test

# 跑整个 sys 聚合的测试（依赖会被一并构建）
./gradlew :kudos-ms:kudos-ms-sys:check
```

> 单测使用 H2（默认 file 模式 `~/h2/sys`，或 H2 TCP server），`Flyway` 在每次启动时跑全套迁移；**不需要** 本地 Postgres / Redis 即可跑通 `*-core` 与 `*-api-admin` 的测试。

### 启动可执行进程（开发态）

`api-admin` / `api-public` / `api-internal` 各自带 Spring Boot `main`，通常组合启动：

```bash
# 启动 admin（含 /api/admin/sys/** REST），最常用
./gradlew :kudos-ms:kudos-ms-sys:kudos-ms-sys-api-admin:bootRun

# 启动 internal（含 /api/internal/sys/** RPC + 需要 Nacos）
./gradlew :kudos-ms:kudos-ms-sys:kudos-ms-sys-api-internal:bootRun

# 仅 Web 装配（基本不直接用）
./gradlew :kudos-ms:kudos-ms-sys:kudos-ms-sys-api-public:bootRun
```

`api-admin` 默认 yml 在 `kudos-ms-sys-api-admin/resources/application.yml`（H2 file + `kudos.ability.cache.enabled=true`）。本地无 Redis 时改为 `false`，否则启动会 `RedisConnectionFailureException`。

### 端口与健康检查

- 端口与 actuator 端点由上层可执行模块（如 `*-api-web` 聚合 jar）的 yml 决定；本仓库内 sys 各模块的 yml 没有显式 `server.port`，默认走 Spring Boot `8080`。
- 健康检查：`GET /actuator/health`（前提是上层装配了 actuator；`kudos-ability-web-springmvc` 是否默认引入以该模块文档为准）。

---

## 子模块文档索引

| 子模块 | 说明文档 |
|--------|----------|
| [kudos-ms-sys-common](kudos-ms-sys-common/README.md) | 跨模块共享契约：API 接口、VO、枚举、校验与常量 |
| [kudos-ms-sys-sql](kudos-ms-sys-sql/README.md) | Flyway 迁移脚本（sys 库表与视图） |
| [kudos-ms-sys-core](kudos-ms-sys-core/README.md) | DAO、Service、缓存、API 实现与 Spring 扫描入口 |
| [kudos-ms-sys-api-admin](kudos-ms-sys-api-admin/README.md) | 管理端 REST 控制器（`/api/admin/sys/**`），控制台 / 管理网关使用 |
| [kudos-ms-sys-api-public](kudos-ms-sys-api-public/README.md) | 对外 Web 进程启动入口与自动配置（`sys-api-web`，无业务控制器） |
| [kudos-ms-sys-api-internal](kudos-ms-sys-api-internal/README.md) | **对内 RPC 控制器（`/api/internal/sys/**`）+ Provider 进程入口**，承接 Feign 客户端调用，附带 Nacos discovery/config 装配 |
| [kudos-ms-sys-client](kudos-ms-sys-client/README.md) | Feign 代理与降级，供其他服务远程调用 sys；含 `FeignDictItemCodeFinder`（不部署 sys-core 时的字典码校验回退） |

---

## 依赖关系与路径暴露（概念）

```
                          ┌─────────────────┐
                          │ kudos-ms-sys-   │   ← 契约层：ISys*Api（含 /api/internal/sys/** 方法注解）
                          │ common          │     VO、enums、SysConsts / SysDictTypes
                          └────────┬────────┘
                                   │
                ┌──────────────────┼──────────────────────────────┐
                ▼                  ▼                              ▼
       ┌─────────────────┐  ┌─────────────────┐         ┌─────────────────┐
       │ kudos-ms-sys-   │  │ kudos-ms-sys-   │         │ kudos-ms-sys-   │
       │ sql             │  │ core            │         │ client          │
       │ (Flyway scripts)│  │ (DAO/Service/   │         │ (Feign proxies, │
       └────────┬────────┘  │  Cache/Event/   │         │  fallbacks,     │
                │           │  @Component Api)│         │  FeignDictItem- │
                └──────────►│                 │         │  CodeFinder)    │
                            └────────┬────────┘         └─────────────────┘
                                     │                          ▲
                ┌────────────────────┼────────────────────┐     │ HTTP /api/internal/sys/**
                ▼                    ▼                    ▼     │
       ┌─────────────────┐  ┌────────────────┐  ┌────────────────┐
       │ kudos-ms-sys-   │  │ kudos-ms-sys-  │  │ kudos-ms-sys-  │
       │ api-public      │  │ api-admin      │  │ api-internal   │
       │ (Web 装配,      │  │ /api/admin/    │  │ /api/internal/ │
       │  无业务路径)    │  │  sys/** REST   │  │  sys/** RPC    │
       │                 │  │ (15 controllers)│ │ (18 controllers,│
       │ Spring Boot     │  │                │  │  + Nacos /     │
       │ main:           │  │ main:          │  │  interservice  │
       │ SysApiWebApp    │  │ SysApiAdminApp │  │  cache provider)│
       └─────────────────┘  └────────────────┘  │ main:          │
                                                │ SysApiProviderApp│
                                                └────────────────┘
```

- **client 仅依赖 common**（及 Feign 能力），避免把 `core` 打进调用方。Feign 代理通过 `/api/internal/sys/**` HTTP 路径调用 **api-internal** 的控制器；后者实现 `common.ISys*Api`，三处共享同一份方法签名。
- **api-admin / api-public / api-internal** 均依赖 **core**，差异在 **挂载的控制器集** 与 **额外能力栈**：
  - `api-admin`：管理端 15 个 `*AdminController`（继承 `BaseCrudController`）
  - `api-public`：仅 Web 装配，无业务控制器
  - `api-internal`：12 个模块共 18 个 `*InternalController`（实现 `ISys*Api`，路径来自接口方法注解）+ Nacos discovery/config + interservice cache provider
- **sql → core**：core 的 `api` 依赖 sql 模块只为把 Flyway 资源打进 classpath，迁移在运行时由 `kudos-ability-data-rdb-flyway` 触发。

### 路径双轨制

| 路径前缀 | 模块 | 鉴权预期 | 调用方 |
|---------|------|---------|--------|
| `/api/admin/sys/**` | api-admin | 网关 / 外部过滤器（**模块内零 `@PreAuthorize`**） | 控制台 / 管理网关 |
| `/api/internal/sys/**` | api-internal | 内网可达性约束（同样无 `@PreAuthorize`） | `kudos-ms-sys-client` 的 Feign 代理 |

> `api-internal` 端点在路径注解上仅由 `common.ISys*Api` 接口定义；`api-admin` 路径由控制器自身的 `@RequestMapping` 定义。两条路径**不允许互相借用**，否则 admin / internal 双轨制崩溃。

---

## 命名与约定（跨模块）

- **默认子系统编码**：`SysConsts.DEFAULT_SUB_SYSTEM_CODE`（`default-sub-system`），与资源、菜单、访问规则等按子系统维度隔离的数据一致。
- **领域 API 三身一体**：`common` 中 `ISys*Api` 在方法上标注 `@*Mapping("/api/internal/sys/...")`，由 **三处** 同时使用：
  - `core` 的 `Sys*Api`（`@Component`）—— 同进程注入实现
  - `api-internal` 的 `Sys*InternalController`（`@RestController`）—— 暴露 HTTP
  - `client` 的 `ISys*Proxy`（`@FeignClient`）—— 远程调用

  三者的方法签名、参数顺序、URL 路径在编译期通过同一接口绑定；fallback 是唯一不参与编译对齐的处，新增方法时需手动补 override。
- **路径前缀双轨制**：管理路径 `/api/admin/sys/**`（仅 `api-admin`） vs 内部 RPC 路径 `/api/internal/sys/**`（仅 `api-internal`，且来自 `common` 接口的方法注解）。这两条路径有不同的鉴权与可达性预期，新增端点时不要混淆。
- **业务模块清单**（与 `common` / `core` 一级目录对齐）：`accessrule`、`cache`、`datasource`、`dict`、`domain`、`i18n`、`locale`、`microservice`、`outline`、`param`、`resource`、`system`、`tenant`，共 13 个。
- **包结构对齐原则**：`common` / `core` / `client` / `api-admin` / `api-internal` 在 `<module>/` 一级目录上完全一致，便于跨模块对照阅读。`api-public` 不含业务子目录，只有 `init`。
