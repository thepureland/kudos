package io.kudos.ms.msg.common.receiver.enums

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test for MsgUnreceivedReasonEnum
 *
 * Covers the code of every entry and that codes equal their enum names and are unique.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgUnreceivedReasonEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        MsgUnreceivedReasonEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
        }
        assertEquals(6, MsgUnreceivedReasonEnum.entries.size)
    }

    @Test
    fun codesAreUnique() {
        val codes = MsgUnreceivedReasonEnum.entries.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun specificEntries() {
        assertEquals("NO_CONTACT", MsgUnreceivedReasonEnum.NO_CONTACT.code)
        assertEquals("CHANNEL_REJECT", MsgUnreceivedReasonEnum.CHANNEL_REJECT.code)
        assertEquals("TIMEOUT", MsgUnreceivedReasonEnum.TIMEOUT.code)
        assertEquals("LISTENER_ERROR", MsgUnreceivedReasonEnum.LISTENER_ERROR.code)
        assertEquals("EMPTY_RECEIVERS", MsgUnreceivedReasonEnum.EMPTY_RECEIVERS.code)
        assertEquals("UNKNOWN", MsgUnreceivedReasonEnum.UNKNOWN.code)
        assertEquals(MsgUnreceivedReasonEnum.TIMEOUT, MsgUnreceivedReasonEnum.valueOf("TIMEOUT"))
    }

}
