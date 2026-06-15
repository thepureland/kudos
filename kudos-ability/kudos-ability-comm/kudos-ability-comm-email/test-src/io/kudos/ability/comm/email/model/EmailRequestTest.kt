package io.kudos.ability.comm.email.model

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for EmailRequest
 *
 * 覆蓋：全部屬性默認值、自定義值賦值與讀回（含空字符串、Unicode、邊界端口值）、Serializable 往返。
 *
 * @author K
 * @since 1.0.0
 */
internal class EmailRequestTest {

    @Test
    fun defaults() {
        val request = EmailRequest()

        assertEquals("smtp", request.protocol)
        assertNull(request.subject)
        assertNull(request.body)
        assertTrue(request.receivers.isEmpty())
        assertNull(request.senderAccount)
        assertNull(request.senderPassword)
        assertNull(request.serverHost)
        assertNull(request.serverPort)
        assertTrue(request.smtpAuth)
        assertTrue(request.sendpartial)
        assertTrue(request.htmlFormat)
        assertEquals("UTF-8", request.encoding)
        assertTrue(request.ssl)
        assertNull(request.extra)
        assertNull(request.fromMailAddress)
    }

    @Test
    fun acceptsConfiguredValues() {
        val request = EmailRequest().apply {
            protocol = "smtps"
            subject = "主題-α"
            body = "<h1>正文</h1>"
            receivers = mutableSetOf("a@example.com", "b@example.com")
            senderAccount = "sender@example.com"
            senderPassword = ""
            serverHost = "smtp.example.com"
            serverPort = 0
            smtpAuth = false
            sendpartial = false
            htmlFormat = false
            encoding = "GBK"
            ssl = false
            extra = mutableMapOf("mail.smtp.timeout" to "1")
            fromMailAddress = "from@example.com"
        }

        assertEquals("smtps", request.protocol)
        assertEquals("主題-α", request.subject)
        assertEquals("<h1>正文</h1>", request.body)
        assertEquals(setOf("a@example.com", "b@example.com"), request.receivers)
        assertEquals("sender@example.com", request.senderAccount)
        assertEquals("", request.senderPassword)
        assertEquals("smtp.example.com", request.serverHost)
        assertEquals(0, request.serverPort)
        assertEquals(false, request.smtpAuth)
        assertEquals(false, request.sendpartial)
        assertEquals(false, request.htmlFormat)
        assertEquals("GBK", request.encoding)
        assertEquals(false, request.ssl)
        assertEquals(mapOf("mail.smtp.timeout" to "1"), request.extra!!)
        assertEquals("from@example.com", request.fromMailAddress)
    }

    @Test
    fun serializableRoundTrip() {
        val request = EmailRequest().apply {
            subject = "subject"
            receivers = mutableSetOf("a@example.com")
            serverPort = 65535
        }

        val bytes = ByteArrayOutputStream().use { byteStream ->
            ObjectOutputStream(byteStream).use { it.writeObject(request) }
            byteStream.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() } as EmailRequest

        assertEquals("subject", restored.subject)
        assertEquals(setOf("a@example.com"), restored.receivers)
        assertEquals(65535, restored.serverPort)
        assertEquals("smtp", restored.protocol)
    }

}
