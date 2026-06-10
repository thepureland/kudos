# kudos-test-container

Testcontainers 的统一封装——给业务测试提供"按需启动 + 跨测试复用 + Docker 不在自己装"
的容器实例。

## 内容

### 注解
- `annotations/EnabledIfDockerInstalled` —— JUnit 5 `ExecutionCondition`，本机没装
  Docker 时跳过该测试类/方法，避免 CI 没 Docker 时整批红
- `annotations/DockerInstalledCondition` —— `EnabledIfDockerInstalled` 的实现，
  跑一次 `docker --version`，结果用静态字段+双检锁缓存（同 JVM 只检一次）

### 工具（`kit/`）
- `TestContainerKit` —— 进程级容器注册表：按 LABEL 在 Docker 里找已有容器、找不到才启
  新的、并注册 JVM shutdown hook 等批量测试跑完再清理。还提供 `execInContainer` 工具
- `XGenericContainer` —— `GenericContainer.bindingPort()` 扩展，固定宿主机端口（业务侧
  默认不用，testcontainers 动态分配端口更稳）
- `DockerKit` —— **不只是探测——会自动启动 Docker**。macOS `open -a Docker`、Windows
  `sc start com.docker.service` / 直接拉起 `Docker Desktop.exe`、Linux `systemctl start
  docker`。等待最长 90s 直到 `docker info` 返回 0

### 容器封装（一容器一文件，`containers/`）

| 容器 | 镜像（hardcoded） | 用途 |
|---|---|---|
| `PostgresTestContainer` | `postgres:18.0-alpine3.22` | RDB |
| `MySqlTestContainer` | —— | RDB |
| `H2TestContainer` | —— | RDB（默认选项） |
| `RedisTestContainer` | `redis:8.6.0-alpine` | KV 缓存 / 分布式锁 |
| `MinioTestContainer` | —— | 对象存储 |
| `SmtpTestContainer` | —— | 邮件 |
| `RabbitMqTestContainer` / `KafkaTestContainer` / `RocketMqTestContainer` | —— | MQ |
| `NacosTestContainer` | —— | 注册 / 配置中心 |
| `SeataTestContainer` | —— | 分布式事务 |
| `WireMockTestContainer` | —— | HTTP Mock |

每个 `*TestContainer` 都是 Kotlin `object`（单例），提供：
- `startIfNeeded(registry)`：幂等启动 + 把 host/port/credentials 注册到
  `DynamicPropertyRegistry`
- `getRunningContainer()`：拿 Docker Java API 的 `Container` 句柄（容器未起返回 null）
- `main(args)`：可单独跑——拉起容器后 `Thread.sleep(MAX_VALUE)` 挂住，给本地调试/手工
  跑测试时复用。**`main` 入口会先 `ManualTestContainerMainSupport.removeExistingContainers`
  把同 label 的旧容器（含 exited）force+volumes 删干净再启**，给手动入口提供一致的起点；
  测试本身走 `startIfNeeded` 路径仍维持复用语义，互不冲突

## 容器复用机制

```
[ 测试 A ]──startIfNeeded──┐
[ 测试 B ]──startIfNeeded──┼─→ TestContainerKit.startContainerIfNeeded(LABEL, container)
[ 测试 C ]──startIfNeeded──┘            │
                                         ▼
                              listContainersCmd().withLabelFilter({LABEL_KEY: LABEL})
                                         │
                            ┌────────────┴────────────┐
                            │                         │
                       hit: 返回                  miss: container.start()
                       已有容器                    + addShutdownHook
```

- **同 JVM 复用**：`TestContainerKit` 自身的 in-memory 状态
- **跨 JVM 复用**：靠 Docker label 过滤——前一个 JVM 没注册 shutdown hook（或被
  `kill -9`）残留的容器，下一个 JVM 跑测试时会被 label 命中、直接复用而不是再起
- **多 JVM 并行共用同一容器**（默认启用，可用 `-Dkudos.testcontainer.shared-lifecycle.enabled=false` 关）：
  - 每个使用容器的 JVM 在 `${java.io.tmpdir}/kudos-testcontainer-leases/<label>/` 下登记一份 pid 租约文件
  - JVM 关闭钩子先释放本进程的租约，再以同一 label 的 file-lock 互斥下判断"还有没有活着的 JVM 在用"
  - 仍有活租约时跳过 stop，只有最后一个存活租约离开时才真正 stop+remove 容器
  - 启动者会用反射开启 Testcontainers 的 reuse（仅 in-memory，不写 `~/.testcontainers.properties`），
    避开 Ryuk 在首启 JVM 退出时把容器当 session 资源回收
