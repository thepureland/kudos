package io.kudos.ms.msg.core.send.channel.email

import org.springframework.boot.context.properties.ConfigurationProperties


/**
 * SMTP configuration for the Email channel.
 *
 * **Current implementation is single-tenant single-SMTP**: all tenants share the same sender account.
 * soul-ms-msg's multi-NoticeEmailInterface (one config per tenant / per business) belongs to Batch 4+ —
 * for now, yml configuration satisfies the first usable end-to-end send loop.
 *
 * Usage (application.yml):
 * ```
 * kudos.msg.email:
 *   server-host: smtp.qq.com
 *   server-port: 465
 *   sender-account: notify@your.app
 *   sender-password: xxx-app-password
 *   from-mail-address: notify@your.app
 *   ssl: true
 * ```
 *
 * @author K
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "kudos.msg.email")
data class MsgEmailProperties(

    /** SMTP server address; the listener refuses to send if not configured */
    val serverHost: String? = null,

    /** SMTP port (SSL is usually 465, STARTTLS is usually 587) */
    val serverPort: Int? = null,

    /** Sender account */
    val senderAccount: String? = null,

    /** Sender password / app token */
    val senderPassword: String? = null,

    /** From display address; defaults to senderAccount if not set */
    val fromMailAddress: String? = null,

    /** Whether SSL is enabled (false uses STARTTLS) */
    val ssl: Boolean = true,

    /** Protocol; usually does not need to be changed */
    val protocol: String = "smtp",

    /** Whether SMTP authentication is enabled */
    val smtpAuth: Boolean = true,

    /** Encoding */
    val encoding: String = "UTF-8",

    /** HTML format */
    val htmlFormat: Boolean = true,

    /** Still attempt to deliver to valid recipients when some recipient addresses are invalid */
    val sendpartial: Boolean = true,
)
