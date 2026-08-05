package io.kudos.ms.sys.core.accessrule.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * junit test for SysAccessRuleDao
 *
 * Test data source: `SysAccessRuleDaoTest.sql`
 *
 * Covers cache-entry projection (by id / by system+tenant / all) and primary-key lookup by dimension,
 * including the tenant-level and the platform-level (`tenant_id IS NULL`) branches plus blank-input guards.
 *
 * @author K
 * @author AI: Cursor
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysAccessRuleDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysAccessRuleDao: SysAccessRuleDao

    private val tenantRuleId = "40000000-0000-0000-0000-000000001791"
    private val tenantId = "40000000-0000-0000-0000-000000001791"
    private val tenantSystemCode = "svc-system-ar-dao-test-1_1456"
    private val platformRuleId = "40000000-0000-0000-0000-000000001792"
    private val platformSystemCode = "svc-system-ar-dao-test-plat_1456"

    @Test
    fun fetchCacheEntryById_found() {
        val entry = sysAccessRuleDao.fetchCacheEntryById(tenantRuleId)
        assertNotNull(entry)
        assertEquals(tenantRuleId, entry.id)
        assertEquals(tenantId, entry.tenantId)
        assertEquals(tenantSystemCode, entry.systemCode)
    }

    @Test
    fun fetchCacheEntryById_notFound_returnsNull() {
        assertNull(sysAccessRuleDao.fetchCacheEntryById("00000000-0000-0000-0000-000000000000"))
    }

    @Test
    fun fetchCacheEntryById_platformLevel_tenantIdNormalizedToEmpty() {
        val entry = sysAccessRuleDao.fetchCacheEntryById(platformRuleId)
        assertNotNull(entry)
        assertEquals("", entry.tenantId)
    }

    @Test
    fun fetchCacheEntryBySystemCodeAndTenantId_tenantBranch() {
        val entry = sysAccessRuleDao.fetchCacheEntryBySystemCodeAndTenantId(tenantSystemCode, tenantId)
        assertNotNull(entry)
        assertEquals(tenantRuleId, entry.id)
    }

    @Test
    fun fetchCacheEntryBySystemCodeAndTenantId_platformBranch_nullTenant() {
        val entry = sysAccessRuleDao.fetchCacheEntryBySystemCodeAndTenantId(platformSystemCode, null)
        assertNotNull(entry)
        assertEquals(platformRuleId, entry.id)
    }

    @Test
    fun fetchCacheEntryBySystemCodeAndTenantId_blankTenantTreatedAsPlatform() {
        val entry = sysAccessRuleDao.fetchCacheEntryBySystemCodeAndTenantId(platformSystemCode, "   ")
        assertNotNull(entry)
        assertEquals(platformRuleId, entry.id)
    }

    @Test
    fun fetchCacheEntryBySystemCodeAndTenantId_blankSystemCode_returnsNull() {
        assertNull(sysAccessRuleDao.fetchCacheEntryBySystemCodeAndTenantId("   ", tenantId))
    }

    @Test
    fun fetchCacheEntryBySystemCodeAndTenantId_noMatch_returnsNull() {
        assertNull(sysAccessRuleDao.fetchCacheEntryBySystemCodeAndTenantId("no-such-system", tenantId))
    }

    @Test
    fun fetchAllCacheEntries_includesSeededRows() {
        val all = sysAccessRuleDao.fetchAllCacheEntries()
        assertTrue(all.any { it.id == tenantRuleId })
        assertTrue(all.any { it.id == platformRuleId && it.tenantId == "" })
    }

    @Test
    fun findIdBySystemCodeAndTenantId_tenantBranch() {
        val id = sysAccessRuleDao.findIdBySystemCodeAndTenantId(tenantSystemCode, tenantId)
        assertEquals(tenantRuleId, id)
    }

    @Test
    fun findIdBySystemCodeAndTenantId_platformBranch_nullTenant() {
        val id = sysAccessRuleDao.findIdBySystemCodeAndTenantId(platformSystemCode, null)
        assertEquals(platformRuleId, id)
    }

    @Test
    fun findIdBySystemCodeAndTenantId_platformBranch_blankTenant() {
        val id = sysAccessRuleDao.findIdBySystemCodeAndTenantId(platformSystemCode, "  ")
        assertEquals(platformRuleId, id)
    }

    @Test
    fun findIdBySystemCodeAndTenantId_blankSystemCode_returnsNull() {
        assertNull(sysAccessRuleDao.findIdBySystemCodeAndTenantId("  ", tenantId))
    }

    @Test
    fun findIdBySystemCodeAndTenantId_noMatch_returnsNull() {
        assertNull(sysAccessRuleDao.findIdBySystemCodeAndTenantId("no-such-system", tenantId))
    }
}
