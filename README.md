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

### 基础层 / 上下文层

| 路径 | 内容 |
|---|---|
| [`kudos-base/README.md`](kudos-base/README.md) | 基础库子系统索引、关键设计约定、已知陷阱、测试约定 |
| [`kudos-context/README.md`](kudos-context/README.md) | 运行时上下文层：上下文传播、锁框架、失败数据重试、Spring 集成、组件初始化 SPI |
| [`kudos-dependencies/README.md`](kudos-dependencies/README.md) | BOM 与版本对齐策略 |
| [`buildSrc/README.md`](buildSrc/README.md) | Gradle 约定插件 |

### 横切能力 `kudos-ability/*`

| 主题 | 顶层 README | 关键子模块 |
|---|---|---|
| 缓存 | [`cache`](kudos-ability/kudos-ability-cache/README.md) | [`-common`](kudos-ability/kudos-ability-cache/kudos-ability-cache-common/README.md) · [`-local-caffeine`](kudos-ability/kudos-ability-cache/kudos-ability-cache-local/kudos-ability-cache-local-caffeine/README.md) · [`-remote-redis`](kudos-ability/kudos-ability-cache/kudos-ability-cache-remote/kudos-ability-cache-remote-redis/README.md) · [`-interservice-client`](kudos-ability/kudos-ability-cache/kudos-ability-cache-interservice/kudos-ability-cache-interservice-client/README.md) |
| 通信 | [`comm`](kudos-ability/kudos-ability-comm/README.md) | [`-common`](kudos-ability/kudos-ability-comm/kudos-ability-comm-common/README.md) · [`-email`](kudos-ability/kudos-ability-comm/kudos-ability-comm-email/README.md) · [`-sms-aliyun`](kudos-ability/kudos-ability-comm/kudos-ability-comm-sms/kudos-ability-comm-sms-aliyun/README.md) · [`-sms-aws`](kudos-ability/kudos-ability-comm/kudos-ability-comm-sms/kudos-ability-comm-sms-aws/README.md) · [`-websocket-ktor`](kudos-ability/kudos-ability-comm/kudos-ability-comm-websocket/kudos-ability-comm-websocket-ktor/README.md) |
| 数据 | [`data`](kudos-ability/kudos-ability-data/README.md) | [`-rdb-ktorm`](kudos-ability/kudos-ability-data/kudos-ability-data-rdb/kudos-ability-data-rdb-ktorm/README.md) · [`-rdb-jdbc`](kudos-ability/kudos-ability-data/kudos-ability-data-rdb/kudos-ability-data-rdb-jdbc/README.md) · [`-rdb-flyway`](kudos-ability/kudos-ability-data/kudos-ability-data-rdb/kudos-ability-data-rdb-flyway/README.md) · [`-memdb-redis`](kudos-ability/kudos-ability-data/kudos-ability-data-memdb/kudos-ability-data-memdb-redis/README.md) |
| 分布式 | [`distributed`](kudos-ability/kudos-ability-distributed/README.md) | [`-client-feign`](kudos-ability/kudos-ability-distributed/kudos-ability-distributed-client/kudos-ability-distributed-client-feign/README.md) · [`-config-nacos`](kudos-ability/kudos-ability-distributed/kudos-ability-distributed-config/kudos-ability-distributed-config-nacos/README.md) · [`-discovery-nacos`](kudos-ability/kudos-ability-distributed/kudos-ability-distributed-discovery/kudos-ability-distributed-discovery-nacos/README.md) · [`-lock-redisson`](kudos-ability/kudos-ability-distributed/kudos-ability-distributed-lock/kudos-ability-distributed-lock-redisson/README.md) · [`-stream-common`](kudos-ability/kudos-ability-distributed/kudos-ability-distributed-stream/kudos-ability-distributed-stream-common/README.md) · [`-tx-seata`](kudos-ability/kudos-ability-distributed/kudos-ability-distributed-tx/kudos-ability-distributed-tx-seata/README.md) |
| 文件 | [`file`](kudos-ability/kudos-ability-file/README.md) | [`-common`](kudos-ability/kudos-ability-file/kudos-ability-file-common/README.md) · [`-local`](kudos-ability/kudos-ability-file/kudos-ability-file-local/README.md) · [`-minio`](kudos-ability/kudos-ability-file/kudos-ability-file-minio/README.md) |
| 日志 | [`log`](kudos-ability/kudos-ability-log/README.md) | [`-audit-common`](kudos-ability/kudos-ability-log/kudos-ability-log-audit/kudos-ability-log-audit-common/README.md) · [`-audit-mq`](kudos-ability/kudos-ability-log/kudos-ability-log-audit/kudos-ability-log-audit-mq/README.md) · [`-audit-rdb-ktorm`](kudos-ability/kudos-ability-log/kudos-ability-log-audit/kudos-ability-log-audit-rdb/kudos-ability-log-audit-rdb-ktorm/README.md) |
| UI | [`ui`](kudos-ability/kudos-ability-ui/README.md) | [`-javafx`](kudos-ability/kudos-ability-ui/kudos-ability-ui-javafx/README.md) |
| Web | [`web`](kudos-ability/kudos-ability-web/README.md) | [`-common`](kudos-ability/kudos-ability-web/kudos-ability-web-common/README.md) · [`-springmvc`](kudos-ability/kudos-ability-web/kudos-ability-web-springmvc/README.md) · [`-ktor`](kudos-ability/kudos-ability-web/kudos-ability-web-ktor/README.md) |

