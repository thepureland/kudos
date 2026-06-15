package io.kudos.ms.sys.common.accessrule.enums

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test for AccessRuleTypeEnum
 *
 * Covers all enum entries' code/displayText values, valueOf and entry count/uniqueness.
 *
 * @author K
 * @since 1.0.0
 */
internal class AccessRuleTypeEnumTest {

    @Test
    fun codesAndDisplayTexts() {
        assertEquals("1", AccessRuleTypeEnum.UNLIMITED.code)
        assertEquals("Unlimited", AccessRuleTypeEnum.UNLIMITED.displayText)
        assertEquals("2", AccessRuleTypeEnum.WHITELIST.code)
        assertEquals("Whitelist", AccessRuleTypeEnum.WHITELIST.displayText)
        assertEquals("3", AccessRuleTypeEnum.BLACKLIST.code)
        assertEquals("Blacklist", AccessRuleTypeEnum.BLACKLIST.displayText)
        assertEquals("4", AccessRuleTypeEnum.WHITELIST_BLACKLIST.code)
        assertEquals("Whitelist + Blacklist", AccessRuleTypeEnum.WHITELIST_BLACKLIST.displayText)
    }

    @Test
    fun entriesAndValueOf() {
        assertEquals(4, AccessRuleTypeEnum.entries.size)
        assertEquals(4, AccessRuleTypeEnum.entries.map { it.code }.toSet().size)
        assertEquals(AccessRuleTypeEnum.BLACKLIST, AccessRuleTypeEnum.valueOf("BLACKLIST"))
    }

}
