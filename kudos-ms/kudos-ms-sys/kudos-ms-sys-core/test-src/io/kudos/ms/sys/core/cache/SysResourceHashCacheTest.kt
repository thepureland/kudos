package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ms.sys.core.dao.SysResourceDao
import io.kudos.ms.sys.core.model.po.SysResource
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
    private lateinit var handler: SysResourceHashCache

    @Resource
    private lateinit var sysResourceDao: SysResourceDao

    private fun isLocalCacheEnabled(): Boolean = HashCacheKit.isLocalCacheEnabled(SysResourceHashCache.CACHE_NAME)

    // ---------- 按主键 id ----------

    @Test
    fun getResourceById() {
        handler.reloadAll(true)
        val id = "srch1001-1a2b-4c5d-8e9f-000000000001"
        val item = handler.getResourceById(id)
        assertNotNull(item)
        assertEquals(id, item.id)
        assertEquals("srch-sys-7f3e2d1c", item.subSystemCode)
        assertEquals("/srch/url/1a2b4c5d/001", item.url)
        val itemAgain = handler.getResourceById(id)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain, "同一 id 再次从缓存获取应返回同一对象引用")
        assertNull(handler.getResourceById("srch-nonexistent-00000000000000000000"))
    }

    @Test
    fun getResourcesByIds() {
        handler.reloadAll(true)
        val ids = listOf(
            "srch1001-1a2b-4c5d-8e9f-000000000001",
            "srch1002-2b3c-5d6e-9f0a-000000000002",
            "srch1003-3c4d-6e7f-0a1b-000000000003"
        )
        val map = handler.getResourcesByIds(ids)
        assertEquals(3, map.size)
        map.forEach { (k, v) -> assertEquals(k, v.id) }
        val mapAgain = handler.getResourcesByIds(ids)
        if (isLocalCacheEnabled()) ids.forEach { assertSame(
            map[it],
            mapAgain[it],
            "同一 id 再次从缓存获取应返回同一对象引用"
        ) }
        assertTrue(handler.getResourcesByIds(emptyList()).isEmpty())
    }

    // ---------- 按子系统+URL ----------

    @Test
    fun listResourcesBySubSysAndUrl() {
        val subSystemCode = "srch-sys-a1b2c3d4"
        val url = "/srch/suburl/a1b2/p01"
        // 先确认测试数据已从 SQL 写入 DB（getResourceIdBySubSysAndUrl 仅查 active=true）
        val idFromDao = sysResourceDao.getResourceIdBySubSysAndUrl(subSystemCode, url)
        assertNotNull(idFromDao, "测试数据未加载：sql/h2/cache/SysResourceHashCacheTest.sql 中应有 sub_system_code=$subSystemCode, url=$url, active=true 的记录")
        assertEquals("srch2001-4d5e-7f8a-1b2c-000000000011", idFromDao)
        val list = handler.listResourcesBySubSysAndUrl(subSystemCode, url)
        assertEquals(1, list.size, "listResourcesBySubSysAndUrl 应返回 1 条")
        assertEquals("srch2001-4d5e-7f8a-1b2c-000000000011", list.first().id)
        val listAgain = handler.listResourcesBySubSysAndUrl(subSystemCode, url)
        if (isLocalCacheEnabled()) assertSame(list.first(), listAgain.first(), "local 启用时同一维度再次从缓存获取应返回同一对象引用")
        val empty = handler.listResourcesBySubSysAndUrl("srch-sys-a1b2c3d4", "/srch/suburl/a1b2/p03", active = true)
        assertTrue(empty.isEmpty())
        val inactive = handler.listResourcesBySubSysAndUrl("srch-sys-a1b2c3d4", "/srch/suburl/a1b2/p03", active = false)
        assertEquals(1, inactive.size)
        val inactiveAgain = handler.listResourcesBySubSysAndUrl("srch-sys-a1b2c3d4", "/srch/suburl/a1b2/p03", active = false)
        if (isLocalCacheEnabled()) assertSame(inactive.first(), inactiveAgain.first(), "local 启用时同一维度再次从缓存获取应返回同一对象引用")
    }

    @Test
    fun getResourceId() {
        handler.reloadAll(true)
        val id = handler.getResourceId("srch-sys-a1b2c3d4", "/srch/suburl/a1b2/p01")
        assertNotNull(id)
        assertEquals("srch2001-4d5e-7f8a-1b2c-000000000011", id)
        assertNull(handler.getResourceId("srch-sys-a1b2c3d4", "/srch/suburl/a1b2/p03"))
        assertNull(handler.getResourceId("srch-sys-nonexistent", "/any/url"))
    }

    // ---------- 按子系统+资源类型 ----------

    @Test
    fun listResourcesBySubSysAndType() {
        handler.reloadAll(true)
        val list = handler.listResourcesBySubSysAndType("srch-sys-e5f6a7b8", "1")
        assertEquals(3, list.size)
        val listAgain = handler.listResourcesBySubSysAndType("srch-sys-e5f6a7b8", "1")
        if (isLocalCacheEnabled()) assertSame(list.first(), listAgain.first(), "local 启用时同一维度再次从缓存获取应返回同一对象引用")
        val list2 = handler.listResourcesBySubSysAndType("srch-sys-e5f6a7b8", "2", active = true)
        assertEquals(1, list2.size)
        assertEquals("srch3004-1e2f-4a5b-8c9d-000000000024", list2.first().id)
        val list2Again = handler.listResourcesBySubSysAndType("srch-sys-e5f6a7b8", "2", active = true)
        if (isLocalCacheEnabled()) assertSame(list2.first(), list2Again.first(), "local 启用时同一维度再次从缓存获取应返回同一对象引用")
    }

    @Test
    fun getResourceIds() {
        handler.reloadAll(true)
        val ids = handler.getResourceIds("srch-sys-e5f6a7b8", "1")
        assertEquals(3, ids.size)
        assertTrue(ids.contains("srch3001-8b9c-1d2e-5f6a-000000000021"))
        val ids2 = handler.getResourceIds("srch-sys-e5f6a7b8", "2")
        assertEquals(1, ids2.size)
        assertTrue(handler.getResourceIds("srch-sys-nonexistent", "1").isEmpty())
    }

    // ---------- 全量刷新 ----------

    @Test
    fun reloadAll() {
        handler.reloadAll(true)
        val id = handler.getResourceId("srch-sys-a1b2c3d4", "/srch/suburl/a1b2/p01")
        assertNotNull(id)
        val newRes = insertNewResource()
        handler.reloadAll(false)
        assertNotNull(handler.getResourceId("srch-sys-a1b2c3d4", "/srch/suburl/a1b2/p01"))
        assertNotNull(handler.getResourceId(newRes.subSystemCode, newRes.url!!))
        handler.reloadAll(true)
        val item = handler.getResourceById("srch3001-8b9c-1d2e-5f6a-000000000021")
        assertNotNull(item)
        val itemAgain = handler.getResourceById("srch3001-8b9c-1d2e-5f6a-000000000021")
        if (isLocalCacheEnabled()) assertSame(item, itemAgain, "同一 id 再次从缓存获取应返回同一对象引用")
    }

    // ---------- 同步 ----------

    @Test
    fun syncOnInsert() {
        handler.reloadAll(true)
        val newRes = insertNewResource()
        handler.syncOnInsert(newRes.id!!)
        val item = handler.getResourceById(newRes.id!!)
        assertNotNull(item)
        val itemAgain = handler.getResourceById(newRes.id!!)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain, "同一 id 再次从缓存获取应返回同一对象引用")
        assertNotNull(handler.getResourceId(newRes.subSystemCode, newRes.url!!))
    }

    @Test
    fun syncOnInsertWithAny() {
        handler.reloadAll(true)
        val newRes = insertNewResource()
        handler.syncOnInsert(Any(), newRes.id!!)
        val item = handler.getResourceById(newRes.id!!)
        assertNotNull(item)
        val itemAgain = handler.getResourceById(newRes.id!!)
        if (isLocalCacheEnabled()) assertTrue(item === itemAgain, "同一 id 再次从缓存获取应返回同一对象引用")
    }

    @Test
    fun syncOnUpdate() {
        handler.reloadAll(true)
        val id = "srch4001-3a4b-6c7d-0e1f-000000000031"
        val newUrl = "/srch/sync/upd/001-new"
        val res = sysResourceDao.get(id)!!
        res.url = newUrl
        sysResourceDao.update(res)
        handler.syncOnUpdate(id)
        val updated = handler.getResourceById(id)
        assertNotNull(updated)
        assertEquals(newUrl, updated.url)
        val updatedAgain = handler.getResourceById(id)
        if (isLocalCacheEnabled()) assertSame(updated, updatedAgain, "同一 id 再次从缓存获取应返回同一对象引用")
    }

    @Test
    fun syncOnUpdateWithOldUrl() {
        handler.reloadAll(true)
        val id = "srch4002-4b5c-7d8e-1f2a-000000000032"
        sysResourceDao.updateProperties(id, mapOf(SysResource::url.name to "/srch/sync/upd/002-new"))
        handler.syncOnUpdate(Any(), id, "/srch/sync/upd/002")
        val item = handler.getResourceById(id)
        assertNotNull(item)
        val itemAgain = handler.getResourceById(id)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain, "同一 id 再次从缓存获取应返回同一对象引用")
    }

    @Test
    fun syncOnUpdateActive() {
        handler.reloadAll(true)
        val id = "srch5001-1a2b-4c5d-8e9f-000000000051"
        val subSystemCode = "srch-sys-sync-active"
        val url = "/srch/sync/active/001"
        val res = sysResourceDao.get(id)!!
        res.active = false
        sysResourceDao.update(res)
        handler.syncOnUpdateActive(id, false)
        assertNull(sysResourceDao.getResourceIdBySubSysAndUrl(subSystemCode, url, true), "DB 层按 active=true 应查不到该记录")
        assertEquals(id, sysResourceDao.getResourceIdBySubSysAndUrl(subSystemCode, url, false), "DB 层按 active=false 应能查到该 id")
    }

    @Test
    fun syncOnDelete() {
        handler.reloadAll(true)
        val id = "srch4003-5c6d-8e9f-2a3b-000000000033"
        val item = handler.getResourceById(id)!!
        sysResourceDao.deleteById(id)
        handler.syncOnDelete(id, item.subSystemCode!!, item.url)
        assertNull(sysResourceDao.get(id), "删除并 sync 后 DB 中该 id 应不存在")
    }

    @Test
    fun syncOnBatchDelete() {
        handler.reloadAll(true)
        val ids = listOf(
            "srch6001-2b3c-5d6e-9f0a-000000000061",
            "srch6002-3c4d-6e7f-0a1b-000000000062"
        )
        ids.forEach { sysResourceDao.deleteById(it) }
        handler.syncOnBatchDelete(ids)
        ids.forEach { assertNull(sysResourceDao.get(it), "批量删除并 sync 后 DB 中该 id 应不存在") }
    }

    // ---------- key 工具 ----------

    @Test
    fun getKeySubSysAndUrl() {
        val key = handler.getKeySubSysAndUrl("srch-sys-a", "/path")
        assertTrue(key.contains("srch-sys-a"))
        assertTrue(key.contains("/path"))
    }

    @Test
    fun getKeySubSysAndType() {
        val key = handler.getKeySubSysAndType("srch-sys-a", "1")
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
