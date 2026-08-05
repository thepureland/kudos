package io.kudos.ability.comm.sms.aws.model

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AwsSmsCallBackParam（純單元測試）
 *
 * 覆蓋：全部屬性的默認值（statusCode=0、其餘 null）、賦值與讀回（含 0/負數狀態碼、Unicode 文本）、
 * toString 內容、以及 Serializable 序列化/反序列化往返一致性。
 *
 * @author K
 * @since 1.0.0
 */
internal class AwsSmsCallBackParamTest {

    @Test
    fun defaults() {
        val param = AwsSmsCallBackParam()

        assertEquals(0, param.statusCode)
        assertNull(param.statusText)
        assertNull(param.messageId)
        assertNull(param.sequenceNumber)
    }

    @Test
    fun acceptsAssignedValues() {
        val param = AwsSmsCallBackParam().apply {
            statusCode = 200
            statusText = "成功 OK"
            messageId = "mid-123"
            sequenceNumber = "seq-456"
        }

        assertEquals(200, param.statusCode)
        assertEquals("成功 OK", param.statusText)
        assertEquals("mid-123", param.messageId)
        assertEquals("seq-456", param.sequenceNumber)

        // 邊界：0 與負數狀態碼（POJO 不校驗）
        param.statusCode = -1
        assertEquals(-1, param.statusCode)
        param.statusCode = 0
        assertEquals(0, param.statusCode)
    }

    @Test
    fun toStringContainsAllFields() {
        val param = AwsSmsCallBackParam().apply {
            statusCode = 429
            statusText = "Too Many Requests"
            messageId = "mid-1"
            sequenceNumber = "seq-1"
        }

        val text = param.toString()
        assertTrue(text.contains("statusCode=429"))
        assertTrue(text.contains("statusText='Too Many Requests'"))
        assertTrue(text.contains("messageId='mid-1'"))
        assertTrue(text.contains("sequenceNumber='seq-1'"))

        // null 字段在 toString 中表現為字符串 "null"，不應拋異常
        val empty = AwsSmsCallBackParam().toString()
        assertTrue(empty.contains("statusCode=0"))
        assertTrue(empty.contains("statusText='null'"))
    }

    @Test
    fun serializationRoundTrip() {
        val param = AwsSmsCallBackParam().apply {
            statusCode = 599
            statusText = "client error"
            messageId = "mid-9"
            sequenceNumber = "seq-9"
        }

        val bytes = ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { it.writeObject(param) }
            bos.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use {
            it.readObject() as AwsSmsCallBackParam
        }

        assertEquals(param.statusCode, restored.statusCode)
        assertEquals(param.statusText, restored.statusText)
        assertEquals(param.messageId, restored.messageId)
        assertEquals(param.sequenceNumber, restored.sequenceNumber)
    }

}
