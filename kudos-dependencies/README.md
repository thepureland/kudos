# kudos-dependencies

Kudos 项目的**统一 BOM（Bill of Materials）**。`java-platform` Gradle 插件——
**不发布 jar**，只发布一份依赖坐标 + 版本约束清单。

## 用途

业务侧引入：

```kotlin
dependencies {
    implementation(platform("io.kudos:kudos-dependencies:<version>"))
    implementation("io.kudos:kudos-base")        // 版本由 BOM 决定
    implementation("io.kudos:kudos-context")
}
```

## 内容

`build.gradle.kts` 中 `constraints` 块声明：
- kudos 自身各模块的统一版本（kudos-base / kudos-context / kudos-test-* 等）
- （历史已注释）外部依赖的版本约束——目前已迁移到 `libs.versions.toml` catalog 集中管理

## 与 `libs.versions.toml` 的关系

- **`libs.versions.toml`** 是**项目内**模块用 catalog 引用版本的来源（`libs.spring.boot.starter.web` 等）
- **`kudos-dependencies`** 是**对外发布**的 BOM——让引用 kudos 的第三方项目能用 Maven /
  Gradle 标准方式管理 kudos 自身版本

两者职责不同；项目内开发主要看 catalog，对外发布看 BOM。
