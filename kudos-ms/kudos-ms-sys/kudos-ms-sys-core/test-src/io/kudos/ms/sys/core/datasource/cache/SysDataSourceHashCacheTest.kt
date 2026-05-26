package io.kudos.ms.sys.core.datasource.cache

import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ms.sys.common.datasource.vo.SysDataSourceCacheEntry
import io.kudos.ms.sys.core.datasource.dao.SysDataSourceDao
import io.kudos.ms.sys.core.datasource.model.po.SysDataSource
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.*

/**
 * Unit tests for [SysDataSourceHashCache] (hash cache, combining lookup by id and by tenantId + subSystemCode + microServiceCode).
 *
 * Covers: get by id single/batch, list by 3-code, full reload, sync after insert/update/active-toggle/delete;
 * when the local cache is enabled, a second fetch returns the same object reference.
 *
 * Test data: `SysDataSourceHashCacheTest.sql`.
 * Requires Docker-run Redis and SYS_DATA_SOURCE__HASH configured in sys_cache (hash=true).
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

    // ---------- By primary key id ----------

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

    // ---------- By tenantId + subSystemCode + microServiceCode ----------

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
        assertEquals(2, list.size)
        assert(list.any { it.id == "33333333-e828-43c5-a512-888888888888" })
    }

    /** DAO's getDataSources does not filter by active, so (tenant-2, subSys-d, ms-c) returns the active=false row. */
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

    // ---------- Full reload ----------

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

        // refreshAll is implemented as clear-then-write list, so with clear=false the cache still reflects the current full DB state (the deleted id is no longer in the list)
        cacheHandler.reloadAll(false)
        assertNotNull(cacheHandler.getDataSourceById(dsNew.id))
        assertEquals(newName, cacheHandler.getDataSourceById(idUpdate)?.name)
        assertNull(cacheHandler.getDataSourceById(idDelete))

        // After clear=true full reload, the deleted id is absent from cache and the updated name is loaded from DB (Mix syncs to local)
        cacheHandler.reloadAll(true)
        assertNull(sysDataSourceDao.getAs<SysDataSourceCacheEntry>(idDelete))
        assertNull(cacheHandler.getDataSourceById(idDelete))
        assertEquals(newName, sysDataSourceDao.getAs<SysDataSourceCacheEntry>(idUpdate)?.name)
        assertEquals(newName, cacheHandler.getDataSourceById(idUpdate)?.name)
    }

    // ---------- Sync ----------

    @Test
    fun syncOnInsert() {
        cacheHandler.reloadAll(true)
        val ds = insertNewRecordToDb()
        cacheHandler.syncOnInsert(ds, ds.id)
        val item = cacheHandler.getDataSourceById(ds.id)
        assertNotNull(item)
        val itemAgain = cacheHandler.getDataSourceById(ds.id)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain)
    }

    @Test
    fun syncOnUpdate() {
        cacheHandler.reloadAll(true)
        val id = "33333333-e828-43c5-a512-222222222222"
        val success = sysDataSourceDao.updateProperties(id, mapOf(SysDataSource::name.name to newName))
        assertTrue(success)
        val ds = assertNotNull(cacheHandler.getDataSourceById(id))
        cacheHandler.syncOnUpdate(ds, id)
        assertEquals(newName, sysDataSourceDao.getAs<SysDataSourceCacheEntry>(id)?.name)
        assertEquals(newName, cacheHandler.getDataSourceById(id)?.name)
    }

    @Test
    fun syncOnUpdateActive() {
        cacheHandler.reloadAll(true)
        val id = "33333333-e828-43c5-a512-888888888888"
        sysDataSourceDao.updateProperties(id, mapOf(SysDataSource::active.name to false))
//        val ds = cacheHandler.getDataSourceById(id)
        cacheHandler.syncOnUpdateActive(id, false)
        assertEquals(false, sysDataSourceDao.getAs<SysDataSourceCacheEntry>(id)?.active)
        val afterFalse = cacheHandler.getDataSourceById(id)
        assertNotNull(afterFalse)
        assertEquals(false, afterFalse.active)

        sysDataSourceDao.updateProperties(id, mapOf(SysDataSource::active.name to true))
        cacheHandler.syncOnUpdateActive(id, true)
        assertEquals(true, sysDataSourceDao.getAs<SysDataSourceCacheEntry>(id)?.active)
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
        assertNull(sysDataSourceDao.getAs<SysDataSourceCacheEntry>(id))
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
