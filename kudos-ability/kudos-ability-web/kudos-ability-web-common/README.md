# kudos-ability-web-common

Web 子模块共享 base 层。**当前为占位模块**——尚无独立代码，仅作为 `kudos-ability-web-springmvc`
和 `kudos-ability-web-ktor` 等具体 web 实现模块的公共依赖入口（间接传递 `kudos-context`）。

## 设计意图

预留给未来 web 框架无关的共享抽象，例如：
- Web 层常量（HTTP header / cookie 名）
- 请求 / 响应通用 DTO
- 跨 servlet / Ktor / Reactor 的工具方法
- 公共安全约束（如 `MutableListSearchPayload` 拒收类的接口契约）

目前这些抽象仍各自散落在具体实现模块里（`web-springmvc` / `web-ktor`），需要复用时再
按"出现两次以上"的标准上移到本模块。

## 模块入口

无源码。仅 `build.gradle.kts` 透传依赖。

## 已知限制 / 后续工作

- ❗ 空壳模块；当前选择保留是为给后续提取留位。若长期没有跨 SpringMVC / Ktor 的共享抽象，
  可删除本模块并让各实现模块直接依赖 `:kudos-context`
- ✅ `kudos-ability-web-springmvc` 和 `kudos-ability-web-ktor` 已统一依赖本模块，避免同一 web
  主题下公共入口不一致

## 依赖

```kotlin
dependencies {
    api(project(":kudos-context"))

    testImplementation(project(":kudos-test:kudos-test-common"))
}
```
