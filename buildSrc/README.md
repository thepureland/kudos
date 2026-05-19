# buildSrc

Gradle 约定插件（convention plugins）。Gradle 识别本目录为"构建逻辑共享区"——这里写的
插件会自动对其它子模块可用，业务模块通过 `plugins { id("kotlin-jvm") }` 应用。

## 内容

| 文件 | 角色 |
|---|---|
| `src/main/kotlin/kotlin-jvm.gradle.kts` | 项目共享的 Kotlin JVM 编译约定：JVM toolchain = 25、JUnit Platform 测试 + 测试日志（成功/失败/跳过均输出） |

## 用途

业务模块（包括 `kudos-base` / `kudos-context` / 所有 `kudos-ability-*` / 所有 `kudos-ms-*`）的
`build.gradle.kts` 顶部都会有：

```kotlin
plugins {
    id("buildsrc.convention.kotlin-jvm")
}
```

或类似形式。这样所有模块都自动用同一个 JDK / Kotlin / 测试日志策略，避免每个模块写一遍。

## 已知限制 / 后续

- ❗ 目前只有一个 convention plugin (`kotlin-jvm`)；其他横切配置（如统一的 `spotless`
  / `detekt` / 发布 metadata）尚未抽到这里
- ❗ JVM 版本 `jvmToolchain(25)` 硬编码——升降 JDK 版本需要改这里 + `libs.versions.toml` 同步

## 依赖

```kotlin
implementation(libs.kotlinGradlePlugin)
```

仅 Kotlin Gradle 插件——让 convention plugin 内部能用 `kotlin("jvm")` 这种 DSL。
