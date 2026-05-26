package io.kudos.ms.sys.core.tenant.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for SysTenantResourceDao
 *
 * Test data source: `SysTenantResourceDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysTenantResourceDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysTenantResourceDao: SysTenantResourceDao

    @Test
    fun searchResourceIdsByTenantId() {
        val tenantId = "40000000-0000-0000-0000-000000004229"
        val resourceIds = sysTenantResourceDao.searchResourceIdsByTenantId(tenantId)
        assertTrue(resourceIds.size >= 2)
        assertTrue(resourceIds.contains("40000000-0000-0000-0000-000000004229"))
        assertTrue(resourceIds.contains("40000000-0000-0000-0000-000000004229"))
    }

    @Test
    fun searchTenantIdsByResourceId() {
        val resourceId = "40000000-0000-0000-0000-000000004229"
        val tenantIds = sysTenantResourceDao.searchTenantIdsByResourceId(resourceId)
        assertTrue(tenantIds.size >= 2)
        assertTrue(tenantIds.contains("40000000-0000-0000-0000-000000004229"))
        assertTrue(tenantIds.contains("40000000-0000-0000-0000-000000004229"))
    }

    @Test
    fun exists() {
        // Test an existing relation
        assertTrue(sysTenantResourceDao.exists("40000000-0000-0000-0000-000000004229", "40000000-0000-0000-0000-000000004229"))

        // Test a non-existing relation
        assertFalse(sysTenantResourceDao.exists("40000000-0000-0000-0000-000000004229", "non-existent-resource-id"))
    }
}
