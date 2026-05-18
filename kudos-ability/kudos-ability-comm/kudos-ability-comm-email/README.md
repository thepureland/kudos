# kudos-ability-comm-email

Spring Boot `spring-boot-starter-mail` 之上的"按请求一封一发"邮件发送封装。给业务侧
一个最小化的同步 API（`EmailHandler.send(request, callback)`），内部走虚拟线程异步执行
并通过 callback 把结果送回。

## 设计要点

### 单一职责：发送，不存储

模块只负责"翻译 EmailRequest → JavaMail MimeMessage → 发出去"。**不维护**：
- 邮件发送记录表（业务侧自管）
- 失败重发队列（业务侧 callback 拿到 FAIL/SUCCESS_PART 后自行入队）
- SMTP 凭证存储（每个 `EmailRequest` 自带 `senderAccount` / `senderPassword`）
- 模板渲染（业务侧渲染好放 `body` 字段）

这种"瘦封装"是有意的——邮件发送语义到了具体业务（验证码 / 通知 / 营销）差异巨大，
公共层只做协议层封装，不强行抽象上层。

### 虚拟线程执行 + callback 通知

```kotlin
fun send(emailRequest: EmailRequest, callback: (EmailCallBackParam) -> Unit) {
    Thread.ofVirtual().start { doSend(emailRequest, callback) }
}
```

`send()` 立即返回；实际 SMTP IO 在虚拟线程跑。要求 JDK 21+（项目 toolchain 已强制 21）。

callback 在虚拟线程上执行，业务侧的 callback 实现**不要做长耗时同步操作**——会拖住
虚拟线程的复用调度。要做重活（如写库）应在 callback 中再 dispatch 到自己的执行器。

### 状态机（[EmailStatusEnum]）

源于 JavaMail `SendFailedException` 的细分：

| Status | 触发条件 |
|---|---|
| `SUCCESS` | 全部 receivers 都发出 |
| `SUCCESS_PART` | 有 `validSentAddresses` **且** 有失败地址（`validUnsentAddresses` ∪ `invalidAddresses`） |
| `FAIL` | 没有任何 receivers 成功（参数校验失败 / SMTP 连不上 / 全部地址非法） |

### SMTP 超时（重要）

JavaMail 默认**无限等待**。本模块强制设三个超时：

| 属性 | 默认 | 用途 |
|---|---|---|
| `mail.smtp.connectiontimeout` | 10s | 建连超时 |
| `mail.smtp.timeout` | 30s | 读超时 |
| `mail.smtp.writetimeout` | 30s | 写超时（大附件可调大） |

业务侧可通过 `EmailRequest.extra` 覆盖。`mail.smtps.*`（SSL 别名）也同步设上，因为不同
JavaMail 版本对哪个键有效不一致。

### 密码处理（安全）

旧实现把 `senderPassword` **同时**写到两个地方：
- `JavaMailSenderImpl.password` —— 正常
- `extra["mail.password"]` → `javaMailProperties` —— **多余**，密码会留在 Session 的 Properties 里

JavaMail 实际用的是 `JavaMailSenderImpl.password`，`mail.password` 不被读取。**已移除**
冗余写入。`Properties.toString()` 包含所有 entry，旧代码下密码可能被 actuator endpoint /
debug 日志意外暴露。

`EmailRequest.senderPassword` 本身仍是明文字段——**不要把整个 `EmailRequest` 实例写日志**。

### Partial-Send 行为

`mail.smtp.sendpartial=true`（默认）让 SMTP server 即便看到部分非法地址也继续发送有效
地址；handler 通过 `SendFailedException.validSentAddresses` 区分哪些发了哪些没发。

如果业务"宁可一封都不发也不要部分发"，把 `EmailRequest.sendpartial = false`。

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/EmailAutoConfiguration` | 装配入口（仅一个 `emailHandler` bean） |
| `handler/EmailHandler` | 发送核心：参数校验 + SMTP 配置 + 发送 + 状态回调 |
| `model/EmailRequest` | 请求 POJO |
| `model/EmailCallBackParam` | 回调载体（status + 成功/失败地址集） |
| `enums/EmailStatusEnum` | 三态枚举 |

## 配置示例

本模块**无自身 yml 配置**——SMTP 凭证完全由调用方在 `EmailRequest` 中填写。典型业务侧：

```kotlin
val req = EmailRequest().apply {
    serverHost = "smtp.example.com"
    serverPort = 465
    senderAccount = "noreply@example.com"
    senderPassword = secretsProvider.smtpPassword()  // 从配置中心 / 密钥管理读
    fromMailAddress = "noreply@example.com"
    subject = "您的验证码"
    body = "<h1>123456</h1>"  // 默认 htmlFormat = true
    receivers = mutableSetOf(user.email)
    ssl = true  // 465 用 SSL；587 / 25 用 STARTTLS 时改 false
}
emailHandler.send(req) { result ->
    when (result.status) {
        SUCCESS -> log.info("发送成功")
        SUCCESS_PART -> retryQueue.enqueue(result.failEmails)
        FAIL -> retryQueue.enqueueAll(req.receivers)
        else -> Unit
    }
}
```

## 测试覆盖

- `EmailTest`（2 case）—— SmtpTestContainer 集成测试，覆盖纯文本 + HTML 两种格式

依赖 Docker 跑 testcontainers。

## 已知限制 / 后续工作

- ❗ 每次 `send` 都 new `JavaMailSenderImpl` + 新建 JavaMail `Session`。低 QPS 业务可以接受；
  高频发送（>10 封/秒）应当 pool 化 sender 或换专业邮件队列（如 SendGrid / SES API）
- ❗ 没有重试机制——transient SMTP 错误（5xx 暂时性）直接 FAIL；调用方需自己处理
- ❗ 没有附件支持。`MimeMessageHelper.addAttachment` 在本模块未暴露，需要时自行扩展
- ❗ 没有发件人速率限制——业务侧能无限调，可能触发 SMTP 服务器封号
- ❗ `EmailRequest.senderPassword` 明文存内存；与 `Serializable` 接口的组合让对象被序列化
  到日志 / 缓存时密码外泄。生产场景应当用 secret manager 在调用前临时填充
- ❗ 没有日志脱敏——`log.error(e, "邮件发送出错")` 异常栈可能含 receivers 邮箱地址，
  GDPR / 隐私敏感场景需要业务层包装
- ❗ 模板渲染 / 国际化 / 退订链接等"成熟邮件能力"全部不提供——属于"运营级邮件"，
  通常单独有专业系统对接

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-comm:kudos-ability-comm-common"))
api(libs.spring.boot.starter.mail)

testImplementation(project(":kudos-test:kudos-test-container"))
```

`comm-common` 当前是占位模块（无源码）；保留依赖给将来的共享 SPI 占位。
