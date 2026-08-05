# kudos-ability-distributed-tx-seata

Apache Seata 2.x 分布式事务接入。提供两套相互独立但常一起部署的能力：

1. **数据源代理装配** —— `SeataDataSourceProxy` + `SeataAutoConfiguration` 把
   baomidou dynamic-datasource 的 DataSource 包成 Seata `DataSourceProxy`（AT）或
   `DataSourceProxyXA`（XA），让 ConnectionProxy 能在每条 SQL commit 时写 undo log
   / 注册 BranchRegister
2. **Feign 跨服务 XID 透传** —— `SeataFeignXidProcessor` (出站) + `SeataXidServletFilter`
   (入站) 解决 Apache Seata 2.x **不带** Spring Cloud OpenFeign 集成的 gap

## 设计要点

### AT vs XA 由 `seata.data-source-proxy-mode` 决定

```yaml
seata:
  enable-auto-data-source-proxy: true
  data-source-proxy-mode: AT        # AT | XA
```

`SeataDataSourceProxy` 按 mode 选 `DataSourceProxy` 或 `DataSourceProxyXA`。
**AT mode 硬约束**：`HikariCP.is-auto-commit=true`——`kudos-ability-data-rdb-jdbc.DsDataSourceCreator`
检测到 `IDataSourceProxy.isSeata()` 时自动覆盖此值。详细原因见 rdb-jdbc README。

### Feign XID 跨服务传播

Apache Seata 2.x 没自带 Spring Cloud OpenFeign 集成：
- **客户端**：发 Feign 请求不会自动把 `RootContext.getXID()` 加到请求头
- **服务端**：收请求不会自动 bind 回 `RootContext`
- **结果**：跨服务 `@GlobalTransactional` 拿不到分支，回滚什么都回滚不了

本模块的解决方案：

```
Client                                                    Provider
─────────                                                ─────────
@GlobalTransactional                                     @Transactional
  ↓                                                        ↑
  RootContext.bind(xid)                                    RootContext.bind(xid)
  ↓                                                        ↑ (by SeataXidServletFilter)
  Feign call → header TX_XID=<xid>  ──────────────────►   HTTP request
              ↑                                            
   SeataFeignXidProcessor                                   
   (IFeignRequestContextProcess)                            
```

- 出站方向：`SeataFeignXidProcessor` 实现 `IFeignRequestContextProcess`，被
  `kudos-ability-distributed-client-feign` 的 `GlobalHeaderRequestInterceptor` 调用
- 入站方向：`SeataXidServletFilter` 通过 `FilterRegistrationBean` 使用
  `Ordered.HIGHEST_PRECEDENCE`——必须在 `@Transactional` 切面之前完成 bind

### 条件激活

```kotlin
@ConditionalOnClass(RequestInterceptor::class, OncePerRequestFilter::class)
```

`SeataFeignXidAutoConfiguration` 仅在 Feign + servlet 容器都在 classpath 时才装配——纯
Seata（无 Feign / 无 servlet）部署不受影响。

## 配置示例

```yaml
seata:
  enable-auto-data-source-proxy: true
  data-source-proxy-mode: AT
  application-id: ${spring.application.name}
  tx-service-group: kudos-tx-group
  service:
    vgroup-mapping:
      kudos-tx-group: default
  registry:
    type: nacos
    nacos:
      server-addr: localhost:8848
      application: seata-server
  config:
    type: nacos
    nacos:
      server-addr: localhost:8848
```

