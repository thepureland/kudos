# kudos-ability-cache-interservice-client

跨服务缓存协作的 **client 端**（调用方）模块。**当前为占位**——无任何源码或测试资源。

## 设计意图

client 角色：通过 Feign 调用 provider 微服务的接口，本地缓存远端响应。需要 provider
端数据失效时同步剔除本地副本。本模块预留给未来 client 端特有的能力：
- Feign 调用的二级缓存装饰器（结合 `@Cacheable` 与 provider 通知）
- 接收 provider 端推送的失效消息（HTTP webhook / pub-sub channel）
- 失败回退策略（远端不可达时本地缓存 stale-while-revalidate）

build.gradle.kts 里 `compileOnly(libs.spring.boot.starter.web)` 表明本模块未来会注册
HTTP 端点（接收 provider 失效推送），但当前无代码。该依赖现在是 dead-weight，等真的
开始写代码时再调整为 `api` / `implementation`。

provider 模块的 `testImplementation` 引用本模块用于"client 端调用 provider 端"的端到端
测试，所以本模块即便没主源码，至少要存在并保持 publishable 坐标。

## 模块入口

无源码。

## 依赖

```kotlin
dependencies {
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-common"))
    compileOnly(libs.spring.boot.starter.web)

    testImplementation(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-client"))
    testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
    testImplementation(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-local:kudos-ability-cache-local-caffeine"))
    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation(libs.spring.boot.starter.web)
}
```

注意：`testImplementation(project(":...cache-interservice-client"))` 实际上是**自引用**
——build.gradle.kts 把当前模块再当 testImplementation 引了一次，是配置遗漏（无害但
是冗余）。

## 已知限制 / 后续工作

- ❗ 主源码与 test-src 都空白；模块仅在依赖图里占位
- ❗ build.gradle.kts 有自引用（`testImplementation(project(":...cache-interservice-client"))`
  指向当前模块本身）——Gradle 容忍这种写法但是 no-op；下次有人动这文件时一并清理
- ❗ `compileOnly(libs.spring.boot.starter.web)` 没人用，等开始写代码再决定保留 / 提升 / 删除
- ❗ provider 端的 yml 测试模板里 `application-client.yml` 是给本模块用的，但本模块本身
  没有测试代码——配置文件目前无处安放，未来补 client 测试时该 yml 应当迁过来
- ❗ 三件套（common / provider / client）目前都没真正"开工"。一旦决定要落地这个能力，
  应该一次性把三个模块的目录结构搭起来，避免每个模块各做一半
