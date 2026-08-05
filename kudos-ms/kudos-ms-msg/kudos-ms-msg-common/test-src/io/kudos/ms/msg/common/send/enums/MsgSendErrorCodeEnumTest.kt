package io.kudos.ms.msg.common.send.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for MsgSendErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, i18nKeyPrefix and the displayText contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgSendErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        MsgSendErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(4, MsgSendErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        MsgSendErrorCodeEnum.entries.forEach { entry ->
            assertEquals("msg.error-msg.send", entry.i18nKeyPrefix)
            assertEquals("msg.error-msg.send.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("Unspecified error", MsgSendErrorCodeEnum.UNSPECIFIED.defaultDisplayText)
        assertEquals("Recipient list is empty", MsgSendErrorCodeEnum.RECEIVER_IDS_EMPTY.defaultDisplayText)
        assertEquals("No matching message template found", MsgSendErrorCodeEnum.TEMPLATE_NOT_FOUND.defaultDisplayText)
        assertEquals("Message dispatch failed", MsgSendErrorCodeEnum.MQ_PUBLISH_FAILED.defaultDisplayText)
        assertEquals(MsgSendErrorCodeEnum.MQ_PUBLISH_FAILED, MsgSendErrorCodeEnum.valueOf("MQ_PUBLISH_FAILED"))
    }

}
