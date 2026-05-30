# kudos-ability-data-tsdb-influxdb

InfluxDB 2.x 基础支持模块（MVP，单数据源）。基于 [InfluxDB Java client](https://github.com/influxdata/influxdb-client-java) 6.7.0。

业务工程把本模块作为依赖加入 + 配置 `kudos.ability.tsdb.influxdb.url` / `token` 即可获得：

1. **`InfluxDBClient` 直接可用**：autoconfig 装配单实例 client，业务侧
   `@Resource InfluxDBClient` 直接拿到
2. **`url` + `token` 缺失时不装配**：`@ConditionalOnProperty(name=["url","token"])` 双条件控制；
   开发/测试环境不配 InfluxDB 也不会让 context refresh 报错
3. **默认 ms 精度 + QUORUM 一致性**：参数固化在 autoconfig（与 soul 对齐）；纳秒精度业务
   每次写入时按 point 自行覆盖

## 设计要点

### 为什么必须同时配 `url` 和 `token`

InfluxDB 2.x 取消了匿名访问 —— 没有 token 就无法认证。如果只配 `url`，autoconfig 装出来的
client 会在第一次写入/查询时返回 401，错误发生在 runtime 而不是 startup。所以 autoconfig
通过 `@ConditionalOnProperty(name=["url","token"])` 同时校验，apps 漏配任一项 → bean
不装配 → 业务侧 `@Resource InfluxDBClient` 报"bean not found" → 立即看到问题。

### 为什么默认 `precision = MS` 而不是 NS

InfluxDB Java client 默认是纳秒精度。但 InfluxDB 服务端 bucket 通常按 ms/s/us/ns 配置精度上限；
ns 客户端写到 ms 服务端的数据会被静默截断（不报错也不警告）。**ms 是监控/IoT 场景最常用的
精度**，autoconfig 默认 ms 与典型业务对齐。需要 ns 精度的业务在写入时按 point 覆盖：

```kotlin
val point = Point.measurement("cpu")
    .addField("usage", 0.85)
    .time(Instant.now(), WritePrecision.NS)
```

### 为什么默认 `consistency = QUORUM`

`WriteConsistency` 仅在 InfluxDB Enterprise（集群版）下生效，OSS 单节点忽略。QUORUM 是"半数
节点写入确认"的中庸默认 —— 不像 ALL 那么严，也不像 ANY 那么松。与 soul 端的默认一致，
让 OSS → Enterprise 迁移不需要改代码。

### 为什么 `readTimeout` / `writeTimeout` 当前不生效

InfluxDB Java client 的 OkHttp timeout 设置在 OkHttpClient.Builder 层 —— InfluxDBClientOptions
没暴露直接的 setter。要支持这两个 yml 字段需要自己构造 OkHttpClient 传给 options。
**MVP 保留这两个 properties 字段是为了二进制兼容**（未来 wiring 到 OkHttp 时不破坏 yml），
但实际生效需要业务侧自己声明 `InfluxDBClient` bean（autoconfig 通过 `@ConditionalOnMissingBean`
让位）。

### 与 soul 的差异：不带多数据源

soul 的 `InfluxdbConfiguration` 同时装单实例 client + `InfluxdbTemplate` 用于多 InfluxDB 实例
路由（按 dsId 切换）。本模块 MVP 不带 multi-DS 路由（参考 docdb-mongo 的同款决定）——
单 InfluxDB 覆盖 90% 监控/IoT 场景；多源路由作为后续独立 commit 提供。

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/InfluxdbAutoConfiguration` | 装配入口；条件性发布 `InfluxDBClient` bean |
| `init/properties/InfluxdbProperties` | yml 绑定根；`kudos.ability.tsdb.influxdb.*` |
| `resources/kudos-ability-data-tsdb-influxdb.yml` | 默认值（全注释 + 模板） |

## 配置示例

```yaml
kudos:
  ability:
    tsdb:
      influxdb:
        url: http://localhost:8086
        token: ${INFLUXDB_TOKEN}    # 生产从环境变量注入
        org: my-org
        bucket: metrics
        log-level: NONE             # 调试时改 BASIC / BODY
```

## 使用示例

```kotlin
@Service
class MetricRecorder(private val influx: InfluxDBClient) {

    fun recordCpuUsage(host: String, usage: Double) {
        val writeApi = influx.writeApi
        val point = Point.measurement("cpu")
            .addTag("host", host)
            .addField("usage", usage)
            .time(Instant.now(), WritePrecision.MS)
        writeApi.writePoint(point)
    }

    fun queryHourlyAvgCpu(host: String): Double {
        val flux = """
            from(bucket: "metrics")
              |> range(start: -1h)
              |> filter(fn: (r) => r._measurement == "cpu" and r.host == "$host")
              |> mean()
        """
        return influx.queryApi.query(flux).single()
            .records.first().value as Double
    }
}
```

## 自定义 `InfluxDBClient` 的接入方式

需要非默认 timeout / proxy / interceptor 时业务工程自己 declare bean，autoconfig 让位：

```kotlin
@Bean
fun influxDBClient(props: InfluxdbProperties): InfluxDBClient {
    val okHttp = OkHttpClient.Builder()
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(MyAuditInterceptor())
    val options = InfluxDBClientOptions.builder()
        .url(props.url!!)
        .authenticateToken(props.token!!.toCharArray())
        .org(props.org)
        .bucket(props.bucket)
        .okHttpClient(okHttp)
        .build()
    return InfluxDBClientFactory.create(options)
}
```

## 测试覆盖

- `InfluxdbAutoConfigurationTest` —— 用 `ApplicationContextRunner` 测装配条件矩阵：
  - 仅有默认 yml（无 `url` / `token`）时 `InfluxDBClient` bean 不装配
  - 仅设 `url` 时不装配；仅设 `token` 时也不装配
  - `url` + `token` 都设时 client 装配（实例真实可用，但因测试不依赖真实 InfluxDB 服务，
    只验证 bean 类型，不发请求）
  - 业务自定义 `InfluxDBClient` bean 时 `@ConditionalOnMissingBean` 让位

未覆盖：真实 InfluxDB 集成（write/query 端到端）—— testcontainer 启动开销大且非业务核心
路径，业务工程在自己的 IT 测试中按需补全。

## 已知限制 / 后续工作

- ❗ 单 InfluxDB 数据源；多源动态路由 deferred
- ❗ `readTimeout` / `writeTimeout` properties 字段不生效（OkHttp 层需要自定义 client；
  README 已给出方案）
- ❗ 默认 ms 精度全局固定；纳秒精度按 point 覆盖
- ❗ 仅支持 InfluxDB 2.x（token 鉴权）；InfluxDB 1.x（user/password）不在 scope

## 依赖

```kotlin
api(project(":kudos-context"))
api(libs.influxdb.client.java)

testImplementation(project(":kudos-test:kudos-test-common"))
```
