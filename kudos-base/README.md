# kudos-base

**定位**：kudos 工程的**最底层基础库**。无 Spring / 无应用框架依赖，Kotlin-first，公共 API 对 Java 调用方兼容。

**在工程中的角色**：所有上层模块（`kudos-ability` / `kudos-context` / `kudos-ms` / `kudos-tools`）的**最小公共依赖**。这里**不出现**业务领域类型——只提供语言层工具、数据契约、校验框架、查询构造、序列化、异常体系等通用基础设施。

---

## 子系统索引

按职责分组（不是 Java 包路径，是逻辑分组）。每组列出主要包 / 关键类。

### 数据契约与响应

| 子系统 | 关键类型 | 说明 |
|---|---|---|
| `model.response` | [`ApiResponse`](src/io/kudos/base/model/response/ApiResponse.kt), [`ErrorDetail`](src/io/kudos/base/model/response/ErrorDetail.kt) | 统一 API 响应；sealed `Success` / `Failure` 让消费端 `when` 强制穷尽 |
| `model.payload` | `PagingSearchPayload` 等 | 请求/查询载体 |
| `model.contract` | `IIdEntity` / `IAuditable` 等接口 | 跨模块的领域契约接口 |
| `model.vo` | 通用 VO | |
| `enums.ienums` | `IErrorCodeEnum`, `IDictEnum`, `IModuleEnum` | 错误码 / 字典 / 模块枚举的统一接口 |
| `enums.impl` | `CommonErrorCodeEnum` 等 | 公共错误码默认实现 |

### 异常体系

| 子系统 | 关键类型 | 说明 |
|---|---|---|
| `error` | [`CustomRuntimeException`](src/io/kudos/base/error/CustomRuntimeException.kt), [`ServiceException`](src/io/kudos/base/error/ServiceException.kt) | 业务异常基类；支持 `IErrorCodeEnum` 错误码 + `MessageFormat` 参数化消息；精简模式默认保留 20 帧业务栈 |

### Bean 校验框架

| 子系统 | 关键类型 | 说明 |
|---|---|---|
| `bean.validation.support` | [`ValidatorFactory`](src/io/kudos/base/bean/validation/support/ValidatorFactory.kt), [`ValidationContext`](src/io/kudos/base/bean/validation/support/ValidationContext.kt) | 注解→验证器分发表 + 实例缓存；HV initCtx 适配 |
| `bean.validation.kit` | `ValidationKit` | 应用层入口（`validateBean` 等） |
| `bean.validation.constraint.annotations` | `@Compare`, `@Constraints`, `@Custom`, `@DictItemCode`, `@AtLeast`, `@DateTime`, `@Series`, `@NotNullOn` 等 | Kudos 自定义约束注解 |
| `bean.validation.constraint.validator` | 各约束对应的 `ConstraintValidator` | |

校验框架的更多设计要点见下文 **关键设计约定**。

### 序列化

| 子系统 | 关键类型 | 说明 |
|---|---|---|
| `data.json` | [`JsonKit`](src/io/kudos/base/data/json/JsonKit.kt) (门面), [`JsonAdapters`](src/io/kudos/base/data/json/JsonAdapters.kt) (Json 实例 + 时间序列化器), [`JsonFallbackEncoder`](src/io/kudos/base/data/json/JsonFallbackEncoder.kt) (反射兜底) | 基于 kotlinx.serialization，三文件分职责 |
| `data.xml` | JAXB 适配 | |
| `data.excel` | jxl 封装 | |

### 查询构造

| 子系统 | 关键类型 | 说明 |
|---|---|---|
| `query` | [`Criteria`](src/io/kudos/base/query/Criteria.kt), [`Criterion`](src/io/kudos/base/query/Criterion.kt), `CriterionDsl` | AND/OR 任意嵌套；smart-filter 空值；`getCriterionGroups()` 返回 unmodifiable view |
| `query.enums` | [`OperatorEnum`](src/io/kudos/base/query/enums/OperatorEnum.kt) | 28 个操作符（EQ / GT / LIKE / IN / BETWEEN / IS_NULL / ...）+ 内存比较语义 |
| `query.sort` | `Sort`, `Order`, `DirectionEnum` | |

