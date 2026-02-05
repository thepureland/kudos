package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ms.sys.core.dao.SysSystemDao
import io.kudos.ms.sys.core.model.po.SysSystem
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.util.*
import kotlin.test.*

/**
 * [SysSystemHashCache] 单元测试（Hash 缓存实现）。
 *
 * 覆盖按 code 单条/批量获取、全量刷新、新增/更新/删除/批量删除后同步；
 * LOCAL_REMOTE/SINGLE_LOCAL 策略下二次取为同一对象引用。
 *
 * 测试数据：`sql/h2/cache/SystemByCodeCacheTest.sql`。
 * 需 Docker 运行 Redis，且 sys_cache 中已配置 SYS_SYSTEM__HASH（hash=true）。
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysSystemHashCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: SysSystemHashCache

    @Resource
    private lateinit var sysSystemDao: SysSystemDao

    override fun getTestDataSqlPath(): String = "sql/h2/cache/SystemByCodeCacheTest.sql"

    private fun isLocalCacheEnabled(): Boolean = HashCacheKit.isLocalCacheEnabled(SysSystemHashCache.CACHE_NAME)

    private val newSystemName = "新系统名称"

    @Test
    fun getSystemByCode() {
        cacheHandler.reloadAll(true)
        val code = "SbcCH_7a3f9b2c4e5f6_1"
        val cacheItem = cacheHandler.getSystemByCode(code)
        assertNotNull(cacheItem)
        assertEquals(code, cacheItem?.code)
        assertEquals("SbcCH-name-1", cacheItem?.name)
        val cacheItemAgain = cacheHandler.getSystemByCode(code)
        if (isLocalCacheEnabled()) assertSame(cacheItem, cacheItemAgain, "同一 code 再次从缓存获取应返回同一对象引用")
        assertNull(cacheHandler.getSystemByCode("no_exist_code"))
    }

    @Test
    fun getSystemsByCodes() {
        cacheHandler.reloadAll(true)
        val code1 = "SbcCH_7a3f9b2c4e5f6_1"
        val code2 = "SbcCH_7a3f9b2c4e5f6_2"
        val result = cacheHandler.getSystemsByCodes(listOf(code1, code2))
        assertTrue(result.isNotEmpty())
        val resultAgain = cacheHandler.getSystemsByCodes(listOf(code1, code2))
        if (isLocalCacheEnabled()) {
            assertSame(result[code1], resultAgain[code1], "同一 code 再次从缓存获取应返回同一对象引用")
            assertSame(result[code2], resultAgain[code2], "同一 code 再次从缓存获取应返回同一对象引用")
        }
        val partial = cacheHandler.getSystemsByCodes(listOf("no_exist_code-1_8400", code2))
        assertEquals(1, partial.size)
        assertTrue(cacheHandler.getSystemsByCodes(emptyList()).isEmpty())
    }

    @Test
    fun getSystemsByType() {
        cacheHandler.reloadAll(true)
        val subSystems = cacheHandler.getSystemsByType(true)
        assertTrue(subSystems.size >= 3, "测试数据中 sub_system=true 至少包含 _3/_4/_5")
        assertTrue(subSystems.all { it.subSystem == true })
        val expectedSubCodes = setOf("SbcCH_7a3f9b2c4e5f6_3", "SbcCH_7a3f9b2c4e5f6_4", "SbcCH_7a3f9b2c4e5f6_5")
        assertTrue(subSystems.mapNotNull { it.code }.toSet().containsAll(expectedSubCodes))
        val nonSubSystems = cacheHandler.getSystemsByType(false)
        assertTrue(nonSubSystems.size >= 7, "测试数据中 sub_system=false 至少 7 条")
        assertTrue(nonSubSystems.all { it.subSystem == false })
        val subAgain = cacheHandler.getSystemsByType(true)
        if (isLocalCacheEnabled()) {
            val code = "SbcCH_7a3f9b2c4e5f6_3"
            val a = subSystems.find { it.code == code }
            val b = subAgain.find { it.code == code }
            assertSame(a, b, "二次按 subSystem 查询命中缓存应返回同一对象引用")
        }
    }

    @Test
    fun listSubSystems() {
        cacheHandler.reloadAll(true)
        val list = cacheHandler.listSubSystems()
        assertTrue(list.size >= 3, "子系统列表至少包含测试数据 _3/_4/_5")
        assertTrue(list.all { it.subSystem == true })
        val codes = list.mapNotNull { it.code }.toSet()
        assertTrue(codes.contains("SbcCH_7a3f9b2c4e5f6_3"))
        assertTrue(codes.contains("SbcCH_7a3f9b2c4e5f6_4"))
        assertTrue(codes.contains("SbcCH_7a3f9b2c4e5f6_5"))
    }

    @Test
    fun syncOnInsert() {
        cacheHandler.reloadAll(true)
        val code = insertNewRecordToDb()
        cacheHandler.syncOnInsert(code)
        val cacheItem = cacheHandler.getSystemByCode(code)
        assertNotNull(cacheItem)
        val cacheItemAgain = cacheHandler.getSystemByCode(code)
        if (isLocalCacheEnabled()) assertSame(cacheItem, cacheItemAgain, "同一 code 再次从缓存获取应返回同一对象引用")
    }

    @Test
    fun syncOnUpdate() {
        cacheHandler.reloadAll(true)
        val code = "SbcCH_7a3f9b2c4e5f6_2"
        val success = sysSystemDao.updateProperties(code, mapOf(SysSystem::name.name to newSystemName))
        assertTrue(success)
        cacheHandler.syncOnUpdate(code)
        val cacheItem = cacheHandler.getSystemByCode(code)
        assertNotNull(cacheItem)
        assertEquals(newSystemName, cacheItem?.name)
        val cacheItemAgain = cacheHandler.getSystemByCode(code)
        if (isLocalCacheEnabled()) assertSame(cacheItem, cacheItemAgain, "同一 code 再次从缓存获取应返回同一对象引用")
    }

    @Test
    fun syncOnDelete() {
        cacheHandler.reloadAll(true)
        val code = insertNewRecordToDb()
        val count = sysSystemDao.batchDelete(listOf(code))
        assertEquals(1, count)
        cacheHandler.syncOnDelete(code)
        assertNull(cacheHandler.getSystemByCode(code))
    }

    @Test
    fun syncOnBatchDelete() {
        cacheHandler.reloadAll(true)
        val code1 = insertNewRecordToDb()
        val code2 = insertNewRecordToDb()
        val codes = listOf(code1, code2)
        val count = sysSystemDao.batchDelete(codes)
        assertEquals(2, count)
        cacheHandler.syncOnBatchDelete(codes)
        assertNull(cacheHandler.getSystemByCode(code1))
        assertNull(cacheHandler.getSystemByCode(code2))
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
