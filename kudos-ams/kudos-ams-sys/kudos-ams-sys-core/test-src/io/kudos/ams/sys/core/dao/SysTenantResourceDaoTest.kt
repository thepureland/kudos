package io.kudos.ams.sys.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for SysTenantResourceDao
 *
 * 测试数据来源：`SysTenantResourceDaoTest.sql`
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
        val tenantId = "40000000-0000-0000-0000-000000000080"
        val resourceIds = sysTenantResourceDao.searchResourceIdsByTenantId(tenantId)
        assertTrue(resourceIds.size >= 2)
        assertTrue(resourceIds.contains("40000000-0000-0000-0000-000000000082"))
        assertTrue(resourceIds.contains("40000000-0000-0000-0000-000000000083"))
    }

    @Test
    fun searchTenantIdsByResourceId() {
        val resourceId = "40000000-0000-0000-0000-000000000082"
        val tenantIds = sysTenantResourceDao.searchTenantIdsByResourceId(resourceId)
        assertTrue(tenantIds.size >= 2)
        assertTrue(tenantIds.contains("40000000-0000-0000-0000-000000000080"))
        assertTrue(tenantIds.contains("40000000-0000-0000-0000-000000000081"))
    }

    @Test
    fun exists() {
        // 测试存在的关系
        assertTrue(sysTenantResourceDao.exists("40000000-0000-0000-0000-000000000080", "40000000-0000-0000-0000-000000000082"))
        
        // 测试不存在的关系
        assertFalse(sysTenantResourceDao.exists("40000000-0000-0000-0000-000000000080", "non-existent-resource-id"))
    }
}
