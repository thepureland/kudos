package io.kudos.ability.comm.sms.aws.init.properties

/**
 * AWS SDK forward proxy configuration; corresponds to `kudos.ability.comm.sms.aws.proxy.*`.
 *
 * When enabled, `AwsSmsHandler` constructs a shared
 * [software.amazon.awssdk.http.apache.ApacheHttpClient] during `@PostConstruct` and assigns it to
 * the process-level static field (`HTTP_CLIENT`). **Changing the proxy configuration requires a
 * process restart** - if multi-tenant scenarios need different proxy egress, a separate client
 * factory must be designed.
 *
 * @property enable whether to enable the proxy
 * @property url proxy server URL (`http://proxy.example.com:1234`)
 * @property username proxy authentication username (optional)
 * @property password proxy authentication password (optional, **sensitive** - do not log)
 * @author K
 * @since 1.0.0
 */
class SmsAwsProxyProperties {
    var enable: Boolean = false
    var url: String? = null
    var username: String? = null
    var password: String? = null
}
