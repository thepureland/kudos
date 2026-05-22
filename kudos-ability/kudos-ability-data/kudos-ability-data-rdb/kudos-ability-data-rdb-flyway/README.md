# kudos-ability-data-rdb-flyway

Spring Boot 启动期的多数据源 Flyway 迁移器。把 SQL 脚本按"模块 × 数据库类型"分目录组织，
启动时按声明顺序逐模块跑 Flyway，任何一个模块失败都打断启动（不会在错位的 schema 上跑业务）。

## 何时用它

- 单进程同时管理多个 RDB 数据源（比如 `master` + `audit_log` + `tenant_*`），每个数据源
  跑自己一套迁移
- 同一份 SQL 脚本要兼容多种数据库类型（h2 跑测试 / postgresql 跑生产），脚本按 dbType 分目录

如果只有一个数据源，Spring Boot 自带的 `spring-boot-starter-flyway` 已经够用，本模块就是 overkill。

## 约定

```
classpath:sql/
    └─ <moduleName>/                ← 一个 kudos 业务模块一个目录
        └─ <dbType>/                ← postgresql / h2 / mysql ...（RdbTypeEnum#name.lowercase()）
            ├─ V1.0.0__init.sql     ← Flyway 标准命名
            └─ V1.0.1__add_x.sql
```

每个业务模块用独立的 Flyway 元数据表 `flyway_history_<moduleName>`，互不污染。

## 配置

```yaml
kudos:
  ability:
    flyway:
      enabled: true                # 可设 false 整体禁用启动迁移（只读副本场景）
      datasource-config:           # 模块 → 动态数据源 key 映射，按声明顺序迁移
        sys: master
        audit_log: audit
        tenant: master

# Flyway 自身参数走 Spring Boot 标准前缀
spring:
  flyway:
    baseline-on-migrate: true
    encoding: UTF-8
    out-of-order: false
    validate-on-migrate: true
    placeholder-replacement: true
    placeholders:
      app_schema: public
```

`kudos.ability.flyway.*` 和 `spring.flyway.*` 不重叠：前者只决定"哪个模块用哪个数据源"，
后者控制 Flyway 自身行为（baseline / encoding / outOfOrder ...）。

## 模块入口

| 类 | 角色 |
|---|---|
| `FlywayAutoConfiguration` | 装配入口；`@ConditionalOnProperty(kudos.ability.flyway.enabled, default=true)` 决定是否启用 |
| `FlywayMultiDataSourceMigrator` | 启动期被调用的迁移器，扫 classpath + 走 properties + 逐模块迁移 |
| `FlywayMultiDataSourceProperties` | 模块名 → 数据源 key 的 yml 绑定 |
| `FlywayKit` | 单模块迁移的纯函数实现，**脱离 Spring 也能用**（代码生成器 / CLI 工具直接调） |

## 失败语义

- Flyway `migrate()` 报 `success=false` —— 抛 `IllegalStateException`，打断启动
- 配置里模块对应的数据源 key 不存在 —— 抛 `IllegalStateException`
- 同一个模块名在多个 classpath URL 同时出现 —— 抛 `IllegalStateException`
- 配置里声明的模块名磁盘上找不到 —— 仅打 warn 日志，继续
- `spring.flyway.placeholders`、`placeholder-prefix`、`placeholder-suffix`、`placeholder-separator`
  会透传给每个模块的 Flyway 实例

设计原则：**宁可启动不来，也不让应用跑在不一致的 schema 上**。

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc"))
api(libs.spring.boot.starter.flyway)
api(libs.baomidou.dynamic.datasource.starter)
```

通过 `DsContextProcessor` 拿动态数据源 —— 因此**目前依赖 baomidou dynamic-datasource starter**。
如果未来要解耦，需要把"按 key 取 DataSource"抽象成一个 SPI。

## 已知限制 / 后续工作

- ❗ 没有 Flyway callback / hook 暴露（pre-migrate / post-migrate）
- ✅ Flyway placeholders 已透传：支持 `spring.flyway.placeholders` 以及 placeholder 前缀 / 后缀 /
  分隔符配置
- ❗ 紧耦合 `DsContextProcessor` —— 不用 baomidou dynamic-datasource 就用不了本模块
- ❗ 没有 dry-run / repair / clean 入口；运维脚本要绕过本模块直接用 Flyway CLI
- ❗ 测试覆盖到 happy path、缺失数据源、placeholders 替换；jar 协议路径扫描、重复模块检测、
  Flyway 失败等分支未直接单测

## 用法示例：脱离 Spring 跑迁移（代码生成器场景）

```kotlin
val ds = HikariDataSource(/* ... */)
val flywayProps = FlywayProperties().apply {
    isBaselineOnMigrate = true
    encoding = "UTF-8"
}
FlywayKit.migrate(moduleName = "sys", dataSource = ds, flywayProperties = flywayProps)
```