### 树形结构

| 子系统 | 关键类型 | 说明 |
|---|---|---|
| `tree` | [`TreeKit`](src/io/kudos/base/tree/TreeKit.kt), [`ListToTreeConverter`](src/io/kudos/base/tree/ListToTreeConverter.kt), `ITreeNode`, `IdAndNameTreeNode` | 扁平列表→树；可选 `strict` 模式拒绝孤儿节点 |

### 语言 / 平台工具

| 子系统 | 关键类型 |
|---|---|
| `lang` | `StringKit`, `ArrayKit`, `EnumKit`, `lang.reflect`, `lang.math`, `lang.collections`（`Registry`, `KeyLockRegistry`, `BlockingHashMap`） |
| `time` | `TimeKit`, `LocalDateKit` 等 |
| `io` | 文件、路径、ClassPath 扫描 |
| `net` | IP、HTTP、FTP |
| `security` | `CryptoKit`, `DigestKit`, `GoogleAuthenticator` |
| `image` | 二维码、SVG |
| `cn` | 中国身份证校验等 |

### 运行时支持

| 子系统 | 关键类型 | 说明 |
|---|---|---|
| `support` | [`Registry`](src/io/kudos/base/support/Registry.kt), [`KeyLockRegistry`](src/io/kudos/base/support/KeyLockRegistry.kt), `GroupExecutor`, `ICallback`, `support.dao`, `support.service` | 通用注册器、按 key 锁、回调接口、DAO/Service 基类 |

### 日志 / 国际化 / 注解

| 子系统 | 关键类型 |
|---|---|
| `logger` | `LogFactory`（适配 slf4j） |
| `i18n` | 多语言资源工具 |
| `annotations` | `@IgnoreApiResponseWrap` 等 |

---

## 关键设计约定

理解这几条能省下读源码的大量时间：

### 1. 校验框架：注解→验证器走注册表

`ValidatorFactory.BUILDERS` 是 `Map<KClass<out Annotation>, ValidatorBuilder>`。新增一种约束注解 = 在注册表加一行，不需要修改任何 `when` 大表达式。

数值型 / 日期型 / 集合型注解共享 `numericBound` / `dateBound` / `sizeBound` 三个分发模板，按 value 运行时类型选具体 validator。

校验器实例按 `(annotation, value.javaClass)` 缓存——JDK Annotation 内容相等契约保证两个相等的 `@Min(10)` 命中同一缓存项。`ConstraintsValidator` 进一步保证每个 validator 实例只 initialize 一次。

### 2. JSON 三段式：门面 / 反射兜底 / 引擎

- **JsonKit**：对外 API（`toJson` / `fromJson` / `readValue` / `writeValueAsBytes`）。
- **JsonFallbackEncoder**：当类没有 `@Serializable` 时，反射递归转 `JsonElement`（支持 data class / Map / Collection / 各种原始数组 / Java Bean）。
- **JsonAdapters**：内部的两个 `Json` 实例（`defaultJson` 与 `preserveJson`）+ 时间序列化器 + 用户注册的多态 `SerializersModule`。

多态：sealed 层级自动工作；open 层级用 `JsonKit.registerSerializersModule(...)` 在应用启动期注册。

### 3. 异常 + 错误码：枚举驱动

- `IErrorCodeEnum`：`code` + `defaultDisplayText` + `i18nKeyPrefix`，displayText 默认按是否配 i18nKey 决定走文案还是 i18n key。
- `ServiceException(errorCode, ...args)` 用 `MessageFormat` 将 args 套到 `displayText` 占位符（如 `"用户{0}操作失败"`）。
- 精简模式（默认）保留 JVM 捕获堆栈的前 **20** 帧业务栈；`printAllStackTrace=true` 保留完整堆栈。

### 4. ApiResponse：sealed 而非 data class

```kotlin
when (response) {
    is ApiResponse.Success -> response.data    // 只 Success 才有 data
    is ApiResponse.Failure -> response.errors  // 只 Failure 才有 errors
}
```

