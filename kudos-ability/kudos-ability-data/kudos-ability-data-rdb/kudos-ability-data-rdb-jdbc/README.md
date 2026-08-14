# kudos-ability-data-rdb-jdbc

JDBC 层基础设施模块。负责三件事：

1. **动态数据源路由** —— 按"包路径 + 租户 + 服务编码 + 模式（master/readonly）"决定本次方法走哪个数据源
2. **数据库元数据访问** —— 从 JDBC `DatabaseMetaData` 反射出 Table / Column / index 等结构信息（代码生成器 / Ktorm 适配的基础设施）
3. **JDBC 通用工具** —— RDB 类型识别、连接构造、test query、ORDER BY 拼装等

底层用 baomidou `dynamic-datasource-spring-boot4-starter` 做路由能力，本模块在它之上加：
- 注解驱动的强制切换（`@DsChange` / `@TenantDsChange`）
- `_context::*` 模式的"上下文动态解析"路由（`DynamicDataSourceInterceptor` + 可配置 pointcut 的 advisor）
- DataSource 创建期的 Seata 兼容处理（autoCommit 修正）
- 几个扩展点（`IDataSourceFinder` / `IDataSourceProxy` / `IDynamicDataSourceLoad`）

## 设计要点

### 路由决策链

切面命中业务方法后按下列优先级查路由：

```
1. DbContext.forcedDs 非空且非 _context 前缀 → 直接切到 forcedDs
2. MultipleDataSourceProperties.lookDataSourceKey(包路径) 有结果
   a. 配置以 _context 开头 → 走"租户+服务+模式"动态解析（DsContextProcessor）
   b. 否则配置即数据源 key
3. 都不匹配 → 不切换，沿用调用栈上层已设的数据源
```

### 注解 vs 配置

| 切换方式 | 触发器 | 适用场景 |
|---|---|---|
| `@DsChange("master_ds")` | 方法注解 | 单方法临时切到固定数据源 |
| `@TenantDsChange("user_svc")` | 方法注解 | 按当前租户路由到"user_svc 在该租户下的数据源" |
| `MultipleDataSourceProperties.packageDataSource` | yml 配置 | 整个 service 包统一路由 |
| `DbContext.set(DbParam(forcedDs=...))` | 代码主动设 | 高级场景，跳过注解 / 配置 |

### 线程局部上下文管理

`DbContext` 用普通 `ThreadLocal` —— **子线程天然不继承父线程的路由意图**，防止线程池里的子任务串台。

**线程池场景必须在请求/任务结束时调 `DbContext.clear()`**，否则线程被复用时会带着旧 `DbParam` 跑下个任务。切面（`DsChangeAspect` / `TenantDsChangeAspect`）进入时会快照旧上下文，finally 恢复外层快照；进入时无线程上下文则调用 `DbContext.clear()`。手写代码场景调用方自负。

`DbContext.get()` 取出为 null 时**会自动塞一个空 `DbParam` 进去**（历史 API 语义，方便链式赋值）—— 框架内部已全部改用无副作用的 `DbContext.getOrNull()`，新代码请勿再用 `get()` 做只读侦测。

## 模块入口

| 路径 | 角色 |
|---|---|
| `aop/` | `@DsChange` / `@TenantDsChange` 注解 + 2 个注解 Aspect + 路由通知 `DynamicDataSourceInterceptor`（经可配置 pointcut 的 advisor 生效） |
| `context/` | `DbContext`（ThreadLocal 持有者）+ `DbParam`（路由参数，data class） |
| `consts/` | `DatasourceConst`（`master` / `readonly` / `CONSOLE_TENANT_ID` / `_context` 前缀等字面常量） |
| `datasource/` | 路由核心 + `RoutingCache`（解析缓存）+ 3 个 SPI 接口 + 默认实现 |
| `init/` | `JdbcAutoConfiguration` 装配入口（本模块所有 bean 均在此显式声明）+ `MultipleDataSourceProperties` 配置 |
| `kit/` | 4 个工具类（`RdbKit`、`RdbMetadataKit`、`DataSourceKit`、`DatasourceKeyTool`） |
| `metadata/` | DB 元数据 POJO（`Table`、`Column`、`RdbTypeEnum`、`TableTypeEnum`、`JdbcTypeToKotlinType`） |

## 三个扩展 SPI

| 接口 | 用途 | 默认行为 |
|---|---|---|
| `IDataSourceFinder` | 按 `(tenantId, serverCode, mode)` 解析 dsId | 容器无此 bean → 回退到上下文默认 dsId |
| `IDataSourceProxy` | 包装新建的 DataSource（典型应用：Seata 代理） | 无 bean → 不包装 |
| `IDynamicDataSourceLoad` | 按 dsId 拉 `DataSourceProperty` | `DefaultDynamicDataSourceLoad` 全 null（仅打 warn） |

生产应用应注册自己的 `IDynamicDataSourceLoad` 实现（典型：从配置中心 / 元数据表读 (host, port, user, pass, ...)）。

## 配置示例

