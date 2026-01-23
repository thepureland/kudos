package io.kudos.ams.sys.provider.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ams.sys.common.vo.atomicservice.SysAtomicServiceCacheItem
import io.kudos.ams.sys.provider.dao.SysAtomicServiceDao
import io.kudos.ams.sys.provider.model.po.SysAtomicService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.cache.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for AtomicServiceByCodeCacheHandler
 *
 * 测试数据来源：`V1.0.0.16__AtomicServiceByCodeCacheHandlerTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AtomicServiceByCodeCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: AtomicServiceByCodeCacheHandler

    @Resource
    private lateinit var sysAtomicServiceDao: SysAtomicServiceDao

    private val newAtomicServiceName = "新原子服务名称"

    @Test
    fun getAtomicServiceByCode() {
        // 存在的
        var code = "code-1"
        val cacheItem = cacheHandler.getAtomicServiceByCode(code)
        assertNotNull(cacheItem)
        assert(cacheItem === cacheHandler.getAtomicServiceByCode(code))

        // 不存在的
        code = "no_exist_code"
        assertNull(cacheHandler.getAtomicServiceByCode(code))
    }

    @Test
    fun getAtomicServicesByCodes() {
        // 都存在的
        var code1 = "code-1"
        var code2 = "code-2"
        val result = cacheHandler.getAtomicServicesByCodes(listOf(code1, code2))
        assert(result.isNotEmpty())
        assert(result == cacheHandler.getAtomicServicesByCodes(listOf(code1, code2)))

        // 部分存在的
        code1 = "no_exist_code-1"
        var cacheItems = cacheHandler.getAtomicServicesByCodes(listOf(code1, code2))
        assertEquals(1, cacheItems.size)

        // 都不存在的
        code2 = "no_exist_code-2"
        cacheItems = cacheHandler.getAtomicServicesByCodes(listOf(code1, code2))
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
        val cacheItem2 = cacheHandler.getAtomicServiceByCode(code)
        assertNotNull(cacheItem2)
        val cacheItem3 = cacheHandler.getAtomicServiceByCode(code)
        assert(cacheItem2 === cacheItem3)
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val code = "code-2"
        val success = sysAtomicServiceDao.updateProperties(code, mapOf(SysAtomicService::name.name to newAtomicServiceName))
        assert(success)

        // 同步缓存
        cacheHandler.syncOnUpdate(code)

        // 验证缓存中的记录
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), code) as SysAtomicServiceCacheItem?
        assertNotNull(cacheItem1)
        assertEquals(newAtomicServiceName, cacheItem1.name)
        val cacheItem2 = cacheHandler.getAtomicServiceByCode(code)
        assertNotNull(cacheItem2)
        assertEquals(newAtomicServiceName, cacheItem2.name)
    }

    @Test
    fun syncOnDelete() {
        // 删除数据库中的记录
        val code = insertNewRecordToDb()
        val count = sysAtomicServiceDao.batchDelete(listOf(code))
        assert(count == 1)

        // 同步缓存
        cacheHandler.syncOnDelete(code)

        // 验证缓存中有没有
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), code)
        assertNull(cacheItem1)
        val cacheItem2 = cacheHandler.getAtomicServiceByCode(code)
        assertNull(cacheItem2)
    }

    @Test
    fun syncOnBatchDelete() {
        // 批量删除数据库中的记录
        val code1 = insertNewRecordToDb()
        val code2 = insertNewRecordToDb()
        val codes = listOf(code1, code2)
        val count = sysAtomicServiceDao.batchDelete(codes)
        assert(count == 2)

        // 同步缓存
        cacheHandler.syncOnBatchDelete(codes)

        // 验证缓存中有没有
        var cacheItem = CacheKit.getValue(cacheHandler.cacheName(), code1)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getAtomicServiceByCode(code1)
        assertNull(cacheItem)
        cacheItem = CacheKit.getValue(cacheHandler.cacheName(), code2)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getAtomicServiceByCode(code2)
        assertNull(cacheItem)
    }

    private fun insertNewRecordToDb(): String {
        val timestamp = System.currentTimeMillis()
        val sysAtomicService = SysAtomicService().apply {
            code = "test_code_${timestamp}"
            name = "测试原子服务_${timestamp}"
            active = true
        }
        return sysAtomicServiceDao.insert(sysAtomicService)
    }

}