`when` 不带 `else` 编译通过=穷尽性已检查。`Failure : ApiResponse<Nothing>`，借助 `out T` 协变可赋给任意 `ApiResponse<T>`。

JSON 输出形态与旧 data class 版本兼容（`explicitNulls=false` 配合下：Success 不出 errors 字段、Failure 不出 data 字段）。

### 5. Criterion：全 val

所有字段（`property` / `operator` / `value` / `alias` / `encrypt`）都是 `val`，且 `encrypt` 在主构造器内——`equals` / `hashCode` / `copy` 都对齐。可安全用作 HashMap key。

要"改"字段就 `criterion.copy(value = newValue)`。

### 6. Criteria：内部结构

`criterionGroups: List<Any>` 的元素类型可能是：
- `Criterion` —— 单条件（外层 AND）
- `Criteria` —— 嵌套查询（外层 AND）
- `Array<*>` —— OR 组，数组内元素 OR、数组与外部 AND

`getCriterionGroups()` 返回 unmodifiable view，下转型 + mutate 会抛 `UnsupportedOperationException`——这是有意的封装防御。

### 7. 锁与注册：并发原语统一

`Registry`、`KeyLockRegistry`、`ConstraintsValidator` 的内部缓存等并发 hot path 一律用 `ConcurrentHashMap.computeIfAbsent` 而非 `synchronized(map)`，保证 per-key 锁粒度。

---

## 已知约束与陷阱

> 写在这里避免踩坑。每条都有相应的 KNOWN BEHAVIOR 测试钉住，方便日后想"修复"时先看代价。

1. **`ApiResponse.success("hello")`** 单参歧义：Kotlin 解析到 `success(message, data)` overload，把 `"hello"` 绑到 `message`。要传 data 请用 named arg：`success(data = "hello")`。
2. **`JsonKit.unwrap()` 类型探测顺序**：JSON 中 `"100"` 在动态/脚本场景下会被识别为 `Long`，不是 `String`。类型敏感场景请走 `readValue<T>()`。
3. **`Criteria` 纯空白字符串 `"   "` 是有效值**：用的是 `isNotEmpty` 不是 `isNotBlank`。
   空集合、空 `Map`、空对象数组和空原始类型数组都会被过滤。
4. **`OperatorEnum.BETWEEN` 只识别 `ClosedFloatingPointRange<*>`**：`5 in 1..10`（IntRange）不被识别；要写 `1.0..10.0`。
5. **`OperatorEnum.*_P`（属性间比较）** 在内存 `compare()` 中一律返回 false——这些操作符由 SQL 生成器处理，不在内存里。
6. **`BigDecimal.equals` 区分 scale**：`EQ.compare(BigDecimal("1.0"), BigDecimal("1.00"))` 是 false。
7. **`ValidationContext` 依赖 HV internal API**：用反射读取 HV 的 `ValidatorFactoryScopedContext` 与 `ConstraintValidatorContext.constraintDescriptor`，缓存了反射结果。HV 大版本升级时若上述类/方法/字段被改名，会在初次校验时触发明确的错误提示。

---

## 测试约定

- 测试目录：`test-src/`，与 `src/` 包结构一一对应。
- 框架：JUnit 5 + `kotlin.test`。
- 1100+ 测试用例（截至 2026-05），核心子系统（query / response / 校验框架 / tree / error）都做了深度覆盖（含 KNOWN BEHAVIOR pin-down 测试）。
- 跑全套：`./gradlew :kudos-base:test`。
- 凡是涉及 HV 校验器初始化的测试，**必须**先经过 `ValidationKit.validateBean(...)` 或 `ValidationKit.getValidator()` 触发 HV factory 构建，否则 `ValidationContext.getHvInitCtx()` 会抛错。
- 校验框架的 `ValidatorFactory.clearCacheForTest()` 提供测试间隔离能力（生产代码请勿调用）。

---

## 何时改 kudos-base

`kudos-base` 是工程最底层、所有模块都依赖。每次修改影响面大，请遵守：