- **多 JVM 并行启动同一容器**：每个 `startIfNeeded` 走 `TestContainerCrossProcessLock`（文件锁，
  路径 `${java.io.tmpdir}/kudos-testcontainer-<id>.lock`），把"检测—启动"临界区跨 JVM 串行，
  避免两个 JVM 都看到"未启动"然后同时 `docker run` 起两份。可用 `-Dkudos.testcontainer.<id>.lock.disable=true`
  退回到旧行为
- **批量测试结束才清理**：`Runtime.addShutdownHook` 仅在 JVM 正常退出时跑——故意不用
  testcontainers 的 `@Testcontainers` 注解（那个会每测试类启停容器）

## Postgres：动态端口而非固定 25432

历史曾用 `bindingPort(25432 to 5432)` 固定宿主机端口——结果一旦本机有别的 postgres 占着
该端口（IDE 里之前的 JVM 残留 / 另一个项目跑过测试），整批测试 `port is already allocated`
启动失败。

现在 `withExposedPorts(5432)` 让 testcontainers 动态分配，`PORT` getter 在容器启动后
通过 `getRunningContainer().ports.firstOrNull()?.publicPort` 拿真正的宿主端口；启动前
调用直接 `error()`。

另：`PostgresTestContainer.startIfNeeded(registry, database)` 还会**连管理库 `postgres`
建目标库**——重试最多 20 次、间隔 500 ms，等 daemon 真起来。可选 `SYS_PROP_HOST_DATA_DIR`
/ `ENV_HOST_DATA_DIR` 把 `/var/lib/postgresql/data` bind 到宿主机做持久化。

## 使用模式

```kotlin
@EnableKudosTest
@EnabledIfDockerInstalled
class MyDbTest {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            PostgresTestContainer.startIfNeeded(registry)
        }
    }
}
```

`startIfNeeded(registry)`：
- 同 JVM 多次调用复用容器
- 跨 JVM 通过 Docker label 复用残留容器
- 自动把 `host` / `port` / `username` / `password` / 其它 spring property 注册到
  `DynamicPropertyRegistry`

## 已知限制

- ❗ 镜像版本 hardcoded 在各 `*TestContainer` 文件——升级镜像要改代码、没法走配置。
  这是有意的：测试要可重现，不能让 `latest` 的漂移让今天通过的测试明天挂掉。
- ✅ `NacosTestContainer` 已改为探测 Nacos readiness API，并把 startup timeout 放宽到
  90 秒，降低 Nacos 镜像启动慢导致的 CI 抖动；首次拉镜像仍建议本地 / CI 预拉。
- ❗ `DockerKit.ensureDockerRunning()` 会**主动启动** Docker Desktop——在用户没期待 IDE
  弹应用的环境下（无人值守 CI 跑本地用例）可能有副作用。
- ❗ `EnabledIfDockerInstalled` 只查 `docker --version` 退码，不能区分"已安装但 daemon
  挂了"——后者会进 `DockerKit` 然后等 90s 才 timeout。
- ❗ JVM 被 `kill -9`（IDE 强停测试）时不会跑 shutdown hook，容器残留——下次靠 label
  复用兜底，但偶尔状态污染要手工 `docker rm -f`。
- ❗ `XGenericContainer.bindingPort` 仍在文件里，但 Postgres 已弃用、其它容器若再用容易
  踩"端口被占"的老坑。仅在需要从容器外固定连接（如 IDE 调试看 DB）时再考虑。

## 改进建议（自动分析 2026-06-11）

> 本次直接修复（不在下列待办内）：`RocketMqTestContainer.getRunningContainer()` 误用静态导入的
> `H2TestContainer.LABEL`、始终返回 H2 容器的 bug；`DockerInstalledCondition` 双检锁缺 `@Volatile`
> 且 `waitFor()` 无超时可能挂死测试发现阶段；`RabbitMqTestContainer` 容器定义里误把 `withLabel`
> 链到 `bindingPort` 返回值上的写法；`PostgresTestContainer` 未使用的 import；`KafkaTestContainer`
> 不必要的 `var`；`DockerKit.startDockerOnMac` 两个完全相同的 if/else 分支。

