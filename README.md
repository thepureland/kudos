# kudos

**定位**：Kotlin 微服务工程脚手架。按 **基础库 → 运行时上下文 → 横切能力 → 微服务 → 工具/测试** 分层组织，所有层面 Kotlin-first，对 Java 调用方友好。

---

## 模块概览

| 模块 | 定位 | 关键内容 |
|---|---|---|
| [`kudos-dependencies`](kudos-dependencies/) | **依赖管理（BOM）** | 集中声明三方库版本与对齐策略 |
| [`kudos-base`](kudos-base/README.md) | **最底层基础库**（无 Spring 依赖） | 数据契约、Bean 校验框架、查询构造、JSON 序列化、异常体系、语言层工具——所有上层模块的最小公共依赖 |
| [`kudos-context`](kudos-context/) | **运行时上下文** | Spring 集成、缓存配置、分布式锁、重试、ID 生成、validation 钩子等运行期共用设施 |
| [`kudos-ability`](kudos-ability/) | **横切能力** | 按主题拆分的可插拔能力模块：缓存、数据、分布式、文件、日志、UI、Web |
| [`kudos-ms`](kudos-ms/) | **微服务集合** | 平台级原子服务：[`auth`](kudos-ms/kudos-ms-auth/) / [`msg`](kudos-ms/kudos-ms-msg/) / [`sys`](kudos-ms/kudos-ms-sys/README.md) / [`user`](kudos-ms/kudos-ms-user/) |
| [`kudos-test`](kudos-test/) | **测试支持** | `kudos-test-api` / `-common` / `-container`（含 TestContainers 封装）/ `-rdb` |
| [`kudos-tools`](kudos-tools/) | **开发期工具** | 代码生成、SQL 工具 |

---

## 模块依赖层次

```
                  ┌──────────────────────┐
                  │  kudos-dependencies  │   ← 版本/依赖对齐 (BOM)
                  └──────────────────────┘
                              ↑
                  ┌──────────────────────┐
                  │      kudos-base      │   ← 无 Spring、纯 Kotlin/Java 基础设施
                  └──────────────────────┘
                              ↑
                  ┌──────────────────────┐
                  │     kudos-context    │   ← Spring 上下文 + 运行时设施
                  └──────────────────────┘
                              ↑
                  ┌──────────────────────┐
                  │     kudos-ability    │   ← 横切能力 (cache/data/web/...)
                  └──────────────────────┘
                              ↑
                  ┌──────────────────────┐
                  │       kudos-ms       │   ← 平台原子服务 (auth/msg/sys/user)
                  └──────────────────────┘

       kudos-test   ──→  作为 testImplementation 横向被多层引用
       kudos-tools  ──→  独立的开发期辅助工具，不参与运行时依赖
```

修改下层模块时**优先考虑对上层的传导影响**——`kudos-base` 改动可能波及全部上层。

---

## 技术栈

- **Kotlin** 2.x，target JVM 25
- **Gradle 9.x** + Version Catalog（`gradle/libs.versions.toml`）+ `buildSrc` 约定插件
- 运行期依赖：Spring Boot 4.x（仅在 `kudos-context` 及以上）、Jackson 3 (`tools.jackson`)、kotlinx.serialization、Hibernate Validator 9.x、Apache Commons (BeanUtils/Codec/Lang3/Text/Net) 等
- 测试：JUnit 5、kotlin.test、TestContainers（Postgres 等）
- 缓存配置、构建缓存：见 `gradle.properties`

---

## 构建与运行

使用 Gradle Wrapper（`./gradlew`），无需本地安装 Gradle。

| 命令 | 用途 |
|---|---|
| `./gradlew build` | 编译 + 测试 + 打包 |
| `./gradlew check` | 跑所有静态检查与测试 |
| `./gradlew :kudos-base:test` | 跑单模块测试 |
| `./gradlew clean` | 清理产物 |
| `./gradlew :kudos-base:dependencySizes` | 列出 runtime 依赖 jar 体积（自定义任务） |

模块级命令模板：`./gradlew :模块路径:任务`，如 `./gradlew :kudos-ms:kudos-ms-sys:kudos-ms-sys-core:test`。

---

## 文档索引

| 路径 | 内容 |
|---|---|
| [`kudos-base/README.md`](kudos-base/README.md) | 基础库子系统索引、关键设计约定、已知陷阱、测试约定 |
| [`kudos-context/README.md`](kudos-context/README.md) | 运行时上下文层：上下文传播、锁框架、失败数据重试、Spring 集成 |
| [`kudos-ms/kudos-ms-sys/README.md`](kudos-ms/kudos-ms-sys/README.md) | sys 原子服务的子模块边界与依赖关系 |
| 其它模块 README | 计划中（按需补充） |

阅读源码时优先看对应模块的 README——核心设计约定与陷阱都集中在那里。