`kudos.ability.distributed.tx-seata.yml` 包提供默认值，业务侧仅需覆盖 server-addr / group 等
环境相关参数。

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/SeataAutoConfiguration` | 装配 `dataSourceProxy` bean，覆盖 jdbc 模块默认 |
| `init/SeataDataSourceProxy` | DataSource → `DataSourceProxy`(AT) / `DataSourceProxyXA`(XA) |
| `feign/SeataFeignXidProcessor` | 出站：往 Feign 请求头写 `TX_XID` |
| `feign/SeataXidServletFilter` | 入站：读 `TX_XID` bind 回 `RootContext` |
| `feign/SeataFeignXidAutoConfiguration` | Feign 透传链路装配（条件激活） |

## 测试覆盖

测试套件覆盖 AT mode 端到端（5/5 通过）；XA 模式 testcontainer 受限，相关测试 `@Disabled`
保留 documented reason —— XA proxy 需要 XADataSource 实例（HikariCP 默认包装的是普通
PgDataSource）。详见测试代码注释。

无容器单测覆盖：
- `SeataDataSourceProxyTest` —— mode 拼写校验和错误信息
- `SeataFeignXidProcessorTest` —— 出站 XID header 写入 / 无 XID 不写
- `SeataXidServletFilterTest` —— 入站 bind / unbind、不覆盖当前线程已有 XID
- `SeataFeignXidAutoConfigurationTest` —— filter registration order 为 `HIGHEST_PRECEDENCE`

## 已知限制

- ℹ️ XA mode 在 testcontainer 环境下未端到端验证——生产 XA 用法需自行用 PGXADataSource 等
  XADataSource 实例验证
- ✅ `data-source-proxy-mode` 拼写错误时会抛 `IllegalArgumentException`，错误信息已列出
  合法值 `AT | XA`，并补单测锁住提示内容
- ℹ️ `SeataXidServletFilter` 只处理 servlet web；reactive web (WebFlux) 没有对应实现。
  本模块通过 `@ConditionalOnClass(RequestInterceptor, OncePerRequestFilter)` 限定了装配边界
- ✅ Feign XID 出站 / 入站处理器已补无容器单测；完整链路仍依赖
  `kudos-ability-distributed-client-feign` 装好 `GlobalHeaderRequestInterceptor`，这是模块间装配契约
- ℹ️ `RootContext.getXID()` 只读 ThreadLocal——跨线程业务（`@Async`、`CompletableFuture` 等）
  仍需要业务方自己显式 propagate，避免把 Seata ThreadLocal 隐式扩散到线程池

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc"))
api(libs.alibaba.cloud.seata)
api(libs.apache.seata)

compileOnly(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
compileOnly(libs.spring.boot.starter.web)

testImplementation(project(":kudos-test:kudos-test-container"))
testImplementation(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))
testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-discovery:kudos-ability-distributed-discovery-nacos"))
testImplementation(libs.spring.boot.starter.web)
```

Feign + web 是 `compileOnly`——本模块编译时需要 `IFeignRequestContextProcess` / `OncePerRequestFilter`
类型，但只在条件装配时使用，业务侧需要自己引入完整 web / feign 依赖。

## 改进建议（自动分析 2026-06-11）

- 【功能缺陷】`feign/SeataXidServletFilter.kt`：`OncePerRequestFilter` 默认
  `shouldNotFilterAsyncDispatch() = true`，Servlet 异步派发（`DeferredResult` / `Callable`）的
  工作线程不会经过本 filter，异步链路中 `RootContext` 拿不到 XID，分支注册会静默失效。
  建议在 README 配置示例中明确该限制，或针对异步派发补充 bind 逻辑。
- 【文档】AT 模式要求业务库存在 `undo_log` 表，但 README 与模块资源都没有提供该表的 DDL
  或指引（测试里的 `test-resources/sql/postgres/schema.sql` 有，业务侧看不到）；建议在 README
  补充 undo_log 建表说明或链接 Seata 官方脚本。
- 【安全性】配置示例中 nacos registry/config 未提及鉴权参数（`username` / `password` /
  `access-key`）；启用了鉴权的 nacos 环境若缺省会启动失败或匿名接入，建议示例中以环境变量
  占位补充。
- 【可观测性】XID 透传链路（出站写 header / 入站 bind）没有任何 debug 日志，跨服务回滚失效
  时排障只能抓包；建议在 `SeataFeignXidProcessor` / `SeataXidServletFilter` 各加一行 debug 日志。
- 【可扩展性】`feign/SeataFeignXidProcessor.kt` 仅覆盖 Feign 出站；RestTemplate / WebClient
  调用方没有对应的 XID 注入实现，跨服务事务只对 Feign 生效——建议在 README 标注边界或预留
  对应扩展点。
