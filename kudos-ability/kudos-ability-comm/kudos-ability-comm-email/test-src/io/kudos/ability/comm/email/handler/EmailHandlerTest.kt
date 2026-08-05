package io.kudos.ability.comm.email.handler

import io.kudos.ability.comm.email.enums.EmailStatusEnum
import io.kudos.ability.comm.email.model.EmailCallBackParam
import io.kudos.ability.comm.email.model.EmailRequest
import jakarta.mail.Address
import jakarta.mail.MessagingException
import jakarta.mail.SendFailedException
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.util.Properties
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for EmailHandler (純單元測試，不依賴真實 SMTP 服務)
 *
 * 覆蓋：
 * - checkParams 全部校驗分支（賬號/密碼/服務器/收件人為空或空白；subject/from/receiver 含 CR/LF 的
 *   SMTP 頭注入攔截；合法參數通過）
 * - doSend 參數校驗失敗回調 FAIL；serverPort/subject/body 缺失走通用異常分支回調 FAIL
 * - doSend 成功路徑（通過 Mockito mockConstruction 攔截 JavaMailSenderImpl 構造）：
 *   ssl=true/false 對應的 javaMailProperties、默認超時、extra 覆蓋、fromMailAddress 空白回退 senderAccount
 * - MailSendException 分支：含 SendFailedException 的部分成功（SUCCESS_PART）、全部失敗（FAIL）、
 *   僅成功地址（FAIL）、不含 SendFailedException 的兜底 FAIL
 * - send 公開方法（虛擬線程異步回調）
 * - putAddressToSet 地址映射
 *
 * 真實 SMTP 收發鏈路由 EmailTest（testcontainers）覆蓋。
 *
 * @author K
 * @since 1.0.0
 */
internal class EmailHandlerTest {

    private val handler = EmailHandler()

    private fun validRequest() = EmailRequest().apply {
        serverHost = "smtp.example.com"
        serverPort = 25
        subject = "test subject"
        body = "test body"
        senderAccount = "sender@example.com"
        senderPassword = "secret"
        receivers = mutableSetOf("r1@example.com")
        ssl = false
    }

    /** 同步調用 doSend 並取回唯一一次回調參數。 */
    private fun doSendAndGetCallback(request: EmailRequest): EmailCallBackParam {
        val callbacks = mutableListOf<EmailCallBackParam>()
        handler.doSend(request) { callbacks.add(it) }
        assertEquals(1, callbacks.size, "callback 應且僅應被調用一次")
        return callbacks.first()
    }

    // -------------------- checkParams --------------------

    @Test
    fun checkParamsAcceptsValidRequest() {
        assertTrue(handler.checkParams(validRequest()))
    }

    @Test
    fun checkParamsRejectsMissingRequiredFields() {
        assertFalse(handler.checkParams(validRequest().apply { senderAccount = null }))
        assertFalse(handler.checkParams(validRequest().apply { senderAccount = "  " }))
        assertFalse(handler.checkParams(validRequest().apply { senderPassword = null }))
        assertFalse(handler.checkParams(validRequest().apply { senderPassword = "" }))
        assertFalse(handler.checkParams(validRequest().apply { serverHost = null }))
        assertFalse(handler.checkParams(validRequest().apply { serverHost = " " }))
        assertFalse(handler.checkParams(validRequest().apply { receivers = mutableSetOf() }))
    }

    @Test
    fun checkParamsRejectsHeaderInjection() {
        // SMTP 頭注入：subject / fromMailAddress / receiver 含 CR 或 LF 必須拒絕
        assertFalse(handler.checkParams(validRequest().apply { subject = "hi\r\nBcc: attacker@evil.com" }))
        assertFalse(handler.checkParams(validRequest().apply { subject = "hi\nX-Evil: 1" }))
        assertFalse(handler.checkParams(validRequest().apply { fromMailAddress = "a@b.com\rBcc: x@evil.com" }))
        assertFalse(handler.checkParams(validRequest().apply { receivers = mutableSetOf("ok@b.com", "bad\n@b.com") }))
        // subject / fromMailAddress 為 null 時不參與注入檢查，不應 NPE
        assertTrue(handler.checkParams(validRequest().apply { subject = null; fromMailAddress = null }))
    }

    // -------------------- doSend：參數校驗失敗與通用異常分支 --------------------

    @Test
    fun doSendCallbacksFailWhenParamsInvalid() {
        val request = validRequest().apply { senderPassword = null }
        val callback = doSendAndGetCallback(request)

        assertEquals(EmailStatusEnum.FAIL, callback.status)
        assertEquals(request.receivers, callback.failEmails)
        assertNull(callback.successEmails)
    }

    @Test
    fun doSendCallbacksFailWhenServerPortMissing() {
        // serverPort 為 null -> requireNotNull 拋 IllegalArgumentException -> 通用異常分支
        val request = validRequest().apply { serverPort = null }
        val callback = doSendAndGetCallback(request)

        assertEquals(EmailStatusEnum.FAIL, callback.status)
        assertEquals(request.receivers, callback.failEmails)
    }

