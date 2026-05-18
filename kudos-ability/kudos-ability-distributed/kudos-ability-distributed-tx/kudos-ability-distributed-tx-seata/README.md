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
- 入站方向：`SeataXidServletFilter` `Ordered.HIGHEST_PRECEDENCE`——必须在 `@Transactional`
  切面之前完成 bind

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

## 已知限制

- ❗ XA mode 在 testcontainer 环境下未端到端验证——生产 XA 用法需自行用 PGXADataSource 等
- ❗ `data-source-proxy-mode` 拼写错误时（如 `aT` 之外的字符）会抛 `IllegalArgumentException`
  —— 但错误信息只说"Unknown dataSourceProxyMode"，没列出合法值
- ❗ `SeataXidServletFilter` 只处理 servlet web；reactive web (WebFlux) 没有对应实现
- ❗ Feign XID 透传依赖 `kudos-ability-distributed-client-feign` 装好
  `GlobalHeaderRequestInterceptor`——没装就走不通；测试套件不专门测这套联调
- ❗ `RootContext.getXID()` 只读 ThreadLocal——跨线程业务（`@Async`、`CompletableFuture` 等）
  需要业务方自己显式 propagate

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
