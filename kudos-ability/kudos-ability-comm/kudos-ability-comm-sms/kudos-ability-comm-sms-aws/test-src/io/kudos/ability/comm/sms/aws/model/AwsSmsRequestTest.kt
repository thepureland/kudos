package io.kudos.ability.comm.sms.aws.model

import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * test for AwsSmsRequest（純單元測試）
 *
 * 覆蓋：全部屬性的默認值（null）、賦值與讀回（含空字符串、Unicode、超長輸入、含 null 鍵值的
 * messageAttributes），以及 Serializable 序列化/反序列化往返一致性。
 *
 * @author K
 * @since 1.0.0
 */
internal class AwsSmsRequestTest {

    @Test
    fun defaultsAreNull() {
        val request = AwsSmsRequest()

        assertNull(request.region)
        assertNull(request.accessKeyId)
        assertNull(request.accessKeySecret)
        assertNull(request.phoneNumber)
        assertNull(request.message)
        assertNull(request.messageAttributes)
    }

    @Test
    fun acceptsAssignedValues() {
        val attr = MessageAttributeValue.builder().dataType("String").stringValue("交易α").build()
        val request = AwsSmsRequest().apply {
            region = "us-east-1"
            accessKeyId = "ak-id"
            accessKeySecret = "ak-secret"
            phoneNumber = "+15550100"
            message = "你好 AWS"
            messageAttributes = mutableMapOf("AWS.SNS.SMS.SMSType" to attr, null to null)
        }

        assertEquals("us-east-1", request.region)
        assertEquals("ak-id", request.accessKeyId)
        assertEquals("ak-secret", request.accessKeySecret)
        assertEquals("+15550100", request.phoneNumber)
        assertEquals("你好 AWS", request.message)
        assertEquals(2, request.messageAttributes!!.size)
        assertEquals(attr, request.messageAttributes!!["AWS.SNS.SMS.SMSType"])
        assertNull(request.messageAttributes!![null])
    }

    @Test
    fun acceptsBoundaryAndUnusualValues() {
        // POJO 請求體不做校驗，邊界/異常值由 AWS 服務端負責約束
        val request = AwsSmsRequest()

        request.phoneNumber = ""
        assertEquals("", request.phoneNumber)

        val longMessage = "x".repeat(10_000)
        request.message = longMessage
        assertEquals(longMessage, request.message)

        request.messageAttributes = mutableMapOf()
        assertEquals(0, request.messageAttributes!!.size)

        request.message = null
        assertNull(request.message)
    }

    @Test
    fun serializationRoundTrip() {
        val request = AwsSmsRequest().apply {
            region = "ap-southeast-1"
            accessKeyId = "ak"
            accessKeySecret = "sk"
            phoneNumber = "+15550100"
            message = "hello"
            messageAttributes = mutableMapOf(
                "AWS.SNS.SMS.SenderID" to
                        MessageAttributeValue.builder().dataType("String").stringValue("KUDOS").build()
            )
        }

        val bytes = ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { it.writeObject(request) }
            bos.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use {
            it.readObject() as AwsSmsRequest
        }

        assertEquals(request.region, restored.region)
        assertEquals(request.accessKeyId, restored.accessKeyId)
        assertEquals(request.accessKeySecret, restored.accessKeySecret)
        assertEquals(request.phoneNumber, restored.phoneNumber)
        assertEquals(request.message, restored.message)
        assertEquals(request.messageAttributes, restored.messageAttributes)
    }

}
