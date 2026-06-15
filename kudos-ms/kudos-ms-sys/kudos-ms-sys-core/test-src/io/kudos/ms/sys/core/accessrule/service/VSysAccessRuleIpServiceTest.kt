package io.kudos.ms.sys.core.accessrule.service

import io.kudos.ms.sys.core.accessrule.service.iservice.IVSysAccessRuleIpService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * junit test for [io.kudos.ms.sys.core.accessrule.service.impl.VSysAccessRuleIpService] (read-only view service).
 *
 * Test data source: `VSysAccessRuleIpServiceTest.sql`
 *
 * Covers query-by-parentId and query-by-(systemCode, tenantId) including the tenant-level branch,
 * the platform-level (`tenant_id IS NULL`) branch, and the blank-tenant-treated-as-platform branch.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class VSysAccessRuleIpServiceTest : RdbTestBase() {

    @Resource
    private lateinit var service: IVSysAccessRuleIpService

    private val tenantParentId = "50000000-0000-0000-0000-000000000801"
    private val platformParentId = "50000000-0000-0000-0000-000000000802"
    private val tenantId = "50000000-0000-0000-0000-000000000901"
    private val tenantSystemCode = "svc-system-vsys-test-1"
    private val platformSystemCode = "svc-system-vsys-test-plat"

    @Test
    fun searchByParentId_returnsAllIpRowsOrderedByIpStart() {
        val rows = service.searchByParentId(tenantParentId)
        assertEquals(2, rows.size)
        // ordered by ipStart ascending
        assertTrue(rows[0].ipStart!! <= rows[1].ipStart!!)
        assertTrue(rows.all { it.parentId == tenantParentId })
    }

    @Test
    fun searchByParentId_noMatch_returnsEmpty() {
        assertTrue(service.searchByParentId("00000000-0000-0000-0000-000000000000").isEmpty())
    }

    @Test
    fun searchBySystemCodeAndTenantId_tenantBranch() {
        val rows = service.searchBySystemCodeAndTenantId(tenantSystemCode, tenantId)
        assertEquals(2, rows.size)
        assertTrue(rows.all { it.systemCode == tenantSystemCode && it.tenantId == tenantId })
    }

    @Test
    fun searchBySystemCodeAndTenantId_platformBranch_nullTenant() {
        val rows = service.searchBySystemCodeAndTenantId(platformSystemCode, null)
        assertEquals(1, rows.size)
        assertEquals(platformParentId, rows.first().parentId)
        assertEquals(null, rows.first().tenantId)
    }

    @Test
    fun searchBySystemCodeAndTenantId_blankTenant_treatedAsPlatform() {
        // service normalizes a blank tenantId to null (platform level)
        val rows = service.searchBySystemCodeAndTenantId(platformSystemCode, "   ")
        assertEquals(1, rows.size)
        assertEquals(platformParentId, rows.first().parentId)
    }

    @Test
    fun searchBySystemCodeAndTenantId_noMatch_returnsEmpty() {
        assertTrue(service.searchBySystemCodeAndTenantId("no-such-system", tenantId).isEmpty())
    }
}
