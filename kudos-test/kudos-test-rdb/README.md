# kudos-test-rdb

RDB 测试基类——给业务测试一套"启 RDB 容器 + 装数据 + 重置缓存 + 串行执行"的模板。
继承层次按"上来就要装多少东西"组织：纯 SQL → +RDB 容器 → +Redis & 应用缓存。

## 继承层次

```
SqlTestBase                       仅 SQL 文件加载 + 事务回滚（容器自带）
  ↑
RdbTestBase                       + H2 TestContainer（cache 禁用）
RdbAndRedisCacheTestBase          + H2 + Redis 双容器（cache 启用，单机本地策略）
                                    每次 setUp 后 flushdb + 应用缓存 reload
```

> `RdbAndRedisCacheTestBase` 并不继承 `RdbTestBase`，两者都直接继承 `SqlTestBase`——
> 因为前者要在 `@DynamicPropertySource` 里同时启 H2+Redis 两个容器并设 cache=true，
> 后者只启 H2 并设 cache=false，属性互斥不可叠加。

| 类 | 容器 | `kudos.ability.cache.enabled` | `cache.config.strategy` | 用途 |
|---|---|---|---|---|
| `SqlTestBase` | 无（由调用方决定） | —— | —— | DAO 单测 / 已有外部容器的复用 |
| `RdbTestBase` | H2 | `false` | —— | DAO 集成测试，不需要缓存 |
| `RdbAndRedisCacheTestBase` | H2 + Redis | `true` | `SINGLE_LOCAL` | Service 集成测试，要验证缓存路径 |
| `CacheTestResetSupport` | —— | —— | —— | `RdbAndRedisCacheTestBase.afterTestDataSetup()` 调用的内部工具 |

## SQL 文件加载契约

`SqlTestBase.getTestDataSqlPath()` 默认按规则在 classpath 上找：

```
sql/<rdbType-lowercase>/<TestClassSimpleName>.sql
                                   └─ this::class.java.simpleName，运行时类（非声明类）
```

- `rdbType` 由 `RdbKit.determineRdbTypeByDataSource(dataSource)` 实时探测——所以同一测试
  类切到 PG / H2 容器时不需要改代码。
- 未找到：只打 `WARN` 日志、不抛错，测试照常跑（场景：fixture 完全在 `@Sql` 注解里
  或代码里写）。
- 子类可 `override getTestDataSqlPath()` 给自定义路径。

## 测试数据 = "事务外提交" 而非 "事务回滚"（关键设计）

`@BeforeTransaction` + `populator.execute(dataSource)`——SQL 在测试事务**启动前**直接
跑、数据被提交到数据库。**`@Transactional` 只回滚测试方法本身写入的数据，不回滚 fixture**。

为什么这样做：
- 之前用 `@BeforeEach` 时，`ResourceDatabasePopulator` 实际在测试事务里跑，多个测试类同时
  跑时 `@BeforeEach` 数据彼此可见、造成串扰。
- 改成 `@BeforeTransaction` 后数据落地为"已提交"，**但每次测试方法开始前都会重新 execute
  一遍 SQL**——文件是全量 fixture（含 `delete`/`truncate`），所以数据始终干净。
- 副作用：测试结束后**库里残留最后一次 fixture 的数据**，不是"全空"。要看数据库现状的人
  会觉得奇怪。

`@Execution(ExecutionMode.SAME_THREAD)`：强制类内方法串行。结合容器复用，整体执行模型是
"多测试类串行复用同一容器、每方法 reload 全量 fixture"。

## 缓存重置：故意走反射

`CacheTestResetSupport` 通过 `Class.forName` + 反射调 `ICacheConfigProvider` /
`KeyValueCacheKit` / `HashCacheKit` / `RedisTemplates`——不直接 `import` 缓存模块。

为什么：`kudos-test-rdb` 只 `api` 依赖 `kudos-ability-data-rdb-jdbc`，**没有依赖
`kudos-ability-cache-*`**。如果 import 业务缓存模块，测试基础设施会反向耦合到业务能力，
依赖图就成环。反射 + try/catch 保留了"测试上下文里没缓存模块时静默跳过"的能力。

调用顺序：`flushdb` → 读 `sys_cache` 配置 → 按 hash/非 hash 分组 → `doClear` → `reloadAll`。
仅在 `RdbAndRedisCacheTestBase.afterTestDataSetup()` 触发。

## 使用模式

```kotlin
class UserServiceTest : RdbAndRedisCacheTestBase() {
    @Resource lateinit var userService: UserService

    @Test
    fun `findById should hit cache on second call`() {
        userService.findById(1L)  // 第一次：miss → 落库 → 写缓存
        userService.findById(1L)  // 第二次：hit
    }
}
```

测试数据放在 `test-resources/sql/h2/UserServiceTest.sql`，每个测试方法跑之前会自动 reload。

## 覆盖默认配置

`RdbTestBase` / `RdbAndRedisCacheTestBase` 的 companion 用 `@DynamicPropertySource` 注册
容器与 cache 策略——业务子类要换组合时，**自己写一份 companion 完全覆盖**：

```kotlin
class UserServiceMySqlTest : RdbAndRedisCacheTestBase() {
    companion object {
        @JvmStatic @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.cache.enabled") { "true" }
            registry.add("cache.config.strategy") { "LOCAL_AND_REMOTE" }
            MySqlTestContainer.startIfNeeded(registry)
            RedisTestContainer.startIfNeeded(registry)
        }
    }
}
```

Spring Test 框架同一类内允许多个 `@DynamicPropertySource`，**子类的会覆盖父类同名 key**。

## 已知问题

- ❗ `RdbAndRedisCacheTestBase` 强制 `testcontainers.ryuk.disabled=true`——ryuk 是
  testcontainers 用来兜底清理容器的 sidecar，禁掉之后**容器只靠 JVM shutdown hook 清理**，
  IDE 强杀 JVM 时会残留容器。设这个值是为了少一个 sidecar 容器、加快测试。
- ❗ 测试数据**不会回滚**（见上"事务外提交"），库残留最后一次 fixture——不是 bug 是
  trade-off，但容易让第一次接触的人困惑。
