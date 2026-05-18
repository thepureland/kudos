package io.kudos.ms.msg.core.send.channel.email

import org.springframework.boot.context.properties.ConfigurationProperties


/**
 * Email 渠道 SMTP 配置。
 *
 * **当前阶段实现是单租户单 SMTP**：所有租户共享同一份发送账号。soul-ms-msg 的多
 * NoticeEmailInterface（每个租户 / 每个业务一个配置）属于 Batch 4+ 的范围 —— 暂用 yml
 * 配置满足首条可用的发送闭环。
 *
 * 用法 (application.yml)：
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

    /** SMTP 服务器地址；未配置时 listener 拒绝发送 */
    val serverHost: String? = null,

    /** SMTP 端口（SSL 通常 465，STARTTLS 通常 587） */
    val serverPort: Int? = null,

    /** 发送者账号 */
    val senderAccount: String? = null,

    /** 发送者密码 / app token */
    val senderPassword: String? = null,

    /** From 显示地址；不填则默认 senderAccount */
    val fromMailAddress: String? = null,

    /** 是否启用 SSL（false 走 STARTTLS） */
    val ssl: Boolean = true,

    /** 协议；通常无需改 */
    val protocol: String = "smtp",

    /** 是否 SMTP 鉴权 */
    val smtpAuth: Boolean = true,

    /** 编码 */
    val encoding: String = "UTF-8",

    /** HTML 格式 */
    val htmlFormat: Boolean = true,

    /** 部分收件人地址错误时仍尽量投递有效者 */
    val sendpartial: Boolean = true,
)
