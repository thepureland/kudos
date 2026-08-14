# kudos-ability-web-common

Web 子模块共享 base 层：存放 **web 框架无关**的契约与策略，由 `kudos-ability-web-springmvc`
和 `kudos-ability-web-ktor` 共同依赖（并间接传递 `kudos-context`）。

## 设计意图

抽象默认留在具体实现模块里，按"**出现两次以上**"的标准上移到本模块。目标是消除
"同一份规则在两种 runtime 下各写一遍、然后悄悄跑偏"这类问题。

候选清单：
- Web 层常量（HTTP header / cookie 名）
- 请求 / 响应通用 DTO
- 跨 servlet / Ktor / Reactor 的工具方法
- 公共安全约束（如 `MutableListSearchPayload` 拒收类的接口契约）

## 已上移的内容

### `trace/TraceKeys` —— trace key 策略

第一个满足"出现两次"标准并上移的抽象。

| 成员 | 说明 |
|---|---|
| `TRACE_ID_HEADER` | 公开 trace 头名 `X-Trace-Id`：外部调用方与浏览器使用，响应也回写它 |
| `DEFAULT_MAX_LENGTH` | 采信的调用方 trace key 长度上限（128） |
| `isAcceptable(value, maxLength)` | 校验：非空白、不超长、字符集限定为字母数字与 `-_.` |
| `resolve(headerNames, maxLength, lookup)` | 按序取第一个**合法**头值；某个头不合法只跳过，不连累后续；全无则生成 UUID |

**为什么校验是安全需求而非洁癖**：trace key 会被拼进日志行、并回写进响应头。未校验的值可以用
CR/LF 伪造日志条目或做 HTTP 响应头拆分；无长度上限还能撑爆按 traceId 建索引的日志 / 指标后端。

**为什么 `X-Trace-Id` 不放进 `Consts.RequestHeader`**：那个对象是**内部**跨进程头的命名空间，
有 `_` 前缀约定且被 `ConstsTest` 强制。`_UUID`（`Consts.RequestHeader.TRACE_KEY`）属于那里——
它由 Feign 拦截器设置、并被 `FeignContextSignatureVerifier` 纳入请求签名计算。
`X-Trace-Id` 是对外头，归属 web 层。

**上移的由来**：这套规则原本只在 springmvc 侧实现，ktor 的 `KudosContextPlugin` 直接采信
`headers["X-Trace-Id"]`。同一个请求是否安全，取决于恰好由哪种 runtime 处理——这正是本模块要
消除的那类偏差。

两侧的接法：

| runtime | 读取的头 | 配置项 |
|---|---|---|
| springmvc | `traceIdHeader` → `additionalTraceKeyHeaders`（默认 `[_UUID]`） | `kudos.ability.web.springmvc.context.*` |
| ktor | `traceKeyHeaders`（默认 `[X-Trace-Id]`） | `install(KudosContextPlugin) { ... }` |

> ktor 侧默认不读 `_UUID`。若某个 ktor 服务也接收内部 Feign 流量，把
> `Consts.RequestHeader.TRACE_KEY` 追加进 `traceKeyHeaders` 即可延续调用链，无需改代码。

## 模块入口

| 路径 | 角色 |
|---|---|
| `trace/TraceKeys` | trace key 头名、校验与解析策略 |

## 测试覆盖

- `TraceKeysTest` —— 策略本身的唯一真源：合法/非法形态、长度边界、多头优先级、
  坏值跳过、空头名忽略、兜底生成 UUID

> 各 runtime 的测试只验证**接线**（配置项是否生效、头是否按序读），规则本身不再各测一遍——
> 这正是上移的收益。注意 ktor 侧无法驱动 CR/LF 用例：Ktor 自己的 client 在发送前就会拒绝
> 这种头值。那是客户端的礼貌，不是服务端的防御，对不使用 Ktor 的调用方毫无约束力，
> 因此该规则由 `TraceKeysTest` 锁定。

## 已知限制 / 后续工作

- ❗ 响应包装契约（`ApiResponse` 的构造与解包）仍只在 springmvc 侧；ktor 的 StatusPages
  兜底不产出 `ApiResponse`，业务从 springmvc 迁到 ktor 会丢失统一响应结构
- ❗ `ClientInfo` 装配规则（IP / UA / OS 解析）仍只在 springmvc 侧（`XHttpServletRequest`），
  ktor 侧无对应实现
- ❗ ktor 侧不回写 trace 响应头；springmvc 会回写 `X-Trace-Id`

## 依赖

```kotlin
dependencies {
    api(project(":kudos-context"))

    testImplementation(project(":kudos-test:kudos-test-common"))
}
```
