package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ams.sys.common.vo.datasource.SysDataSourceCacheItem
import io.kudos.ams.sys.service.dao.SysDataSourceDao
import io.kudos.ams.sys.service.model.po.SysDataSource
import org.junit.jupiter.api.AfterAll
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for DataSourceByIdCacheHandler
 *
 * @author K
 * @since 1.0.0
 */
class DataSourceByIdCacheHandlerTest : CacheHandlerTestBase() {

    @Autowired
    private lateinit var dataSourceByIdCacheHandler: DataSourceByIdCacheHandler

    @Autowired
    private lateinit var sysDataSourceDao: SysDataSourceDao

    @AfterAll
    fun reloadAll() {

    }

    @Test
    fun getDataSourceById() {
        val id = "3d2acef6-e828-43c5-a512-111111111111"
        dataSourceByIdCacheHandler.getDataSourceById(id) // 第一次当放入远程缓存后，会发送清除本地缓存，所以最终取到的是远程缓存反序列化后的对象
        val cacheItem2 = dataSourceByIdCacheHandler.getDataSourceById(id)
        val cacheItem3 = dataSourceByIdCacheHandler.getDataSourceById(id)
        assert(cacheItem3 === cacheItem2)
    }

    @Test
    fun getDataSourcesByIds() {
        val id1 = "3d2acef6-e828-43c5-a512-111111111111"
        val id2 = "3d2acef6-e828-43c5-a512-222222222222"
        dataSourceByIdCacheHandler.getDataSourcesByIds(listOf(id1, id2)) // 第一次当放入远程缓存后，会发送清除本地缓存，所以最终取到的是远程缓存反序列化后的对象
        val result2 = dataSourceByIdCacheHandler.getDataSourcesByIds(listOf(id1, id2))
        val result3 = dataSourceByIdCacheHandler.getDataSourcesByIds(listOf(id1, id2))
        assert(result3 == result2)
    }

    @Test
    fun syncOnInsert() {
        // 插入新的记录到数据库
        val sysDataSource = SysDataSource().apply {
            name = "a_new_test_ds"
            url = "url"
            username = "sa"
            password = "sa"
            atomicServiceCode = "default"
            subSystemCode = "default"
        }
        val id = sysDataSourceDao.insert(sysDataSource)

        // 同步缓存
        dataSourceByIdCacheHandler.syncOnInsert(id)

        // 验证新记录是否在缓存中
        val cacheItem1 = CacheKit.getValue(dataSourceByIdCacheHandler.cacheName(), id)
        assertNotNull(cacheItem1)
        val cacheItem2 = dataSourceByIdCacheHandler.getDataSourceById(id)
//        assert(cacheItem1 === cacheItem2) //??? 在有断点时，有时会成立
        val cacheItem3 = dataSourceByIdCacheHandler.getDataSourceById(id)
        assert(cacheItem2 === cacheItem3)
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val id = "3d2acef6-e828-43c5-a512-222222222222"
        val newUrl = "new_url"
        val success = sysDataSourceDao.updateProperties(id, mapOf(SysDataSource::url.name to newUrl))
        assert(success)

        // 同步缓存
        dataSourceByIdCacheHandler.syncOnUpdate(id)

        // 验证缓存中的记录
        val cacheItem1 = CacheKit.getValue(dataSourceByIdCacheHandler.cacheName(), id)
        assertEquals(newUrl, (cacheItem1 as SysDataSourceCacheItem).url)
        val cacheItem2 = dataSourceByIdCacheHandler.getDataSourceById(id)
        assertEquals(newUrl, (cacheItem2 as SysDataSourceCacheItem).url)
    }

    @Test
    fun syncOnDelete() {
        // 删除数据库中的记录
        val id = "3d2acef6-e828-43c5-a512-333333333333"
        val deleteSuccess = sysDataSourceDao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        dataSourceByIdCacheHandler.syncOnDelete(id)

        // 验证缓存中有没有
        val cacheItem1 = CacheKit.getValue(dataSourceByIdCacheHandler.cacheName(), id)
        assertNull(cacheItem1)
        val cacheItem2 = dataSourceByIdCacheHandler.getDataSourceById(id)
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
        dataSourceByIdCacheHandler.syncOnBatchDelete(ids)

        // 验证缓存中有没有
        val cacheItem1 = CacheKit.getValue(dataSourceByIdCacheHandler.cacheName(), id1)
        assertNull(cacheItem1)
        val cacheItem2 = dataSourceByIdCacheHandler.getDataSourceById(id1)
        assertNull(cacheItem2)
        val cacheItem3 = CacheKit.getValue(dataSourceByIdCacheHandler.cacheName(), id2)
        assertNull(cacheItem3)
        val cacheItem4 = dataSourceByIdCacheHandler.getDataSourceById(id2)
        assertNull(cacheItem4)
    }

}