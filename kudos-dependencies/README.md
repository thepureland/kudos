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
- kudos 自身各子模块的统一版本（从当前 Gradle `subprojects` 自动生成，排除 BOM 自身）
- （历史已注释）外部依赖的版本约束——目前已迁移到 `libs.versions.toml` catalog 集中管理

## 与 `libs.versions.toml` 的关系

- **`libs.versions.toml`** 是**项目内**模块用 catalog 引用版本的来源（`libs.spring.boot.starter.web` 等）
- **`kudos-dependencies`** 是**对外发布**的 BOM——让引用 kudos 的第三方项目能用 Maven /
  Gradle 标准方式管理 kudos 自身版本

两者职责不同；项目内开发主要看 catalog，对外发布看 BOM。

## 已知限制 / 后续工作

- ✅ BOM 约束已从手写的少量核心模块改为按 `settings.gradle.kts` 中的 Gradle 子项目自动生成，
  新增 kudos 子模块后不需要再手动补 `constraints`
- ❗ BOM 只约束 kudos 自身发布坐标；Spring / Ktor / Jackson 等外部依赖版本仍由项目内
  `libs.versions.toml` 和运行时 starter/BOM 控制，暂不对外透出
