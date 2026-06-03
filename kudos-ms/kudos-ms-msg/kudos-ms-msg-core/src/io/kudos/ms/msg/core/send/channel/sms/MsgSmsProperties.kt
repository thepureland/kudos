package io.kudos.ms.msg.core.send.channel.sms

import org.springframework.boot.context.properties.ConfigurationProperties


/**
 * AWS SNS credentials for the SMS channel.
 *
 * **Current implementation is single-tenant single-account**: all tenants share one AWS IAM key.
 * Per-tenant credentials (soul-style multi-account) belong to a later batch; for now yml config
 * satisfies the first usable end-to-end SMS loop.
 *
 * Usage (application.yml):
 * ```
 * kudos.msg.sms.aws:
 *   region: ap-southeast-1
 *   access-key-id: AKIA...
 *   access-key-secret: xxx
 * ```
 *
 * The SMS dispatch listener only registers once `access-key-id` is present.
 *
 * @author K
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "kudos.msg.sms.aws")
data class MsgSmsProperties(

    /** AWS region, e.g. `ap-southeast-1`; the listener refuses to send if not configured. */
    val region: String? = null,

    /** AWS IAM access key id. */
    val accessKeyId: String? = null,

    /** AWS IAM access key secret. */
    val accessKeySecret: String? = null,
)
