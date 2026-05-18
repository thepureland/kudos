# kudos-ability-log-audit-rdb-ktorm

审计日志的 **Ktorm + RDB 落地实现**。**当前为占位模块**——无源码，`build.gradle.kts` 仅
`dependencies {}` 空块。

## 当前状态

```
build.gradle.kts  (3 行，dependencies 块为空)
```

- 在 `settings.gradle.kts` 中已注册
- 但**没有任何模块依赖它**——业务侧没办法注入这里的 `IAuditService` 实现
- 也没声明依赖 `log-audit-common` / `data-rdb-ktorm` 等明显需要的模块

## 设计意图

未来要实现的：把 `SysAuditLogModel` / `SysAuditLogVo` / `SysAuditDetailLogVo` 通过 Ktorm
落到 RDB 表（典型表名 `sys_audit_log` / `sys_audit_detail_log`）。装配为 `IAuditService` bean，
让没接 MQ 的部署也能持久化审计记录。

实现时至少需要：
1. `IAuditService` 的 Ktorm 实现类（事务边界 + 批量插入）
2. `SysAuditLogTable` / `SysAuditDetailLogTable` Ktorm `Table` 定义
3. `LogAuditRdbKtormAutoConfiguration` 装 bean（注意与 `kudos-ability-log-audit-mq`
   的 `@Primary` 冲突——非 MQ 优先时 `@Primary` 应给本模块）
4. flyway 迁移脚本（建表 DDL）

## 与已实现的 MQ 路径对照

| 维度 | log-audit-mq | log-audit-rdb-ktorm（待实现） |
|---|---|---|
| `IAuditService` 实现 | `MqAuditService`（AOP 占位 + `@MqProducer`） | 缺 |
| `@Primary` | ✅ MQ 优先 | 缺，需协调 |
| 异步 / 同步 | 异步投递 | 同步 SQL（建议异步化） |
| 失败语义 | 取决于切面 + broker | 抛 SQLException 直接传到业务方法 |
| 测试 | 1 case（不验证投递） | 缺 |

## 已知限制 / 后续工作

- ❗ 完全占位。要实现，需要先决定：
  - DDL 是否走 flyway（与 `kudos-ability-data-rdb-flyway` 协同）
  - 是否单独的 audit 数据源（避免审计写挂掉业务交易）
  - 失败语义：阻塞业务、还是 fire-and-forget 入内存队列
- ❗ 与 `kudos-ability-log-audit-rdb-common` 一样，属于"按命名约定预留"的占位模块；批量
  决策保留 / 删除时一起处理
