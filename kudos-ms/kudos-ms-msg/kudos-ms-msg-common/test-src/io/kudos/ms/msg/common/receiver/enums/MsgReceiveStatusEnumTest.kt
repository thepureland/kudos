package io.kudos.ms.msg.common.receiver.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * test for MsgReceiveStatusEnum
 *
 * Covers the dictCode of every entry, uniqueness of dict codes, and the UNREAD_CODES set
 * (which must contain RECEIVED + UNREAD, and must not contain READ / DELETED).
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiveStatusEnumTest {

    @Test
    fun dictCodes() {
        assertEquals("11", MsgReceiveStatusEnum.RECEIVED.dictCode)
        assertEquals("01", MsgReceiveStatusEnum.UNREAD.dictCode)
        assertEquals("12", MsgReceiveStatusEnum.READ.dictCode)
        assertEquals("21", MsgReceiveStatusEnum.DELETED.dictCode)
        assertEquals(4, MsgReceiveStatusEnum.entries.size)
    }

    @Test
    fun dictCodesAreUnique() {
        val codes = MsgReceiveStatusEnum.entries.map { it.dictCode }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun unreadCodesContainReceivedAndUnreadOnly() {
        val unread = MsgReceiveStatusEnum.UNREAD_CODES
        assertEquals(2, unread.size)
        assertTrue(MsgReceiveStatusEnum.RECEIVED.dictCode in unread)
        assertTrue(MsgReceiveStatusEnum.UNREAD.dictCode in unread)
        assertFalse(MsgReceiveStatusEnum.READ.dictCode in unread)
        assertFalse(MsgReceiveStatusEnum.DELETED.dictCode in unread)
        assertEquals(setOf("11", "01"), unread)
    }

}
