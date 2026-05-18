# kudos-ability-distributed-discovery-nacos

Nacos 服务发现接入。和 `kudos-ability-distributed-config-nacos` 一样，大部分能力来自
spring-cloud-alibaba 的 `alibaba.cloud.nacos.discovery` starter；本模块补充：

1. **Zone-aware 负载均衡**：`HintZoneServiceInstanceListSupplier` 让 client 通过请求头 hint
   选目标服务实例所在 zone
2. **Provider 端上下文反向适配**（`FeignContextWebFilter`）：把 Feign client 透传过来的
   `TENANT_ID` / `TRACE_KEY` / `DATASOURCE_ID` 等 header **写回 provider 进程的 `KudosContext`**——
   **此 filter 目前未注册**（见已知限制）
3. **Provider 端扩展 SPI**（`IFeignProviderContextProcess`）：业务侧可注册自定义 processor
   把额外 header 解析回 ThreadLocal

## 设计要点

### Client ↔ Provider 上下文契约

| 步骤 | 模块 | 类 |
|---|---|---|
| ① client 把 KudosContext 写到 Feign 请求头 | `kudos-ability-distributed-client-feign` | `GlobalHeaderRequestInterceptor` |
| ② client 走 Spring Cloud LoadBalancer 选实例（可按 hint zone） | 本模块 | `HintZoneServiceInstanceListSupplier` |
| ③ 网络传输 | — | — |
| ④ provider 端 web filter 把 header 写回 KudosContext | 本模块 | `FeignContextWebFilter` ⚠ |
| ⑤ provider 端 controller / service 用 `KudosContextHolder` 拿到上下文 | 业务代码 | — |

第④步是关键——**没注册 filter 就等于整条上下文契约只走了一半**：client 端辛辛苦苦写的
header 到 provider 这边就被丢弃。

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

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/NacosDiscoveryAutoConfiguration` | 装配入口（**当前不注册任何 bean**——见已知限制） |
| `init/DiscoveryLoadbalancerConfiguration` | Zone preference 负载均衡装配 |
| `loadbalancer/HintZoneServiceInstanceListSupplier` | 按 hint header 过滤实例 |
| `filter/FeignContextWebFilter` | Provider 端 header → KudosContext 反向写回（**未注册**） |
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
```

## 测试覆盖

- `NacosServiceDiscoveryTest.test` —— 启 `MockMsApplication` 注册到 nacos，验证
  `discoveryClient.getInstances("discovery")` 非空

**该测试只验证 nacos discovery 客户端可用，不验证 `FeignContextWebFilter` 是否注册 / 工作**——
这是为什么 filter 长期 dormant 没人发现的原因。

## 已知限制 / 后续工作

- ❗ **`FeignContextWebFilter` 未在 `NacosDiscoveryAutoConfiguration` 中注册**——历史上有
  完整的 `@Bean FilterRegistrationBean<FeignContextWebFilter>` 注册块被注释停用，git log
  没有记录原因。**这意味着 `kudos-ability-distributed-client-feign` 在 client 端写入的
  `TENANT_ID` / `SUB_SYS_CODE` / `TRACE_KEY` / `DATASOURCE_ID` 等 header 在 provider 端被
  全部丢弃**。Spring Boot 4 升级后这条契约是否还工作请优先验证；推断的恢复方案见
  `NacosDiscoveryAutoConfiguration` 的 kdoc。**需要用户决策后再恢复**——直接恢复会改动所
  有下游 web 应用的运行时行为
- ❗ `NacosDiscoveryAutoConfiguration` 不注册任何 bean——Nacos discovery 客户端由
  spring-cloud-alibaba starter 装配；本类近乎"占位类"
- ❗ `HintZoneServiceInstanceListSupplier.filteredByHint` 把 metadata 字段名 `"zone"` 硬编码——
  与 spring-cloud-loadbalancer 的 `ZONE` 约定一致但不可配
- ❗ `DiscoveryLoadbalancerConfiguration.REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER` 是 magic
  number（保持与 NacosLoadBalancerClientConfiguration 一致）；升级 Spring Cloud Alibaba 时
  需要核对
- ❗ `FeignContextWebFilter` 的"无显式标记不修改上下文"是安全护栏，但**也意味着非 Feign 调用方
  (REST 客户端、Postman、curl) 无法手动指定租户 / trace 等**——开发期调试可能不便。需要时
  业务方可加 dev profile 下的兜底 filter
- ❗ 测试只验证 nacos 可达，没覆盖 zone preference / FeignContextWebFilter / 上下文回写——
  补测时需要 client + provider 两端都跑

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
