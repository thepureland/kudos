# kudos-ability-data-docdb-mongo

基于 [spring-data-mongodb](https://spring.io/projects/spring-data-mongodb) 的 MongoDB
基础支持模块（MVP，单 Mongo 数据源）。

业务工程把本模块作为依赖加入即可获得：

1. **`MongoTemplate` 直接可用** —— 走 Spring Boot 原生 `spring.data.mongodb.*` 配置；
   本模块只在它的基础上加 kudos 特化的转换器与开关。
2. **BigInteger ↔ String 自动转换** —— Mongo BSON 无原生任意精度整数 codec，本模块默认
   把 `@Document` 上的 `BigInteger` 字段以 String 形式持久化，保住精度。`kudos.ability.docdb.mongo.big-integer-as-string=false` 可关闭。
3. **不影响标准 Spring Data Mongo 用法** —— `MongoRepository`、`MongoTemplate`、`@Document`、
   `@Indexed`、`MongoCustomConversions` 等照常使用。

## 设计要点

### 为什么直接复用 `spring.data.mongodb.*` 而不自建命名空间

soul 的 `MongoCustomProperties extends MongoProperties` 在原 namespace 之上加了
`hosts: List<String>` / `ports: List<Object>` / `pool` 嵌套——这种重叠会让 IDE 补全和
Spring 文档给出的指引互相打架。kudos MVP 直接让业务工程用 `spring.data.mongodb.host`、
`spring.data.mongodb.connection-pool-size` 这一套官方字段，本命名空间只放 kudos 特有开关
（目前只有 `big-integer-as-string`）。

### 为什么 BigInteger 默认存 String

可选项及 trade-off：

| 方案 | 精度 | 范围查询 |
|---|---|---|
| `Long` | 上限 2^63-1 (约 9.2e18) | ✅ 原生 |
| `Decimal128` | 34 位十进制 | ✅ 原生 |
| `String` | 任意 | ❌ 退化为字典序 |

业务上 BigInteger 字段一般用来存：token 金额（精度可达数十位）、大用户 ID、密钥派生值。
这些场景几乎全部不依赖数据库侧的 `$gt`/`$lt` 数值比较。如果应用确需范围查询，自己设计
方案（拆成 high/low 两段 Long，或换 Decimal128），用 `big-integer-as-string=false` 关闭
默认转换器。

### 与 soul-ability-data-docdb-mongo 的差异

soul 的完整版包含 **多 Mongo 数据源动态路由 + AOP 切换切面**（mirror baomidou 的 JDBC
dynamic-datasource）。本 MVP 不带这套，单 Mongo 数据源能覆盖 90% 业务场景；多源路由作为
后续独立 commit 提供。

soul 还把 `MongoDatabaseHelper` 静态化以便业务代码不依赖注入直接使用 `MongoTemplate`。
kudos 倾向于通过 DI 注入避免静态依赖，因此不移植这个 helper——业务代码直接
`@Resource private lateinit var mongoTemplate: MongoTemplate` 即可。

### `BigIntegerConverters` 的小差异

soul 的 ReadingConverter 在 source 为空白时返回 null。这会让一条数据库里"误存空字符串"的
记录默默变成业务侧的 null，掩盖问题。kudos 版本不做这种 swallow——如果数据库里出现
非法 BigInteger 字符串，直接抛 `NumberFormatException`，让问题尽早暴露。

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/MongoAutoConfiguration` | 装配入口；注册 BigInteger-aware `MongoCustomConversions` |
| `init/properties/MongoCustomProperties` | kudos 特有属性根（`kudos.ability.docdb.mongo.*`） |
| `convert/BigIntegerConverters` | Writing + Reading 转换器 |
| `resources/kudos-ability-data-docdb-mongo.yml` | 默认值（`big-integer-as-string=true`） |

## 配置示例

```yaml
spring:
  data:
    mongodb:
      host: localhost
      port: 27017
      database: myapp
      username: app
      password: ${MONGO_PWD}
      authentication-database: admin

kudos:
  ability:
    docdb:
      mongo:
        big-integer-as-string: true   # 默认值；明示便于排查
```

## 使用示例

```kotlin
@Document(collection = "wallets")
data class Wallet(
    @Id val id: String,
    val owner: String,
    val balance: BigInteger,    // 自动以 String 持久化
)

@Repository
interface WalletRepository : MongoRepository<Wallet, String>

@Service
class WalletService(
    private val template: MongoTemplate,
    private val repo: WalletRepository,
) {
    fun deposit(owner: String, amount: BigInteger) {
        val wallet = repo.findById(owner).orElseGet {
            Wallet(id = owner, owner = owner, balance = BigInteger.ZERO)
        }
        repo.save(wallet.copy(balance = wallet.balance + amount))
    }
}
```

## 测试覆盖

- `BigIntegerConvertersTest` —— 纯单元；Writing/Reading 转换器各自往返
- `MongoAutoConfigurationTest` —— 与真实 Mongo 7.0 (testcontainer) 集成；BigInteger 写
  Mongo 后以 String 读回 / 大整数精度保住 / 35 位数字往返

### 测试时序坑

Spring Boot 4 的 `MongoAutoConfiguration` 在上下文 refresh 阶段就把 `MongoClient` bean 装
配死，时序上 **早于** `@DynamicPropertySource` 把 `spring.data.mongodb.uri` 写进 Environment。
所以即使 `MongoTestContainer` 把 testcontainer 的 URI 注入了 Environment，Spring Boot 自
建的 `MongoClient` 仍然指向默认 `localhost:27017`，连不上。Redis 这边不存在这个时序差。

测试侧的解法：用 `@TestConfiguration + @Primary` 显式注册 `MongoClient` bean，从
Environment 在 `@Bean` 工厂方法被调用的那一刻读 URI（此时 `@DynamicPropertySource` 已生效）。
参考 `MongoAutoConfigurationTest.TestMongoClientConfig`。**业务应用不受这个坑影响**——
业务的 `application.yml` 在 context refresh 之前就已经把 URI 写进 Environment 了。

未覆盖：动态数据源（未移植）；MongoTemplate 一般 CRUD（由 spring-data 自身测试覆盖）。

## 已知限制 / 后续工作

- ❗ 仅单 Mongo 数据源；多源动态路由作为后续 commit
- ❗ `big-integer-as-string` 是全局开关，无法字段级控制；需要混合精度场景的业务工程自行
  定义 `MongoCustomConversions` 子类覆盖本模块的 `@Primary` bean
- ❗ Spring Boot 4 中 `MongoProperties` 仍在 `spring.data.mongodb.*`，未来若迁到 `spring.mongo`
  需同步本模块 README

## 依赖

```kotlin
api(project(":kudos-context"))
api(libs.spring.boot.starter.data.mongodb)

testImplementation(project(":kudos-test:kudos-test-common"))
testImplementation(project(":kudos-test:kudos-test-container"))
```