```yaml
spring:
  datasource:
    dynamic:
      primary: master
      seata: false       # 用 Seata 时打开，本模块会按需修正 autoCommit
      datasource:
        master:
          url: jdbc:postgresql://primary/app
          username: app
          password: ${DB_PASSWORD}
        readonly:
          url: jdbc:postgresql://replica/app
          username: app_ro
          password: ${DB_RO_PASSWORD}

kudos:
  ability:
    jdbc:
      routingPointcut: "within(*..biz..*)"    # 路由通知的 AspectJ pointcut,业务代码不在 biz 包时改这里
      dataSourceCreateWaitSeconds: 30         # 等待他人建同一个数据源连接池的上限,超时快速失败
      packageDataSource:
        com.example.audit.biz: master_audit   # 该包下所有 service 走 master_audit
        com.example.tenant.biz: _context      # 该包下走"按当前租户上下文解析"
```

包前缀匹配按**包段边界 + 最长前缀优先**：`com.example.audit` 匹配 `com.example.audit(.x)` 但不匹配
`com.example.auditor`；`com.example` 与 `com.example.audit` 同时配置时后者赢。

## JDBC url 安全校验

数据源 url 并不总是运维手写的 —— 它也可能来自 sys 控制台(`sys_datasource` 行、连通性测试)。而 JDBC url
**本身就是可执行载荷**:某些驱动参数会让"客户端"(也就是应用服务器)去做危险的事:

| 参数 | 驱动 | 后果 |
|---|---|---|
| `allowLoadLocalInfile` 等 | MySQL/MariaDB | 恶意服务器索要文件,驱动交出 → **读取应用服务器任意文件** |
| `autoDeserialize`、`queryInterceptors`、`propertiesTransform` | MySQL | 反序列化 / 任意类加载 → **RCE** |
| `socketFactory`、`sslfactory`(及 `*Arg`) | PostgreSQL | 以攻击者指定参数实例化任意类 → **RCE** |
| `loggerFile` | PostgreSQL | 任意路径写文件 |
| `INIT`、`RUNSCRIPT` | H2 | 连接时执行任意 SQL → **RCE** |

`JdbcUrlValidator` 在三个入口拦截这些参数:`DataSourceKit.createDataSource`、`RdbKit.newConnection`、
以及**所有动态数据源的必经之路** `DsDataSourceCreator.createDataSource`(覆盖 `IDynamicDataSourceLoad`
从元数据表读来的 url)。

采用**黑名单而非白名单**:连接参数是开放集合且各厂商不同,白名单会误伤 `characterEncoding`、
`currentSchema` 等正常调优参数。只拦"在服务端连接串里没有正当用途"的参数;仅放大既有风险、
不新增能力的参数(如 `allowMultiQueries`)**故意不拦**,以免打断正在运行的部署。

> 注意:这不等于 url 就安全了 —— 它仍然指向作者选定的主机。限制**可达主机范围**属于部署层的
> 网络策略,不由本模块负责。

## Seata 兼容关键

`DsDataSourceCreator.createDataSource()` 检测到 `IDataSourceProxy.isSeata()` 时，**会强制把 `autoCommit` 设 true** 覆盖任何 yml 配置。这不是 bug：

> Seata AT 模式靠每条 SQL 自动 commit 时 ConnectionProxy 的拦截链来写 undo log / register branch；
> `autoCommit=false` 时拦截链根本不触发，写最后会被还池时回滚。

详细背景见 `kudos-ability-distributed-tx-seata` 模块。请不要随意"修复"此处。

## 已知限制 / 后续工作

- ✅ 路由 pointcut 已可配置（`kudos.ability.jdbc.routingPointcut`，默认 `within(*..biz..*)`）
- ✅ `DsChangeAspect` / `TenantDsChangeAspect` 已支持嵌套保留：内层调用结束后恢复外层 `DbParam`
- ✅ 解析缓存已抽为 `RoutingCache` 组件（lock-free），读写锁/`@Synchronized` 历史残留已移除
- ✅ `DsContextProcessor` 单数据源场景全方法优雅退化（不再抛 ClassCastException）
- ✅ `_context` 解析缓存键的租户维度使用 `_datasourceTenantId`（无租户时回退 `dataSourceId`），
  不再出现"两个租户共享 dataSourceId 时串缓存"的问题
- ✅ JDBC url 危险参数已拦截(`JdbcUrlValidator`,见下节)
- ❗ `JdbcTypeToKotlinType.getKotlinType` 仍是按 RDB 分支的大段 `when`，可进一步数据驱动化
- ❗ readonly 意图只在 `_context` 路由下生效，命中普通 key/未配置包时会打一次 warn 后忽略 ——
  静态数据源的 readonly 约定（如 `<ds>_readonly` 回退）待设计

## 依赖

```kotlin
api(project(":kudos-context"))
api(libs.spring.boot.starter.jdbc)
api(libs.baomidou.dynamic.datasource.starter)
```

`baomidou.dynamic.datasource.starter` 是硬依赖；本模块的多数据源能力建立在它的 `DynamicRoutingDataSource` 之上。
