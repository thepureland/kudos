package io.kudos.ams.sys.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for SysTenantSubSystemDao
 *
 * 测试数据来源：`SysTenantSubSystemDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysTenantSubSystemDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysTenantSubSystemDao: SysTenantSubSystemDao

    @Test
    fun searchSubSystemCodesByTenantId() {
        val tenantId = "40000000-0000-0000-0000-000000000070"
        val codes = sysTenantSubSystemDao.searchSubSystemCodesByTenantId(tenantId)
        assertTrue(codes.size >= 2)
        assertTrue(codes.contains("svc-subsys-ts-dao-test-1"))
        assertTrue(codes.contains("svc-subsys-ts-dao-test-2"))
    }

    @Test
    fun searchTenantIdsBySubSystemCode() {
        val subSystemCode = "svc-subsys-ts-dao-test-1"
        val tenantIds = sysTenantSubSystemDao.searchTenantIdsBySubSystemCode(subSystemCode)
        assertTrue(tenantIds.size >= 2)
        assertTrue(tenantIds.contains("40000000-0000-0000-0000-000000000070"))
        assertTrue(tenantIds.contains("40000000-0000-0000-0000-000000000071"))
    }

    @Test
    fun groupingSubSystemCodesByTenantIds() {
        val tenantIds = listOf("40000000-0000-0000-0000-000000000070", "40000000-0000-0000-0000-000000000071")
        val grouping = sysTenantSubSystemDao.groupingSubSystemCodesByTenantIds(tenantIds)
        assertTrue(grouping.containsKey("40000000-0000-0000-0000-000000000070"))
        assertTrue(grouping["40000000-0000-0000-0000-000000000070"]!!.size >= 2)
        
        // 测试null参数（查询所有）
        val allGrouping = sysTenantSubSystemDao.groupingSubSystemCodesByTenantIds(null)
        assertTrue(allGrouping.isNotEmpty())
    }

    @Test
    fun groupingTenantIdsBySubSystemCodes() {
        val subSystemCodes = listOf("svc-subsys-ts-dao-test-1", "svc-subsys-ts-dao-test-2")
        val grouping = sysTenantSubSystemDao.groupingTenantIdsBySubSystemCodes(subSystemCodes)
        assertTrue(grouping.containsKey("svc-subsys-ts-dao-test-1"))
        assertTrue(grouping["svc-subsys-ts-dao-test-1"]!!.size >= 2)
        
        // 测试null参数（查询所有）
        val allGrouping = sysTenantSubSystemDao.groupingTenantIdsBySubSystemCodes(null)
        assertTrue(allGrouping.isNotEmpty())
    }

    @Test
    fun exists() {
        // 测试存在的关系
        assertTrue(sysTenantSubSystemDao.exists("40000000-0000-0000-0000-000000000070", "svc-subsys-ts-dao-test-1"))
        
        // 测试不存在的关系
        assertFalse(sysTenantSubSystemDao.exists("40000000-0000-0000-0000-000000000070", "non-existent-subsys"))
    }
}
