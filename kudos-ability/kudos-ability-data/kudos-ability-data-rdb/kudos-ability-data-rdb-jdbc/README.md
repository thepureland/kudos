# kudos-ability-data-rdb-jdbc

JDBC 层基础设施模块。负责三件事：

1. **动态数据源路由** —— 按"包路径 + 租户 + 服务编码 + 模式（master/readonly）"决定本次方法走哪个数据源
2. **数据库元数据访问** —— 从 JDBC `DatabaseMetaData` 反射出 Table / Column / index 等结构信息（代码生成器 / Ktorm 适配的基础设施）
3. **JDBC 通用工具** —— RDB 类型识别、连接构造、test query、ORDER BY 拼装等

底层用 baomidou `dynamic-datasource-spring-boot4-starter` 做路由能力，本模块在它之上加：
- 注解驱动的强制切换（`@DsChange` / `@TenantDsChange`）
- `_context::*` 模式的"上下文动态解析"路由
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

`DbContext` 用 `InheritableThreadLocal`，但 `childValue` 返回 null —— **子线程不继承父线程的路由意图**，防止线程池里的子任务串台。

**线程池场景必须在请求/任务结束时调 `DbContext.clear()`**，否则线程被复用时会带着旧 `DbParam` 跑下个任务。切面（`DsChangeAspect` / `TenantDsChangeAspect`）进入时会快照旧上下文，finally 恢复外层快照；进入时无线程上下文则调用 `DbContext.clear()`。手写代码场景调用方自负。

`DbContext.get()` 取出为 null 时**会自动塞一个空 `DbParam` 进去**（历史 API 语义，方便链式赋值）—— 只读侦测请用新增的 `DbContext.getOrNull()`。

## 模块入口

| 路径 | 角色 |
|---|---|
| `aop/` | `@DsChange` / `@TenantDsChange` 注解 + 对应的 3 个 Aspect（`DsChangeAspect`、`TenantDsChangeAspect`、`DynamicDataSourceAspect`） |
| `context/` | `DbContext`（ThreadLocal 持有者）+ `DbParam`（路由参数） |
| `consts/` | `DatasourceConst`（`master` / `readonly` / `CONSOLE_TENANT_ID` 等字面常量） |
| `datasource/` | 路由核心 + 3 个 SPI 接口 + 默认实现 |
| `init/` | `JdbcAutoConfiguration` 装配入口 + `MultipleDataSourceProperties` 配置 |
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
      packageDataSource:
        com.example.audit.biz: master_audit   # 该包下所有 service 走 master_audit
        com.example.tenant.biz: _context      # 该包下走"按当前租户上下文解析"
```

## Seata 兼容关键

`DsDataSourceCreator.createDataSource()` 检测到 `IDataSourceProxy.isSeata()` 时，**会强制把 `autoCommit` 设 true** 覆盖任何 yml 配置。这不是 bug：

> Seata AT 模式靠每条 SQL 自动 commit 时 ConnectionProxy 的拦截链来写 undo log / register branch；
> `autoCommit=false` 时拦截链根本不触发，写最后会被还池时回滚。

详细背景见 `kudos-ability-distributed-tx-seata` 模块。请不要随意"修复"此处。

## 已知限制 / 后续工作

- ❗ `DynamicDataSourceAspect` 的 pointcut 写死 `within(*..biz..*)`，不可配置 —— 项目结构强约束
- ✅ `DsChangeAspect` / `TenantDsChangeAspect` 已支持嵌套保留：内层调用结束后恢复外层 `DbParam`
- ❗ `MultipleDataSourceProperties.lookDataSourceKey` 同时用 `ConcurrentHashMap.computeIfAbsent` + readLock，后者实际保护不了什么 —— 仅与 [forceChangeDataSource] 的 writeLock 形成"对仗"
- ❗ `DataSourceKit.createDataSource` 不做 url 注入校验 —— 调用方应自行白名单
- ❗ `DsContextProcessor` 紧耦合 baomidou `DynamicRoutingDataSource`；单数据源场景部分方法（`refreshDatasource`）会直接抛 ClassCastException
- ❗ `JdbcTypeToKotlinType.getKotlinType` 200 行嵌套 `when`，每个 RDB 类型一段，可拆分成独立函数提升可读性
- ❗ 测试覆盖：已有 `RdbKit` / `RdbMetadataKit` 单测和数据源注解切面的嵌套恢复测试；
  `DynamicDataSourceAspect`、DataSource 路由、`DatasourceKeyTool`、`JdbcTypeToKotlinType` 仍缺测试

## 依赖

```kotlin
api(project(":kudos-context"))
api(libs.spring.boot.starter.jdbc)
api(libs.baomidou.dynamic.datasource.starter)
```

`baomidou.dynamic.datasource.starter` 是硬依赖；本模块的多数据源能力建立在它的 `DynamicRoutingDataSource` 之上。
