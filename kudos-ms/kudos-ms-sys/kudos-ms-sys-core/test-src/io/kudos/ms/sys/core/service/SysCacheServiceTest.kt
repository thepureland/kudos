package io.kudos.ms.sys.core.service

import io.kudos.ms.sys.common.vo.cache.SysCacheCacheEntry
import io.kudos.ms.sys.core.model.po.SysCache
import io.kudos.ms.sys.core.service.iservice.ISysCacheService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

    private val existingId = "20000000-0000-0000-0000-000000007838"
    private val existingAtomicServiceCode = "svc-as-cache-test-1_7838"

    @Resource
    private lateinit var sysCacheService: ISysCacheService

    @Test
    fun getCacheFromCache() {
        val cacheItem = sysCacheService.getCacheFromCache(existingId)
        assertNotNull(cacheItem)
        assertEquals(existingId, cacheItem.id)
        assertEquals("svc-cache-test-1", cacheItem.name)
    }

    @Test
    fun getCachesFromCache() {
        val caches = sysCacheService.getCachesFromCache(existingAtomicServiceCode)
        assertTrue(caches.any { it.name == "svc-cache-test-1" })
    }

    @Test
    fun updateActive() {
        assertTrue(sysCacheService.updateActive(existingId, false))
        assertFalse(requireNotNull(sysCacheService.getCacheFromCache(existingId)).active)

        assertTrue(sysCacheService.updateActive(existingId, true))
        assertTrue(requireNotNull(sysCacheService.getCacheFromCache(existingId)).active)
    }

    @Test
    fun insertAndDeleteById() {
        val cache = newCache(name = "svc-cache-service-test-insert")

        val id = sysCacheService.insert(cache)
        val inserted = sysCacheService.getCacheFromCache(id)
        assertNotNull(inserted)
        assertEquals("svc-cache-service-test-insert", inserted.name)
        assertTrue(sysCacheService.getCachesFromCache(existingAtomicServiceCode).any { it.id == id })

        assertTrue(sysCacheService.deleteById(id))
        assertNull(sysCacheService.getCacheFromCache(id))
        assertFalse(sysCacheService.getCachesFromCache(existingAtomicServiceCode).any { it.id == id })
    }

    @Test
    fun update() {
        val cache = newCache(name = "svc-cache-service-test-update-before")
        val id = sysCacheService.insert(cache)

        val update = SysCache().apply {
            this.id = id
            this.name = "svc-cache-service-test-update-after"
            this.atomicServiceCode = existingAtomicServiceCode
            this.strategyDictCode = "SINGLE_LOCAL"
            this.writeOnBoot = true
            this.writeInTime = true
            this.ttl = 7200
            this.hash = false
        }

        assertTrue(sysCacheService.update(update))

        val updated = sysCacheService.getCacheFromCache(id)
        assertNotNull(updated)
        assertEquals("svc-cache-service-test-update-after", updated.name)
        assertEquals(7200, updated.ttl)
    }

    @Test
    fun batchDelete() {
        val id1 = sysCacheService.insert(newCache(name = "svc-cache-service-test-batch-1"))
        val id2 = sysCacheService.insert(newCache(name = "svc-cache-service-test-batch-2"))

        val count = sysCacheService.batchDelete(listOf(id1, id2))
        assertEquals(2, count)
        assertNull(sysCacheService.getCacheFromCache(id1))
        assertNull(sysCacheService.getCacheFromCache(id2))
        assertFalse(sysCacheService.getCachesFromCache(existingAtomicServiceCode).any { it.id == id1 || it.id == id2 })
    }

    @Test
    fun getAsCacheEntry() {
        val cacheEntry = sysCacheService.get(existingId, SysCacheCacheEntry::class)
        assertNotNull(cacheEntry)
        assertEquals(existingId, cacheEntry.id)
        assertEquals("svc-cache-test-1", cacheEntry.name)
    }

    @Test
    fun existsKey() {
        val hashCacheId = getSysCacheHashConfigId()

        assertTrue(sysCacheService.existsKey(hashCacheId, existingId))

        sysCacheService.evict(hashCacheId, existingId)
        assertFalse(sysCacheService.existsKey(hashCacheId, existingId))

        sysCacheService.reload(hashCacheId, existingId)
        assertTrue(sysCacheService.existsKey(hashCacheId, existingId))
    }

    @Test
    fun getValueJson() {
        val hashCacheId = getSysCacheHashConfigId()

        val json = sysCacheService.getValueJson(hashCacheId, existingId)
        assertTrue(json.contains(existingId))
        assertTrue(json.contains("svc-cache-test-1"))
    }

    @Test
    fun reload() {
        val hashCacheId = getSysCacheHashConfigId()

        sysCacheService.evict(hashCacheId, existingId)
        assertFalse(sysCacheService.existsKey(hashCacheId, existingId))

        sysCacheService.reload(hashCacheId, existingId)
        assertTrue(sysCacheService.existsKey(hashCacheId, existingId))
    }

    @Test
    fun reloadAll() {
        val hashCacheId = getSysCacheHashConfigId()

        sysCacheService.evictAll(hashCacheId)
        assertFalse(sysCacheService.existsKey(hashCacheId, existingId))

        sysCacheService.reloadAll(hashCacheId)
        assertTrue(sysCacheService.existsKey(hashCacheId, existingId))
    }

    @Test
    fun evict() {
        val hashCacheId = getSysCacheHashConfigId()
        sysCacheService.reload(hashCacheId, existingId)

        sysCacheService.evict(hashCacheId, existingId)
        assertFalse(sysCacheService.existsKey(hashCacheId, existingId))
    }

    @Test
    fun evictAll() {
        val hashCacheId = getSysCacheHashConfigId()
        sysCacheService.reload(hashCacheId, existingId)

        sysCacheService.evictAll(hashCacheId)
        assertFalse(sysCacheService.existsKey(hashCacheId, existingId))
    }

    private fun newCache(name: String): SysCache {
        return SysCache().apply {
            this.name = name
            this.atomicServiceCode = existingAtomicServiceCode
            this.strategyDictCode = "SINGLE_LOCAL"
            this.writeOnBoot = true
            this.writeInTime = true
            this.ttl = 3600
            this.hash = false
        }
    }

    private fun getSysCacheHashConfigId(): String {
        return sysCacheService.getCachesFromCache("sys")
            .first { it.name == "SYS_CACHE__HASH" }
            .id
    }
}
