package io.kudos.ams.sys.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for SysTenantSystemDao
 *
 * 测试数据来源：`SysTenantSystemDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysTenantSystemDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysTenantSystemDao: SysTenantSystemDao

    @Test
    fun searchSystemCodesByTenantId() {
        val tenantId = "40000000-0000-0000-0000-000000000070"
        val codes = sysTenantSystemDao.searchSystemCodesByTenantId(tenantId)
        assertTrue(codes.size >= 2)
        assertTrue(codes.contains("svc-subsys-ts-dao-test-1"))
        assertTrue(codes.contains("svc-subsys-ts-dao-test-2"))
    }

    @Test
    fun searchTenantIdsBySystemCode() {
        val systemCode = "svc-subsys-ts-dao-test-1"
        val tenantIds = sysTenantSystemDao.searchTenantIdsBySystemCode(systemCode)
        assertTrue(tenantIds.size >= 2)
        assertTrue(tenantIds.contains("40000000-0000-0000-0000-000000000070"))
        assertTrue(tenantIds.contains("40000000-0000-0000-0000-000000000071"))
    }

    @Test
    fun groupingSystemCodesByTenantIds() {
        val tenantIds = listOf("40000000-0000-0000-0000-000000000070", "40000000-0000-0000-0000-000000000071")
        val grouping = sysTenantSystemDao.groupingSystemCodesByTenantIds(tenantIds)
        assertTrue(grouping.containsKey("40000000-0000-0000-0000-000000000070"))
        assertTrue(grouping["40000000-0000-0000-0000-000000000070"]!!.size >= 2)

        // 测试null参数（查询所有）
        val allGrouping = sysTenantSystemDao.groupingSystemCodesByTenantIds(null)
        assertTrue(allGrouping.isNotEmpty())
    }

    @Test
    fun groupingTenantIdsBySystemCodes() {
        val systemCodes = listOf("svc-subsys-ts-dao-test-1", "svc-subsys-ts-dao-test-2")
        val grouping = sysTenantSystemDao.groupingTenantIdsBySystemCodes(systemCodes)
        assertTrue(grouping.containsKey("svc-subsys-ts-dao-test-1"))
        assertTrue(grouping["svc-subsys-ts-dao-test-1"]!!.size >= 2)

        // 测试null参数（查询所有）
        val allGrouping = sysTenantSystemDao.groupingTenantIdsBySystemCodes(null)
        assertTrue(allGrouping.isNotEmpty())
    }

    @Test
    fun exists() {
        // 测试存在的关系
        assertTrue(sysTenantSystemDao.exists("40000000-0000-0000-0000-000000000070", "svc-subsys-ts-dao-test-1"))

        // 测试不存在的关系
        assertFalse(sysTenantSystemDao.exists("40000000-0000-0000-0000-000000000070", "non-existent-subsys"))
    }
}
