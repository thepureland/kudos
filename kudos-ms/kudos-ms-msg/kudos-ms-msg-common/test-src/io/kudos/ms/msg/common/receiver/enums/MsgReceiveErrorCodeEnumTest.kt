package io.kudos.ms.msg.common.receiver.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for MsgReceiveErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, i18nKeyPrefix and the displayText contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiveErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        MsgReceiveErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(4, MsgReceiveErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        MsgReceiveErrorCodeEnum.entries.forEach { entry ->
            assertEquals("msg.error-msg.receive", entry.i18nKeyPrefix)
            assertEquals("msg.error-msg.receive.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("Unspecified error", MsgReceiveErrorCodeEnum.UNSPECIFIED.defaultDisplayText)
        assertEquals("Message receive record does not exist", MsgReceiveErrorCodeEnum.RECEIVE_NOT_FOUND.defaultDisplayText)
        assertEquals("Not authorized to operate on this message", MsgReceiveErrorCodeEnum.RECEIVE_NOT_OWNED.defaultDisplayText)
        assertEquals("Message has already been read", MsgReceiveErrorCodeEnum.ALREADY_READ.defaultDisplayText)
        assertEquals(MsgReceiveErrorCodeEnum.ALREADY_READ, MsgReceiveErrorCodeEnum.valueOf("ALREADY_READ"))
    }

}