    @Test
    fun doSendCallbacksFailWhenSubjectMissing() {
        val request = validRequest().apply { subject = null }
        val callback = doSendAndGetCallback(request)

        assertEquals(EmailStatusEnum.FAIL, callback.status)
        assertEquals(request.receivers, callback.failEmails)
    }

    @Test
    fun doSendCallbacksFailWhenBodyMissing() {
        val request = validRequest().apply { body = null }
        val callback = doSendAndGetCallback(request)

        assertEquals(EmailStatusEnum.FAIL, callback.status)
        assertEquals(request.receivers, callback.failEmails)
    }

    // -------------------- doSend：成功路徑（mock JavaMailSenderImpl 構造） --------------------

    @Test
    fun doSendSuccessWithStartTlsWhenSslDisabled() {
        val mimeMessage = MimeMessage(Session.getInstance(Properties()))
        Mockito.mockConstruction(JavaMailSenderImpl::class.java) { mock, _ ->
            Mockito.`when`(mock.createMimeMessage()).thenReturn(mimeMessage)
        }.use { construction ->
            val request = validRequest().apply {
                ssl = false
                fromMailAddress = "from@example.com"
                htmlFormat = false
            }
            val callback = doSendAndGetCallback(request)

            assertEquals(EmailStatusEnum.SUCCESS, callback.status)
            assertEquals(request.receivers, callback.successEmails)
            assertNull(callback.failEmails)

            val sender = construction.constructed().single()
            Mockito.verify(sender).send(mimeMessage)

            // 服務器連接信息必須逐項落到 JavaMailSenderImpl 上
            Mockito.verify(sender).host = "smtp.example.com"
            Mockito.verify(sender).port = 25
            Mockito.verify(sender).username = "sender@example.com"
            Mockito.verify(sender).password = "secret"
            Mockito.verify(sender).protocol = "smtp"
            Mockito.verify(sender).defaultEncoding = "UTF-8"

            val captor = ArgumentCaptor.forClass(Properties::class.java)
            Mockito.verify(sender).setJavaMailProperties(captor.capture())
            val props = captor.value
            assertEquals("true", props["mail.smtp.starttls.enable"])
            assertNull(props["mail.smtp.ssl.enable"])
            assertEquals("smtp", props["mail.transport.protocol"])
            assertEquals("true", props["mail.smtp.auth"])
            assertEquals("sender@example.com", props["mail.user"])
            assertEquals("smtp.example.com", props["mail.smtp.host"])
            assertEquals("true", props["mail.smtp.sendpartial"])
            // 默認三類超時（smtp 與 smtps 兩套別名）
            assertEquals("10000", props["mail.smtp.connectiontimeout"])
            assertEquals("30000", props["mail.smtp.timeout"])
            assertEquals("30000", props["mail.smtp.writetimeout"])
            assertEquals("10000", props["mail.smtps.connectiontimeout"])
            assertEquals("30000", props["mail.smtps.timeout"])
            assertEquals("30000", props["mail.smtps.writetimeout"])
            // 密碼不得進入 javaMailProperties（防 toString/actuator 泄漏）
            assertFalse(props.values.contains("secret"))

            // 顯式指定 fromMailAddress 時，From 頭應為該地址
            assertEquals("from@example.com", (mimeMessage.from.single() as InternetAddress).address)
            assertEquals("test subject", mimeMessage.subject)
            assertEquals(
                setOf("r1@example.com"),
                mimeMessage.getRecipients(jakarta.mail.Message.RecipientType.TO)
                    .map { (it as InternetAddress).address }.toSet()
            )
        }
    }

    @Test
    fun doSendSuccessWithSslAndExtraOverride() {
        val mimeMessage = MimeMessage(Session.getInstance(Properties()))
        Mockito.mockConstruction(JavaMailSenderImpl::class.java) { mock, _ ->
            Mockito.`when`(mock.createMimeMessage()).thenReturn(mimeMessage)
        }.use { construction ->
            val request = validRequest().apply {
                ssl = true
                smtpAuth = false
                sendpartial = false
                // fromMailAddress 空白時回退到 senderAccount
                fromMailAddress = "   "
                // extra 可覆蓋默認超時並追加自定義鍵
                extra = mutableMapOf("mail.smtp.timeout" to "5000", "custom.key" to "自定義值")
            }
            val callback = doSendAndGetCallback(request)

            assertEquals(EmailStatusEnum.SUCCESS, callback.status)
            assertEquals(request.receivers, callback.successEmails)

            val sender = construction.constructed().single()
            val captor = ArgumentCaptor.forClass(Properties::class.java)
            Mockito.verify(sender).setJavaMailProperties(captor.capture())
            val props = captor.value
            assertEquals("true", props["mail.smtp.ssl.enable"])
            assertNull(props["mail.smtp.starttls.enable"])
            assertEquals("false", props["mail.smtp.auth"])
            assertEquals("false", props["mail.smtp.sendpartial"])
            assertEquals("5000", props["mail.smtp.timeout"])
            assertEquals("自定義值", props["custom.key"])

            // From 回退為 senderAccount
            assertEquals("sender@example.com", (mimeMessage.from.single() as InternetAddress).address)
        }
    }

