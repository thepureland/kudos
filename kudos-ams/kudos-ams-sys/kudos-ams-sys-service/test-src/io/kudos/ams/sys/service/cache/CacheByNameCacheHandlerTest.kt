package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ams.sys.common.vo.cache.SysCacheCacheItem
import io.kudos.ams.sys.service.biz.ibiz.ISysCacheBiz
import io.kudos.ams.sys.service.model.po.SysCache
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.containers.H2TestContainer
import org.soul.ability.cache.common.enums.CacheStrategy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
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
@EnableKudosTest
@EnabledIfDockerAvailable
class CacheByNameCacheHandlerTest {

    companion object {

        @DynamicPropertySource
        @JvmStatic
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.cache.enabled") { "true" }
            registry.add("cache.config.strategy") { CacheStrategy.SINGLE_LOCAL.name }
            H2TestContainer.startIfNeeded(registry)
        }

    }

    @Autowired
    private lateinit var cacheByNameCacheHandler: CacheByNameCacheHandler

    @Autowired
    private lateinit var sysCacheBiz: ISysCacheBiz

    @Test
    fun reloadAll() {
        cacheByNameCacheHandler.reloadAll()
        val cacheItem = CacheKit.getValue(cacheByNameCacheHandler.cacheName(), cacheByNameCacheHandler.cacheName())
        assert(cacheItem is SysCacheCacheItem)
        assertEquals(cacheByNameCacheHandler.cacheName(), (cacheItem as SysCacheCacheItem).name)
    }

    @Test
    fun getCacheFromCache() {
        val cacheItem1 = cacheByNameCacheHandler.getCacheFromCache(cacheByNameCacheHandler.cacheName())
        val cacheItem2 = cacheByNameCacheHandler.getCacheFromCache(cacheByNameCacheHandler.cacheName())
        assert(cacheItem1 === cacheItem2)
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
        val cacheId = "14a9adc4-6bb5-45bd-96bb-d8afe3060bea"
        val cacheName = cacheByNameCacheHandler.cacheName()
        val newTtl = 666666
        sysCacheBiz.updateProperties(cacheId, mapOf(SysCache::ttl.name to newTtl))

        // 同步缓存
        val sysCache = SysCache().apply { name = cacheName }
        cacheByNameCacheHandler.syncOnUpdate(sysCache, cacheId)

        // 验证缓存中的ttl
        val cacheItem1 = CacheKit.getValue(cacheName, cacheName)
        assertEquals(newTtl, (cacheItem1 as SysCacheCacheItem).ttl)
        val cacheItem2 = cacheByNameCacheHandler.getCacheFromCache(cacheName)
        assertEquals(newTtl, (cacheItem2 as SysCacheCacheItem).ttl)
    }

    @Test
    fun syncOnDelete() {
        // 删除数据库中的缓存配置
        val id = "e5340806-97b4-43a4-84c6-97e5e2966371"
        val name = TenantByIdCacheHandler.CACHE_NAME
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
        val id = "2da8e352-6e6f-4cd4-93e0-259ad3c7ea83"
        val name = TenantsBySubSysCacheHandler.CACHE_NAME
        val deleteSuccess = sysCacheBiz.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheByNameCacheHandler.synchOnBatchDelete(listOf(id), listOf(name))

        // 验证缓存中有没有
        val cacheItem1 = CacheKit.getValue(cacheByNameCacheHandler.cacheName(), name)
        assertNull(cacheItem1)
        val cacheItem2 = cacheByNameCacheHandler.getCacheFromCache(name)
        assertNull(cacheItem2)
    }

}