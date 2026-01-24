package io.kudos.ams.sys.provider.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for SysMicroServiceAtomicServiceDao
 *
 * 测试数据来源：`SysMicroServiceAtomicServiceDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysMicroServiceAtomicServiceDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysMicroServiceAtomicServiceDao: SysMicroServiceAtomicServiceDao

    @Test
    fun searchAtomicServiceCodesByMicroServiceCode() {
        val microServiceCode = "svc-ms-msas-dao-test-1"
        val codes = sysMicroServiceAtomicServiceDao.searchAtomicServiceCodesByMicroServiceCode(microServiceCode)
        assertTrue(codes.size >= 2)
        assertTrue(codes.contains("svc-as-msas-dao-test-1"))
        assertTrue(codes.contains("svc-as-msas-dao-test-2"))
    }

    @Test
    fun searchMicroServiceCodesByAtomicServiceCode() {
        val atomicServiceCode = "svc-as-msas-dao-test-1"
        val codes = sysMicroServiceAtomicServiceDao.searchMicroServiceCodesByAtomicServiceCode(atomicServiceCode)
        assertTrue(codes.size >= 2)
        assertTrue(codes.contains("svc-ms-msas-dao-test-1"))
        assertTrue(codes.contains("svc-ms-msas-dao-test-2"))
    }

    @Test
    fun exists() {
        // 测试存在的关系
        assertTrue(sysMicroServiceAtomicServiceDao.exists("svc-ms-msas-dao-test-1", "svc-as-msas-dao-test-1"))
        
        // 测试不存在的关系
        assertFalse(sysMicroServiceAtomicServiceDao.exists("svc-ms-msas-dao-test-1", "non-existent-as"))
    }
}