    // -------------------- doSend：MailSendException 分支 --------------------

    private fun doSendWithSendError(request: EmailRequest, error: MailSendException): EmailCallBackParam {
        val mimeMessage = MimeMessage(Session.getInstance(Properties()))
        Mockito.mockConstruction(JavaMailSenderImpl::class.java) { mock, _ ->
            Mockito.`when`(mock.createMimeMessage()).thenReturn(mimeMessage)
            Mockito.doThrow(error).`when`(mock).send(any(MimeMessage::class.java))
        }.use {
            return doSendAndGetCallback(request)
        }
    }

    private fun sendFailedException(
        validSent: Array<Address>,
        validUnsent: Array<Address>,
        invalid: Array<Address>
    ) = SendFailedException("send failed", Exception("root"), validSent, validUnsent, invalid)

    @Test
    fun doSendCallbacksSuccessPartWhenSomeRecipientsFail() {
        val request = validRequest().apply {
            receivers = mutableSetOf("ok@example.com", "unsent@example.com", "bad@example.com")
        }
        val sfe = sendFailedException(
            validSent = arrayOf(InternetAddress("ok@example.com")),
            validUnsent = arrayOf(InternetAddress("unsent@example.com")),
            invalid = arrayOf(InternetAddress("bad@example.com"))
        )
        val callback = doSendWithSendError(request, MailSendException(mapOf<Any, Exception>(request to sfe)))

        assertEquals(EmailStatusEnum.SUCCESS_PART, callback.status)
        assertEquals(setOf("ok@example.com"), callback.successEmails!!)
        assertEquals(setOf("unsent@example.com", "bad@example.com"), callback.failEmails!!)
    }

    @Test
    fun doSendCallbacksFailWhenAllRecipientsFail() {
        val request = validRequest().apply { receivers = mutableSetOf("bad@example.com") }
        val sfe = sendFailedException(
            validSent = emptyArray(),
            validUnsent = emptyArray(),
            invalid = arrayOf(InternetAddress("bad@example.com"))
        )
        val callback = doSendWithSendError(request, MailSendException(mapOf<Any, Exception>(request to sfe)))

        assertEquals(EmailStatusEnum.FAIL, callback.status)
        assertEquals(setOf("bad@example.com"), callback.failEmails!!)
        assertTrue(callback.successEmails!!.isEmpty())
    }

    @Test
    fun doSendCallbacksFailWhenOnlySentAddressesPresent() {
        // 異常中只有成功地址、無失敗地址：代碼按 FAIL 處理（非部分成功）
        val request = validRequest()
        val sfe = sendFailedException(
            validSent = arrayOf(InternetAddress("r1@example.com")),
            validUnsent = emptyArray(),
            invalid = emptyArray()
        )
        val callback = doSendWithSendError(request, MailSendException(mapOf<Any, Exception>(request to sfe)))

        assertEquals(EmailStatusEnum.FAIL, callback.status)
        assertEquals(setOf("r1@example.com"), callback.successEmails!!)
        assertTrue(callback.failEmails!!.isEmpty())
    }

    @Test
    fun doSendCallbacksFailWhenMailSendExceptionWithoutSendFailedException() {
        // messageExceptions 不含 SendFailedException（如連接失敗）-> 兜底 FAIL，失敗名單為全部收件人
        val request = validRequest()
        val callback = doSendWithSendError(
            request,
            MailSendException(mapOf<Any, Exception>(request to MessagingException("connect refused")))
        )

        assertEquals(EmailStatusEnum.FAIL, callback.status)
        assertEquals(request.receivers, callback.failEmails)
        assertNull(callback.successEmails)
    }

    // -------------------- send（異步公開入口） --------------------

    @Test
    fun sendInvokesCallbackAsynchronously() {
        // 用參數校驗失敗的請求走異步入口，避免真實網絡訪問
        val request = validRequest().apply { senderAccount = null }
        val callbacks = mutableListOf<EmailCallBackParam>()
        val latch = CountDownLatch(1)

        handler.send(request) {
            try {
                callbacks.add(it)
            } finally {
                latch.countDown()
            }
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "回調應在 30 秒內被執行")
        assertEquals(EmailStatusEnum.FAIL, callbacks.single().status)
        assertEquals(request.receivers, callbacks.single().failEmails)
    }

    // -------------------- putAddressToSet --------------------

    @Test
    fun putAddressToSet() {
        val set = mutableSetOf("existing@example.com")
        handler.putAddressToSet(set, arrayOf(InternetAddress("a@example.com"), InternetAddress("b@example.com")))
        assertEquals(setOf("existing@example.com", "a@example.com", "b@example.com"), set)

        // 空數組不改變集合
        handler.putAddressToSet(set, emptyArray())
        assertEquals(3, set.size)
    }

}
