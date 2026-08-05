package io.kudos.ability.comm.email.model

import io.kudos.ability.comm.email.enums.EmailStatusEnum
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * test for EmailCallBackParam
 *
 * 覆蓋：默認值、賦值讀回、toString（null 與非 null 字段）、Serializable 往返。
 *
 * @author K
 * @since 1.0.0
 */
internal class EmailCallBackParamTest {

    @Test
    fun defaults() {
        val param = EmailCallBackParam()

        assertNull(param.status)
        assertNull(param.successEmails)
        assertNull(param.failEmails)
    }

    @Test
    fun acceptsConfiguredValues() {
        val param = EmailCallBackParam().apply {
            status = EmailStatusEnum.SUCCESS_PART
            successEmails = mutableSetOf("ok@example.com")
            failEmails = mutableSetOf("bad@example.com")
        }

        assertEquals(EmailStatusEnum.SUCCESS_PART, param.status)
        assertEquals(setOf("ok@example.com"), param.successEmails!!)
        assertEquals(setOf("bad@example.com"), param.failEmails!!)
    }

    @Test
    fun toStringContainsAllFields() {
        val empty = EmailCallBackParam()
        assertEquals("EmailCallBackParam{status=null, successEmails=null, failEmails=null}", empty.toString())

        val param = EmailCallBackParam().apply {
            status = EmailStatusEnum.SUCCESS
            successEmails = mutableSetOf("ok@example.com")
            failEmails = mutableSetOf()
        }
        assertEquals(
            "EmailCallBackParam{status=SUCCESS, successEmails=[ok@example.com], failEmails=[]}",
            param.toString()
        )
    }

    @Test
    fun serializableRoundTrip() {
        val param = EmailCallBackParam().apply {
            status = EmailStatusEnum.FAIL
            failEmails = mutableSetOf("bad@example.com")
        }

        val bytes = ByteArrayOutputStream().use { byteStream ->
            ObjectOutputStream(byteStream).use { it.writeObject(param) }
            byteStream.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() } as EmailCallBackParam

        assertEquals(EmailStatusEnum.FAIL, restored.status)
        assertEquals(setOf("bad@example.com"), restored.failEmails!!)
        assertNull(restored.successEmails)
    }

}
