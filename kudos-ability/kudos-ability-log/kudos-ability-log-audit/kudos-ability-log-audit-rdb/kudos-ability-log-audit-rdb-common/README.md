# kudos-ability-log-audit-rdb-common

审计日志的 **RDB（关系型数据库）落地共享层**。**当前为占位模块**——无源码，
`build.gradle.kts` 仅 `dependencies {}` 空块。

## 当前状态

```
build.gradle.kts (3 行，dependencies 块为空)
```

- 在 `settings.gradle.kts` 中已注册（参与 build graph）
- 但**没有任何模块依赖它**——既不被 `kudos-ability-log-audit-rdb-ktorm` 依赖，也不被
  业务侧引用
- 也不依赖 `kudos-ability-log-audit-common`——一旦真有抽象代码就需要补这条依赖

## 设计意图

按 kudos 的 `xxx-rdb-common + xxx-rdb-<orm>`（如 `kudos-ability-data-rdb-jdbc` +
`kudos-ability-data-rdb-ktorm`）双层结构，本模块预留给：
- ORM 无关的审计日志 RDB 抽象 SPI（`IAuditLogRepository` 之类）
- 表结构 / 字段名常量
- DDL / 迁移脚本基线（可挪到 flyway resources 目录）

具体的 ORM 实现（目前仅 Ktorm 一种）放在 `kudos-ability-log-audit-rdb-ktorm`。

## 模块入口

无源码。

## 依赖

```kotlin
dependencies {
}
```

完全空 —— 占位模块的极简形态。一旦补内容需要：
1. `api(project(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-common"))` 拿 `IAuditService` 等基础接口
2. 让 `kudos-ability-log-audit-rdb-ktorm` 添加 `api(project(":...rdb-common"))`

## 已知限制 / 后续工作

- ❗ 完全占位模块。可选处理：
  - (a) 等真有共享抽象再保留
  - (b) 暂时删除并让 `rdb-ktorm` 直接依赖 `log-audit-common`，与 ktorm 模块 audit 共享代码的约定明确后再加
- ❗ 与 `kudos-ability-cache-interservice-common` / `kudos-ability-web-common` 是同一类"按
  `xxx-common + xxx-<impl>`双层结构强行配齐"的产物——批量决策保留 / 删除可一次性收口
