package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ams.sys.common.vo.cache.SysCacheCacheItem
import io.kudos.ams.sys.service.biz.ibiz.ISysCacheBiz
import io.kudos.ams.sys.service.model.po.SysCache
import org.junit.jupiter.api.AfterAll
import org.soul.ability.cache.common.enums.CacheStrategy
import org.springframework.beans.factory.annotation.Autowired
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for CacheByNameCacheHandler
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerAvailable
class CacheByNameCacheHandlerTest : CacheHandlerTestBase() {

    @Autowired
    private lateinit var cacheByNameCacheHandler: CacheByNameCacheHandler

    @Autowired
    private lateinit var sysCacheBiz: ISysCacheBiz

    @AfterAll
    fun reloadAll() {
        val cacheId = "e5340806-97b4-43a4-84c6-22222"
        val newTtl = 999999999
        val success = sysCacheBiz.updateProperties(cacheId, mapOf(SysCache::ttl.name to newTtl))
        assert(success)

        cacheByNameCacheHandler.reloadAll()
        val cacheItem = CacheKit.getValue(cacheByNameCacheHandler.cacheName(), cacheByNameCacheHandler.cacheName())
        assert(cacheItem is SysCacheCacheItem)
        assertEquals(cacheByNameCacheHandler.cacheName(), (cacheItem as SysCacheCacheItem).name)

        val cacheName = "TEST_CACHE_2"
        val cacheItem1 = CacheKit.getValue(cacheByNameCacheHandler.cacheName(), cacheName)
        assertEquals(999999999, (cacheItem1 as SysCacheCacheItem).ttl)
        val cacheItem2 = cacheByNameCacheHandler.getCacheFromCache(cacheName)
        assertEquals(999999999, (cacheItem2 as SysCacheCacheItem).ttl)
    }

    @Test
    fun getCacheFromCache() {
        val cacheName = "TEST_CACHE_1"
        cacheByNameCacheHandler.getCacheFromCache(cacheName)
        val cacheItem2 = cacheByNameCacheHandler.getCacheFromCache(cacheName)
        val cacheItem3 = cacheByNameCacheHandler.getCacheFromCache(cacheName)
        assert(cacheItem2 === cacheItem3)
    }

    @Test
    fun syncOnInsert() {
        // 插入新的缓存配置到数据库
        val newCacheName = "a_new_test_cache"
        val sysCache = SysCache().apply {
            name = newCacheName
            atomicServiceCode = "ams-sys"
            strategyDictCode = CacheStrategy.SINGLE_LOCAL.name
            writeOnBoot = true
            writeInTime = true
            ttl = 666666
        }
        val id = sysCacheBiz.insert(sysCache)

        // 同步缓存
        cacheByNameCacheHandler.syncOnInsert(sysCache, id)

        // 验证新对象是否在缓存中
        val cacheItem1 = CacheKit.getValue(cacheByNameCacheHandler.cacheName(), newCacheName)
        assertNotNull(cacheItem1)
        val cacheItem2 = cacheByNameCacheHandler.getCacheFromCache(newCacheName)
        assert(cacheItem1 === cacheItem2)
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的缓存配置的ttl
        val cacheId = "e5340806-97b4-43a4-84c6-22222"
        val cacheName = "TEST_CACHE_2"
        val newTtl = 666666
        val success = sysCacheBiz.updateProperties(cacheId, mapOf(SysCache::ttl.name to newTtl))
        assert(success)

        // 同步缓存
        val sysCache = SysCache().apply { name = cacheName }
        cacheByNameCacheHandler.syncOnUpdate(sysCache, cacheId)

        // 验证缓存中的ttl
        val cacheItem1 = CacheKit.getValue(cacheByNameCacheHandler.cacheName(), cacheName)
        assertEquals(newTtl, (cacheItem1 as SysCacheCacheItem).ttl)
        val cacheItem2 = cacheByNameCacheHandler.getCacheFromCache(cacheName)
        assertEquals(newTtl, (cacheItem2 as SysCacheCacheItem).ttl)
    }

    @Test
    fun syncOnDelete() {
        // 删除数据库中的缓存配置
        val id = "e5340806-97b4-43a4-84c6-33333"
        val name = "TEST_CACHE_3"
        val deleteSuccess = sysCacheBiz.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheByNameCacheHandler.syncOnDelete(id, name)

        // 验证缓存中有没有
        val cacheItem1 = CacheKit.getValue(cacheByNameCacheHandler.cacheName(), name)
        assertNull(cacheItem1)
        val cacheItem2 = cacheByNameCacheHandler.getCacheFromCache(name)
        assertNull(cacheItem2)
    }

    @Test
    fun synchOnBatchDelete() {
        // 删除数据库中的缓存配置
        val id1 = "2da8e352-6e6f-4cd4-93e0-44444"
        val name1 = "TEST_CACHE_4"
        var deleteSuccess = sysCacheBiz.deleteById(id1)
        assert(deleteSuccess)
        val id2 = "2da8e352-6e6f-4cd4-93e0-55555"
        val name2 = "TEST_CACHE_5"
        deleteSuccess = sysCacheBiz.deleteById(id2)
        assert(deleteSuccess)


        // 同步缓存
        cacheByNameCacheHandler.synchOnBatchDelete(listOf(id1, id2), listOf(name1, name2))

        // 验证缓存中有没有
        val cacheItem1 = CacheKit.getValue(cacheByNameCacheHandler.cacheName(), name1)
        assertNull(cacheItem1)
        val cacheItem2 = cacheByNameCacheHandler.getCacheFromCache(name1)
        assertNull(cacheItem2)
        val cacheItem3 = CacheKit.getValue(cacheByNameCacheHandler.cacheName(), name2)
        assertNull(cacheItem3)
        val cacheItem4 = cacheByNameCacheHandler.getCacheFromCache(name2)
        assertNull(cacheItem4)
    }

}