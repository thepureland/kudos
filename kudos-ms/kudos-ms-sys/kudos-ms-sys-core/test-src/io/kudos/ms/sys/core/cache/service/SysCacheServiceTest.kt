package io.kudos.ms.sys.core.cache.service

import io.kudos.base.error.ServiceException
import io.kudos.ms.sys.common.cache.enums.SysCacheErrorCodeEnum
import io.kudos.ms.sys.common.cache.vo.SysCacheCacheEntry
import io.kudos.ms.sys.core.cache.model.po.SysCache
import io.kudos.ms.sys.core.cache.service.iservice.ISysCacheService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * junit test for SysCacheService
 *
 * Test data source: `SysCacheServiceTest.sql`
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
    /** Read cache configuration by id from the configuration cache. */
    fun getCacheFromCache() {
        val cacheItem = sysCacheService.getCacheFromCache(existingId)
        assertNotNull(cacheItem)
        assertEquals(existingId, cacheItem.id)
        assertEquals("svc-cache-test-1", cacheItem.name)
    }

    @Test
    /** Read the list of cache configurations by atomic service code. */
    fun getCachesFromCache() {
        val caches = sysCacheService.getCachesFromCache(existingAtomicServiceCode)
        assertTrue(caches.any { it.name == "svc-cache-test-1" })
    }

    @Test
    /** Update enabled state and verify cache sync. */
    fun updateActive() {
        assertTrue(sysCacheService.updateActive(existingId, false))
        assertFalse(requireNotNull(sysCacheService.getCacheFromCache(existingId)).active)

        assertTrue(sysCacheService.updateActive(existingId, true))
        assertTrue(requireNotNull(sysCacheService.getCacheFromCache(existingId)).active)
    }

    @Test
    /** After insert, the entry is in the cache; after delete, it should be removed. */
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
    /** Updating a cache configuration should sync to the configuration cache. */
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
    /** Batch delete should also clean the configuration cache. */
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
    /** get(id, SysCacheCacheEntry::class) goes through the cache-read branch. */
    fun getAsCacheEntry() {
        val cacheEntry = sysCacheService.get(existingId, SysCacheCacheEntry::class)
        assertNotNull(cacheEntry)
        assertEquals(existingId, cacheEntry.id)
        assertEquals("svc-cache-test-1", cacheEntry.name)
    }

    @Test
    /** get(id, SysCache::class) goes through the DAO query branch. */
    fun getAsEntity() {
        val entity = sysCacheService.get(existingId, SysCache::class)
        assertNotNull(entity)
        assertEquals(existingId, entity.id)
        assertEquals("svc-cache-test-1", entity.name)
    }

    @Test
    /** Verify exists / evict / reloadAll behavior for hash caches. */
    fun existsKey() {
        val hashCacheId = getSysCacheHashConfigId()
        sysCacheService.reloadAll(hashCacheId)

        assertTrue(sysCacheService.existsKey(hashCacheId, existingId))

        sysCacheService.evict(hashCacheId, existingId)
        assertFalse(sysCacheService.existsKey(hashCacheId, existingId))

        sysCacheService.reloadAll(hashCacheId)
        assertTrue(sysCacheService.existsKey(hashCacheId, existingId))
    }

    @Test
    /** Verify getValueJson serialization for hash caches. */
    fun getValueJson() {
        val hashCacheId = getSysCacheHashConfigId()
        sysCacheService.reloadAll(hashCacheId)

        val json = sysCacheService.getValueJson(hashCacheId, existingId)
        assertTrue(json.contains(existingId))
        assertTrue(json.contains("svc-cache-test-1"))
    }

    @Test
    /** Verify single-key reload behavior for hash caches. */
    fun reload() {
        val hashCacheId = getSysCacheHashConfigId()
        sysCacheService.reloadAll(hashCacheId)
        assertTrue(sysCacheService.existsKey(hashCacheId, existingId))

        sysCacheService.reload(hashCacheId, existingId)
        assertTrue(sysCacheService.existsKey(hashCacheId, existingId))
    }

    @Test
    /** Verify full-reload backfill for hash caches. */
    fun reloadAll() {
        val hashCacheId = getSysCacheHashConfigId()

        sysCacheService.evictAll(hashCacheId)
        assertFalse(sysCacheService.existsKey(hashCacheId, existingId))

        sysCacheService.reloadAll(hashCacheId)
        assertTrue(sysCacheService.existsKey(hashCacheId, existingId))
    }

    @Test
    /** Verify single-key evict behavior for hash caches. */
    fun evict() {
        val hashCacheId = getSysCacheHashConfigId()
        sysCacheService.reload(hashCacheId, existingId)

        sysCacheService.evict(hashCacheId, existingId)
        assertFalse(sysCacheService.existsKey(hashCacheId, existingId))
    }

    @Test
    /** Verify evictAll clearing behavior for hash caches. */
    fun evictAll() {
        val hashCacheId = getSysCacheHashConfigId()
        sysCacheService.reload(hashCacheId, existingId)

        sysCacheService.evictAll(hashCacheId)
        assertFalse(sysCacheService.existsKey(hashCacheId, existingId))
    }

    @Test
    /** update returns false when id does not exist. */
    fun update_whenIdNotExists_returnsFalse() {
        val update = SysCache().apply {
            id = "not-exists-id"
            name = "svc-cache-service-test-update-not-exists"
            atomicServiceCode = existingAtomicServiceCode
            strategyDictCode = "SINGLE_LOCAL"
            writeOnBoot = true
            writeInTime = true
            ttl = 3600
            hash = false
        }
        assertFalse(sysCacheService.update(update))
    }

    @Test
    /** updateActive returns false when id does not exist. */
    fun updateActive_whenIdNotExists_returnsFalse() {
        assertFalse(sysCacheService.updateActive("not-exists-id", true))
    }

    @Test
    /** deleteById returns false when id does not exist. */
    fun deleteById_whenIdNotExists_returnsFalse() {
        assertFalse(sysCacheService.deleteById("not-exists-id"))
    }

    @Test
    /** In the key-value branch, a missing key should throw CACHE_KEY_NOT_FOUND. */
    fun keyValueCacheBranch_whenKeyNotFound_throwServiceException() {
        val keyValueCacheId = existingId // in the test SQL this config has hash=false, so it goes through the key-value branch
        val missingKey = "missing-key"

        val reloadEx = assertFailsWith<ServiceException> { sysCacheService.reload(keyValueCacheId, missingKey) }
        assertEquals(SysCacheErrorCodeEnum.CACHE_KEY_NOT_FOUND.code, reloadEx.errorCode.code)

        val evictEx = assertFailsWith<ServiceException> { sysCacheService.evict(keyValueCacheId, missingKey) }
        assertEquals(SysCacheErrorCodeEnum.CACHE_KEY_NOT_FOUND.code, evictEx.errorCode.code)

        val getValueEx = assertFailsWith<ServiceException> { sysCacheService.getValueJson(keyValueCacheId, missingKey) }
        assertEquals(SysCacheErrorCodeEnum.CACHE_KEY_NOT_FOUND.code, getValueEx.errorCode.code)
    }

    @Test
    /** In the key-value branch, existsKey returns false when the key is missing. */
    fun keyValueCacheBranch_existsKey_whenKeyNotFound_returnsFalse() {
        val keyValueCacheId = existingId // hash=false
        assertFalse(sysCacheService.existsKey(keyValueCacheId, "missing-key"))
    }

    @Test
    /** In the key-value branch, reloadAll / evictAll execute without throwing. */
    fun keyValueCacheBranch_reloadAllAndEvictAll_doNotThrow() {
        val keyValueCacheId = existingId // hash=false
        sysCacheService.reloadAll(keyValueCacheId)
        sysCacheService.evictAll(keyValueCacheId)
    }

    @Test
    /** id/key being blank should throw IllegalArgumentException. */
    fun cacheMethods_whenIdOrKeyBlank_throwIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> { sysCacheService.reload("", "k") }
        assertFailsWith<IllegalArgumentException> { sysCacheService.reload(existingId, "") }
        assertFailsWith<IllegalArgumentException> { sysCacheService.reloadAll("") }
        assertFailsWith<IllegalArgumentException> { sysCacheService.evict("", "k") }
        assertFailsWith<IllegalArgumentException> { sysCacheService.evict(existingId, "") }
        assertFailsWith<IllegalArgumentException> { sysCacheService.evictAll("") }
        assertFailsWith<IllegalArgumentException> { sysCacheService.existsKey("", "k") }
        assertFailsWith<IllegalArgumentException> { sysCacheService.existsKey(existingId, "") }
        assertFailsWith<IllegalArgumentException> { sysCacheService.getValueJson("", "k") }
        assertFailsWith<IllegalArgumentException> { sysCacheService.getValueJson(existingId, "") }
    }

    @Test
    /** Should throw CACHE_CONFIG_NOT_FOUND when the cache configuration does not exist. */
    fun cacheMethods_whenCacheConfigNotFound_throwServiceException() {
        val notFoundId = "00000000-0000-0000-0000-000000000000"

        val reloadEx = assertFailsWith<ServiceException> { sysCacheService.reload(notFoundId, "k") }
        assertEquals(SysCacheErrorCodeEnum.CACHE_CONFIG_NOT_FOUND.code, reloadEx.errorCode.code)

        val reloadAllEx = assertFailsWith<ServiceException> { sysCacheService.reloadAll(notFoundId) }
        assertEquals(SysCacheErrorCodeEnum.CACHE_CONFIG_NOT_FOUND.code, reloadAllEx.errorCode.code)

        val evictEx = assertFailsWith<ServiceException> { sysCacheService.evict(notFoundId, "k") }
        assertEquals(SysCacheErrorCodeEnum.CACHE_CONFIG_NOT_FOUND.code, evictEx.errorCode.code)

        val evictAllEx = assertFailsWith<ServiceException> { sysCacheService.evictAll(notFoundId) }
        assertEquals(SysCacheErrorCodeEnum.CACHE_CONFIG_NOT_FOUND.code, evictAllEx.errorCode.code)

        val existsEx = assertFailsWith<ServiceException> { sysCacheService.existsKey(notFoundId, "k") }
        assertEquals(SysCacheErrorCodeEnum.CACHE_CONFIG_NOT_FOUND.code, existsEx.errorCode.code)

        val getValueEx = assertFailsWith<ServiceException> { sysCacheService.getValueJson(notFoundId, "k") }
        assertEquals(SysCacheErrorCodeEnum.CACHE_CONFIG_NOT_FOUND.code, getValueEx.errorCode.code)
    }

    @Test
    /** In the hash branch, a missing key should throw CACHE_KEY_NOT_FOUND. */
    fun cacheMethods_whenKeyNotFound_throwServiceException() {
        val hashCacheId = getSysCacheHashConfigId()
        val missingKey = "missing-key"

        val reloadEx = assertFailsWith<ServiceException> { sysCacheService.reload(hashCacheId, missingKey) }
        assertEquals(SysCacheErrorCodeEnum.CACHE_KEY_NOT_FOUND.code, reloadEx.errorCode.code)

        val evictEx = assertFailsWith<ServiceException> { sysCacheService.evict(hashCacheId, missingKey) }
        assertEquals(SysCacheErrorCodeEnum.CACHE_KEY_NOT_FOUND.code, evictEx.errorCode.code)

        val getValueEx = assertFailsWith<ServiceException> { sysCacheService.getValueJson(hashCacheId, missingKey) }
        assertEquals(SysCacheErrorCodeEnum.CACHE_KEY_NOT_FOUND.code, getValueEx.errorCode.code)
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
