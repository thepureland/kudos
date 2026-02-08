package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ms.sys.core.dao.SysResourceDao
import io.kudos.ms.sys.core.model.po.SysResource
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.*

/**
 * SysResourceCacheHandler 测试。
 *
 * 测试数据来源：`sql/h2/cache/SysResourceHashCacheTest.sql`
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

    // ---------- 按主键 id ----------

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
        if (isLocalCacheEnabled()) assertSame(item, itemAgain, "同一 id 再次从缓存获取应返回同一对象引用")
        assertNull(cache.getResourceById("srch-nonexistent-00000000000000000000"))
    }

    @Test
    fun getResourcesByIds() {
        cache.reloadAll(true)
        val ids = listOf(
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
            "同一 id 再次从缓存获取应返回同一对象引用"
        ) }
        assertTrue(cache.getResourcesByIds(emptyList()).isEmpty())
    }

    // ---------- 按子系统+URL ----------

    @Test
    fun getResourceBySubSystemCodeAndUrl() {
        val subSystemCode = "srch-sys-a1b2c3d4"
        val url = "/srch/suburl/a1b2/p01"
        // 先确认测试数据已从 SQL 写入 DB（getResourceIdBySubSysAndUrl 仅查 active=true）
        val idFromDao = sysResourceDao.getResourceBySubSysAndUrl(subSystemCode, url)?.id
        assertNotNull(idFromDao, "测试数据未加载：sql/h2/cache/SysResourceHashCacheTest.sql 中应有 sub_system_code=$subSystemCode, url=$url, active=true 的记录")
        assertEquals("srch2001-4d5e-7f8a-1b2c-000000000011", idFromDao)
        val res1 = cache.getResourceBySubSystemCodeAndUrl(subSystemCode, url)
        val id = res1?.id
        assertEquals("srch2001-4d5e-7f8a-1b2c-000000000011", id)
        val res2 = cache.getResourceBySubSystemCodeAndUrl(subSystemCode, url)
        if (isLocalCacheEnabled()) assertSame(res1, res2, "local 启用时同一维度再次从缓存获取应返回同一对象引用")
    }


    // ---------- 按子系统+资源类型 ----------

    @Test
    fun getResourcesBySubSystemCodeAndType() {
        cache.reloadAll(true)
        val list = cache.getResourcesBySubSystemCodeAndType("srch-sys-e5f6a7b8", "1")
        assertEquals(3, list.size)
        val listAgain = cache.getResourcesBySubSystemCodeAndType("srch-sys-e5f6a7b8", "1")
        if (isLocalCacheEnabled()) assertSame(list.first(), listAgain.first(), "local 启用时同一维度再次从缓存获取应返回同一对象引用")
        val list2 = cache.getResourcesBySubSystemCodeAndType("srch-sys-e5f6a7b8", "2")
        assertEquals(2, list2.size)
        assertEquals("srch3004-1e2f-4a5b-8c9d-000000000024", list2.first().id)
        val list2Again = cache.getResourcesBySubSystemCodeAndType("srch-sys-e5f6a7b8", "2")
        if (isLocalCacheEnabled()) assertSame(list2.first(), list2Again.first(), "local 启用时同一维度再次从缓存获取应返回同一对象引用")
    }


    // ---------- 全量刷新 ----------

    @Test
    fun reloadAll() {
        cache.reloadAll(true)
        val id = cache.getResourceBySubSystemCodeAndUrl("srch-sys-a1b2c3d4", "/srch/suburl/a1b2/p01")?.id
        assertNotNull(id)
        val newRes = insertNewResource()
        cache.reloadAll(false)
        assertNotNull(cache.getResourceBySubSystemCodeAndUrl("srch-sys-a1b2c3d4", "/srch/suburl/a1b2/p01"))
        assertNotNull(cache.getResourceBySubSystemCodeAndUrl(newRes.subSystemCode, newRes.url!!))
        cache.reloadAll(true)
        val item = cache.getResourceById("srch3001-8b9c-1d2e-5f6a-000000000021")
        assertNotNull(item)
        val itemAgain = cache.getResourceById("srch3001-8b9c-1d2e-5f6a-000000000021")
        if (isLocalCacheEnabled()) assertSame(item, itemAgain, "同一 id 再次从缓存获取应返回同一对象引用")
    }

    // ---------- 同步 ----------

    @Test
    fun syncOnInsert() {
        cache.reloadAll(true)
        val newRes = insertNewResource()
        cache.syncOnInsert(newRes.id!!)
        val item = cache.getResourceById(newRes.id!!)
        assertNotNull(item)
        val itemAgain = cache.getResourceById(newRes.id!!)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain, "同一 id 再次从缓存获取应返回同一对象引用")
        assertNotNull(cache.getResourceBySubSystemCodeAndUrl(newRes.subSystemCode, newRes.url!!))
    }

    @Test
    fun syncOnInsertWithAny() {
        cache.reloadAll(true)
        val newRes = insertNewResource()
        cache.syncOnInsert(Any(), newRes.id!!)
        val item = cache.getResourceById(newRes.id!!)
        assertNotNull(item)
        val itemAgain = cache.getResourceById(newRes.id!!)
        if (isLocalCacheEnabled()) assertTrue(item === itemAgain, "同一 id 再次从缓存获取应返回同一对象引用")
    }

    @Test
    fun syncOnUpdate() {
        cache.reloadAll(true)
        val id = "srch4001-3a4b-6c7d-0e1f-000000000031"
        val newUrl = "/srch/sync/upd/001-new"
        val res = sysResourceDao.getAs(id)!!
        res.url = newUrl
        sysResourceDao.update(res)
        cache.syncOnUpdate(id)
        val updated = cache.getResourceById(id)
        assertNotNull(updated)
        assertEquals(newUrl, updated.url)
        val updatedAgain = cache.getResourceById(id)
        if (isLocalCacheEnabled()) assertSame(updated, updatedAgain, "同一 id 再次从缓存获取应返回同一对象引用")
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
        if (isLocalCacheEnabled()) assertSame(item, itemAgain, "同一 id 再次从缓存获取应返回同一对象引用")
    }

    @Test
    fun syncOnUpdateActive() {
        cache.reloadAll(true)
        val id = "srch5001-1a2b-4c5d-8e9f-000000000051"
        val subSystemCode = "srch-sys-sync-active"
        val url = "/srch/sync/active/001"
        val res = sysResourceDao.getAs(id)!!
        res.active = false
        sysResourceDao.update(res)
        cache.syncOnUpdateActive(id, false)
        assertNull(sysResourceDao.getResourceBySubSysAndUrl(subSystemCode, url, true), "DB 层按 active=true 应查不到该记录")
        assertEquals(id, sysResourceDao.getResourceBySubSysAndUrl(subSystemCode, url, false)?.id, "DB 层按 active=false 应能查到该 id")
    }

    @Test
    fun syncOnDelete() {
        cache.reloadAll(true)
        val id = "srch4003-5c6d-8e9f-2a3b-000000000033"
        val item = cache.getResourceById(id)!!
        sysResourceDao.deleteById(id)
        cache.syncOnDelete(id, item.subSystemCode!!, item.url)
        assertNull(sysResourceDao.getAs(id), "删除并 sync 后 DB 中该 id 应不存在")
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
        ids.forEach { assertNull(sysResourceDao.getAs(it), "批量删除并 sync 后 DB 中该 id 应不存在") }
    }

    // ---------- key 工具 ----------

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
