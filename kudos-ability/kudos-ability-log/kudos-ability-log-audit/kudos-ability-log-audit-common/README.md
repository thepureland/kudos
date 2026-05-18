# kudos-ability-log-audit-common

审计日志框架的**共享层**：注解 / Aspect / 上下文 / 格式化 SPI / 实体 POJO。具体的"日志
落到哪里"由下游 `kudos-ability-log-audit-rdb-*` / `kudos-ability-log-audit-mq` 子模块实现
[IAuditService] 处理。

## 设计要点

### 两套注解的边界

| 注解 | 切面 | 处理对象 |
|---|---|---|
| `@Audit(opType, moduleCode, ...)` | `LogAuditAspect` | 通用 service 方法——从 `joinPoint.args[0]` 取业务对象 |
| `@WebAudit(opType, moduleCode, ...)` | `WebLogAuditAspect` | Web controller 方法——从 `RequestContextHolder` 拿 HTTP 请求 |

**WebAudit 切面会跳过 multipart 请求**（避免读 body 阻塞文件上传）。

### 切面装配链

```
before(joinPoint)
   ↓
AuditLogTool.createLogVo(annotation, model/request, joinPoint)
   → 构造 LogVo（含 BaseLog 列表 + 模块解析 + 业务侧自定义旧数据加载）
   → LogAuditContext.set(logVo)            # ThreadLocal
   ↓
[ business method 执行 ]
   ↓
after(joinPoint)
   ↓
LogAuditContext.get() → LogVo
AuditLogTool.createSysAuditLogModel(logVo, arg JSON / request body)
   → SysAuditLogModel(entities + sysAuditDetailLogs + tenantId + subSysCode)
   → setOperator()  填充操作人 / IP / 客户端信息
   ↓
auditService.submit(modelAudit)            # 下游实现：DB / MQ / ...
```

### `LogAuditContext` 的 get/set/clear 约束

- `LogAuditContext.get()` —— **没有时自动创建空 LogVo 并塞回 ThreadLocal**。这是历史 API
  语义（某些路径只走 `after` 而没走 `before`）。副作用是只读侦测会污染 ThreadLocal。
- `LogAuditContext.getOrNull()` —— **新增**。纯只读检查请用这个，避免污染。
- `LogAuditContext.clear()` —— 请求 / 任务结束的 finally 中必须调，否则线程池复用线程
  时下个任务会读到陈旧的 LogVo。

> `InheritableThreadLocal` 的 `childValue` 返回 null —— 子线程**不**继承父线程的审计
> 上下文，避免线程池里的子任务串台。

### `IAuditLogDetailDescriptionFormatter` 的两种使用方式

```kotlin
// 方式 1: 注解里显式指定
@Audit(opType = UPDATE, moduleCode = "USER", descriptionFormatter = MyFmt::class)
fun updateUser(...) { ... }

// 方式 2: 注解留默认值（DefaultAuditLogDetailDescriptionFormatter::class）
//        → AuditLogTool.descriptionFormatter 把所有 IAuditLogDetailDescriptionFormatter
//        bean 拿出来，按 needFormat(baseLog) 自动选一个
@Audit(opType = UPDATE, moduleCode = "USER")
fun updateUser(...) { ... }
```

**修复前的 bug**：方式 2 的 auto-pick 路径里把 `KClass` 与 `Class.java` 比较——永远 false，
所以**所有调用都走"找默认实现"分支**，业务定义的 `needFormat()` 自定义 formatter **从来
没被自动选过**。已修：改为 `KClass == KClass` 比较。

### `AuditLogTool.getRequestData` 的安全转型

旧实现：`request as HttpServletRequestWrapper` 无条件强制转型，对未被 Spring Security 包过
的原始请求会 `ClassCastException`，直接挂掉整条业务请求链路。修正为 `as?` 安全转型 + 早返回 `""`，
让没启用 `WebLogAuditFilter` 的部署不会因 audit 切面崩溃。

### `WebLogAuditFilter` 注册位置

```kotlin
class WebLogAuditFilter : OncePerRequestFilter() {
    override fun doFilterInternal(req, res, chain) {
        val wrapper = ContentCachingRequestWrapper(req, 0)
        chain.doFilter(wrapper, res)
    }
}
```

把请求包成 `ContentCachingRequestWrapper`，让 `WebLogAuditAspect.after` 能从 `getRequestData`
回放出 body。**使用 Spring Security 时必须把本 filter 注册到 security filter chain 内**——
否则会被 security 的请求包装顶替掉。

### `OperationTypeEnum.code` 必须是数字字符串

`BaseLog.toSysLogVo` 用 `Integer.valueOf(op.code)` 转 `operateTypeId: Int?`。
**code 字段是字符串型 enum value 但内容必须能解析为整数**——新增枚举值时不要写英文 code。

