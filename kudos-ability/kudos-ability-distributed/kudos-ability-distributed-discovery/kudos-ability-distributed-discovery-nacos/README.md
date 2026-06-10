# kudos-ability-distributed-discovery-nacos

Nacos 服务发现接入。和 `kudos-ability-distributed-config-nacos` 一样，大部分能力来自
spring-cloud-alibaba 的 `alibaba.cloud.nacos.discovery` starter；本模块补充：

1. **Zone-aware 负载均衡**：`HintZoneServiceInstanceListSupplier` 让 client 通过请求头 hint
   选目标服务实例所在 zone
2. **Provider 端上下文反向适配**（`FeignContextWebFilter`）：把 Feign client 透传过来的
   `TENANT_ID` / `TRACE_KEY` / `DATASOURCE_ID` 等 header **写回 provider 进程的 `KudosContext`**——
   已在 `NacosDiscoveryAutoConfiguration` 中注册为 `FilterRegistrationBean`，默认开启
3. **Provider 端扩展 SPI**（`IFeignProviderContextProcess`）：业务侧可注册自定义 processor
   把额外 header 解析回 ThreadLocal

## 设计要点

### Client ↔ Provider 上下文契约

| 步骤 | 模块 | 类 |
|---|---|---|
| ① client 把 KudosContext 写到 Feign 请求头 | `kudos-ability-distributed-client-feign` | `GlobalHeaderRequestInterceptor` |
| ② client 走 Spring Cloud LoadBalancer 选实例（可按 hint zone） | 本模块 | `HintZoneServiceInstanceListSupplier` |
| ③ 网络传输 | — | — |
| ④ provider 端 web filter 把 header 写回 KudosContext | 本模块 | `FeignContextWebFilter` ✅ 已注册 |
| ⑤ provider 端 controller / service 用 `KudosContextHolder` 拿到上下文 | 业务代码 | — |

### Zone-aware 负载均衡（`HintZoneServiceInstanceListSupplier`）

启用方式：

```yaml
spring:
  cloud:
    loadbalancer:
      configurations: zone-preference
      zone: cn-east-1                # 缺省 zone
```

筛选规则：
- **请求 hint header 有值** → 只选 `metadata.zone == hint` 的实例；命中为空时降级返回全部
- **请求 hint header 为空 + 配了默认 zone** → 选 `metadata.zone == 默认zone` 或未设 zone 的实例
- **请求 hint header 为空 + 没配默认 zone** → 全量返回（等效不做 zone 过滤）

hint header 名由 spring-cloud-loadbalancer 标准属性
`spring.cloud.loadbalancer.{serviceId}.hint-header-name` 配置（默认 `X-SC-LB-Hint`）。

### `FeignContextWebFilter` 仅在带"显式透传标记"时启用

```kotlin
val isFeign = !request.getHeader(FEIGN_REQUEST).isNullOrBlank()
val isNotify = !request.getHeader(NOTIFY_REQUEST).isNullOrBlank()
if (!isFeign && !isNotify) {
    filterChain.doFilter(...)
    return    // 不修改上下文
}
```

普通浏览器 / curl 请求不带这两个 header，filter 不会修改其上下文——避免外部不可信请求
"伪造租户 ID"等头来污染服务端状态。**这要求 client 端必须显式打这两个标记 header 之一**——
`GlobalHeaderRequestInterceptor` 已经强制写 `FEIGN_REQUEST=true`。

开发期如需用 Postman / curl 手动透传上下文，可显式打开：

```yaml
kudos:
  ability:
    distributed:
      discovery:
        nacos:
          feign-context-filter:
            allow-unmarked-context-headers: true
```

默认仍是 `false`，生产不建议打开。

### 上下文头 HMAC 验签（`FeignContextSignatureVerifier`）

`FEIGN_REQUEST` 标记只能挡外部浏览器请求，挡不住内网被攻破节点伪造 `TENANT_ID`。配置共享
密钥后 filter 会对 client-feign 发来的签名头做完整校验：

```yaml
kudos:
  ability:
    distributed:
      discovery:
        nacos:
          feign-context-filter:
            context-signature-secret: ${KUDOS_CONTEXT_SIGNATURE_SECRET}   # 与 client 的 contextSignatureSecret 同值
            context-signature-timestamp-window-millis: 300000             # 可选，默认 ±5 分钟
            context-signature-nonce-cache-max-size: 100000                # 可选，nonce 缓存上限
```

