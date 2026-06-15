package io.kudos.ms.msg.common.send.enums

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test for MsgSendStatusEnum
 *
 * Covers the dictCode of every entry and that dict codes are unique.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgSendStatusEnumTest {

    @Test
    fun dictCodes() {
        assertEquals("00", MsgSendStatusEnum.PENDING.dictCode)
        assertEquals("01", MsgSendStatusEnum.CANCELLED.dictCode)
        assertEquals("11", MsgSendStatusEnum.SENT_TO_MQ.dictCode)
        assertEquals("21", MsgSendStatusEnum.FAILED_TO_SEND_TO_MQ.dictCode)
        assertEquals("22", MsgSendStatusEnum.FAILED_FINAL.dictCode)
        assertEquals("31", MsgSendStatusEnum.CONSUMED_FROM_MQ.dictCode)
        assertEquals("32", MsgSendStatusEnum.SUCCESS_PARTIAL.dictCode)
        assertEquals("33", MsgSendStatusEnum.SUCCESS.dictCode)
        assertEquals(8, MsgSendStatusEnum.entries.size)
    }

    @Test
    fun dictCodesAreUnique() {
        val codes = MsgSendStatusEnum.entries.map { it.dictCode }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun valueOfRoundTrip() {
        MsgSendStatusEnum.entries.forEach {
            assertEquals(it, MsgSendStatusEnum.valueOf(it.name))
        }
    }

}
