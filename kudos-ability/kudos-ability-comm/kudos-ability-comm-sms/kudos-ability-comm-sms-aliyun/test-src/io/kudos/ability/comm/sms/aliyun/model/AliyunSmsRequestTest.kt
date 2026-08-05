package io.kudos.ability.comm.sms.aliyun.model

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * test for AliyunSmsRequest
 *
 * 覆蓋：全部屬性的默認值（null）、賦值與讀回（含空字符串、Unicode、超長輸入、多手機號逗號分隔），
 * 以及 Serializable 序列化/反序列化往返一致性。
 *
 * @author K
 * @since 1.0.0
 */
internal class AliyunSmsRequestTest {

    @Test
    fun defaultsAreNull() {
        val request = AliyunSmsRequest()

        assertNull(request.region)
        assertNull(request.accessKeyId)
        assertNull(request.accessKeySecret)
        assertNull(request.phoneNumbers)
        assertNull(request.signName)
        assertNull(request.templateCode)
        assertNull(request.templateParam)
    }

    @Test
    fun acceptsAssignedValues() {
        val request = AliyunSmsRequest().apply {
            region = "cn-hangzhou"
            accessKeyId = "ak-id"
            accessKeySecret = "ak-secret"
            phoneNumbers = "13800000000,13900000000"
            signName = "簽名α"
            templateCode = "SMS_123456"
            templateParam = """{"name":"張三","number":"1390000****"}"""
        }

        assertEquals("cn-hangzhou", request.region)
        assertEquals("ak-id", request.accessKeyId)
        assertEquals("ak-secret", request.accessKeySecret)
        assertEquals("13800000000,13900000000", request.phoneNumbers)
        assertEquals("簽名α", request.signName)
        assertEquals("SMS_123456", request.templateCode)
        assertEquals("""{"name":"張三","number":"1390000****"}""", request.templateParam)
    }

    @Test
    fun acceptsBoundaryAndUnusualValues() {
        // POJO 請求體不做校驗，邊界/異常值由阿里雲服務端負責約束
        val request = AliyunSmsRequest()

        request.phoneNumbers = ""
        assertEquals("", request.phoneNumbers)

        val longParam = "x".repeat(10_000)
        request.templateParam = longParam
        assertEquals(longParam, request.templateParam)

        request.signName = null
        assertNull(request.signName)
    }

    @Test
    fun serializationRoundTrip() {
        val request = AliyunSmsRequest().apply {
            region = "cn-shanghai"
            accessKeyId = "ak"
            accessKeySecret = "sk"
            phoneNumbers = "13800000000"
            signName = "SIGN"
            templateCode = "SMS_1"
            templateParam = """{"code":"1234"}"""
        }

        val bytes = ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { it.writeObject(request) }
            bos.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use {
            it.readObject() as AliyunSmsRequest
        }

        assertEquals(request.region, restored.region)
        assertEquals(request.accessKeyId, restored.accessKeyId)
        assertEquals(request.accessKeySecret, restored.accessKeySecret)
        assertEquals(request.phoneNumbers, restored.phoneNumbers)
        assertEquals(request.signName, restored.signName)
        assertEquals(request.templateCode, restored.templateCode)
        assertEquals(request.templateParam, restored.templateParam)
    }

}