- 验签内容：HMAC-SHA256（常数时间比较）+ 时间戳窗口 + nonce 防重放（进程内有界 TTL 缓存，
  TTL 与时间戳窗口一致）
- 缺签名头或校验失败 → 401，且**不写回上下文**；WARN 日志只记失败原因与请求坐标，不落
  签名值 / 上下文值
- 未配置密钥 → 保持旧行为（不校验），首个透传请求打一次 WARN 提醒
- 待办：nonce 缓存是进程内的，多实例部署需换 Redis（`SET NX PX`）做集群级防重放

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/NacosDiscoveryAutoConfiguration` | 装配入口，注册 `FeignContextWebFilter` |
| `init/DiscoveryLoadbalancerConfiguration` | Zone preference 负载均衡装配 |
| `loadbalancer/HintZoneServiceInstanceListSupplier` | 按 hint header 过滤实例 |
| `filter/FeignContextWebFilter` | Provider 端 header → KudosContext 反向写回（默认已装配） |
| `filter/FeignContextSignatureVerifier` | 上下文头 HMAC 验签（签名 + 时间戳窗口 + nonce 防重放） |
| `support/IFeignProviderContextProcess` | Provider 端上下文扩展 SPI |

## 配置示例

```yaml
spring:
  application:
    name: my-service
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        namespace: ${NACOS_NAMESPACE:public}
        metadata:
          zone: cn-east-1            # 实例所属 zone，供 HintZone supplier 匹配
    loadbalancer:
      configurations: zone-preference
      zone: cn-east-1                # 客户端的"默认目标 zone"
kudos:
  ability:
    distributed:
      discovery:
        nacos:
          zone-metadata-key: zone
          loadbalancer:
            service-instance-supplier-order: 183827465
