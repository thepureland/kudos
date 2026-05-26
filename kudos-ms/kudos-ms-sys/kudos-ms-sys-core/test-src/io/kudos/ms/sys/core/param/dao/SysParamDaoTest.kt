package io.kudos.ms.sys.core.param.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for SysParamDao
 *
 * Test data source: `SysParamDaoTest.sql`
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
        // Case: atomicServiceCode provided
        val cacheItem1 = sysParamDao.getActiveParamsForCache("svc-module-param-dao-test-1", "svc-param-dao-test-1")
        assertNotNull(cacheItem1)
        
        // Case: atomicServiceCode is an empty string
        val cacheItem2 = sysParamDao.getActiveParamsForCache("", "svc-param-dao-test-3")
        assertNotNull(cacheItem2)
        
        // Case: parameter does not exist
        val cacheItem3 = sysParamDao.getActiveParamsForCache("svc-module-param-dao-test-1", "non-existent-param")
        assertNull(cacheItem3)
        
        // Case: inactive parameter (should not be found)
        val cacheItem4 = sysParamDao.getActiveParamsForCache("svc-module-param-dao-test-1", "svc-param-dao-test-4")
        assertNull(cacheItem4)
    }
}
