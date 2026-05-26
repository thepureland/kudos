package io.kudos.ms.sys.core.tenant.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for SysTenantLocaleDao
 *
 * Test data source: `SysTenantLocaleDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysTenantLocaleDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysTenantLocaleDao: SysTenantLocaleDao

    @Test
    fun searchLocaleCodesByTenantId() {
        val tenantId = "40000000-0000-0000-0000-000000003459"
        val codes = sysTenantLocaleDao.searchLocaleCodesByTenantId(tenantId)
        assertTrue(codes.size >= 2)
        assertTrue(codes.contains("zh-CN"))
        assertTrue(codes.contains("en-US"))
    }

    @Test
    fun searchTenantIdsByLocaleCode() {
        val localeCode = "zh-CN"
        val tenantIds = sysTenantLocaleDao.searchTenantIdsByLocaleCode(localeCode)
        assertTrue(tenantIds.size >= 2)
        assertTrue(tenantIds.contains("40000000-0000-0000-0000-000000003459"))
        assertTrue(tenantIds.contains("40000000-0000-0000-0000-000000003459"))
    }

    @Test
    fun exists() {
        // Test an existing relation
        assertTrue(sysTenantLocaleDao.exists("40000000-0000-0000-0000-000000003459", "zh-CN"))

        // Test a non-existing relation
        assertFalse(sysTenantLocaleDao.exists("40000000-0000-0000-0000-000000003459", "non-existent-lang"))
    }
}
