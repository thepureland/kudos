package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ms.sys.common.vo.param.SysParamCacheItem
import io.kudos.ms.sys.core.dao.SysParamDao
import io.kudos.ms.sys.core.model.po.SysParam
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for ParamByModuleAndNameCacheHandler
 *
 * 测试数据来源：`ParamByModuleAndNameCacheTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ParamByModuleAndNameCacheTest : RdbAndRedisCacheTestBase() {
    
    @Resource
    private lateinit var cacheHandler: ParamByModuleAndNameCache
    
    @Resource
    private lateinit var dao: SysParamDao

    private val newValue = "new-value"

    @Test
    fun reloadAll() {
        // 清除并重载缓存，保证与数据库中的数据一致
        cacheHandler.reloadAll(true)

        // 获取当前缓存中的记录
        var atomicServiceCode = "atomicServiceCode-a"
        var paramName = "paramName-1"
        val cacheItem = cacheHandler.getParam(atomicServiceCode, paramName)

        // 插入新的记录到数据库
        val sysParamNew = insertNewRecordToDb()

        // 更新数据库的记录
        val idUpdate = "9edc0327-99f1-4767-b42b-222222229755"
        dao.updateProperties(idUpdate, mapOf(SysParam::paramValue.name to newValue))

        // 从数据库中删除记录
        val idDelete = "9edc0327-99f1-4767-b42b-333333339755"
        dao.deleteById(idDelete)

        // 重载缓存，但不清除旧缓存
        cacheHandler.reloadAll(false)

        // 原来缓存中的记录内存地址会变
        assert(cacheItem !== cacheHandler.getParam(atomicServiceCode, paramName))

        // 数据库中新增的记录在缓存应该要存在
        assertNotNull(cacheHandler.getParam(sysParamNew.atomicServiceCode, sysParamNew.paramName))

        // 数据库中更新的记录在缓存中应该也更新了
        atomicServiceCode = "atomicServiceCode-a"
        paramName = "paramName-2"
        val cacheItemUpdate = assertNotNull(cacheHandler.getParam(atomicServiceCode, paramName))
        assertEquals(newValue, cacheItemUpdate.paramValue)

        // 数据库中删除的记录在缓存中应该还存在
        atomicServiceCode = "atomicServiceCode-a"
        paramName = "paramName-3"
        assertNotNull(cacheHandler.getParam(atomicServiceCode, paramName))

        // 清除旧缓存，并重载缓存
        cacheHandler.reloadAll(true)

        // 数据库中删除的记录在缓存中应该不存在
        atomicServiceCode = "atomicServiceCode-a"
        paramName = "paramName-3"
        assertNull(cacheHandler.getParam(atomicServiceCode, paramName))
    }

    @Test
    fun getParam() {
        var atomicServiceCode = "atomicServiceCode-a"
        var paramName = "paramName-1"
        assertNotNull(cacheHandler.getParam(atomicServiceCode, paramName))

        // active为false的应该没有在缓存中
        atomicServiceCode = "atomicServiceCode-d"
        paramName = "paramName-0"
        assertNull(cacheHandler.getParam(atomicServiceCode, paramName))
    }

    @Test
    fun syncOnInsert() {
        // 插入新的记录到数据库
        val sysParam = insertNewRecordToDb()

        // 同步缓存
        cacheHandler.syncOnInsert(sysParam, sysParam.id)

        // 验证新记录是否在缓存中
        val key = cacheHandler.getKey(sysParam.atomicServiceCode, sysParam.paramName)
        assertNotNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertNotNull(cacheHandler.getParam(sysParam.atomicServiceCode, sysParam.paramName))
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val id = "9edc0327-99f1-4767-b42b-444444449755"
        val success = dao.updateProperties(id, mapOf(SysParam::paramValue.name to newValue))
        assert(success)

        val sysParam = assertNotNull(dao.get(id))

        // 同步缓存
        cacheHandler.syncOnUpdate(sysParam, id)

        // 验证缓存中的记录
        val key = cacheHandler.getKey(sysParam.atomicServiceCode, sysParam.paramName)
        var cacheItem = CacheKit.getValue(cacheHandler.cacheName(), key) as SysParamCacheItem?
        assertNotNull(cacheItem)
        assertEquals(newValue, cacheItem.paramValue)
        cacheItem = cacheHandler.getParam(sysParam.atomicServiceCode, sysParam.paramName)
        assertNotNull(cacheItem)
        assertEquals(newValue, cacheItem.paramValue)
    }

    @Test
    fun syncOnUpdateActive() {
        // 由true更新为false
        var id = "9edc0327-99f1-4767-b42b-555555559755"
        var success = dao.updateProperties(id, mapOf(SysParam::active.name to false))
        assert(success)
        var sysParam = assertNotNull(dao.get(id))
        cacheHandler.syncOnUpdateActive(id, false)
        var key = cacheHandler.getKey(sysParam.atomicServiceCode, sysParam.paramName)
        assertNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertNull(cacheHandler.getParam(sysParam.atomicServiceCode, sysParam.paramName))

        // 由false更新为true
        id = "9edc0327-99f1-4767-b42b-000000009755"
        success = dao.updateProperties(id, mapOf(SysParam::active.name to true))
        assert(success)
        sysParam = assertNotNull(dao.get(id))
        cacheHandler.syncOnUpdateActive(id, true)
        key = cacheHandler.getKey(sysParam.atomicServiceCode, sysParam.paramName)
        assertNotNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertNotNull(cacheHandler.getParam(sysParam.atomicServiceCode, sysParam.paramName))
    }

    @Test
    fun syncOnDelete() {
        val id = "9edc0327-99f1-4767-b42b-666666669755"
        val sysParam = assertNotNull(dao.get(id))

        // 删除数据库中的记录
        val deleteSuccess = dao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(sysParam, id)

        // 验证缓存中有没有
        val key = cacheHandler.getKey(sysParam.atomicServiceCode, sysParam.paramName)
        assertNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertNull(cacheHandler.getParam(sysParam.atomicServiceCode, sysParam.paramName))
    }

    @Test
    fun syncOnBatchDelete() {
        val id1 = "9edc0327-99f1-4767-b42b-777777779755"
        val id2 = "9edc0327-99f1-4767-b42b-888888889755"
        val ids = listOf(id1, id2)
        val atomicServiceCode1 = "atomicServiceCode-b"
        val atomicServiceCode2 = "atomicServiceCode-c"
        val paramName1 = "paramName-7"
        val paramName2 = "paramName-8"
        val moduleAndNames = listOf(
            Pair(atomicServiceCode1, paramName1),
            Pair(atomicServiceCode2, paramName2)
        )

        // 批量删除数据库中的记录
        val count = dao.batchDelete(ids)
        assertEquals(2, count)

        // 同步缓存
        cacheHandler.syncOnBatchDelete(ids, moduleAndNames)

        // 验证缓存中有没有
        var key = cacheHandler.getKey(atomicServiceCode1, paramName1)
        assertNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertNull(cacheHandler.getParam(atomicServiceCode1, paramName1))
        key = cacheHandler.getKey(atomicServiceCode2, paramName2)
        assertNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertNull(cacheHandler.getParam(atomicServiceCode2, paramName2))
    }

    private fun insertNewRecordToDb() : SysParam {
        val sysParam = SysParam().apply {
            paramName = "a_new_paramName"
            paramValue = "a_new_paramValue"
            atomicServiceCode = "atomicServiceCode-a"
        }
        dao.insert(sysParam)
        return sysParam
    }

}