package io.kudos.ms.sys.core.cache.cache

import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ms.sys.common.cache.vo.SysCacheCacheEntry
import io.kudos.ms.sys.core.cache.cache.SysCacheHashCache
import io.kudos.ms.sys.core.cache.dao.SysCacheDao
import io.kudos.ms.sys.core.cache.model.po.SysCache
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * [SysCacheHashCache] 单元测试（Hash 缓存，按 id / name / atomicServiceCode）。
 *
 * 覆盖：按 id 单条/批量、按 name 单条、按 atomicServiceCode 列表、全量刷新、新增/更新/删除后同步；
 * 本地缓存开启时二次取为同一对象引用。
 *
 * 测试数据：`SysCacheHashCacheTest.sql`。
 * 需 Docker 运行 Redis，且 sys_cache 中已配置 SYS_CACHE__HASH（hash=true）。
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysCacheHashCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: SysCacheHashCache

    @Resource
    private lateinit var sysCacheDao: SysCacheDao

    private fun isLocalCacheEnabled(): Boolean = HashCacheKit.isLocalCacheEnabled(SysCacheHashCache.Companion.CACHE_NAME)

    private val id1 = "a1000000-0000-0000-0000-000000001002"
    private val id2 = "a1000000-0000-0000-0000-000000001003"
    private val name1 = "SYS_CACHE_HASH_TEST_1"
    private val name2 = "SYS_CACHE_HASH_TEST_2"
    private val atomicServiceCode = "ams-sys-hash-test"
    private val newName = "SYS_CACHE_HASH_TEST_1_UPD"

    // ---------- 按主键 id ----------

    @Test
    fun getCacheById() {
        cacheHandler.reloadAll(true)
        val item = cacheHandler.getCacheById(id1)
        assertNotNull(item)
        assertEquals(id1, item.id)
        assertEquals(name1, item.name)
        assertEquals(atomicServiceCode, item.atomicServiceCode)
        val itemAgain = cacheHandler.getCacheById(id1)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain)
        assertNull(cacheHandler.getCacheById("no_exist_id"))
    }

    @Test
    fun getCachesByIds() {
        cacheHandler.reloadAll(true)
        val result = cacheHandler.getCachesByIds(setOf(id1, id2))
        assertEquals(2, result.size)
        assertNotNull(result[id1])
        assertNotNull(result[id2])
        val resultAgain = cacheHandler.getCachesByIds(setOf(id1, id2))
        if (isLocalCacheEnabled()) {
            assertSame(result[id1], resultAgain[id1])
            assertSame(result[id2], resultAgain[id2])
        }
        val partial = cacheHandler.getCachesByIds(setOf("no_exist_id", id2))
        assertEquals(1, partial.size)
        assertTrue(cacheHandler.getCachesByIds(emptySet()).isEmpty())
    }

    // ---------- 按 atomicServiceCode + name ----------

    @Test
    fun getCache() {
        cacheHandler.reloadAll(true)
        val item = cacheHandler.getCache(atomicServiceCode, name1)
        assertNotNull(item)
        assertEquals(id1, item.id)
        assertEquals(name1, item.name)
        assertEquals(atomicServiceCode, item.atomicServiceCode)
        val itemAgain = cacheHandler.getCache(atomicServiceCode, name1)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain)
        assertNull(cacheHandler.getCache(atomicServiceCode, "no_exist_name"))
    }

    // ---------- 按 atomicServiceCode ----------

    @Test
    fun getCaches() {
        cacheHandler.reloadAll(true)
        val list = cacheHandler.getCaches(atomicServiceCode)
        assertTrue(list.size >= 2)
        assertTrue(list.any { it.id == id1 && it.name == name1 })
        assertTrue(list.any { it.id == id2 && it.name == name2 })
        val listAgain = cacheHandler.getCaches(atomicServiceCode)
        if (isLocalCacheEnabled() && listAgain.isNotEmpty()) {
            assertSame(list.first { it.id == id1 }, listAgain.first { it.id == id1 })
        }
    }

    // ---------- 全量刷新 ----------

    @Test
    fun reloadAll() {
        cacheHandler.reloadAll(true)
        val item = cacheHandler.getCacheById(id1)
        assertNotNull(item)

        val newEntry = insertNewRecordToDb()
        sysCacheDao.updateProperties(id2, mapOf(SysCache::name.name to newName))
        sysCacheDao.deleteById(id1)

        cacheHandler.reloadAll(false)
        assertNotNull(cacheHandler.getCacheById(newEntry.id))
        assertEquals(newName, cacheHandler.getCacheById(id2)?.name)
        assertNull(cacheHandler.getCacheById(id1))

        cacheHandler.reloadAll(true)
        assertNull(cacheHandler.getCacheById(id1))
        assertEquals(newName, cacheHandler.getCacheById(id2)?.name)
    }

    // ---------- 同步 ----------

    @Test
    fun syncOnInsert() {
        cacheHandler.reloadAll(true)
        val entry = insertNewRecordToDb()
        cacheHandler.syncOnInsert(entry, entry.id)
        val item = cacheHandler.getCacheById(entry.id)
        assertNotNull(item)
        val itemAgain = cacheHandler.getCacheById(entry.id)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain)
    }

    @Test
    fun syncOnUpdate() {
        cacheHandler.reloadAll(true)
        val success = sysCacheDao.updateProperties(id1, mapOf(SysCache::name.name to newName))
        assertTrue(success)
        val entry = assertNotNull(cacheHandler.getCacheById(id1))
        cacheHandler.syncOnUpdate(entry, id1)
        assertEquals(newName, sysCacheDao.getAs<SysCacheCacheEntry>(id1)?.name)
        assertEquals(newName, cacheHandler.getCacheById(id1)?.name)
    }

    @Test
    fun syncOnDelete() {
        cacheHandler.reloadAll(true)
        cacheHandler.getCacheById(id1)
        sysCacheDao.deleteById(id1)
        cacheHandler.syncOnDelete(id1)
        assertNull(sysCacheDao.getAs<SysCacheCacheEntry>(id1))
        assertNull(cacheHandler.getCacheById(id1))
    }

    private fun insertNewRecordToDb(): SysCacheCacheEntry {
        val po = SysCache.Companion().apply {
            name = "SYS_CACHE_HASH_TEST_NEW"
            atomicServiceCode = atomicServiceCode
            strategyDictCode = "CACHE_LOCAL"
            writeOnBoot = true
            writeInTime = true
            hash = false
        }
        val id = sysCacheDao.insert(po)
        return requireNotNull(sysCacheDao.getAs<SysCacheCacheEntry>(id)) { "inserted cache row not found: $id" }
    }
}