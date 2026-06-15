package io.kudos.ms.sys.common.accessrule.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for SysAccessRuleErrorCodeEnum
 *
 * Covers code/defaultDisplayText of every entry, i18nKeyPrefix, and the
 * IErrorCodeEnum.displayText contract (prefix + "." + code when the prefix is not blank).
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAccessRuleErrorCodeEnumTest {

    @Test
    fun codeEqualsEnumNameForAllEntries() {
        SysAccessRuleErrorCodeEnum.entries.forEach { entry ->
            assertEquals(entry.name, entry.code)
            assertTrue(entry.defaultDisplayText.isNotBlank())
        }
        assertEquals(8, SysAccessRuleErrorCodeEnum.entries.size)
    }

    @Test
    fun i18nKeyPrefixAndDisplayText() {
        SysAccessRuleErrorCodeEnum.entries.forEach { entry ->
            assertEquals("sys.error-msg.accessrule", entry.i18nKeyPrefix)
            // non-blank prefix -> displayText is the i18n key
            assertEquals("sys.error-msg.accessrule.${entry.code}", entry.displayText)
        }
    }

    @Test
    fun specificEntries() {
        assertEquals("UNSPECIFIED", SysAccessRuleErrorCodeEnum.UNSPECIFIED.code)
        assertEquals("Invalid IP start address", SysAccessRuleErrorCodeEnum.INVALID_IP_START_ADDRESS.defaultDisplayText)
        assertEquals("Invalid IP end address", SysAccessRuleErrorCodeEnum.INVALID_IP_END_ADDRESS.defaultDisplayText)
        assertEquals(
            SysAccessRuleErrorCodeEnum.PARENT_ACCESS_RULE_NOT_FOUND,
            SysAccessRuleErrorCodeEnum.valueOf("PARENT_ACCESS_RULE_NOT_FOUND")
        )
    }

}
