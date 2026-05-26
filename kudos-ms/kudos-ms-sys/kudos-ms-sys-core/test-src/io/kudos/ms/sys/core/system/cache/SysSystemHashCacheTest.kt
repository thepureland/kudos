package io.kudos.ms.sys.core.system.cache

import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ms.sys.core.system.dao.SysSystemDao
import io.kudos.ms.sys.core.system.model.po.SysSystem
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.util.UUID
import kotlin.test.*

/**
 * [SysSystemHashCache] unit tests (Hash cache implementation).
 *
 * Covers single/batch get by code, full refresh, and sync after insert/update/delete/batch delete;
 * under LOCAL_REMOTE/SINGLE_LOCAL strategies, a second fetch returns the same object reference.
 *
 * Test data: `sql/h2/system/cache/SystemByCodeCacheTest.sql`.
 * Requires Docker to run Redis, and SYS_SYSTEM__HASH (hash=true) configured in sys_cache.
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

    override fun getTestDataSqlPath(): String = "sql/h2/system/cache/SystemByCodeCacheTest.sql"

    private fun isLocalCacheEnabled(): Boolean = HashCacheKit.isLocalCacheEnabled(SysSystemHashCache.CACHE_NAME)

    private val newSystemName = "new system name"

    @Test
    fun getSystemByCode() {
        cacheHandler.reloadAll(true)
        val code = "SbcCH_7a3f9b2c4e5f6_1"
        val cacheItem = cacheHandler.getSystemByCode(code)
        assertNotNull(cacheItem)
        assertEquals(code, cacheItem.code)
        assertEquals("SbcCH-name-1", cacheItem.name)
        val cacheItemAgain = cacheHandler.getSystemByCode(code)
        if (isLocalCacheEnabled()) assertSame(cacheItem, cacheItemAgain, "A second fetch of the same code from cache should return the same object reference")
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
            assertSame(result[code1], resultAgain[code1], "A second fetch of the same code from cache should return the same object reference")
            assertSame(result[code2], resultAgain[code2], "A second fetch of the same code from cache should return the same object reference")
        }
        val partial = cacheHandler.getSystemsByCodes(listOf("no_exist_code-1_8400", code2))
        assertEquals(1, partial.size)
        assertTrue(cacheHandler.getSystemsByCodes(emptyList()).isEmpty())
    }

    @Test
    fun getSystemsByType() {
        cacheHandler.reloadAll(true)
        val subSystems = cacheHandler.getSystemsByType(true)
        assertTrue(subSystems.size >= 3, "Test data should contain at least _3/_4/_5 with sub_system=true")
        assertTrue(subSystems.all { it.subSystem == true })
        val expectedSubCodes = setOf("SbcCH_7a3f9b2c4e5f6_3", "SbcCH_7a3f9b2c4e5f6_4", "SbcCH_7a3f9b2c4e5f6_5")
        assertTrue(subSystems.mapNotNull { it.code }.toSet().containsAll(expectedSubCodes))
        val nonSubSystems = cacheHandler.getSystemsByType(false)
        assertTrue(nonSubSystems.size >= 7, "Test data should contain at least 7 rows with sub_system=false")
        assertTrue(nonSubSystems.all { it.subSystem == false })
        val subAgain = cacheHandler.getSystemsByType(true)
        if (isLocalCacheEnabled()) {
            val code = "SbcCH_7a3f9b2c4e5f6_3"
            val a = subSystems.find { it.code == code }
            val b = subAgain.find { it.code == code }
            assertSame(a, b, "A second query by subSystem hitting the cache should return the same object reference")
        }
    }

    @Test
    fun listSubSystems() {
        cacheHandler.reloadAll(true)
        val list = cacheHandler.listSubSystems()
        assertTrue(list.size >= 3, "Subsystem list should contain at least test data _3/_4/_5")
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
        if (isLocalCacheEnabled()) assertSame(cacheItem, cacheItemAgain, "A second fetch of the same code from cache should return the same object reference")
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
        assertEquals(newSystemName, cacheItem.name)
        val cacheItemAgain = cacheHandler.getSystemByCode(code)
        if (isLocalCacheEnabled()) assertSame(cacheItem, cacheItemAgain, "A second fetch of the same code from cache should return the same object reference")
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
            name = "test system_${unique}"
            active = true
        }
        return sysSystemDao.insert(sysSystem)
    }
}
