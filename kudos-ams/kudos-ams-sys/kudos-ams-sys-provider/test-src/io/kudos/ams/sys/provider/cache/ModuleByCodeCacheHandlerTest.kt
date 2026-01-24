package io.kudos.ams.sys.provider.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ams.sys.common.vo.module.SysModuleCacheItem
import io.kudos.ams.sys.provider.dao.SysModuleDao
import io.kudos.ams.sys.provider.model.po.SysModule
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for ModuleByCodeCacheHandler
 *
 * 测试数据来源：`ModuleByCodeCacheHandlerTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ModuleByCodeCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: ModuleByCodeCacheHandler

    @Resource
    private lateinit var sysModuleDao: SysModuleDao

    private val newModuleName = "新模块名称"

    @Test
    fun getModuleByCode() {
        // 存在的
        var code = "code-1"
        val cacheItem = cacheHandler.getModuleByCode(code)
        assertNotNull(cacheItem)
        assert(cacheItem === cacheHandler.getModuleByCode(code))

        // 不存在的
        code = "no_exist_code"
        assertNull(cacheHandler.getModuleByCode(code))
    }

    @Test
    fun getModulesByCodes() {
        // 都存在的
        var code1 = "code-1"
        var code2 = "code-2"
        val result = cacheHandler.getModulesByCodes(listOf(code1, code2))
        assert(result.isNotEmpty())
        assert(result == cacheHandler.getModulesByCodes(listOf(code1, code2)))

        // 部分存在的
        code1 = "no_exist_code-1"
        var cacheItems = cacheHandler.getModulesByCodes(listOf(code1, code2))
        assertEquals(1, cacheItems.size)

        // 都不存在的
        code2 = "no_exist_code-2"
        cacheItems = cacheHandler.getModulesByCodes(listOf(code1, code2))
        assert(cacheItems.isEmpty())
    }

    @Test
    fun syncOnInsert() {
        // 插入新的记录到数据库
        val code = insertNewRecordToDb()

        // 同步缓存
        cacheHandler.syncOnInsert(code)

        // 验证新记录是否在缓存中
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), code)
        assertNotNull(cacheItem1)
        val cacheItem2 = cacheHandler.getModuleByCode(code)
        assertNotNull(cacheItem2)
        val cacheItem3 = cacheHandler.getModuleByCode(code)
        assert(cacheItem2 === cacheItem3)
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val code = "code-2"
        val success = sysModuleDao.updateProperties(code, mapOf(SysModule::name.name to newModuleName))
        assert(success)

        // 同步缓存
        cacheHandler.syncOnUpdate(code)

        // 验证缓存中的记录
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), code) as SysModuleCacheItem?
        assertNotNull(cacheItem1)
        assertEquals(newModuleName, cacheItem1.name)
        val cacheItem2 = cacheHandler.getModuleByCode(code)
        assertNotNull(cacheItem2)
        assertEquals(newModuleName, cacheItem2.name)
    }

    @Test
    fun syncOnDelete() {
        // 删除数据库中的记录
        val code = insertNewRecordToDb()
        val count = sysModuleDao.batchDelete(listOf(code))
        assert(count == 1)

        // 同步缓存
        cacheHandler.syncOnDelete(code)

        // 验证缓存中有没有
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), code)
        assertNull(cacheItem1)
        val cacheItem2 = cacheHandler.getModuleByCode(code)
        assertNull(cacheItem2)
    }

    @Test
    fun syncOnBatchDelete() {
        // 批量删除数据库中的记录
        val code1 = insertNewRecordToDb()
        val code2 = insertNewRecordToDb()
        val codes = listOf(code1, code2)
        val count = sysModuleDao.batchDelete(codes)
        assert(count == 2)

        // 同步缓存
        cacheHandler.syncOnBatchDelete(codes)

        // 验证缓存中有没有
        var cacheItem = CacheKit.getValue(cacheHandler.cacheName(), code1)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getModuleByCode(code1)
        assertNull(cacheItem)
        cacheItem = CacheKit.getValue(cacheHandler.cacheName(), code2)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getModuleByCode(code2)
        assertNull(cacheItem)
    }

    private fun insertNewRecordToDb(): String {
        val timestamp = System.currentTimeMillis()
        val sysModule = SysModule().apply {
            code = "test_code_${timestamp}"
            name = "测试模块_${timestamp}"
            atomicServiceCode = "kudos-sys"
            active = true
        }
        return sysModuleDao.insert(sysModule)
    }

}