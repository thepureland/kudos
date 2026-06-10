# kudos-ability-log

日志能力主题——目前只有审计日志一类。

| 子目录 | 内容 |
|---|---|
| [`kudos-ability-log-audit`](kudos-ability-log-audit/README.md) | 审计日志框架 + MQ / RDB 落地 |

预留位置给其他日志类能力（如运营日志、操作日志特殊场景等）。

## 改进建议（自动分析 2026-06-11）

以下为本轮深度审查中**不宜直接修改**（涉及 public API、行为取舍或需要业务决策）的发现，按维度归类。
本轮已直接修复的三处见各文件注释：`WebLogAuditFilter` 的 `ContentCachingRequestWrapper(request, 0)`
缓存上限 bug、`LogAuditAspect` 非 Web 路径脱敏缺失、`AuditLogTool` 死代码清理。

### 功能缺陷 / 值得补充的功能

- **submit 失败无兜底**：`LogAuditAspect` / `WebLogAuditAspect`（`kudos-ability-log-audit-common/src/.../annotation/`）
  忽略 `IAuditService.submit` 的 Boolean 返回值；RDB/Mongo/ClickHouse 实现失败时返回 false 仅打 ERROR 日志，
  无重试、无本地文件 spool。建议增加可插拔的 `IAuditFallbackHandler` SPI（如本地 append-only 文件 + 启动补偿回放）。
- **无批量/异步缓冲写入**：除 MQ 后端外，所有 `submit` 都在业务线程同步逐次执行（每次 @Audit 调用 1~2 条 SQL）。
  高频审计场景建议提供 write-behind 缓冲装饰器（BlockingQueue + 定时/定量 flush，复用各后端已有的 batchInsert）。
- **无保留策略（retention）**：三个存储后端均无 TTL/清理机制。建议：ClickHouse DDL
  （`kudos-ability-log-audit-rdb-clickhouse/resources/db/V1.0.0__create_sys_audit_log_clickhouse.sql`）加 `TTL` 子句；
  Mongo（`SysAuditLogDocument`）加 TTL index；RDB 提供按 `operate_time` 分区或清理 job 的文档指引。
- **`pagingSearch` 无 pageSize 上限**：接口 KDoc 写明 "implementations may further cap this"，
  但 ktorm / Mongo / ClickHouse 三个实现（`RdbKtormAuditLogReadOnlyService` / `MongoAuditLogReadOnlyService` /
  `RdbClickhouseAuditLogReadOnlyService`）都只做了下限 clamp，未 cap 上限——超大 pageSize 可拖垮存储。

### 安全性

- **`@LogDesensitize` 扫描只看 `args[0]`**：`AuditLogTool.applyRequestDesensitizeFromFirstJoinPointArg`
  （`kudos-ability-log-audit-common/src/.../support/AuditLogTool.kt`）固定扫描首参数；当业务用
  `@Audit(modelArgIndex = N)` 指向非首参 model 时，脱敏字段名不会被收集，敏感字段仍会明文入库。
  建议让扫描跟随 `modelArgIndex` 解析出的实际 model 参数。
- **监控消息日志注入面**：`LoggingMonitorService`（同模块 `api/`）把 `exceptionMsg`（含调用方拼接的换行栈帧）
  原样写入 ERROR 日志，构造的换行可伪造日志行。纯文本 appender 部署建议对 CR/LF 做转义，或在文档中
  要求 JSON 编码 appender。
- **嵌套 JSON 不脱敏**：`LogDesensitize` KDoc 已声明只处理顶层 key——属已知边界，但若业务 DTO 普遍嵌套，
  需要评估递归遍历（注意数组与深度上限）。

### 测试覆盖

- **`WebLogAuditFilter` 无任何测试**（`kudos-ability-log-audit-common`，对照 test-src 缺失）——本轮修复的
  "缓存上限传 0 导致 body 永远为空" bug 正是缺测试才长期潜伏。建议补 "filter 包装后 body 可重复读" 的
  MockMvc/集成测试。
- **`MonitorMsgTool` 无单测**：StackWalker 跳帧数（skip(2)）与栈截断 10 行的契约靠注释维持，重构易破坏。
- **非 Web 路径脱敏**（本轮修复点）建议补一条 `LogAuditAspectTest` 用例：带 `@LogDesensitize` 字段的 model
  经 `ignoreForm = NOT` 提交后，`requestFormData` 中敏感值已被掩码。

### 可扩展性

- **MQ topic/binding 硬编码**：`MqAuditService` / `MqMonitorService`（`kudos-ability-log-audit-mq/src/.../beans/`）
  的 `@MqProducer(topic = "LOG_AUDIT_TOPIC", bindingName = "logAudit-out-0")` 是编译期常量，换 topic 必须改代码。
  受注解机制限制无法直接配置化，建议在 MQ 模块文档中说明覆盖方式（自定义 bean 同名替换）。
- **`OperationTypeEnum.code` 隐式数字契约**：`BaseLog.toSysLogVo` 用 `Integer.valueOf(op.code)`，
  新增非数字 code 的枚举值会运行时 NumberFormatException——契约只在注释里。建议给枚举加 `init { require(...) }`
  编译外校验或新增 `codeAsInt` 属性。

### 可观测性

- **无指标**：审计写入成功/失败次数、延迟无 Micrometer 计数；`submit` 返回 false 时切面无 WARN。
  建议在两个切面统一打点（counter: audit.submit.success/failure, timer: audit.submit.duration）。
- **MQ 实现恒返回 true**：`MqAuditService.submit` 为占位方法，发送失败完全依赖 stream 切面自身的日志，
  调用方无法感知投递失败——与 `IAuditService.submit: Boolean` 的语义不符（见下"对外接口"）。

### 可维护性

- **`BaseLog.SEPERATOR` 拼写错误**（应为 `SEPARATOR`，`kudos-ability-log-audit-common/src/.../entity/BaseLog.kt`）：
  public const 不能直接改名；建议新增正确拼写常量并 `@Deprecated` 旧名过渡。
- **`Audit.subSysCode` 与 `WebAudit.subsysCode` 大小写不一致**（同模块 `annotation/`）：public API 不能改，
  建议下个 major 版本统一为 `subSysCode`。
- **`applyAuditLog` / `applyDetailLog` 在 ktorm 与 ClickHouse 模块重复**（`RdbKtormAuditService.kt` 与
  `RdbClickhouseAuditService.kt` 各 ~40 行近似代码）：两边表对象不同型导致不能直接共用，
  可在 `rdb-common` 提供以列名常量为键的字段映射模板消除重复。
- **`AuditLogTool` 过长（~430 行）**：desensitize 相关 6 个函数自成一块，可拆出 `AuditLogDesensitizer` 内部对象。

### 对外接口

- **`IAuditService.submit: Boolean` 语义失真**：MQ 实现恒 true、RDB 实现 true/false、切面又不消费返回值——
  三方对 Boolean 的理解不一致。建议下个版本改为返回 `Unit` + 失败回调/事件，或明确文档化 "true 仅代表已受理"。
- **`LogAuditContext.get()` 的自动创建语义**已在 common README 记录，长期建议 `@Deprecated` 引导到 `getOrNull()`。

### 文档

- 各子模块 README 与 KDoc 非常充分（含决策树、SPI 对照表、多后端共存示例），无明显缺口。
  唯一同步项：common README 中 `WebLogAuditFilter` 代码片段已随本轮 bug 修复更新。
