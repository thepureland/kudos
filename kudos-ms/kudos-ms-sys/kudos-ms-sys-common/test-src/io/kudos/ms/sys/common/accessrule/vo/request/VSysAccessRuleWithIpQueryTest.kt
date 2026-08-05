package io.kudos.ms.sys.common.accessrule.vo.request

import io.kudos.ms.sys.common.accessrule.vo.response.VSysAccessRuleWithIpRow
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for VSysAccessRuleWithIpQuery
 *
 * Covers default values, full-args construction, ListSearchPayload overrides,
 * getNullProperties mapping branches (null passthrough, empty list, ipStartStr/ipEndStr
 * renaming, other-name passthrough), IIpStringToBigDecimalSupport accessors
 * and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class VSysAccessRuleWithIpQueryTest {

    @Test
    fun defaultsAreAllNull() {
        val q = VSysAccessRuleWithIpQuery()
        assertNull(q.id)
        assertNull(q.parentId)
        assertNull(q.tenantId)
        assertNull(q.systemCode)
        assertNull(q.accessRuleTypeDictCode)
        assertNull(q.parentActive)
        assertNull(q.parentBuiltIn)
        assertNull(q.ipId)
        assertNull(q.ipStartStr)
        assertNull(q.ipEndStr)
        assertNull(q.ipTypeDictCode)
        assertNull(q.explicitNullProperties)
    }

    @Test
    fun fullArgsConstruction() {
        val q = VSysAccessRuleWithIpQuery(
            id = "v-1",
            parentId = "p-1",
            tenantId = "t-1",
            systemCode = "sys-a",
            accessRuleTypeDictCode = "2",
            parentActive = true,
            parentBuiltIn = false,
            ipId = "ip-1",
            ipStartStr = "192.168.0.1",
            ipEndStr = "192.168.0.2",
            ipTypeDictCode = "ipv4",
            explicitNullProperties = listOf("tenantId"),
        )
        assertEquals("v-1", q.id)
        assertEquals("p-1", q.parentId)
        assertEquals("t-1", q.tenantId)
        assertEquals("sys-a", q.systemCode)
        assertEquals("2", q.accessRuleTypeDictCode)
        assertEquals(true, q.parentActive)
        assertEquals(false, q.parentBuiltIn)
        assertEquals("ip-1", q.ipId)
        assertEquals("192.168.0.1", q.getIpStartString())
        assertEquals("192.168.0.2", q.getIpEndString())
        assertEquals("ipv4", q.getIpTypeDictCodeString())
        assertEquals(BigDecimal("3232235521"), q.getIpStart())
        assertEquals(BigDecimal("3232235522"), q.getIpEnd())
    }

    @Test
    fun nullPropertiesPassthroughWhenNull() {
        assertNull(VSysAccessRuleWithIpQuery().getNullProperties())
    }

    @Test
    fun nullPropertiesEmptyListStaysEmpty() {
        val q = VSysAccessRuleWithIpQuery(explicitNullProperties = emptyList())
        assertEquals(emptyList(), q.getNullProperties())
    }

    @Test
    fun nullPropertiesRenamesIpStringFields() {
        val q = VSysAccessRuleWithIpQuery(
            explicitNullProperties = listOf("ipStartStr", "ipEndStr", "tenantId")
        )
        assertEquals(listOf("ipStart", "ipEnd", "tenantId"), q.getNullProperties())
    }

    @Test
    fun payloadOverrides() {
        val q = VSysAccessRuleWithIpQuery()
        assertEquals(VSysAccessRuleWithIpRow::class, q.getReturnEntityClass())
        assertTrue(q.isUnpagedSearchAllowed())
    }

    @Test
    fun dataClassContract() {
        val a = VSysAccessRuleWithIpQuery(id = "v-1", systemCode = "sys-a")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(ipId = "ip-9"))
        assertTrue(a.toString().contains("v-1"))
    }

}
