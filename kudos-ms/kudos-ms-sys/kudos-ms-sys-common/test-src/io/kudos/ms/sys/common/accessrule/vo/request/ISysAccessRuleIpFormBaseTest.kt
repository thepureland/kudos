package io.kudos.ms.sys.common.accessrule.vo.request

import io.kudos.base.bean.validation.support.Group
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test for ISysAccessRuleIpFormBase
 *
 * Covers GroupSequenceProvider.getGroups: ipv4 type code selects Group.First,
 * any other type code selects Group.Second.
 *
 * @author K
 * @since 1.0.0
 */
internal class ISysAccessRuleIpFormBaseTest {

    private fun form(ipTypeDictCode: String) = SysAccessRuleIpFormCreate(
        ipv4StartStr = "1.2.3.4",
        ipv4EndStr = "1.2.3.5",
        ipv6StartStr = "::1",
        ipv6EndStr = "::2",
        ipTypeDictCode = ipTypeDictCode,
        expirationDate = null,
        systemCode = "sys",
        tenantId = "t-1",
        remark = null,
    )

    @Test
    fun ipv4TypeSelectsFirstGroup() {
        val provider = ISysAccessRuleIpFormBase.GroupSequenceProvider()
        assertEquals(listOf(Group.First::class), provider.getGroups(form("ipv4")))
    }

    @Test
    fun nonIpv4TypeSelectsSecondGroup() {
        val provider = ISysAccessRuleIpFormBase.GroupSequenceProvider()
        assertEquals(listOf(Group.Second::class), provider.getGroups(form("ipv6")))
        assertEquals(listOf(Group.Second::class), provider.getGroups(form("")))
        assertEquals(listOf(Group.Second::class), provider.getGroups(form("other")))
    }

}
