package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ms.sys.common.vo.datasource.SysDataSourceCacheItem
import io.kudos.ms.sys.core.dao.SysDataSourceDao
import io.kudos.ms.sys.core.model.po.SysDataSource
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
 * [SysDataSourceHashCache] 单元测试（Hash 缓存，整合按 id 与按 tenantId+subSystemCode+microServiceCode）。
 *
 * 覆盖：按 id 单条/批量、按 3 码列表、全量刷新、新增/更新/更新启用状态/删除后同步；
 * 本地缓存开启时二次取为同一对象引用。
 *
 * 测试数据：`sql/h2/cache/SysDataSourceHashCacheTest.sql`。
 * 需 Docker 运行 Redis，且 sys_cache 中已配置 SYS_DATA_SOURCE__HASH（hash=true）。
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDataSourceHashCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: SysDataSourceHashCache

    @Resource
    private lateinit var sysDataSourceDao: SysDataSourceDao

    private fun isLocalCacheEnabled(): Boolean = HashCacheKit.isLocalCacheEnabled(SysDataSourceHashCache.CACHE_NAME)

    private val newName = "test_ds_hash_new_name"

    // ---------- 按主键 id ----------

    @Test
    fun getDataSourceById() {
        cacheHandler.reloadAll(true)
        val id = "33333333-e828-43c5-a512-111111114033"
        val item = cacheHandler.getDataSourceById(id)
        assertNotNull(item)
        assertEquals(id, item.id)
        assertEquals("test_ds_11", item.name)
        val itemAgain = cacheHandler.getDataSourceById(id)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain)
        assertNull(cacheHandler.getDataSourceById("no_exist_id"))
    }

    @Test
    fun getDataSourcesByIds() {
        cacheHandler.reloadAll(true)
        val id1 = "33333333-e828-43c5-a512-111111114033"
        val id2 = "33333333-e828-43c5-a512-222222222222"
        val result = cacheHandler.getDataSourcesByIds(listOf(id1, id2))
        assertEquals(2, result.size)
        assertNotNull(result[id1])
        assertNotNull(result[id2])
        val resultAgain = cacheHandler.getDataSourcesByIds(listOf(id1, id2))
        if (isLocalCacheEnabled()) {
            assertSame(result[id1], resultAgain[id1])
            assertSame(result[id2], resultAgain[id2])
        }
        val partial = cacheHandler.getDataSourcesByIds(listOf("no_exist_id", id2))
        assertEquals(1, partial.size)
        assertTrue(cacheHandler.getDataSourcesByIds(emptyList()).isEmpty())
    }

    // ---------- 按 tenantId + subSystemCode + microServiceCode ----------

    @Test
    fun getDataSources_byTenantIdAnd3Codes() {
        cacheHandler.reloadAll(true)
        val tenantId = "10a45fe6-4c8d-40c7-8f23-bba-tenant-1"
        val subSystemCode = "subSys-a"
        val microServiceCode = "ms-a"
        val list = cacheHandler.getDataSources(tenantId, subSystemCode, microServiceCode)
        assertEquals(1, list.size)
        val first = list.first()
        assertEquals(tenantId, first.tenantId)
        assertEquals(subSystemCode, first.subSystemCode)
        assertEquals(microServiceCode, first.microServiceCode)
        val listAgain = cacheHandler.getDataSources(tenantId, subSystemCode, microServiceCode)
        if (isLocalCacheEnabled() && listAgain.isNotEmpty()) {
            assertSame(list.first(), listAgain.first())
        }
    }

    @Test
    fun getDataSources_microServiceCodeNull() {
        cacheHandler.reloadAll(true)
        val tenantId = "10a45fe6-4c8d-40c7-8f23-bba-tenant-1"
        val subSystemCode = "subSys-c"
        val list = cacheHandler.getDataSources(tenantId, subSystemCode, null)
        assertEquals(1, list.size)
        assertEquals("33333333-e828-43c5-a512-888888888888", list.first().id)
    }

    /** DAO 的 getDataSources 不过滤 active，故 (tenant-2, subSys-d, ms-c) 会返回 active=false 的那条 */
    @Test
    fun getDataSources_inactiveRecord() {
        cacheHandler.reloadAll(true)
        val list = cacheHandler.getDataSources(
            "10a45fe6-4c8d-40c7-8f23-bba-tenant-2",
            "subSys-d",
            "ms-c"
        )
        assertEquals(1, list.size)
        assertEquals(false, list.first().active)
    }

    // ---------- 全量刷新 ----------

    @Test
    fun reloadAll() {
        cacheHandler.reloadAll(true)
        val id = "33333333-e828-43c5-a512-111111114033"
        val item = cacheHandler.getDataSourceById(id)
        assertNotNull(item)

        val dsNew = insertNewRecordToDb()
        val idUpdate = "33333333-e828-43c5-a512-222222222222"
        val idDelete = "33333333-e828-43c5-a512-333333333333"
        sysDataSourceDao.updateProperties(idUpdate, mapOf(SysDataSource::name.name to newName))
        sysDataSourceDao.deleteById(idDelete)

        // refreshAll 实现为先清空再写入 list，故 clear=false 时缓存仍为当前 DB 全量（已删 id 已不在 list 中）
        cacheHandler.reloadAll(false)
        assertNotNull(cacheHandler.getDataSourceById(dsNew.id!!))
        assertEquals(newName, cacheHandler.getDataSourceById(idUpdate)?.name)
        assertNull(cacheHandler.getDataSourceById(idDelete))

        // clear=true 后全量重载，已删 id 不在缓存，已更新 name 从 DB 加载（Mix 已同步写本地）
        cacheHandler.reloadAll(true)
        assertNull(sysDataSourceDao.getCacheItem(idDelete))
        assertNull(cacheHandler.getDataSourceById(idDelete))
        assertEquals(newName, sysDataSourceDao.getCacheItem(idUpdate)?.name)
        assertEquals(newName, cacheHandler.getDataSourceById(idUpdate)?.name)
    }

    // ---------- 同步 ----------

    @Test
    fun syncOnInsert() {
        cacheHandler.reloadAll(true)
        val ds = insertNewRecordToDb()
        cacheHandler.syncOnInsert(ds, ds.id!!)
        val item = cacheHandler.getDataSourceById(ds.id!!)
        assertNotNull(item)
        val itemAgain = cacheHandler.getDataSourceById(ds.id!!)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain)
    }

    @Test
    fun syncOnUpdate() {
        cacheHandler.reloadAll(true)
        val id = "33333333-e828-43c5-a512-222222222222"
        val success = sysDataSourceDao.updateProperties(id, mapOf(SysDataSource::name.name to newName))
        assertTrue(success)
        val ds = cacheHandler.getDataSourceById(id)!!
        cacheHandler.syncOnUpdate(ds, id)
        assertEquals(newName, sysDataSourceDao.getCacheItem(id)?.name)
        assertEquals(newName, cacheHandler.getDataSourceById(id)?.name)
    }

    @Test
    fun syncOnUpdateActive() {
        cacheHandler.reloadAll(true)
        val id = "33333333-e828-43c5-a512-888888888888"
        sysDataSourceDao.updateProperties(id, mapOf(SysDataSource::active.name to false))
        val ds = cacheHandler.getDataSourceById(id)!!
        cacheHandler.syncOnUpdateActive(id, false)
        assertEquals(false, sysDataSourceDao.getCacheItem(id)?.active)
        val afterFalse = cacheHandler.getDataSourceById(id)
        assertNotNull(afterFalse)
        assertEquals(false, afterFalse.active)

        sysDataSourceDao.updateProperties(id, mapOf(SysDataSource::active.name to true))
        cacheHandler.syncOnUpdateActive(id, true)
        assertEquals(true, sysDataSourceDao.getCacheItem(id)?.active)
        val afterTrue = cacheHandler.getDataSourceById(id)
        assertNotNull(afterTrue)
        assertEquals(true, afterTrue.active)
    }

    @Test
    fun syncOnDelete() {
        cacheHandler.reloadAll(true)
        val id = "33333333-e828-43c5-a512-333333333333"
        cacheHandler.getDataSourceById(id)
        sysDataSourceDao.deleteById(id)
        cacheHandler.syncOnDelete(id)
        assertNull(sysDataSourceDao.getCacheItem(id))
        assertNull(cacheHandler.getDataSourceById(id))
    }

    private fun insertNewRecordToDb(): SysDataSource {
        val ds = SysDataSource().apply {
            name = "a_new_test_ds_hash"
            url = "url"
            username = "sa"
            password = "sa"
            subSystemCode = "default"
            microServiceCode = "default"
            tenantId = "10a45fe6-4c8d-40c7-8f23-bba-tenant-n"
            active = true
        }
        sysDataSourceDao.insert(ds)
        return ds
    }
}
