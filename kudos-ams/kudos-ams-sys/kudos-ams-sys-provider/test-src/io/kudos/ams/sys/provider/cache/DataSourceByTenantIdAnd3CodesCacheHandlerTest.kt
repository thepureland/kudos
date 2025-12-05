package io.kudos.ams.sys.provider.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ams.sys.common.vo.datasource.SysDataSourceCacheItem
import io.kudos.ams.sys.provider.dao.SysDataSourceDao
import io.kudos.ams.sys.provider.model.po.SysDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for DataSourceByTenantIdAnd3CodesCacheHandler
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerAvailable
class DataSourceByTenantIdAnd3CodesCacheHandlerTest : CacheHandlerTestBase() {

    @Autowired
    private lateinit var cacheHandler: DataSourceByTenantIdAnd3CodesCacheHandler

    @Autowired
    private lateinit var dataSourceByIdCacheHandler: DataSourceByIdCacheHandler

    @Autowired
    private lateinit var sysDataSourceDao: SysDataSourceDao

    private val newName = "test_ds_2222"

    @Test
    fun reloadAll() {
        // 清除并重载缓存，保证与数据库中的数据一致
        cacheHandler.reloadAll(true)

        // 获取当前缓存中的记录
        var tenantId = "10a45fe6-4c8d-40c7-8f23-bba-tenant-1"
        val subSystemCode = "subSys-a"
        val microServiceCode = "ms-a"
        val atomicServiceCode = "ams-a"
        val cacheItem = cacheHandler.getDataSource(tenantId, subSystemCode, microServiceCode, atomicServiceCode)

        // 插入新的记录到数据库
        val sysDataSourceNew = insertNewRecordToDb()

        // 更新数据库的记录
        val idUpdate = "33333333-e828-43c5-a512-222222222222"
        sysDataSourceDao.updateProperties(idUpdate, mapOf(SysDataSource::name.name to newName))

        // 从数据库中删除记录
        val idDelete = "33333333-e828-43c5-a512-333333333333"
        sysDataSourceDao.deleteById(idDelete)

        // 重载缓存，但不清除旧缓存
        cacheHandler.reloadAll(false)

        // 原来缓存中的记录内存地址会变
        val cacheItem1 = cacheHandler.getDataSource(tenantId, subSystemCode, microServiceCode, atomicServiceCode)
        assert(cacheItem !== cacheItem1)

        // 数据库中新增的记录在缓存应该要存在
        val cacheItemNew = cacheHandler.getDataSource(
            sysDataSourceNew.tenantId!!, sysDataSourceNew.subSystemCode,
            sysDataSourceNew.microServiceCode, sysDataSourceNew.atomicServiceCode
        )
        assertNotNull(cacheItemNew)

        // 数据库中更新的记录在缓存中应该也更新了
        tenantId = "10a45fe6-4c8d-40c7-8f23-bba-tenant-2"
        val cacheItemUpdate = cacheHandler.getDataSource(tenantId, subSystemCode, microServiceCode, atomicServiceCode)
        assertEquals(newName, cacheItemUpdate!!.name)

        // 数据库中删除的记录在缓存中应该还在
        tenantId = "10a45fe6-4c8d-40c7-8f23-bba-tenant-3"
        var cacheItemDelete = cacheHandler.getDataSource(tenantId, subSystemCode, microServiceCode, atomicServiceCode)
        assertNotNull(cacheItemDelete)


        // 清除并重载缓存
        cacheHandler.reloadAll(true)

        // 数据库中删除的记录在缓存中应该不存在了
        cacheItemDelete = cacheHandler.getDataSource(tenantId, subSystemCode, microServiceCode, atomicServiceCode)
        assertNull(cacheItemDelete)
    }

    @Test
    fun getDataSource() {
        var tenantId = "10a45fe6-4c8d-40c7-8f23-bba-tenant-1"
        var subSystemCode = "subSys-a"
        var microServiceCode = "ms-a"
        var atomicServiceCode = "ams-a"
        val cacheItem2 = cacheHandler.getDataSource(tenantId, subSystemCode, microServiceCode, atomicServiceCode)
        val cacheItem3 = cacheHandler.getDataSource(tenantId, subSystemCode, microServiceCode, atomicServiceCode)
        assert(cacheItem3 === cacheItem2)

        // tenantId为null的应该没被加载
        subSystemCode = "subSys-c"
        val key = cacheHandler.getKey(null, subSystemCode, null, null)
        val cacheItemNull = CacheKit.getValue(cacheHandler.cacheName(), key)
        assertNull(cacheItemNull)

        // atomicServiceCode为null的情况
        tenantId = "10a45fe6-4c8d-40c7-8f23-bba-tenant-1"
        subSystemCode = "subSys-c"
        microServiceCode = "ms-c"
        var cacheItem = cacheHandler.getDataSource(tenantId, subSystemCode, microServiceCode, null)
        assertEquals("33333333-e828-43c5-a512-777777777777", cacheItem!!.id)

        // microServiceCode和atomicServiceCode均为null的情况
        tenantId = "10a45fe6-4c8d-40c7-8f23-bba-tenant-1"
        subSystemCode = "subSys-c"
        cacheItem = cacheHandler.getDataSource(tenantId, subSystemCode, null, null)
        assertEquals("33333333-e828-43c5-a512-888888888888", cacheItem!!.id)

        // active为false的应该没被加载
        tenantId = "10a45fe6-4c8d-40c7-8f23-bba-tenant-2"
        subSystemCode = "subSys-d"
        microServiceCode = "ms-c"
        atomicServiceCode = "ams-b"
        val cacheItemFalse = cacheHandler.getDataSource(tenantId, subSystemCode, microServiceCode, atomicServiceCode)
        assertNull(cacheItemFalse)
    }

