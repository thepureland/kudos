package io.kudos.ability.comm.sms.aws.init.properties

/**
 * AWS SDK 正向代理配置；对应 `kudos.ability.comm.sms.aws.proxy.*`。
 *
 * 启用后 `AwsSmsHandler` 在 `@PostConstruct` 阶段构造一个共享的 [software.amazon.awssdk.http.apache.ApacheHttpClient]
 * 并赋给进程级静态字段（`HTTP_CLIENT`）。**代理配置变更需重启进程**——多租户若需不同
 * 代理出口需要另行设计客户端工厂。
 *
 * @property enable 是否启用代理
 * @property url 代理服务器 URL（`http://proxy.example.com:1234`）
 * @property username 代理认证用户名（可选）
 * @property password 代理认证密码（可选，**敏感**不要落日志）
 * @author K
 * @since 1.0.0
 */
class SmsAwsProxyProperties {
    var enable: Boolean = false
    var url: String? = null
    var username: String? = null
    var password: String? = null
}