```

## 测试覆盖

- `NacosServiceDiscoveryTest.test` —— 启 `MockMsApplication` 注册到 nacos，验证
  `discoveryClient.getInstances("discovery")` 非空
- `HintZoneServiceInstanceListSupplierTest` —— 纯函数覆盖 hint 命中 / 未命中 fallback /
  默认 zone / 自定义 metadata key
- `FeignContextWebFilterTest` —— 不依赖 Nacos，覆盖无透传标记时不污染上下文、Feign 标记
  请求的 header 回写、开发期允许未标记请求透传、provider 扩展 SPI 调用
- `FeignContextWebFilterSignatureTest` —— 覆盖 HMAC 验签：合法签名通过、篡改租户头被拒、
  过期时间戳被拒、nonce 重放被拒、配密钥但缺签名头被拒、未配密钥保持旧行为放行
- `NacosDiscoveryAutoConfigurationTest` —— 覆盖 `FeignContextWebFilter` 注册 bean 的 filter、
  order、name、url pattern

Nacos 端到端测试仍只验证 discovery 客户端可用；filter / zone 逻辑已拆成无容器单测。

## 已知限制 / 后续工作

- ✅ **`FeignContextWebFilter` 已通过 `NacosDiscoveryAutoConfiguration` 注册为
  `FilterRegistrationBean`**，order 为 `HIGHEST_PRECEDENCE+1`、urlPatterns `/*`、
  受 `@ConditionalOnClass(FilterRegistrationBean::class)` 保护以兼容非 servlet 应用；
  可用 `kudos.ability.distributed.discovery.nacos.feign-context-filter.enabled=false`
  关闭。Filter 本体在请求头无 `FEIGN_REQUEST` / `NOTIFY_REQUEST` 标记时不修改上下文，
  对普通浏览器请求安全
- ℹ️ `NacosDiscoveryAutoConfiguration` 除 kudos 自有 filter / properties 外不装配 Nacos
  discovery 客户端——核心客户端仍由 spring-cloud-alibaba starter 完成
- ✅ `HintZoneServiceInstanceListSupplier` 的 metadata 字段名可配置，构造参数
  `zoneMetadataKey` 由 `kudos.ability.distributed.discovery.nacos.zone-metadata-key` 注入，
  缺省 `"zone"`——业务侧 nacos 实例挂 `region` / `cluster-zone` 等键名时配置覆盖即可
- ✅ `DiscoveryLoadbalancerConfiguration` 的 `ServiceInstanceListSupplier` order 已支持配置：
  `kudos.ability.distributed.discovery.nacos.loadbalancer.service-instance-supplier-order`；
  不配置时 blocking / reactive 默认值仍分别保持与 NacosLoadBalancerClientConfiguration 一致
- ✅ `FeignContextWebFilter` 已支持开发期调试开关
  `kudos.ability.distributed.discovery.nacos.feign-context-filter.allow-unmarked-context-headers=true`；
  默认关闭，生产仍要求 `FEIGN_REQUEST` / `NOTIFY_REQUEST` 显式标记以避免外部请求伪造上下文
- ✅ 已补无容器单测覆盖 zone preference、`FeignContextWebFilter` 注册、上下文回写和 provider
  扩展 SPI 调用；Nacos 端到端测试仍只覆盖 discovery 客户端可用

## 依赖

```kotlin
api(project(":kudos-context"))
api(libs.alibaba.cloud.nacos.discovery)
api(libs.spring.cloud.loadbalancer)
compileOnly(libs.spring.boot.starter.web)

testImplementation(project(":kudos-test:kudos-test-container"))
testImplementation(libs.spring.boot.starter.web)
```

`spring.boot.starter.web` 是 `compileOnly`——本模块编译时需要 servlet API 来定义
`FeignContextWebFilter`，但运行时由消费方应用决定带哪个 web 实现（servlet / reactive）。

## 改进建议（自动分析 2026-06-11）

- ✅ 已修复（2026-06-11）**【安全】`FeignContextWebFilter` 未校验客户端的 HMAC 签名头**：
  新增 `filter/FeignContextSignatureVerifier`，配置
  `kudos.ability.distributed.discovery.nacos.feign-context-filter.context-signature-secret`
  （与 client-feign 的 `contextSignatureSecret` 同值）后，filter 对
  `X-Kudos-Context-Timestamp/Nonce/Signature` 做共享密钥 HMAC-SHA256 验签 + 时间戳窗口
  （`context-signature-timestamp-window-millis`，默认 ±5 分钟）+ nonce 防重放（进程内有界
  TTL 缓存，`context-signature-nonce-cache-max-size` 默认 10 万）校验；缺签名头或校验失败
  返回 401 并记录来源 WARN（不落签名值与上下文值），不写回上下文。签名比较用
  `MessageDigest.isEqual` 常数时间比较。未配置密钥保持旧行为，仅首个透传请求打一次 WARN。
  剩余待办：nonce 缓存为进程内实现，多实例部署可被"每实例重放一次"，需换 Redis
  （`SET NX PX`）做集群级防重放。
- **【缺陷风险】filter 不负责清理 `KudosContext`**：
  `src/io/kudos/ability/distributed/discovery/nacos/filter/FeignContextWebFilter.kt`
  写回上下文后没有 try/finally 清理，依赖 `kudos-ability-web-springmvc` 的
  `WebContextInitFilter` 在 finally 中 `KudosContextHolder.clear()` 兜底。若 provider 应用未引入
  该 web 模块，容器线程复用时上一请求的租户/数据源会泄漏给下一个请求。建议本 filter 自带
  finally 还原（保存进入前快照、退出时恢复），或至少在 KDoc/README 标注该强依赖。
- **【功能】locale 解析仅支持两段式 `lang_COUNTRY`**：
  `src/io/kudos/ability/distributed/discovery/nacos/filter/FeignContextWebFilter.kt`
  `local.split("_")` 要求至少两段，纯语言（`zh`）或三段（`zh_Hans_CN`）会被静默忽略。
  建议改用 `Locale.forLanguageTag` 或兼容 1~3 段解析。
- **【可维护性】LB order 默认值为未注明来源的魔法数**：
  `src/io/kudos/ability/distributed/discovery/nacos/init/DiscoveryLoadbalancerConfiguration.kt`
  `183827463` / `183827465` 与 spring-cloud-alibaba `NacosLoadBalancerClientConfiguration`
  内部值对齐，但代码中无注释说明来源与"必须比谁大/小"的约束，升级 spring-cloud 时易被破坏。
  建议补注释并在 supplier 装配处断言相对顺序。
- **【测试】reactive 分支无测试覆盖**：
  `src/io/kudos/ability/distributed/discovery/nacos/init/DiscoveryLoadbalancerConfiguration.kt`
  `ReactiveSupportConfiguration` 的装配条件与 order 包装目前零覆盖（blocking 分支同样只覆盖了
  纯函数 `filteredByHint`）。可用 `ApplicationContextRunner` 补条件装配单测。
