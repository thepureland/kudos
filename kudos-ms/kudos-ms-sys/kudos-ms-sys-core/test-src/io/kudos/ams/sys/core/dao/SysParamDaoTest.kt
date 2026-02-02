package io.kudos.ms.sys.core.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for SysParamDao
 *
 * 测试数据来源：`SysParamDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysParamDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysParamDao: SysParamDao

    @Test
    fun getActiveParamsForCache() {
        // 测试有atomicServiceCode的情况
        val cacheItem1 = sysParamDao.getActiveParamsForCache("svc-module-param-dao-test-1", "svc-param-dao-test-1")
        assertNotNull(cacheItem1)
        
        // 测试atomicServiceCode为空字符串的情况
        val cacheItem2 = sysParamDao.getActiveParamsForCache("", "svc-param-dao-test-3")
        assertNotNull(cacheItem2)
        
        // 测试不存在的参数
        val cacheItem3 = sysParamDao.getActiveParamsForCache("svc-module-param-dao-test-1", "non-existent-param")
        assertNull(cacheItem3)
        
        // 测试非启用的参数（应该找不到）
        val cacheItem4 = sysParamDao.getActiveParamsForCache("svc-module-param-dao-test-1", "svc-param-dao-test-4")
        assertNull(cacheItem4)
    }
}
