package io.kudos.ms.msg.common.receiver.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for MsgReceiverGroupErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, i18nKeyPrefix and the displayText contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiverGroupErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        MsgReceiverGroupErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(4, MsgReceiverGroupErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        MsgReceiverGroupErrorCodeEnum.entries.forEach { entry ->
            assertEquals("msg.error-msg.receivergroup", entry.i18nKeyPrefix)
            assertEquals("msg.error-msg.receivergroup.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("Unspecified error", MsgReceiverGroupErrorCodeEnum.UNSPECIFIED.defaultDisplayText)
        assertEquals("Receiver group does not exist", MsgReceiverGroupErrorCodeEnum.GROUP_NOT_FOUND.defaultDisplayText)
        assertEquals("Receiver group is disabled", MsgReceiverGroupErrorCodeEnum.GROUP_INACTIVE.defaultDisplayText)
        assertEquals("Unsupported receiver group type", MsgReceiverGroupErrorCodeEnum.INVALID_GROUP_TYPE.defaultDisplayText)
        assertEquals(MsgReceiverGroupErrorCodeEnum.GROUP_INACTIVE, MsgReceiverGroupErrorCodeEnum.valueOf("GROUP_INACTIVE"))
    }

}