### 安全性
- `kit/XGenericContainer.kt`：`bindingPort` 通过 `Ports.Binding.bindPort` 绑定 **0.0.0.0**。
  使用固定端口的容器（H2 1521、MySQL 23306 root/mysql、Nacos 28848/38848 且 auth 关闭、
  Seata 28091、RocketMQ 9876/10909/10911/10912）在测试期间对整个局域网可达。建议提供
  `bindingPort(hostIp, ...)` 重载并默认绑 127.0.0.1；RocketMQ/Seata 因用宿主机 LAN IP 注册
  （`IpKit.getLocalIp()`）需逐个评估后再收紧。
- `containers/NacosTestContainer.kt`：测试实例以 `NACOS_AUTH_ENABLE=false` 运行且端口固定对外，
  与上一条叠加意味着办公网/共享 CI 上任何人可写该 Nacos。

### 功能缺陷
- `containers/NacosTestContainer.kt` `registerProperties`：硬编码 `localhost:38848`（Seata 专用
  实例端口）。常规 `startIfNeeded(registry)` 启动的实例绑定的是 28848，注册出的
  `spring.cloud.nacos.*.server-addr` 指向错误端口。两个调用方应各自传入正确端口。
- `containers/RocketMqTestContainer.kt`：只有 name-server 的 `getRunningContainer()`，broker
  容器没有对应查询方法；`startIfNeeded` 返回 `Pair` 而非具名类型，调用方易混淆先后。
- `kit/DockerKit.kt` `runCommand`：`catch (e: Exception)` 会吞掉 `InterruptedException` 且不
  恢复中断标志，建议单独捕获并 `Thread.currentThread().interrupt()`。

### 可扩展性
- 镜像版本 hardcode 是有意设计（可重现性），但目前只有 H2 支持 `-Dkudos.test.h2.image` 覆盖。
  建议为所有容器提供统一的 `kudos.test.<id>.image` 系统属性/环境变量覆盖机制（如 arm64 主机
  替换不兼容镜像时需要）。
- 14 个 `*TestContainer` 的 `startIfNeeded` / `registerProperties` / `getRunningContainer` /
  `main` 模板高度重复（约 60 行 × 14 份）。本次修复的 RocketMQ label 复制粘贴 bug 即源于此。
  建议抽象 `AbstractTestContainer`（持有 label / id / container 定义 / 属性注册回调）统一模板。

### 可观测性
- `kit/TestContainerKit.kt`、`kit/ManualTestContainerMainSupport.kt` 及各容器用 `println` /
  `System.err.println` 输出生命周期日志，建议统一换 slf4j（`SeataTestContainer` 已示范
  `Slf4jLogConsumer` 用法），便于 CI 日志按级别过滤。

### 测试覆盖
- 本模块自身没有任何测试（无 test-src）。`support/CrossProcessLock`、`TestContainerKit` 的
  lease 管理（`pruneAndCountActiveLeases` / `readLeasePid` / `safeLabel`）是不依赖 Docker 的
  纯逻辑，完全可单元测试；`DockerKit.runCommand` 的超时/异常分支同理。

### 可维护性 / API
- `containers/RocketMqTestContainer.kt`：`LABEL_NANE_SERVER` 拼写错误（NANE→NAME），但属
  public const，建议新增正确命名常量 + `@Deprecated` 旧名过渡。
- `containers/NacosTestContainer.kt`：`tokenBytes` 公开暴露了可变 `ByteArray`，外部可改写其
  内容造成与 `tokenBase64` 不一致；建议降为 private（属 public API 变更，需评审）。
- `containers/MinioTestContainer.kt`：凭据 `"admin"/"12345678"` 为内联魔法值，其余容器均提取
  为 `const val USERNAME/PASSWORD`，建议对齐（也便于调用方引用）。
- `containers/SmtpTestContainer.kt` / `WireMockTestContainer.kt`：`registerProperties` 为空实现，
  SMTP 建议至少注册 `spring.mail.host/port`，否则调用方还要自己拿端口拼配置。
