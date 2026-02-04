package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ms.sys.common.vo.system.SysSystemCacheItem
import io.kudos.ms.sys.core.dao.SysSystemDao
import io.kudos.ms.sys.core.model.po.SysSystem
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for SystemByCodeCacheHandler
 *
 * 测试数据来源：`SystemByCodeCacheTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SystemByCodeCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: SystemByCodeCache

    @Resource
    private lateinit var sysSystemDao: SysSystemDao

    private val newSystemName = "新系统名称"

    @Test
    fun getSystemByCode() {
        // 存在的
        var code = "SbcCH_7a3f9b2c4e5f6_1"
        val cacheItem = cacheHandler.getSystemByCode(code)
        assertNotNull(cacheItem)
        assert(cacheItem === cacheHandler.getSystemByCode(code))

        // 不存在的
        code = "no_exist_code"
        assertNull(cacheHandler.getSystemByCode(code))
    }

    @Test
    fun getSystemsByCodes() {
        // 都存在的
        var code1 = "SbcCH_7a3f9b2c4e5f6_1"
        var code2 = "SbcCH_7a3f9b2c4e5f6_2"
        val result = cacheHandler.getSystemsByCodes(listOf(code1, code2))
        assert(result.isNotEmpty())
        assert(result == cacheHandler.getSystemsByCodes(listOf(code1, code2)))

        // 部分存在的
        code1 = "no_exist_code-1_8400"
        var cacheItems = cacheHandler.getSystemsByCodes(listOf(code1, code2))
        assertEquals(1, cacheItems.size)

        // 都不存在的
        code2 = "no_exist_code-2_8400"
        cacheItems = cacheHandler.getSystemsByCodes(listOf(code1, code2))
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
        val cacheItem2 = cacheHandler.getSystemByCode(code)
        assertNotNull(cacheItem2)
        val cacheItem3 = cacheHandler.getSystemByCode(code)
        assert(cacheItem2 === cacheItem3)
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val code = "SbcCH_7a3f9b2c4e5f6_2"
        val success = sysSystemDao.updateProperties(code, mapOf(SysSystem::name.name to newSystemName))
        assert(success)

        // 同步缓存
        cacheHandler.syncOnUpdate(code)

        // 验证缓存中的记录
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), code) as SysSystemCacheItem?
        assertNotNull(cacheItem1)
        assertEquals(newSystemName, cacheItem1.name)
        val cacheItem2 = cacheHandler.getSystemByCode(code)
        assertNotNull(cacheItem2)
        assertEquals(newSystemName, cacheItem2.name)
    }

    @Test
    fun syncOnDelete() {
        // 删除数据库中的记录
        val code = insertNewRecordToDb()
        val count = sysSystemDao.batchDelete(listOf(code))
        assert(count == 1)

        // 同步缓存
        cacheHandler.syncOnDelete(code)

        // 验证缓存中有没有
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), code)
        assertNull(cacheItem1)
        val cacheItem2 = cacheHandler.getSystemByCode(code)
        assertNull(cacheItem2)
    }

    @Test
    fun syncOnBatchDelete() {
        // 批量删除数据库中的记录
        val code1 = insertNewRecordToDb()
        val code2 = insertNewRecordToDb()
        val codes = listOf(code1, code2)
        val count = sysSystemDao.batchDelete(codes)
        assert(count == 2)

        // 同步缓存
        cacheHandler.syncOnBatchDelete(codes)

        // 验证缓存中有没有
        var cacheItem = CacheKit.getValue(cacheHandler.cacheName(), code1)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getSystemByCode(code1)
        assertNull(cacheItem)
        cacheItem = CacheKit.getValue(cacheHandler.cacheName(), code2)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getSystemByCode(code2)
        assertNull(cacheItem)
    }

    private fun insertNewRecordToDb(): String {
        val unique = UUID.randomUUID().toString().replace("-", "").take(12)
        val sysSystem = SysSystem().apply {
            code = "tc_${unique}"
            name = "测试系统_${unique}"
            active = true
        }
        return sysSystemDao.insert(sysSystem)
    }

}
