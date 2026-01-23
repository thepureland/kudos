package io.kudos.ams.sys.provider.service

import io.kudos.ams.sys.provider.service.iservice.ISysCacheService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.cache.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysCacheService
 *
 * 测试数据来源：`V1.0.0.27__SysCacheServiceTest.sql`
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
        val name = "svc-cache-test-1"
        val cacheItem = sysCacheService.getCacheFromCache(name)
        assertNotNull(cacheItem)
    }

    @Test
    fun getCachesByAtomicServiceCode() {
        val atomicServiceCode = "svc-as-cache-test-1"
        val caches = sysCacheService.getCachesByAtomicServiceCode(atomicServiceCode)
        assertTrue(caches.any { it.name == "svc-cache-test-1" })
    }

    @Test
    fun getAllActiveCaches() {
        val caches = sysCacheService.getAllActiveCaches()
        assertTrue(caches.isNotEmpty())
    }

    @Test
    fun updateActive() {
        val id = "20000000-0000-0000-0000-000000000027"
        assertTrue(sysCacheService.updateActive(id, false))
        assertTrue(sysCacheService.updateActive(id, true))
    }
}