1. **不引入业务领域类型**——业务模型属于 `kudos-ms` 各微服务。
2. **不引入 Spring / Spring Boot 依赖**——这一层要能在脱离 Spring 的场景下使用（如纯 SDK、测试夹具）。
3. **不破坏现有公共 API**——`@PublishedApi internal` 的内部细节可重构，公开 `fun`/`val` 改签名需配合下游审计（grep 全工程 caller）。
4. **新增公开类必须配测试**——本模块测试覆盖是工程基线。

---

## 改进建议（自动分析 2026-06-11）

> 本次审查已直接修复：XmlKit XXE 加固与 JAXBContext 缓存失效、HttpClientKit.downloadToDir 文件名路径穿越、RandomStringKit.randomLong 的 `abs(Long.MIN_VALUE)` 负数边界、I18nKit 回退填充共享可变 Map 引用、CryptoKit 正则替换前缀改 removePrefix、两处冗余自引用 import。以下为**未直接修改**的待办，按维度分类。

### 安全性

1. `src/io/kudos/base/lang/SerializationKit.kt` — `deserialize` 直接包装 commons-lang3 的 Java 原生反序列化，无 `ObjectInputFilter` 白名单。对不可信输入存在 gadget 链 RCE 风险。建议：KDoc 加显著警告，并提供带 `ObjectInputFilter` 的安全重载。
2. `src/io/kudos/base/security/CryptoKit.kt` — 单参 `aesEncrypt(input)` 使用硬编码默认密钥 `CryptoKey.KEY_DEFAULT`，任何持有 jar 的人都可解密此类密文。建议支持从环境变量/外部配置注入默认密钥，硬编码仅作最后回退。
3. `src/io/kudos/base/security/DigestKit.kt` — 仅支持 MD5/SHA-1（均已不抗碰撞）；`isMatchMD5` 为非常量时间比较，且对空盐有静默回退。建议：KDoc 标注"仅限遗留数据校验，密码存储请用 PasswordKit"；比较改用 `MessageDigest.isEqual`。
4. `src/io/kudos/base/security/Base36Kit.kt` — 自制移位/洗牌算法本质是可逆混淆而非加密（密钥空间极小、默认 KEY 硬编码）。建议在类 KDoc 顶部显式声明"不可用于敏感数据保护"。
5. `src/io/kudos/base/security/GoogleAuthenticator.kt` — `generateSecretKey` 的"常量 SEED + SHA1PRNG.setSeed"写法易被静态扫描误报且具误导性（实际 `generateSeed` 走系统熵源，无真实漏洞）。建议简化为 `SecureRandom().nextBytes(...)`；`getQRBarcodeURL` 指向已停服的 Google Chart API，应改为生成 `otpauth://` URI 由本地 QrCodeKit 渲染。
6. `src/io/kudos/base/net/http/HttpClientKit.kt` — `download`/`asyncDownload` 在 resume 模式下对所有 2xx 都执行 APPEND，若服务器忽略 Range 返回 200 全量内容会产生重复拼接的脏文件。建议校验 206 与 `Content-Range` 后再决定 APPEND/TRUNCATE。

### 功能缺陷

7. `src/io/kudos/base/lang/SystemKit.kt` — `executeCommand` 不调用 `waitFor()`，**不论命令退出码如何都返回 success=true**；stderr 非空时丢弃 stdout。建议改返回 `(exitCode, stdout, stderr)` 三元组（需下游审计，属 API 行为变更）。
8. `src/io/kudos/base/io/FileKit.kt` — `zip` 只支持单文件且失败吞异常返回 null（与模块"抛异常"风格不一致）；zip4j 已在依赖中但无 `unzip` 对应方法。
9. `src/io/kudos/base/lang/SystemKit.kt` — `setEnvVars` 反射改 JDK 内部字段，JDK 17+ 需 `--add-opens java.base/java.lang=ALL-UNNAMED`，两条路径都失败时**静默不生效**。建议失败时至少 log.warn。

### 可扩展性

