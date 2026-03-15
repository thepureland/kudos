package io.kudos.ms.sys.core.service

import io.kudos.ms.sys.core.service.iservice.ISysCacheService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysCacheService
 *
 * 测试数据来源：`SysCacheServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysCacheServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysCacheService: ISysCacheService

    @Test
    fun getCacheFromCache() {
        val id = "20000000-0000-0000-0000-000000007838"
        val cacheItem = sysCacheService.getCacheFromCache(id)
        assertNotNull(cacheItem)
    }

    @Test
    fun getCachesFromCache() {
        val atomicServiceCode = "svc-as-cache-test-1_7838"
        val caches = sysCacheService.getCachesFromCache(atomicServiceCode)
        assertTrue(caches.any { it.name == "svc-cache-test-1" })
    }

    @Test
    fun updateActive() {
        val id = "20000000-0000-0000-0000-000000007838"
        assertTrue(sysCacheService.updateActive(id, false))
        assertTrue(sysCacheService.updateActive(id, true))
    }
}
