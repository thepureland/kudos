package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ams.sys.service.dao.SysDataSourceDao
import io.kudos.ams.sys.service.model.po.SysDataSource
import org.junit.jupiter.api.AfterAll
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertNotNull

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
        val id = "3d2acef6-e828-43c5-a512-11111"
        val cacheItem1 = dataSourceByIdCacheHandler.getDataSourceById(id)
        val cacheItem2 = dataSourceByIdCacheHandler.getDataSourceById(id)
        assert(cacheItem1 === cacheItem2)
    }

    @Test
    fun getDataSourcesByIds() {
        val id1 = "3d2acef6-e828-43c5-a512-11111"
        val id2 = "3d2acef6-e828-43c5-a512-22222"
        val result1 = dataSourceByIdCacheHandler.getDataSourcesByIds(listOf(id1, id2))
        val result2 = dataSourceByIdCacheHandler.getDataSourcesByIds(listOf(id1, id2))
        assert(result1 === result2)
    }

    @Test
    fun syncOnInsert() {
        // 插入新的数据源到数据库
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

        // 验证新对象是否在缓存中
        val cacheItem1 = CacheKit.getValue(dataSourceByIdCacheHandler.cacheName(), id)
        assertNotNull(cacheItem1)
        val cacheItem2 = dataSourceByIdCacheHandler.getDataSourceById(id)
        assert(cacheItem1 === cacheItem2)
    }

    @Test
    fun syncOnUpdate() {

    }

    @Test
    fun syncOnDelete() {

    }

    @Test
    fun syncOnBatchDelete() {

    }

}