## 模块入口

| 路径 | 角色 |
|---|---|
| `annotation/Audit` / `WebAudit` | 两个注解（通用方法 / Web controller） |
| `annotation/LogAuditAspect` / `WebLogAuditAspect` | 对应两个切面 |
| `api/IAuditService` | 审计日志提交 SPI（下游 RDB / MQ 模块实现） |
| `api/IMonitorService` | 监控告警提交 SPI（与审计平行） |
| `filter/WebLogAuditFilter` | `ContentCachingRequestWrapper` 包装 filter |
| `entity/LogVo` / `BaseLog` / `LogParamVo` | 切面内部使用的中间对象 |
| `entity/SysAuditLogModel` / `SysAuditLogVo` / `SysAuditDetailLogVo` / `SysMonitorMsgVo` | 提交到下游存储的最终模型 |
| `enums/OperationTypeEnum` | 操作类型（CREATE / UPDATE / DELETE / LOGIN ...） |
| `enums/LogAuditDictTypeEnum` | 字典类型（与 IDictTypeEnum 集成） |
| `enums/LogParamTypeEnum` | 描述参数类型（STRING / CURRENCY / DATE） |
| `support/LogAuditContext` | ThreadLocal LogVo 持有者 |
| `support/AuditLogTool` | 切面内部使用的构造工具 |
| `support/MonitorMsgTool` | 监控消息构造工具（StackWalker 取调用源） |
| `support/IAuditLogDetailDescriptionFormatter` | 描述格式化 SPI（业务侧扩展点） |
| `support/DefaultAuditLogDetailDescriptionFormatter` | 默认实现（返回空串——用作"业务侧没自定义"标记） |
| `support/ILogSourceTenantProvider` | 跨租户审计的"来源租户"解析 SPI |
| `support/ISysAuditModule` | 模块码 → (id, name) 解析 SPI |
| `support/ILogVo` | LogVo 标记接口 |
| `starter/LogAuditCommonConfiguration` | 切面 bean 装配 |

## 配置示例

本模块**无自身 yml**——所有装配通过 `@Component` / `LogAuditCommonConfiguration` 自动注入。
下游存储模块（RDB / MQ）各自有自己的 yml。

## 测试覆盖

- **暂无测试**——24 个源文件全部依赖 Spring AOP + ThreadLocal + SpringKit，单测成本高、
  端到端测试在下游 RDB / MQ 模块里间接覆盖

## 已知限制 / 后续工作

- ❗ **无单元测试**——核心切面 / context / tool 没有专门测试，回归风险靠下游模块的集成测试间接
  兜底。`AuditLogTool.descriptionFormatter` 比较 bug 能潜伏这么久就是缺测的反映
- ❗ `AuditLogTool` 是 `object`，`tenantProvider` 在静态 `init` 块里调 `SpringKit.getBean()`
  ——如果 SpringKit 在类加载时还没初始化，`tenantProvider` 永远为 null（无 retry）。
  当前实现 `try { ... } catch (_: Exception) { null }`，吞 exception 后无重试机制
- ❗ `LogAuditContext.get()` 的"没有时自动创建"语义对线程池场景不友好——业务侧必须配合
  `clear()`。已有 `getOrNull()` 作为只读侦测的替代品
- ❗ `LogAuditAspect.before` 只看 `joinPoint.args[0]`——多参数业务方法只能从第一个参数取
  entityId，其余参数的修改不会触发 oldBizData 加载
- ❗ `LogAuditAspect.after` 在 service 方法**抛异常时也会走**（`@After` 而非 `@AfterReturning`），
  失败操作也会留下审计记录。需要区分时应换 `@AfterReturning` + `@AfterThrowing`
- ❗ `LogAuditCommonConfiguration` 类没标 `@Configuration`——靠下游 `@ComponentScan` 包到。
  本模块自身没有 auto-configuration entry，集成方需要自己 import 这个 class 或开启
  对应 package 的 component scan
- ❗ `BaseLog.SEPERATOR = "┼"` 在 stringParams 拼接时使用——如果业务参数内容本身含 `┼`
  反向解析会错；目前没有转义机制
- ❗ `BaseLog.initModule` 用 `SpringKit.getBeansOfType<ISysAuditModule>()` 然后 `values.first()`
  ——多个 ISysAuditModule 实现时**只用第一个**，没有合并 / 优先级机制
- ❗ Web controller 的 multipart 请求被 WebLogAuditAspect 直接跳过——文件上传场景没有
  审计记录。需要时可自定义 Aspect 用 `request.getParameter` 抓 metadata 但跳过 body

## 依赖

```kotlin
api(project(":kudos-context"))
api(libs.spring.web)
api(libs.servlet.api)
```
