package io.kudos.ms.sys.common.accessrule.api

import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * test for ISysAccessRuleApi
 *
 * Covers the default parameter values of getAccessRuleByTenantAndSystem ("default"/"default")
 * via a fake implementation, including partial default-argument invocations, and updateActive dispatch.
 *
 * @author K
 * @since 1.0.0
 */
internal class ISysAccessRuleApiTest {

    private class FakeApi : ISysAccessRuleApi {

        var lastSystemCode: String? = null
        var lastTenantId: String? = null

        override fun getAccessRuleByTenantAndSystem(systemCode: String, tenantId: String): SysAccessRuleRow? {
            lastSystemCode = systemCode
            lastTenantId = tenantId
            return SysAccessRuleRow(id = "id-1", tenantId = tenantId, systemCode = systemCode)
        }

        override fun updateActive(id: String, active: Boolean): Boolean = active
    }

    @Test
    fun defaultArgumentsAreApplied() {
        val api = FakeApi()
        val row = api.getAccessRuleByTenantAndSystem()
        assertEquals("default", api.lastSystemCode)
        assertEquals("default", api.lastTenantId)
        assertEquals("default", row?.systemCode)
        assertEquals("default", row?.tenantId)
    }

    @Test
    fun partialDefaultArguments() {
        val api = FakeApi()
        api.getAccessRuleByTenantAndSystem(systemCode = "sys-a")
        assertEquals("sys-a", api.lastSystemCode)
        assertEquals("default", api.lastTenantId)

        api.getAccessRuleByTenantAndSystem(tenantId = "tenant-b")
        assertEquals("default", api.lastSystemCode)
        assertEquals("tenant-b", api.lastTenantId)
    }

    @Test
    fun explicitArgumentsOverrideDefaults() {
        val api = FakeApi()
        val row = api.getAccessRuleByTenantAndSystem("s1", "t1")
        assertEquals("s1", row?.systemCode)
        assertEquals("t1", row?.tenantId)
    }

    @Test
    fun updateActivePassesThrough() {
        val api = FakeApi()
        assertTrue(api.updateActive("id-1", true))
        assertFalse(api.updateActive("id-1", false))
    }

}
