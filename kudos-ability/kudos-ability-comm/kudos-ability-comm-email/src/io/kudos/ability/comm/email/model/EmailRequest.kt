package io.kudos.ability.comm.email.model

import java.io.Serial
import java.io.Serializable

/**
 * 邮件发送请求体。
 *
 * 字段语义参见各 var 的内联 kdoc；典型用法是业务层填好后交给 [io.kudos.ability.comm.email.handler.EmailHandler.send]。
 *
 * **不要把本类的实例输出到日志** —— `senderPassword` 是明文，序列化后会带出去。
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
class EmailRequest : Serializable {
    /**
     * 发送邮件的协议
     */
    var protocol: String = "smtp"

    /**
     * 邮件主题
     */
    var subject: String? = null

    /**
     * 邮件正文
     */
    var body: String? = null

    /**
     * 邮件接收人
     */
    var receivers = mutableSetOf<String>()

    /**
     * 发送者邮箱账号
     */
    var senderAccount: String? = null

    /**
     * 发送者邮箱密码
     */
    var senderPassword: String? = null

    /**
     * 发件人所使用的邮箱的邮件服务器地址
     */
    var serverHost: String? = null

    /**
     * 发件人所使用的邮箱的邮件服务器端口
     */
    var serverPort: Int? = null

    /**
     * 是否需要SMTP服务器验证
     */
    var smtpAuth: Boolean = true

    /**
     * 设置是否部分发送(当收件人列表中出现部分收件人地址错误时忽略这些错误地址)默认true
     */
    var sendpartial: Boolean = true

    /**
     * 以HTML格式发送
     */
    var htmlFormat: Boolean = true

    /**
     * 邮件正文编码
     */
    var encoding: String? = "UTF-8"

    /**
     * SSL加密
     */
    var ssl: Boolean = true

    /**
     * 扩展信息
     */
    var extra: MutableMap<String, String>? = null

    /**
     * 显示在邮件头 `From` 中的发件人邮箱地址。
     * 与 [senderAccount] 区分：[senderAccount] 是用于鉴权登录 SMTP 的账号，[fromMailAddress] 是收件方实际看到的来源地址。
     * 留空时大多数 SMTP 服务器会回退到 [senderAccount]。
     */
    var fromMailAddress: String? = null

    companion object {
        /** Serializable 版本号 */
        @Serial
        private val serialVersionUID = -6829180589038163995L
    }
}
