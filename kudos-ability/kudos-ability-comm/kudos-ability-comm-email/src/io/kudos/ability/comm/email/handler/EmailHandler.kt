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
import java.util.*


/**
 * @Description 邮件发送处理器
 * @Author paul
 * @Date 2023/2/9 11:09
 */
@Component
class EmailHandler {
    /**
     * 执行发送邮件，并处理回调
     *
     * @param emailRequest
     * @param callback
     */
    fun send(emailRequest: EmailRequest, callback: (EmailCallBackParam) -> Unit) {
        Thread.ofVirtual().start { doSend(emailRequest, callback) }
    }

    private fun doSend(emailRequest: EmailRequest, callback: (EmailCallBackParam) -> Unit) {
        val emailCallBackParam = EmailCallBackParam()
        try {
            //验证参数
            if (!checkParams(emailRequest)) {
                emailCallBackParam.setStatus(EmailStatusEnum.FAIL)
                emailCallBackParam.failEmails = emailRequest.receivers
                return
            }

            val sender = JavaMailSenderImpl()

            //设置邮件服务器信息
            sender.host = emailRequest.serverHost
            sender.port = emailRequest.serverPort!!
            sender.username = emailRequest.senderAccount
            sender.password = emailRequest.senderPassword
            sender.protocol = emailRequest.protocol
            sender.defaultEncoding = emailRequest.encoding
            val extra = mutableMapOf<String, String?>()
            extra.put("mail.transport.protocol", emailRequest.protocol)
            extra.put("mail.smtp.auth", emailRequest.smtpAuth.toString())
            if (emailRequest.ssl) {
                extra.put("mail.smtp.ssl.enable", "true")
            } else {
                extra.put("mail.smtp.starttls.enable", "true")
            }
            extra.put("mail.user", emailRequest.senderAccount)
            extra.put("mail.password", emailRequest.senderPassword)
            extra.put("mail.smtp.host", emailRequest.serverHost)
            extra.put("mail.smtp.sendpartial", emailRequest.sendpartial.toString())
            if (!emailRequest.extra.isNullOrEmpty()) {
                extra.putAll(emailRequest.extra!!)
            }
            val properties = Properties()
            properties.putAll(extra)
            sender.javaMailProperties = properties

            //建立邮件讯息
            val mailMessage: MimeMessage = sender.createMimeMessage()
            val messageHelper = MimeMessageHelper(mailMessage)

            //设置收件人、寄件人、主题与正文
            val receivers = emailRequest.receivers.toTypedArray()
            messageHelper.setTo(receivers)
            if (!emailRequest.fromMailAddress.isNullOrBlank()) {
                messageHelper.setFrom(emailRequest.fromMailAddress!!)
            } else {
                messageHelper.setFrom(emailRequest.senderAccount!!)
            }

            messageHelper.setSubject(emailRequest.subject!!)
            messageHelper.setText(emailRequest.body!!, emailRequest.htmlFormat)

            //发送邮件
            LOG.info("开始发送邮件...")
            sender.send(mailMessage)

            //设置回调状态
            emailCallBackParam.setStatus(EmailStatusEnum.SUCCESS)
            emailCallBackParam.successEmails = emailRequest.receivers

            LOG.info("发送邮件成功")
        } catch (e: MailSendException) {
            LOG.error(e, "邮件发送出错,可能只是部分邮箱帐号不可用导致的,这种情况下是部分发送失败,部分发送成功")

            //设置回调状态
            for (messageException in e.messageExceptions) {
                if (messageException is SendFailedException) {
                    LOG.error(
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
                    if (emailCallBackParam.successEmails!!.isNotEmpty() && emailCallBackParam.failEmails!!.isNotEmpty()) {
                        emailCallBackParam.setStatus(EmailStatusEnum.SUCCESS_PART)
                    } else {
                        emailCallBackParam.setStatus(EmailStatusEnum.FAIL)
                    }
                    break
                }
            }
            if (emailCallBackParam.getStatus() == null) {
                emailCallBackParam.setStatus(EmailStatusEnum.FAIL)
                emailCallBackParam.failEmails = emailRequest.receivers
            }
        } catch (e: Exception) {
            LOG.error(e, "邮件发送出错")

            //设置回调状态
            emailCallBackParam.setStatus(EmailStatusEnum.FAIL)
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
        if (mail.senderAccount.isNullOrBlank() || mail.senderPassword.isNullOrBlank() ||
            mail.serverHost.isNullOrBlank() || mail.receivers.isEmpty()
        ) {
            LOG.error("发送者邮箱账号、发送者邮箱密码、发件人邮箱服务器地址、接收者邮箱账号之一为空！")
            return false
        }
        return true
    }

    private fun putAddressToSet(set: MutableSet<String>, addresses: Array<Address>) {
        if (addresses.isNotEmpty()) {
            for (add in addresses) {
                val address = (add as InternetAddress).getAddress()
                set.add(address)
            }
        }
    }

    private val LOG = LogFactory.getLog(this)

}