### 微服务 `kudos-ms/*`

| 服务 | 顶层 README | core / common |
|---|---|---|
| auth | [`auth`](kudos-ms/kudos-ms-auth/README.md) | [`-core`](kudos-ms/kudos-ms-auth/kudos-ms-auth-core/README.md) · [`-common`](kudos-ms/kudos-ms-auth/kudos-ms-auth-common/README.md) · [`-client`](kudos-ms/kudos-ms-auth/kudos-ms-auth-client/README.md) |
| msg | [`msg`](kudos-ms/kudos-ms-msg/README.md) | [`-core`](kudos-ms/kudos-ms-msg/kudos-ms-msg-core/README.md) · [`-common`](kudos-ms/kudos-ms-msg/kudos-ms-msg-common/README.md) · [`-client`](kudos-ms/kudos-ms-msg/kudos-ms-msg-client/README.md) |
| sys | [`sys`](kudos-ms/kudos-ms-sys/README.md) | [`-core`](kudos-ms/kudos-ms-sys/kudos-ms-sys-core/README.md) · [`-common`](kudos-ms/kudos-ms-sys/kudos-ms-sys-common/README.md) · [`-client`](kudos-ms/kudos-ms-sys/kudos-ms-sys-client/README.md) |
| user | [`user`](kudos-ms/kudos-ms-user/README.md) | [`-core`](kudos-ms/kudos-ms-user/kudos-ms-user-core/README.md) · [`-common`](kudos-ms/kudos-ms-user/kudos-ms-user-common/README.md) · [`-client`](kudos-ms/kudos-ms-user/kudos-ms-user-client/README.md) |

### 测试 / 工具

| 路径 | 内容 |
|---|---|
| [`kudos-test/README.md`](kudos-test/README.md) | 测试支持顶层索引 |
| [`kudos-test/kudos-test-common/README.md`](kudos-test/kudos-test-common/README.md) | 通用测试 fixture |
| [`kudos-test/kudos-test-container/README.md`](kudos-test/kudos-test-container/README.md) | TestContainers 封装（mysql / postgres / redis / minio / rocketmq / kafka 等） |
| [`kudos-test/kudos-test-rdb/README.md`](kudos-test/kudos-test-rdb/README.md) | RDB 集成测试基类 |
| [`kudos-test/kudos-test-api/kudos-test-api-contract/README.md`](kudos-test/kudos-test-api/kudos-test-api-contract/README.md) | Spring Cloud Contract 契约测试 |
| [`kudos-tools/README.md`](kudos-tools/README.md) | 代码生成 / SQL 工具 |

阅读源码时优先看对应模块的 README——核心设计约定与陷阱都集中在那里。
