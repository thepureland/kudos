package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ams.sys.common.vo.param.SysParamCacheItem
import io.kudos.ams.sys.service.dao.SysParamDao
import io.kudos.ams.sys.service.model.po.SysParam
import org.springframework.beans.factory.annotation.Autowired
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for ParamByModuleAndNameCacheHandler
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerAvailable
class ParamByModuleAndNameCacheHandlerTest : CacheHandlerTestBase() {
    
    @Autowired
    private lateinit var cacheHandler: ParamByModuleAndNameCacheHandler
    
    @Autowired
    private lateinit var dao: SysParamDao

    private val newValue = "new-value"

    @Test
    fun reloadAll() {
        // 清除并重载缓存，保证与数据库中的数据一致
        cacheHandler.reloadAll(true)

        // 获取当前缓存中的记录
        var moduleCode = "moduleCode-a"
        var paramName = "paramName-1"
        val cacheItem = cacheHandler.getParam(moduleCode, paramName)

        // 插入新的记录到数据库
        val sysDictItemNew = insertNewRecordToDb()

        // 更新数据库的记录
        val idUpdate = "9edc0327-99f1-4767-b42b-222222222222"
        dao.updateProperties(idUpdate, mapOf(SysParam::paramValue.name to newValue))

        // 从数据库中删除记录
        val idDelete = "9edc0327-99f1-4767-b42b-333333333333"
        dao.deleteById(idDelete)

        // 重载缓存，但不清除旧缓存
        cacheHandler.reloadAll(false)

        // 原来缓存中的记录内存地址会变
        assert(cacheItem !== cacheHandler.getParam(moduleCode, paramName))

        // 数据库中新增的记录在缓存应该要存在
        assertNotNull(cacheHandler.getParam(sysDictItemNew.moduleCode, sysDictItemNew.paramName))

        // 数据库中更新的记录在缓存中应该也更新了
        moduleCode = "moduleCode-a"
        paramName = "paramName-2"
        val cacheItemsUpdate = cacheHandler.getParam(moduleCode, paramName)!!
        assertEquals(newValue, cacheItemsUpdate.paramValue)

        // 数据库中删除的记录在缓存中应该还存在
        moduleCode = "moduleCode-a"
        paramName = "paramName-3"
        assertNotNull(cacheHandler.getParam(moduleCode, paramName))

        // 清除旧缓存，并重载缓存
        cacheHandler.reloadAll(true)

        // 数据库中删除的记录在缓存中应该不存在
        moduleCode = "moduleCode-a"
        paramName = "paramName-3"
        assertNull(cacheHandler.getParam(moduleCode, paramName))
    }

    @Test
    fun getParam() {
        var moduleCode = "moduleCode-a"
        var paramName = "paramName-1"
        assertNotNull(cacheHandler.getParam(moduleCode, paramName))

        // active为false的应该没有在缓存中
        moduleCode = "moduleCode-d"
        paramName = "paramName-0"
        assertNull(cacheHandler.getParam(moduleCode, paramName))
    }

    @Test
    fun syncOnInsert() {
        // 插入新的记录到数据库
        val sysParam = insertNewRecordToDb()

        // 同步缓存
        cacheHandler.syncOnInsert(sysParam, sysParam.id!!)

        // 验证新记录是否在缓存中
        val key = cacheHandler.getKey(sysParam.moduleCode, sysParam.paramName)
        assertNotNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertNotNull(cacheHandler.getParam(sysParam.moduleCode, sysParam.paramName))
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val id = "9edc0327-99f1-4767-b42b-444444444444"
        val success = dao.updateProperties(id, mapOf(SysParam::paramValue.name to newValue))
        assert(success)

        val sysParam = dao.get(id)!!

        // 同步缓存
        cacheHandler.syncOnUpdate(sysParam, id)

        // 验证缓存中的记录
        val key = cacheHandler.getKey(sysParam.moduleCode, sysParam.paramName)
        var cacheItem = CacheKit.getValue(cacheHandler.cacheName(), key) as SysParamCacheItem?
        assertNotNull(cacheItem)
        assertEquals(newValue, cacheItem.paramValue)
        cacheItem = cacheHandler.getParam(sysParam.moduleCode, sysParam.paramName)
        assertNotNull(cacheItem)
        assertEquals(newValue, cacheItem.paramValue)
    }

    @Test
    fun syncOnUpdateActive() {
        // 由true更新为false
        var id = "9edc0327-99f1-4767-b42b-555555555555"
        var success = dao.updateProperties(id, mapOf(SysParam::active.name to false))
        assert(success)
        var sysParam = dao.get(id)!!
        cacheHandler.syncOnUpdateActive(id, false)
        var key = cacheHandler.getKey(sysParam.moduleCode, sysParam.paramName)
        assertNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertNull(cacheHandler.getParam(sysParam.moduleCode, sysParam.paramName))

        // 由false更新为true
        id = "9edc0327-99f1-4767-b42b-000000000000"
        success = dao.updateProperties(id, mapOf(SysParam::active.name to true))
        assert(success)
        sysParam = dao.get(id)!!
        cacheHandler.syncOnUpdateActive(id, true)
        key = cacheHandler.getKey(sysParam.moduleCode, sysParam.paramName)
        assertNotNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertNotNull(cacheHandler.getParam(sysParam.moduleCode, sysParam.paramName))
    }

    @Test
    fun syncOnDelete() {
        val id = "9edc0327-99f1-4767-b42b-666666666666"
        val sysParam = dao.get(id)!!

        // 删除数据库中的记录
        val deleteSuccess = dao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(sysParam, id)

        // 验证缓存中有没有
        val key = cacheHandler.getKey(sysParam.moduleCode, sysParam.paramName)
        assertNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertNull(cacheHandler.getParam(sysParam.moduleCode, sysParam.paramName))
    }

    @Test
    fun syncOnBatchDelete() {
        val id1 = "9edc0327-99f1-4767-b42b-777777777777"
        val id2 = "9edc0327-99f1-4767-b42b-888888888888"
        val ids = listOf(id1, id2)
        val moduleCode1 = "moduleCode-b"
        val moduleCode2 = "moduleCode-c"
        val paramName1 = "paramName-7"
        val paramName2 = "paramName-8"
        val moduleAndNames = listOf(
            Pair(moduleCode1, paramName1),
            Pair(moduleCode2, paramName2)
        )

        // 批量删除数据库中的记录
        val count = dao.batchDelete(ids)
        assertEquals(2, count)

        // 同步缓存
        cacheHandler.syncOnBatchDelete(ids, moduleAndNames)

        // 验证缓存中有没有
        var key = cacheHandler.getKey(moduleCode1, paramName1)
        assertNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertNull(cacheHandler.getParam(moduleCode1, paramName1))
        key = cacheHandler.getKey(moduleCode2, paramName2)
        assertNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertNull(cacheHandler.getParam(moduleCode2, paramName2))
    }

    private fun insertNewRecordToDb() : SysParam {
        val sysParam = SysParam().apply {
            paramName = "a_new_paramName"
            paramValue = "a_new_paramValue"
            moduleCode = "moduleCode-a"
        }
        dao.insert(sysParam)
        return sysParam
    }

}