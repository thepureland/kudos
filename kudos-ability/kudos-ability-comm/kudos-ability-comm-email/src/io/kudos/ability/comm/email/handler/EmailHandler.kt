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
 * Email send handler.
 *
 * Single responsibility: translate [EmailRequest] into JavaMail's `MimeMessage` + `JavaMailSenderImpl`
 * and execute the send. Regardless of success / partial success / failure, the caller is notified
 * asynchronously via [callback] (a typical business usage is writing to an "email send record" table).
 *
 * The send runs on a virtual thread to avoid blocking the calling thread; requires JDK 21+.
 *
 * **Known limitation**: each send creates a fresh `JavaMailSenderImpl`, so SMTP connections are not
 * reused. High-concurrency scenarios require the business side to pool them or switch to a dedicated
 * email queue.
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
@Component
class EmailHandler {
    /**
     * Asynchronously send an email - immediately starts [doSend] on a virtual thread; this method
     * does not block.
     *
     * @param emailRequest the email request; missing critical fields (account / password / server /
     *   recipients) are intercepted internally by `checkParams`, and the callback receives
     *   [EmailStatusEnum.FAIL]
     * @param callback callback invoked after the send completes (called for success / partial success /
     *   failure), executed on a virtual thread. **Do not perform long-running synchronous operations**
     *   inside the callback - it will tie up virtual thread reuse.
     */
    fun send(emailRequest: EmailRequest, callback: (EmailCallBackParam) -> Unit) {
        Thread.ofVirtual().start { doSend(emailRequest, callback) }
    }

    private fun doSend(emailRequest: EmailRequest, callback: (EmailCallBackParam) -> Unit) {
        val emailCallBackParam = EmailCallBackParam()
        try {
            // Validate parameters
            if (!checkParams(emailRequest)) {
                emailCallBackParam.status = EmailStatusEnum.FAIL
                emailCallBackParam.failEmails = emailRequest.receivers
                return
            }

            val sender = JavaMailSenderImpl()

            // Configure mail server information
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
            // Note: no longer stuffing senderPassword into mail.password - sender.password is already set,
            // and duplicating it would leave the password lingering in javaMailProperties, where it
            // could leak via toString / actuator and similar paths.
            extra["mail.smtp.host"] = emailRequest.serverHost
            extra["mail.smtp.sendpartial"] = emailRequest.sendpartial.toString()
            // Three SMTP timeouts: by default JavaMail waits indefinitely. Set conservative defaults
            // here (10s / 30s / 30s) to prevent the SMTP server from hanging virtual threads; the
            // business side can override via emailRequest.extra.
            extra["mail.smtp.connectiontimeout"] = DEFAULT_CONNECT_TIMEOUT_MS.toString()
            extra["mail.smtp.timeout"] = DEFAULT_READ_TIMEOUT_MS.toString()
            extra["mail.smtp.writetimeout"] = DEFAULT_WRITE_TIMEOUT_MS.toString()
            // Socket property aliases for the SSL channel (different JavaMail versions disagree on
            // which key is effective, so set both).
            extra["mail.smtps.connectiontimeout"] = DEFAULT_CONNECT_TIMEOUT_MS.toString()
            extra["mail.smtps.timeout"] = DEFAULT_READ_TIMEOUT_MS.toString()
            extra["mail.smtps.writetimeout"] = DEFAULT_WRITE_TIMEOUT_MS.toString()
            emailRequest.extra?.takeIf { it.isNotEmpty() }?.let { extra.putAll(it) }
            val properties = Properties()
            properties.putAll(extra)
            sender.javaMailProperties = properties

            // Build the email message
            val mailMessage: MimeMessage = sender.createMimeMessage()
            val messageHelper = MimeMessageHelper(mailMessage)

            // Set recipients, sender, subject, and body
            val receivers = emailRequest.receivers.toTypedArray()
            messageHelper.setTo(receivers)
            val from = emailRequest.fromMailAddress?.takeIf { it.isNotBlank() }
                ?: requireNotNull(emailRequest.senderAccount) { "senderAccount is required" }
            messageHelper.setFrom(from)
            messageHelper.setSubject(requireNotNull(emailRequest.subject) { "subject is required" })
            messageHelper.setText(requireNotNull(emailRequest.body) { "body is required" }, emailRequest.htmlFormat)

            // Send the email
            log.info("Starting to send email...")
            sender.send(mailMessage)

            // Set callback status
            emailCallBackParam.status = EmailStatusEnum.SUCCESS
            emailCallBackParam.successEmails = emailRequest.receivers

            log.info("Email sent successfully")
        } catch (e: MailSendException) {
            log.error(e, "Email send error, possibly caused by some mailbox accounts being unavailable; in this case some recipients fail and some succeed")

            // Set callback status
            for (messageException in e.messageExceptions) {
                if (messageException is SendFailedException) {
                    log.error(
                        "Email send result, success: {0}, failed: {1}, invalid: {2}",
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
            log.error(e, "Email send error")

            // Set callback status
            emailCallBackParam.status = EmailStatusEnum.FAIL
            emailCallBackParam.failEmails = emailRequest.receivers
        } finally {
            // Execute the callback
            callback.invoke(emailCallBackParam)
        }
    }

    /**
     * Validate parameters.
     *
     * @param mail
     */
    private fun checkParams(mail: EmailRequest): Boolean {
        val valid = !mail.senderAccount.isNullOrBlank() && !mail.senderPassword.isNullOrBlank() &&
            !mail.serverHost.isNullOrBlank() && mail.receivers.isNotEmpty()
        if (!valid) log.error("One of sender email account, sender email password, sender mail server address, or recipient email account is empty!")
        return valid
    }

    private fun putAddressToSet(set: MutableSet<String>, addresses: Array<Address>) {
        addresses.mapTo(set) { (it as InternetAddress).address }
    }

    private val log = LogFactory.getLog(this::class)

    companion object {
        /** SMTP connect timeout (milliseconds). JavaMail waits indefinitely by default, so an explicit upper bound is required to prevent virtual threads from hanging. */
        private const val DEFAULT_CONNECT_TIMEOUT_MS = 10_000L

        /** SMTP read timeout (milliseconds). */
        private const val DEFAULT_READ_TIMEOUT_MS = 30_000L

        /** SMTP write timeout (milliseconds); may be increased for large attachment scenarios. */
        private const val DEFAULT_WRITE_TIMEOUT_MS = 30_000L
    }

}
