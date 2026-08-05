package io.kudos.ms.sys.common.accessrule.enums

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test for IpTypeEnum
 *
 * Covers all enum entries' code/displayText values and valueOf.
 *
 * @author K
 * @since 1.0.0
 */
internal class IpTypeEnumTest {

    @Test
    fun codesAndDisplayTexts() {
        assertEquals("ipv4", IpTypeEnum.IPV4.code)
        assertEquals("IPV4", IpTypeEnum.IPV4.displayText)
        assertEquals("ipv6", IpTypeEnum.IPV6.code)
        assertEquals("IPV6", IpTypeEnum.IPV6.displayText)
    }

    @Test
    fun entriesAndValueOf() {
        assertEquals(2, IpTypeEnum.entries.size)
        assertEquals(IpTypeEnum.IPV6, IpTypeEnum.valueOf("IPV6"))
    }

}