    @Test
    fun syncOnInsert() {
        // 插入新的记录到数据库
        val ds = insertNewRecordToDb()

        // 同步缓存
        cacheHandler.syncOnInsert(ds, ds.id!!)

        // 验证新记录是否在缓存中
        val key = cacheHandler.getKey(ds.tenantId, ds.subSystemCode, ds.microServiceCode, ds.atomicServiceCode)
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), key)
        assertNotNull(cacheItem1)
        val cacheItem2 = cacheHandler.getDataSource(
            ds.tenantId!!, ds.subSystemCode, ds.microServiceCode, ds.atomicServiceCode
        )
        val cacheItem3 = cacheHandler.getDataSource(
            ds.tenantId!!, ds.subSystemCode, ds.microServiceCode, ds.atomicServiceCode
        )
        assert(cacheItem2 === cacheItem3)
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val id = "33333333-e828-43c5-a512-222222222222"
        val success = sysDataSourceDao.updateProperties(id, mapOf(SysDataSource::name.name to newName))
        assert(success)

        val ds = dataSourceByIdCacheHandler.getDataSourceById(id)!!

        // 同步缓存
        cacheHandler.syncOnUpdate(ds, id)

        // 验证缓存中的记录
        val key = cacheHandler.getKey(ds.tenantId, ds.subSystemCode, ds.microServiceCode, ds.atomicServiceCode)
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), key)
        assertEquals(newName, (cacheItem1 as SysDataSourceCacheItem).name)
        val cacheItem2 = cacheHandler.getDataSource(
            ds.tenantId!!, ds.subSystemCode!!, ds.microServiceCode, ds.atomicServiceCode
        )
        assertEquals(newName, (cacheItem2 as SysDataSourceCacheItem).name)
    }

    @Test
    fun syncOnUpdateActive() {
        // 由true更新为false
        val id2 = "33333333-e828-43c5-a512-888888888888"
        var success = sysDataSourceDao.updateProperties(id2, mapOf(SysDataSource::active.name to false))
        assert(success)
        val ds2 = dataSourceByIdCacheHandler.getDataSourceById(id2)!!
        cacheHandler.syncOnUpdateActive(id2, false)
        var key = cacheHandler.getKey(ds2.tenantId, ds2.subSystemCode, ds2.microServiceCode, ds2.atomicServiceCode)
        var cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), key)
        assertNull(cacheItem1)
        var cacheItem2 = cacheHandler.getDataSource(
            ds2.tenantId!!, ds2.subSystemCode!!, ds2.microServiceCode, ds2.atomicServiceCode
        )
        assertNull(cacheItem2)

        // 由false更新为true
        val id0 = "33333333-e828-43c5-a512-000000000000"
        success = sysDataSourceDao.updateProperties(id0, mapOf(SysDataSource::active.name to true))
        assert(success)
        val ds0 = dataSourceByIdCacheHandler.getDataSourceById(id0)!!
        cacheHandler.syncOnUpdateActive(id0, true)
        key = cacheHandler.getKey(ds0.tenantId, ds0.subSystemCode, ds0.microServiceCode, ds0.atomicServiceCode)
        cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), key)
        assertNotNull(cacheItem1)
        cacheItem2 = cacheHandler.getDataSource(
            ds0.tenantId!!, ds0.subSystemCode!!, ds0.microServiceCode, ds0.atomicServiceCode
        )
        assert(cacheItem1 === cacheItem2)
    }

    @Test
    fun syncOnDelete() {
        val id = "33333333-e828-43c5-a512-333333333333"
        val ds = dataSourceByIdCacheHandler.getDataSourceById(id)!!

        // 删除数据库中的记录
        val deleteSuccess = sysDataSourceDao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(id)

        // 验证缓存中有没有
        var key = cacheHandler.getKey(ds.tenantId, ds.subSystemCode, ds.microServiceCode, ds.atomicServiceCode)
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), key)
        assertNull(cacheItem1)
        val cacheItem2 = cacheHandler.getDataSource(
            ds.tenantId!!, ds.subSystemCode!!, ds.microServiceCode, ds.atomicServiceCode
        )
        assertNull(cacheItem2)
    }

    private fun insertNewRecordToDb(): SysDataSource {
        val sysDataSource = SysDataSource().apply {
            name = "a_new_test_ds"
            url = "url"
            username = "sa"
            password = "sa"
            subSystemCode = "default"
            microServiceCode = "default"
            atomicServiceCode = "default"
            tenantId = "10a45fe6-4c8d-40c7-8f23-bba-tenant-n"
            active = true
        }
        sysDataSourceDao.insert(sysDataSource)
        return sysDataSource
    }

}