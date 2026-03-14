package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ms.sys.core.dao.SysDictDao
import io.kudos.ms.sys.core.model.po.SysDict
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.*

/**
 * SysDictHashCache 测试。
 *
 * 测试数据来源：`sql/h2/cache/SysDictHashCacheTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDictHashCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cache: SysDictHashCache

    @Resource
    private lateinit var sysDictDao: SysDictDao

    override fun getTestDataSqlPath(): String = "sql/h2/cache/SysDictHashCacheTest.sql"

    private fun isLocalCacheEnabled(): Boolean = HashCacheKit.isLocalCacheEnabled(SysDictHashCache.CACHE_NAME)

    // ---------- 按主键 id ----------

    @Test
    fun getDictById() {
        cache.reloadAll(true)
        val id = "sdch1001-1a2b-4c5d-8e9f-000000000001"
        val item = cache.getDictById(id)
        assertNotNull(item)
        assertEquals(id, item.id)
        assertEquals("sdch-ms-7f3e2d1c", item.atomicServiceCode)
        assertEquals("sdch-type-001", item.dictType)
        val itemAgain = cache.getDictById(id)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain, "同一 id 再次从缓存获取应返回同一对象引用")
        assertNull(cache.getDictById("sdch-nonexistent-00000000000000000000"))
    }

    @Test
    fun getDictsByIds() {
        cache.reloadAll(true)
        val ids = setOf(
            "sdch1001-1a2b-4c5d-8e9f-000000000001",
            "sdch1002-2b3c-5d6e-9f0a-000000000002",
            "sdch1003-3c4d-6e7f-0a1b-000000000003"
        )
        val map = cache.getDictsByIds(ids)
        assertEquals(3, map.size)
        map.forEach { (k, v) -> assertEquals(k, v.id) }
        val mapAgain = cache.getDictsByIds(ids)
        if (isLocalCacheEnabled()) ids.forEach { id ->
            assertSame(map[id], mapAgain[id], "同一 id 再次从缓存获取应返回同一对象引用")
        }
        assertTrue(cache.getDictsByIds(emptySet()).isEmpty())
    }

    // ---------- 按原子服务编码+字典类型 ----------

    @Test
    fun getDictByAtomicServiceCodeAndDictType() {
        val atomicServiceCode = "sdch-ms-a1b2c3d4"
        val dictType = "sdch-type-a1"
        val idFromDao = sysDictDao.fetchDictByAtomicServiceCodeAndDictType(atomicServiceCode, dictType)?.id
        assertNotNull(idFromDao, "测试数据未加载：sql/h2/cache/SysDictHashCacheTest.sql 中应有 atomic_service_code=$atomicServiceCode, dict_type=$dictType, active=true 的记录")
        assertEquals("sdch2001-4d5e-7f8a-1b2c-000000000011", idFromDao)
        val res1 = cache.getDictByAtomicServiceCodeAndDictType(atomicServiceCode, dictType)
        val id = res1?.id
        assertEquals("sdch2001-4d5e-7f8a-1b2c-000000000011", id)
        val res2 = cache.getDictByAtomicServiceCodeAndDictType(atomicServiceCode, dictType)
        if (isLocalCacheEnabled()) assertSame(res1, res2, "local 启用时同一维度再次从缓存获取应返回同一对象引用")
        // 注：sdch-type-a2 为 active=false，从 DB fetch 时不会返回；若此前执行过 reloadAll，缓存中可能含该条，按 secondary 查询会命中
    }

    @Test
    fun getDictsByAtomicServiceCode() {
        cache.reloadAll(true)
        val atomicServiceCode = "sdch-ms-7f3e2d1c"
        val list = cache.getDictsByAtomicServiceCode(atomicServiceCode)
        assertEquals(3, list.size)
        assertTrue(list.all { it.atomicServiceCode == atomicServiceCode })
        val listAgain = cache.getDictsByAtomicServiceCode(atomicServiceCode)
        if (isLocalCacheEnabled()) assertSame(list.first(), listAgain.first(), "local 启用时同一维度再次从缓存获取应返回同一对象引用")
        assertTrue(cache.getDictsByAtomicServiceCode("sdch-ms-nonexistent").isEmpty())
    }

    // ---------- 全量刷新 ----------

    @Test
    fun reloadAll() {
        cache.reloadAll(true)
        val id = cache.getDictByAtomicServiceCodeAndDictType("sdch-ms-a1b2c3d4", "sdch-type-a1")?.id
        assertNotNull(id)
        val newDict = insertNewDict()
        cache.reloadAll(false)
        assertNotNull(cache.getDictByAtomicServiceCodeAndDictType("sdch-ms-a1b2c3d4", "sdch-type-a1"))
        assertNotNull(cache.getDictByAtomicServiceCodeAndDictType(newDict.atomicServiceCode, newDict.dictType))
        cache.reloadAll(true)
        val item = cache.getDictById("sdch1001-1a2b-4c5d-8e9f-000000000001")
        assertNotNull(item)
        val itemAgain = cache.getDictById("sdch1001-1a2b-4c5d-8e9f-000000000001")
        if (isLocalCacheEnabled()) assertSame(item, itemAgain, "同一 id 再次从缓存获取应返回同一对象引用")
    }

    // ---------- 同步 ----------

    @Test
    fun syncOnInsert() {
        cache.reloadAll(true)
        val newDict = insertNewDict()
        cache.syncOnInsert(newDict.id)
        val item = cache.getDictById(newDict.id)
        assertNotNull(item)
        val itemAgain = cache.getDictById(newDict.id)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain, "同一 id 再次从缓存获取应返回同一对象引用")
        assertNotNull(cache.getDictByAtomicServiceCodeAndDictType(newDict.atomicServiceCode, newDict.dictType))
    }

    @Test
    fun syncOnInsertWithAny() {
        cache.reloadAll(true)
        val newDict = insertNewDict()
        cache.syncOnInsert(Any(), newDict.id)
        val item = cache.getDictById(newDict.id)
        assertNotNull(item)
        val itemAgain = cache.getDictById(newDict.id)
        if (isLocalCacheEnabled()) assertTrue(item === itemAgain, "同一 id 再次从缓存获取应返回同一对象引用")
    }

    @Test
    fun syncOnUpdate() {
        cache.reloadAll(true)
        val id = "sdch4001-3a4b-6c7d-0e1f-000000000031"
        val newDictName = "sdch-dict-name-sync-1-updated"
        val dict = assertNotNull(sysDictDao.get(id))
        dict.dictName = newDictName
        sysDictDao.update(dict)
        cache.syncOnUpdate(id)
        val updated = cache.getDictById(id)
        assertNotNull(updated)
        assertEquals(newDictName, updated.dictName)
        val updatedAgain = cache.getDictById(id)
        if (isLocalCacheEnabled()) assertSame(updated, updatedAgain, "同一 id 再次从缓存获取应返回同一对象引用")
    }

    @Test
    fun syncOnUpdateWithOldParams() {
        cache.reloadAll(true)
        val id = "sdch4001-3a4b-6c7d-0e1f-000000000031"
        sysDictDao.updateProperties(id, mapOf(SysDict::dictName.name to "sdch-dict-name-sync-1-new"))
        cache.syncOnUpdate(Any(), id, "sdch-ms-sync-upd", "sdch-type-sync-1")
        val item = cache.getDictById(id)
        assertNotNull(item)
        val itemAgain = cache.getDictById(id)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain, "同一 id 再次从缓存获取应返回同一对象引用")
    }

    @Test
    fun syncOnUpdateActive() {
        cache.reloadAll(true)
        val id = "sdch1001-1a2b-4c5d-8e9f-000000000001"
        val dict = assertNotNull(sysDictDao.get(id))
        dict.active = false
        sysDictDao.update(dict)
        cache.syncOnUpdateActive(id, false)
        val entity = assertNotNull(sysDictDao.get(id))
        assertEquals(false, entity.active)
    }

    @Test
    fun syncOnDelete() {
        cache.reloadAll(true)
        val id = "sdch4003-5c6d-8e9f-2a3b-000000000033"
        sysDictDao.deleteById(id)
        cache.syncOnDelete(id)
        assertNull(sysDictDao.get(id), "删除并 sync 后 DB 中该 id 应不存在")
    }

    @Test
    fun syncOnBatchDelete() {
        cache.reloadAll(true)
        val ids = listOf(
            "sdch6001-2b3c-5d6e-9f0a-000000000061",
            "sdch6002-3c4d-6e7f-0a1b-000000000062"
        )
        ids.forEach { sysDictDao.deleteById(it) }
        cache.syncOnBatchDelete(ids)
        ids.forEach { assertNull(sysDictDao.get(it), "批量删除并 sync 后 DB 中该 id 应不存在") }
    }

    // ---------- key 工具 ----------

    @Test
    fun getKeyAtomicServiceCodeAndDictType() {
        val key = cache.getKeyAtomicServiceCodeAndDictType("sdch-ms-a", "type-1")
        assertTrue(key.contains("sdch-ms-a"))
        assertTrue(key.contains("type-1"))
    }

    private fun insertNewDict(): SysDict {
        val d = SysDict().apply {
            atomicServiceCode = "sdch-ms-a1b2c3d4"
            dictType = "sdch-type-new-${System.currentTimeMillis()}"
            dictName = "sdch-dict-name-new"
        }
        sysDictDao.insert(d)
        return d
    }
}
