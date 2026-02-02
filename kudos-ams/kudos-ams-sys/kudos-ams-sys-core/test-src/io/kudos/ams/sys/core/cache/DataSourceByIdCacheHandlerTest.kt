package io.kudos.ams.sys.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ams.sys.common.vo.datasource.SysDataSourceCacheItem
import io.kudos.ams.sys.core.dao.SysDataSourceDao
import io.kudos.ams.sys.core.model.po.SysDataSource
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for DataSourceByIdCacheHandler
 *
 * 测试数据来源：`DataSourceByIdCacheHandlerTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
open class DataSourceByIdCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: DataSourceByIdCacheHandler

    @Resource
    private lateinit var sysDataSourceDao: SysDataSourceDao

    private val newUrl = "new_url"

    @Test
    fun reloadAll() {
        // 清除并重载缓存，保证与数据库中的数据一致
        cacheHandler.reloadAll(true)

        // 获取当前缓存中的记录
        val idCache = "3d2acef6-e828-43c5-a512-111111111111"
        val cacheItem = cacheHandler.getDataSourceById(idCache)

        // 插入新的记录到数据库
        val idNew = insertNewRecordToDb()

        // 更新数据库的记录
        val idUpdate = "3d2acef6-e828-43c5-a512-222222222222"
        sysDataSourceDao.updateProperties(idUpdate, mapOf(SysDataSource::url.name to newUrl))

        // 从数据库中删除记录
        val idDelete = "3d2acef6-e828-43c5-a512-333333333333"
        sysDataSourceDao.deleteById(idDelete)

        // 重载缓存，但不清除旧缓存
        cacheHandler.reloadAll(false)

        // 原来缓存中的记录内存地址会变
        val cacheItem1 = cacheHandler.getDataSourceById(idCache)
        assert(cacheItem !== cacheItem1)

        // 数据库中新增的记录在缓存应该要存在
        val cacheItemNew = cacheHandler.getDataSourceById(idNew)
        assertNotNull(cacheItemNew)

        // 数据库中更新的记录在缓存中应该也更新了
        val cacheItemUpdate = cacheHandler.getDataSourceById(idUpdate)
        assertEquals(newUrl, cacheItemUpdate!!.url)

        // 数据库中删除的记录在缓存中应该还在
        var cacheItemDelete = cacheHandler.getDataSourceById(idDelete)
        assertNotNull(cacheItemDelete)


        // 清除并重载缓存
        cacheHandler.reloadAll(true)

        // 数据库中删除的记录在缓存中应该不存在了
        cacheItemDelete = cacheHandler.getDataSourceById(idDelete)
        assertNull(cacheItemDelete)
    }

    @Test
    fun getDataSourceById() {
        // 存在的
        var id = "3d2acef6-e828-43c5-a512-111111111111"
        val cacheItem2 = cacheHandler.getDataSourceById(id)
        val cacheItem3 = cacheHandler.getDataSourceById(id)
        assertNotNull(cacheItem2)
        assert(cacheItem3 === cacheItem2)

        // 不存在的
        id = "no_exist_id"
        val cacheItem = cacheHandler.getDataSourceById(id)
        assertNull(cacheItem)
    }

    @Test
    fun getDataSourcesByIds() {
        val id1 = "3d2acef6-e828-43c5-a512-111111111111"
        val id2 = "3d2acef6-e828-43c5-a512-222222222222"
        val result2 = cacheHandler.getDataSourcesByIds(listOf(id1, id2))
        val result3 = cacheHandler.getDataSourcesByIds(listOf(id1, id2))
        assert(result2.isNotEmpty())
        assert(result3 == result2)
    }

    @Test
    fun syncOnInsert() {
        // 插入新的记录到数据库
        val id = insertNewRecordToDb()

        // 同步缓存
        cacheHandler.syncOnInsert(id)

        // 验证新记录是否在缓存中
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), id)
        assertNotNull(cacheItem1)
        val cacheItem2 = cacheHandler.getDataSourceById(id)
//        assert(cacheItem1 === cacheItem2) //??? 在有断点时，有时会成立
        val cacheItem3 = cacheHandler.getDataSourceById(id)
        assert(cacheItem2 === cacheItem3)
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val id = "3d2acef6-e828-43c5-a512-222222222222"
        val success = sysDataSourceDao.updateProperties(id, mapOf(SysDataSource::url.name to newUrl))
        assert(success)

        // 同步缓存
        cacheHandler.syncOnUpdate(id)

        // 验证缓存中的记录
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), id)
        assertEquals(newUrl, (cacheItem1 as SysDataSourceCacheItem).url)
        val cacheItem2 = cacheHandler.getDataSourceById(id)
        assertEquals(newUrl, (cacheItem2 as SysDataSourceCacheItem).url)
    }

    @Test
    fun syncOnDelete() {
        // 删除数据库中的记录
        val id = "3d2acef6-e828-43c5-a512-333333333333"
        val deleteSuccess = sysDataSourceDao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(id)

        // 验证缓存中有没有
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), id)
        assertNull(cacheItem1)
        val cacheItem2 = cacheHandler.getDataSourceById(id)
        assertNull(cacheItem2)
    }

    @Test
    fun syncOnBatchDelete() {
        // 批量删除数据库中的记录
        val id1 = "3d2acef6-e828-43c5-a512-444444444444"
        val id2 = "3d2acef6-e828-43c5-a512-555555555555"
        val ids = listOf(id1, id2)
        val count = sysDataSourceDao.batchDelete(ids)
        assert(count == 2)

        // 同步缓存
        cacheHandler.syncOnBatchDelete(ids)

        // 验证缓存中有没有
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), id1)
        assertNull(cacheItem1)
        val cacheItem2 = cacheHandler.getDataSourceById(id1)
        assertNull(cacheItem2)
        val cacheItem3 = CacheKit.getValue(cacheHandler.cacheName(), id2)
        assertNull(cacheItem3)
        val cacheItem4 = cacheHandler.getDataSourceById(id2)
        assertNull(cacheItem4)
    }

    private fun insertNewRecordToDb() : String {
        val sysDataSource = SysDataSource().apply {
            name = "a_new_test_ds"
            url = "url"
            username = "sa"
            password = "sa"
            subSystemCode = "default"
        }
        return sysDataSourceDao.insert(sysDataSource)
    }

}
