package io.kudos.ability.comm.email.handler

import io.kudos.ability.comm.email.enums.EmailStatusEnum
import io.kudos.ability.comm.email.model.EmailCallBackParam
import io.kudos.ability.comm.email.model.EmailRequest
import io.kudos.base.logger.LogFactory
import jakarta.mail.Address
import jakarta.mail.SendFailedException
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import java.util.Properties


/**
 * 邮件发送处理器。
 *
 * 单一职责：把 [EmailRequest] 翻译成 JavaMail 的 `MimeMessage` + `JavaMailSenderImpl` 并执行发送，
 * 不论成功 / 部分成功 / 失败都通过 [callback] 异步通知调用方（业务侧典型用法是写"邮件发送记录"表）。
 *
 * 发送在虚拟线程上跑——避免阻塞调用线程；要求 JDK 21+。
 *
 * **已知限制**：每次发送都现建 `JavaMailSenderImpl`，SMTP 连接不复用；高并发场景需要业务方
 * 自行 pool 化或换专业邮件队列。
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
@Component
class EmailHandler {
    /**
     * 异步发送邮件——立即在虚拟线程上启动 [doSend]，本方法不阻塞。
     *
     * @param emailRequest 邮件请求；缺关键字段（账号 / 密码 / 服务器 / 收件人）会在内部
     *   `checkParams` 拦截，回调拿到 [EmailStatusEnum.FAIL]
     * @param callback 发送结束后的回调（成功 / 部分成功 / 失败都会调），在虚拟线程上执行，
     *   业务侧 callback 中**不要做长耗时同步操作**，会拖住虚拟线程的复用
     */
    fun send(emailRequest: EmailRequest, callback: (EmailCallBackParam) -> Unit) {
        Thread.ofVirtual().start { doSend(emailRequest, callback) }
    }

    private fun doSend(emailRequest: EmailRequest, callback: (EmailCallBackParam) -> Unit) {
        val emailCallBackParam = EmailCallBackParam()
        try {
            //验证参数
            if (!checkParams(emailRequest)) {
                emailCallBackParam.status = EmailStatusEnum.FAIL
                emailCallBackParam.failEmails = emailRequest.receivers
                return
            }

            val sender = JavaMailSenderImpl()

            //设置邮件服务器信息
            sender.host = emailRequest.serverHost
            sender.port = requireNotNull(emailRequest.serverPort) { "serverPort is required" }
            sender.username = emailRequest.senderAccount
            sender.password = emailRequest.senderPassword
            sender.protocol = emailRequest.protocol
            sender.defaultEncoding = emailRequest.encoding
            val extra = mutableMapOf<String, String?>()
            extra["mail.transport.protocol"] = emailRequest.protocol
            extra["mail.smtp.auth"] = emailRequest.smtpAuth.toString()
            if (emailRequest.ssl) {
                extra["mail.smtp.ssl.enable"] = "true"
            } else {
                extra["mail.smtp.starttls.enable"] = "true"
            }
            extra["mail.user"] = emailRequest.senderAccount
            // 注：不再把 senderPassword 塞到 mail.password —— sender.password 已经设了，
            // 重复一份会让密码长期留在 javaMailProperties 里，被 toString / actuator 等意外泄漏。
            extra["mail.smtp.host"] = emailRequest.serverHost
            extra["mail.smtp.sendpartial"] = emailRequest.sendpartial.toString()
            // SMTP 三个超时：缺省 JavaMail 无限等待。这里给保守默认（10s / 30s / 30s），
            // 避免 SMTP 服务器吊死虚拟线程；业务侧可通过 emailRequest.extra 覆盖。
            extra["mail.smtp.connectiontimeout"] = DEFAULT_CONNECT_TIMEOUT_MS.toString()
            extra["mail.smtp.timeout"] = DEFAULT_READ_TIMEOUT_MS.toString()
            extra["mail.smtp.writetimeout"] = DEFAULT_WRITE_TIMEOUT_MS.toString()
            // SSL 通道对应的 socket 属性别名（不同 JavaMail 版本对哪个键有效不一致，两边都设）
            extra["mail.smtps.connectiontimeout"] = DEFAULT_CONNECT_TIMEOUT_MS.toString()
            extra["mail.smtps.timeout"] = DEFAULT_READ_TIMEOUT_MS.toString()
            extra["mail.smtps.writetimeout"] = DEFAULT_WRITE_TIMEOUT_MS.toString()
            emailRequest.extra?.takeIf { it.isNotEmpty() }?.let { extra.putAll(it) }
            val properties = Properties()
            properties.putAll(extra)
            sender.javaMailProperties = properties

            //建立邮件讯息
            val mailMessage: MimeMessage = sender.createMimeMessage()
            val messageHelper = MimeMessageHelper(mailMessage)

            //设置收件人、寄件人、主题与正文
            val receivers = emailRequest.receivers.toTypedArray()
            messageHelper.setTo(receivers)
            val from = emailRequest.fromMailAddress?.takeIf { it.isNotBlank() }
                ?: requireNotNull(emailRequest.senderAccount) { "senderAccount is required" }
            messageHelper.setFrom(from)
            messageHelper.setSubject(requireNotNull(emailRequest.subject) { "subject is required" })
            messageHelper.setText(requireNotNull(emailRequest.body) { "body is required" }, emailRequest.htmlFormat)

            //发送邮件
            log.info("开始发送邮件...")
            sender.send(mailMessage)

            //设置回调状态
            emailCallBackParam.status = EmailStatusEnum.SUCCESS
            emailCallBackParam.successEmails = emailRequest.receivers

            log.info("发送邮件成功")
        } catch (e: MailSendException) {
            log.error(e, "邮件发送出错,可能只是部分邮箱帐号不可用导致的,这种情况下是部分发送失败,部分发送成功")

            //设置回调状态
            for (messageException in e.messageExceptions) {
                if (messageException is SendFailedException) {
                    log.error(
                        "邮件发送情况,成功:{0},失败:{1},非法:{2}",
                        messageException.validSentAddresses.contentToString(),
                        messageException.validUnsentAddresses.contentToString(),
                        messageException.invalidAddresses.contentToString()
                    )
                    val receiveSet = mutableSetOf<String>()
                    val unreceivedSet = mutableSetOf<String>()
                    putAddressToSet(receiveSet, messageException.validSentAddresses)
                    putAddressToSet(unreceivedSet, messageException.validUnsentAddresses)
                    putAddressToSet(unreceivedSet, messageException.invalidAddresses)
                    emailCallBackParam.successEmails = receiveSet
                    emailCallBackParam.failEmails = unreceivedSet
                    if (receiveSet.isNotEmpty() && unreceivedSet.isNotEmpty()) {
                        emailCallBackParam.status = EmailStatusEnum.SUCCESS_PART
                    } else {
                        emailCallBackParam.status = EmailStatusEnum.FAIL
                    }
                    break
                }
            }
            if (emailCallBackParam.status == null) {
                emailCallBackParam.status = EmailStatusEnum.FAIL
                emailCallBackParam.failEmails = emailRequest.receivers
            }
        } catch (e: Exception) {
            log.error(e, "邮件发送出错")

            //设置回调状态
            emailCallBackParam.status = EmailStatusEnum.FAIL
            emailCallBackParam.failEmails = emailRequest.receivers
        } finally {
            //执行回调
            callback.invoke(emailCallBackParam)
        }
    }

    /**
     * 验证参数
     *
     * @param mail
     */
    private fun checkParams(mail: EmailRequest): Boolean {
        val valid = !mail.senderAccount.isNullOrBlank() && !mail.senderPassword.isNullOrBlank() &&
            !mail.serverHost.isNullOrBlank() && mail.receivers.isNotEmpty()
        if (!valid) log.error("发送者邮箱账号、发送者邮箱密码、发件人邮箱服务器地址、接收者邮箱账号之一为空！")
        return valid
    }

    private fun putAddressToSet(set: MutableSet<String>, addresses: Array<Address>) {
        addresses.mapTo(set) { (it as InternetAddress).address }
    }

    private val log = LogFactory.getLog(this::class)

    companion object {
        /** SMTP 建连超时（毫秒）。JavaMail 默认无限等待，必须显式设上限避免吊死虚拟线程。 */
        private const val DEFAULT_CONNECT_TIMEOUT_MS = 10_000L

        /** SMTP 读超时（毫秒）。 */
        private const val DEFAULT_READ_TIMEOUT_MS = 30_000L

        /** SMTP 写超时（毫秒）；大附件场景可适当调大。 */
        private const val DEFAULT_WRITE_TIMEOUT_MS = 30_000L
    }

}
