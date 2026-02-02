package io.kudos.ms.sys.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for SysSubSystemMicroServiceDao
 *
 * 测试数据来源：`SysSubSystemMicroServiceDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysSubSystemMicroServiceDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysSubSystemMicroServiceDao: SysSubSystemMicroServiceDao

    @Test
    fun searchMicroServiceCodesBySubSystemCode() {
        val subSystemCode = "svc-subsys-subsysms-dao-test-1"
        val codes = sysSubSystemMicroServiceDao.searchMicroServiceCodesBySubSystemCode(subSystemCode)
        assertTrue(codes.size >= 2)
        assertTrue(codes.contains("svc-ms-subsysms-dao-test-1_1506"))
        assertTrue(codes.contains("svc-ms-subsysms-dao-test-2_1506"))
    }

    @Test
    fun searchSubSystemCodesByMicroServiceCode() {
        val microServiceCode = "svc-ms-subsysms-dao-test-1_1506"
        val codes = sysSubSystemMicroServiceDao.searchSubSystemCodesByMicroServiceCode(microServiceCode)
        assertTrue(codes.size >= 2)
        assertTrue(codes.contains("svc-subsys-subsysms-dao-test-1"))
        assertTrue(codes.contains("svc-subsys-subsysms-dao-tes_1506"))
    }

    @Test
    fun exists() {
        // 测试存在的关系
        assertTrue(sysSubSystemMicroServiceDao.exists("svc-subsys-subsysms-dao-test-1", "svc-ms-subsysms-dao-test-1_1506"))
        
        // 测试不存在的关系
        assertFalse(sysSubSystemMicroServiceDao.exists("svc-subsys-subsysms-dao-test-1", "non-existent-ms"))
    }
}
