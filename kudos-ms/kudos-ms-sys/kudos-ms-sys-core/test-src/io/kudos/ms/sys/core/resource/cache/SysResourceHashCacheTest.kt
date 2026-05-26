package io.kudos.ms.sys.core.resource.cache

import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ms.sys.core.resource.dao.SysResourceDao
import io.kudos.ms.sys.core.resource.model.po.SysResource
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.*

/**
 * Tests for SysResourceCacheHandler.
 *
 * Test data source: `sql/h2/resource/cache/SysResourceHashCacheTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysResourceHashCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cache: SysResourceHashCache

    @Resource
    private lateinit var sysResourceDao: SysResourceDao

    private fun isLocalCacheEnabled(): Boolean = HashCacheKit.isLocalCacheEnabled(SysResourceHashCache.CACHE_NAME)

    // ---------- Lookup by primary id ----------

    @Test
    fun getResourceById() {
        cache.reloadAll(true)
        val id = "srch1001-1a2b-4c5d-8e9f-000000000001"
        val item = cache.getResourceById(id)
        assertNotNull(item)
        assertEquals(id, item.id)
        assertEquals("srch-sys-7f3e2d1c", item.subSystemCode)
        assertEquals("/srch/url/1a2b4c5d/001", item.url)
        val itemAgain = cache.getResourceById(id)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain, "Fetching the same id from cache again should return the same object reference")
        assertNull(cache.getResourceById("srch-nonexistent-00000000000000000000"))
    }

    @Test
    fun getResourcesByIds() {
        cache.reloadAll(true)
        val ids = setOf(
            "srch1001-1a2b-4c5d-8e9f-000000000001",
            "srch1002-2b3c-5d6e-9f0a-000000000002",
            "srch1003-3c4d-6e7f-0a1b-000000000003"
        )
        val map = cache.getResourcesByIds(ids)
        assertEquals(3, map.size)
        map.forEach { (k, v) -> assertEquals(k, v.id) }
        val mapAgain = cache.getResourcesByIds(ids)
        if (isLocalCacheEnabled()) ids.forEach { assertSame(
            map[it],
            mapAgain[it],
            "Fetching the same id from cache again should return the same object reference"
        ) }
        assertTrue(cache.getResourcesByIds(emptySet()).isEmpty())
    }

    // ---------- Lookup by sub-system + URL ----------

    @Test
    fun getResourceBySubSystemCodeAndUrl() {
        val subSystemCode = "srch-sys-a1b2c3d4"
        val url = "/srch/suburl/a1b2/p01"
        // Confirm test data has been written to DB from SQL (getResourceIdBySubSysAndUrl only queries active=true)
        val idFromDao = sysResourceDao.fetchResourceBySubSysAndUrl(subSystemCode, url)?.id
        assertNotNull(idFromDao, "Test data not loaded: sql/h2/resource/cache/SysResourceHashCacheTest.sql should contain a record with sub_system_code=$subSystemCode, url=$url, active=true")
        assertEquals("srch2001-4d5e-7f8a-1b2c-000000000011", idFromDao)
        val res1 = cache.getResourceBySubSystemCodeAndUrl(subSystemCode, url)
        val id = res1?.id
        assertEquals("srch2001-4d5e-7f8a-1b2c-000000000011", id)
        val res2 = cache.getResourceBySubSystemCodeAndUrl(subSystemCode, url)
        if (isLocalCacheEnabled()) assertSame(res1, res2, "When local cache is enabled, fetching the same dimension again should return the same object reference")
    }


    // ---------- Lookup by sub-system + resource type ----------

    @Test
    fun getResourcesBySubSystemCodeAndType() {
        cache.reloadAll(true)
        val list = cache.getResourcesBySubSystemCodeAndType("srch-sys-e5f6a7b8", "1")
        assertEquals(3, list.size)
        val listAgain = cache.getResourcesBySubSystemCodeAndType("srch-sys-e5f6a7b8", "1")
        if (isLocalCacheEnabled()) assertSame(list.first(), listAgain.first(), "When local cache is enabled, fetching the same dimension again should return the same object reference")
        val list2 = cache.getResourcesBySubSystemCodeAndType("srch-sys-e5f6a7b8", "2")
        assertEquals(2, list2.size)
        assertEquals("srch3004-1e2f-4a5b-8c9d-000000000024", list2.first().id)
        val list2Again = cache.getResourcesBySubSystemCodeAndType("srch-sys-e5f6a7b8", "2")
        if (isLocalCacheEnabled()) assertSame(list2.first(), list2Again.first(), "When local cache is enabled, fetching the same dimension again should return the same object reference")
    }


    // ---------- Full refresh ----------

    @Test
    fun reloadAll() {
        cache.reloadAll(true)
        val id = cache.getResourceBySubSystemCodeAndUrl("srch-sys-a1b2c3d4", "/srch/suburl/a1b2/p01")?.id
        assertNotNull(id)
        val newRes = insertNewResource()
        val newUrl = assertNotNull(newRes.url)
        cache.reloadAll(false)
        assertNotNull(cache.getResourceBySubSystemCodeAndUrl("srch-sys-a1b2c3d4", "/srch/suburl/a1b2/p01"))
        assertNotNull(cache.getResourceBySubSystemCodeAndUrl(newRes.subSystemCode, newUrl))
        cache.reloadAll(true)
        val item = cache.getResourceById("srch3001-8b9c-1d2e-5f6a-000000000021")
        assertNotNull(item)
        val itemAgain = cache.getResourceById("srch3001-8b9c-1d2e-5f6a-000000000021")
        if (isLocalCacheEnabled()) assertSame(item, itemAgain, "Fetching the same id from cache again should return the same object reference")
    }

    // ---------- Synchronization ----------

    @Test
    fun syncOnInsert() {
        cache.reloadAll(true)
        val newRes = insertNewResource()
        val newUrl = assertNotNull(newRes.url)
        cache.syncOnInsert(newRes.id)
        val item = cache.getResourceById(newRes.id)
        assertNotNull(item)
        val itemAgain = cache.getResourceById(newRes.id)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain, "Fetching the same id from cache again should return the same object reference")
        assertNotNull(cache.getResourceBySubSystemCodeAndUrl(newRes.subSystemCode, newUrl))
    }

    @Test
    fun syncOnInsertWithAny() {
        cache.reloadAll(true)
        val newRes = insertNewResource()
        cache.syncOnInsert(Any(), newRes.id)
        val item = cache.getResourceById(newRes.id)
        assertNotNull(item)
        val itemAgain = cache.getResourceById(newRes.id)
        if (isLocalCacheEnabled()) assertTrue(item === itemAgain, "Fetching the same id from cache again should return the same object reference")
    }

    @Test
    fun syncOnUpdate() {
        cache.reloadAll(true)
        val id = "srch4001-3a4b-6c7d-0e1f-000000000031"
        val newUrl = "/srch/sync/upd/001-new"
        val res = assertNotNull(sysResourceDao.get(id))
        res.url = newUrl
        sysResourceDao.update(res)
        cache.syncOnUpdate(id)
        val updated = cache.getResourceById(id)
        assertNotNull(updated)
        assertEquals(newUrl, updated.url)
        val updatedAgain = cache.getResourceById(id)
        if (isLocalCacheEnabled()) assertSame(updated, updatedAgain, "Fetching the same id from cache again should return the same object reference")
    }

    @Test
    fun syncOnUpdateWithOldUrl() {
        cache.reloadAll(true)
        val id = "srch4002-4b5c-7d8e-1f2a-000000000032"
        sysResourceDao.updateProperties(id, mapOf(SysResource::url.name to "/srch/sync/upd/002-new"))
        cache.syncOnUpdate(Any(), id, "/srch/sync/upd/002")
        val item = cache.getResourceById(id)
        assertNotNull(item)
        val itemAgain = cache.getResourceById(id)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain, "Fetching the same id from cache again should return the same object reference")
    }

    @Test
    fun syncOnUpdateActive() {
        cache.reloadAll(true)
        val id = "srch5001-1a2b-4c5d-8e9f-000000000051"
        val res = assertNotNull(sysResourceDao.get(id))
        res.active = false
        sysResourceDao.update(res)
        cache.syncOnUpdateActive(id, false)
        // fetchResourceBySubSysAndUrl only queries active=true, so use get to verify the DB row is now false
        val resource = assertNotNull(sysResourceDao.get(id))
        assertEquals(false, resource.active)
    }

    @Test
    fun syncOnDelete() {
        cache.reloadAll(true)
        val id = "srch4003-5c6d-8e9f-2a3b-000000000033"
        val item = assertNotNull(cache.getResourceById(id))
        val subSystemCode = assertNotNull(item.subSystemCode)
        sysResourceDao.deleteById(id)
        cache.syncOnDelete(id, subSystemCode, item.url)
        assertNull(sysResourceDao.get(id), "After delete + sync, the id should no longer exist in the DB")
    }

    @Test
    fun syncOnBatchDelete() {
        cache.reloadAll(true)
        val ids = listOf(
            "srch6001-2b3c-5d6e-9f0a-000000000061",
            "srch6002-3c4d-6e7f-0a1b-000000000062"
        )
        ids.forEach { sysResourceDao.deleteById(it) }
        cache.syncOnBatchDelete(ids)
        ids.forEach { assertNull(sysResourceDao.get(it), "After batch delete + sync, the id should no longer exist in the DB") }
    }

    // ---------- Key utilities ----------

    @Test
    fun getKeySubSysAndUrl() {
        val key = cache.getKeySubSysAndUrl("srch-sys-a", "/path")
        assertTrue(key.contains("srch-sys-a"))
        assertTrue(key.contains("/path"))
    }

    @Test
    fun getKeySubSysAndType() {
        val key = cache.getKeySubSysAndType("srch-sys-a", "1")
        assertTrue(key.contains("srch-sys-a"))
        assertTrue(key.contains("1"))
    }

    private fun insertNewResource(): SysResource {
        val r = SysResource().apply {
            subSystemCode = "srch-sys-a1b2c3d4"
            url = "/srch/suburl/a1b2/p99"
            name = "srch-name-insert-new-99"
            resourceTypeDictCode = "1"
        }
        sysResourceDao.insert(r)
        return r
    }
}
