# kudos-ability-cache-interservice-common

跨服务缓存协作的"共享基"模块。**当前为占位**——无独立代码，仅作为
`kudos-ability-cache-interservice-provider` 与 `kudos-ability-cache-interservice-client`
共同依赖的入口，间接传递 `kudos-ability-cache-common`。

## 设计意图

cache-interservice 三件套（common / provider / client）解决的问题：
- **provider 应用** 内部缓存了一份数据，被 **client 应用** 通过 Feign 调过来读
- provider 端写入更新缓存后，client 端怎么得到失效通知

预留给未来的共享抽象：
- 跨服务缓存协议的请求 / 响应 DTO
- provider 与 client 共用的常量（HTTP header 名、broadcast channel 命名约定）
- 公共 SPI 接口（`IInterServiceCacheBroadcaster` 之类）

目前这些抽象散落在 `cache-common` / `cache-remote-redis` 里，待出现"两端共用"需求时再
上移到本模块。

## 模块入口

无源码。仅 `build.gradle.kts` 透传依赖。

## 依赖

```kotlin
dependencies {
    api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-common"))
}
```

## 已知限制 / 后续工作

- ❗ 空壳模块；可考虑：(a) 等到真有共享代码再保留，(b) 暂时删除并让 provider / client
  直接依赖 cache-common。保留是为给后续提取留位
- ❗ 模块名 `interservice` 在英文文档中已用，但中文术语未统一——
  "服务间缓存" / "跨服务缓存" / "跨应用缓存" 三种叫法并存，未来收敛到一种为好