10. `src/io/kudos/base/logger/LogFactory.kt` — `logCreator` 硬编码 `Slf4jLoggerCreator`，`ILogCreator` 接口形同虚设。建议走 `ServiceLoader` SPI 或提供注册入口。
11. `src/io/kudos/base/bean/validation/terminal/convert/ConstraintConvertorFactory.kt` — `when` 大表达式分发，新增约束注解必须改此文件；与 `ValidatorFactory.BUILDERS` 的注册表风格不一致，建议统一为注册表。
12. `src/io/kudos/base/i18n/I18nKit.kt` — 资源根路径 `i18n/` 与文件名约定（模块名_语言_国家）写死；初始化后的 Map 读写无并发保护（运行期热更新语言包会有可见性问题）。
13. `src/io/kudos/base/net/http/HttpClientKit.kt` — 每次请求 `HttpClient.newBuilder().build()`，无连接池复用；`createHttpClient` KDoc 称"为注入预留"但 object 单例无注入点。建议缓存共享默认 client 或开放 client 工厂注册。

### 可观测性

14. `src/io/kudos/base/net/http/HttpClientKit.kt` — 全文件无任何日志：请求失败、下载重命名失败、resume 命中与否均不可观测。建议在 download 成败路径补 debug/warn 日志。
15. `src/io/kudos/base/lang/SystemKit.kt` — `executeCommand` 仅在进程启动失败时记日志，命令非零退出无任何记录。

### 可维护性

16. `src/io/kudos/base/security/Base36Kit.kt` — 循环内字符串 `+=` 拼接 O(n²)、手写冒泡排序、大量魔法值与 2016 年修订史注释。建议以现有测试钉住行为后用 `sortedBy`/`buildString` 重写。
17. `src/io/kudos/base/net/http/HttpClientKit.kt` — `download` 与 `asyncDownload` 约 80 行近乎完全重复，仅最后一步 send 不同；建议抽取公共私有函数。另 `createHttpRequest` 为无调用方的死代码。
18. `src/io/kudos/base/security/GoogleAuthenticator.kt` — `hash[20 - 1]` 魔法写法应为 `hash[hash.size - 1]`（HmacSHA1 恒 20 字节，但表达意图不清）。
19. `src/io/kudos/base/i18n/I18nKit.kt` — `getModuleAndLocale` 用 `length - 6` / `right(5)` 魔法数解析文件名，命名不规范的文件会抛出难定位的 StringIndexOutOfBounds；建议改正则并给出含文件名的错误信息。

### 对外接口（public API）

20. `src/io/kudos/base/i18n/I18nKit.kt` — `initI18nByType(vararg args: String)` 用 vararg 模拟两个可选参数，调用方无法从签名得知参数含义；建议新增 `initI18nByType(type: String, prefix: String = "")` 具名重载并废弃 vararg 版。
21. `src/io/kudos/base/net/http/HttpClientKit.kt` — `__createBodyHandler` 因 inline 需要而 public，双下划线命名暴露在 API 面上；建议改 `@PublishedApi internal` + 常规命名（属 API 变更，需下游审计）。
22. `src/io/kudos/base/data/xml/XmlKit.kt` — `fromXml` 的 `ignoreNameSpace=true` 实际是把 SAX 设为 *namespace-aware*，参数名与行为语义相反，易误用；建议澄清 KDoc 或在大版本时更名。

### 测试覆盖

23. `src/io/kudos/base/support/service/impl/BaseCrudService.kt`、`BaseReadOnlyService.kt` — 无对应测试（接口契约依赖上层模块集成验证，建议至少补桩测试）。
24. `src/io/kudos/base/net/http/HttpClientKit.kt` — 本次新增的 `sanitizeFilename` 路径穿越防护缺针对性测试（恶意 `Content-Disposition: filename=../../x`）；`download` resume 模式下服务器忽略 Range 的场景未覆盖。
25. `src/io/kudos/base/data/xml/XmlKit.kt` — `fromXml(ignoreNameSpace=true)` 分支与 XXE 拒绝行为（DOCTYPE 外部实体应被忽略/拒绝）无测试。
26. `src/io/kudos/base/lang/SystemKit.kt` — `executeCommand` 非零退出码、`setEnvVars` 双路径失败分支未覆盖。

### 文档

27. `README.md` security 行仅列 `CryptoKit, DigestKit, GoogleAuthenticator`，遗漏 `PasswordKit`（推荐的密码哈希入口）与 `Base36Kit`；建议补全并标注各自适用边界。
