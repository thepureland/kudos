package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ms.sys.common.vo.cache.SysCacheCacheItem
import io.kudos.ms.sys.core.dao.SysCacheDao
import io.kudos.ms.sys.core.model.po.SysCache
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for CacheByNameCacheHandler
 *
 * 测试数据来源：`CacheByNameCacheTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class CacheByNameCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheByNameCache: CacheByNameCache

    @Resource
    private lateinit var sysCacheDao: SysCacheDao

    private val newCacheName = "a_new_test_cache"

    @Test
    fun reloadAll() {
        // 清除并重载缓存，保证与数据库中的数据一致
        cacheByNameCache.reloadAll(true)

        // 获取当前缓存中的记录
        val cacheName = "TEST_CACHE_1"
        val cacheItem = cacheByNameCache.getCache(cacheName)

        // 插入新的记录到数据库
        val sysCache = insertNewRecordToDb()

        // 更新数据库的记录
        val cacheIdUpdate = "e5340806-97b4-43a4-84c6-222222225162"
        val cacheNameUpdate = "TEST_CACHE_2"
        val newTtl = 666666
        sysCacheDao.updateProperties(cacheIdUpdate, mapOf(SysCache::ttl.name to newTtl))

        // 从数据库中删除记录
        val idDelete = "e5340806-97b4-43a4-84c6-333333335162"
        val cacheNameDelete = "TEST_CACHE_3"
        sysCacheDao.deleteById(idDelete)

        // 重载缓存，但不清除旧缓存
        cacheByNameCache.reloadAll(false)

        // 原来缓存中的记录内存地址会变
        val cacheItem1 = cacheByNameCache.getCache(cacheName)
        assert(cacheItem !== cacheItem1)

        // 数据库中新增的记录在缓存应该要存在
        val cacheItemNew = cacheByNameCache.getCache(sysCache.name)
        assertNotNull(cacheItemNew)

        // 数据库中更新的记录在缓存中应该也更新了
        val cacheItemUpdate = cacheByNameCache.getCache(cacheNameUpdate)
        assertEquals(newTtl, cacheItemUpdate!!.ttl)

        // 数据库中删除的记录在缓存中应该还在
        var cacheItemDelete = cacheByNameCache.getCache(cacheNameDelete)
        assertNotNull(cacheItemDelete)


        // 清除并重载缓存
        cacheByNameCache.reloadAll(true)

        // 数据库中删除的记录在缓存中应该不存在了
        cacheItemDelete = cacheByNameCache.getCache(cacheNameDelete)
        assertNull(cacheItemDelete)
    }

    @Test
    fun getCache() {
        var cacheName = "TEST_CACHE_1"
        val cacheItem2 = cacheByNameCache.getCache(cacheName)
        val cacheItem3 = cacheByNameCache.getCache(cacheName)
        assert(cacheItem2 === cacheItem3)

        // active为false的在缓存中应该不存在
        cacheName = "TEST_CACHE_6"
        assertNull(cacheByNameCache.getCache(cacheName))
    }

    @Test
    fun syncOnInsert() {
        // 插入新的记录到数据库
        val sysCache = insertNewRecordToDb()

        // 同步缓存
        cacheByNameCache.syncOnInsert(sysCache, sysCache.id!!)

        // 验证新记录是否在缓存中
        val cacheItem1 = CacheKit.getValue(cacheByNameCache.cacheName(), newCacheName)
        assertNotNull(cacheItem1)
        val cacheItem2 = cacheByNameCache.getCache(newCacheName)
        assert(cacheItem1 === cacheItem2)
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val cacheId = "e5340806-97b4-43a4-84c6-222222225162"
        val cacheName = "TEST_CACHE_2"
        val newTtl = 666666
        val success = sysCacheDao.updateProperties(cacheId, mapOf(SysCache::ttl.name to newTtl))
        assert(success)

        // 同步缓存
        val sysCache = SysCache().apply { name = cacheName }
        cacheByNameCache.syncOnUpdate(sysCache, cacheId)

        // 验证缓存中的记录
        val cacheItem1 = CacheKit.getValue(cacheByNameCache.cacheName(), cacheName)
        assertEquals(newTtl, (cacheItem1 as SysCacheCacheItem).ttl)
        val cacheItem2 = cacheByNameCache.getCache(cacheName)
        assertEquals(newTtl, (cacheItem2 as SysCacheCacheItem).ttl)
    }

    @Test
    fun syncOnDelete() {
        // 删除数据库中的记录
        val id = "e5340806-97b4-43a4-84c6-333333335162"
        val name = "TEST_CACHE_3"
        val deleteSuccess = sysCacheDao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheByNameCache.syncOnDelete(id, name)

        // 验证缓存中有没有
        val cacheItem1 = CacheKit.getValue(cacheByNameCache.cacheName(), name)
        assertNull(cacheItem1)
        val cacheItem2 = cacheByNameCache.getCache(name)
        assertNull(cacheItem2)
    }

    @Test
    fun syncOnBatchDelete() {
        // 批量删除数据库中的记录
        val id1 = "2da8e352-6e6f-4cd4-93e0-444444445162"
        val name1 = "TEST_CACHE_4"
        val id2 = "2da8e352-6e6f-4cd4-93e0-555555555162"
        val name2 = "TEST_CACHE_5"
        val ids = listOf(id1, id2)
        val count = sysCacheDao.batchDelete(ids)
        assert(count == 2)

        // 同步缓存
        cacheByNameCache.syncOnBatchDelete(ids, listOf(name1, name2))

        // 验证缓存中有没有
        val cacheItem1 = CacheKit.getValue(cacheByNameCache.cacheName(), name1)
        assertNull(cacheItem1)
        val cacheItem2 = cacheByNameCache.getCache(name1)
        assertNull(cacheItem2)
        val cacheItem3 = CacheKit.getValue(cacheByNameCache.cacheName(), name2)
        assertNull(cacheItem3)
        val cacheItem4 = cacheByNameCache.getCache(name2)
        assertNull(cacheItem4)
    }

    private fun insertNewRecordToDb() : SysCache {
        val sysCache = SysCache().apply {
            name = newCacheName
            atomicServiceCode = "ams-sys"
            strategyDictCode = CacheStrategy.SINGLE_LOCAL.name
            writeOnBoot = true
            writeInTime = true
            ttl = 666666
            hash = false
        }
        sysCacheDao.insert(sysCache)
        return sysCache
    }

}