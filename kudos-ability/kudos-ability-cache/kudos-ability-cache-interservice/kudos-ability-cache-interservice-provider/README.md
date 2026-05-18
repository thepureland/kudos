# kudos-ability-cache-interservice-provider

跨服务缓存协作的 **provider 端**（被调用方）模块。**当前为占位**——主源码尚未补齐，仅
`test-resources` 下有两个 yml 模板用于配套测试。

## 设计意图

provider 角色：在微服务架构里持有"权威数据 + 缓存"，被 client 端通过 Feign 调用。
本模块预留给未来 provider 端特有的能力：
- 接收 client 端的缓存失效请求并触发本地清理
- 暴露缓存状态查询接口（`/cache/keys` / `/cache/stats` 等）
- 通过 Feign / HTTP 主动通知 client 端缓存更新

`build.gradle.kts` 中 `compileOnly(libs.spring.boot.starter.web)` 表明：本模块未来会
注册 Web Controller，但当前没有任何代码。该依赖现在是 dead-weight，等真的开始写代码时
再调整为 `api` 或 `implementation`。

`kudos-tools` 的脚手架模板 `${project}-ams-${module}-api-provider/build.gradle.kts` 引用本模块作为
"ams 微服务 API-provider 子项目"的标准依赖——这是它存在的现实理由（生成出的项目工程
需要这个模块名）。

## 测试资源

| 文件 | 用途 |
|---|---|
| `test-resources/application-ms.yml` | provider 端应用配置（`server.port: 13578` + spring app name `inter-service-test`） |
| `test-resources/application-client.yml` | client 端配置（同 spring app name，复用 provider 的 testcontainer 启动一对 jvm） |

未来 provider 实测需要 client 应用同时启动调用（如端到端缓存失效广播），这两个 yml 文件
是配套环境。

## 模块入口

无主源码。

## 依赖

```kotlin
dependencies {
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-common"))
    api(libs.spring.boot.starter.web)
    compileOnly(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))

    testImplementation(project(":kudos-test:kudos-test-container"))
}
```

`spring.boot.starter.web` 当前未被任何代码使用，但脚手架模板里 provider 应用预期是 Web
应用，所以保留 `api` 透传给下游。

## 已知限制 / 后续工作

- ❗ 主源码空白。当前的"价值"仅在于：(a) 给脚手架模板提供稳定的依赖坐标，(b) yml 模板
  给跨服务测试占位
- ❗ `spring-boot-starter-web` 与 `distributed-client-feign` 两个依赖都没有代码引用——
  `web` 是 api 透传暂留，`feign` 是 compileOnly 也暂留；真的开始写代码后需要重新评估
- ❗ 一旦补 provider 实际能力（缓存失效接收端点等），应当：
  1. 把控制器 / endpoint 放在 `src/io/kudos/ability/cache/interservice/provider/`
  2. 复用 cache-common 的 `ICacheMessageHandler` SPI，避免重新发明
  3. 给 yml 模板加 README 说明启动方式
