package io.kudos.ms.auth.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ms.auth.common.vo.group.AuthGroupCacheItem
import io.kudos.ms.auth.core.dao.AuthGroupDao
import io.kudos.ms.auth.core.model.po.AuthGroup
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


/**
 * junit test for GroupByIdCacheHandler
 *
 * 测试数据来源：`GroupByIdCacheTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class GroupByIdCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: GroupByIdCache

    @Resource
    private lateinit var authGroupDao: AuthGroupDao

    private val newGroupName = "新用户组名称"

    @Test
    fun getGroupById() {
        // 存在的
        var id = "bd9f1e96-1111-1111-1111-aaaaaaaaaaaa"
        val cacheItem2 = cacheHandler.getGroupById(id)
        val cacheItem3 = cacheHandler.getGroupById(id)
        assertNotNull(cacheItem2)
        assert(cacheItem3 === cacheItem2)

        // 不存在的
        id = "no_exist_id"
        assertNull(cacheHandler.getGroupById(id))
    }

    @Test
    fun getGroupsByIds() {
        // 都存在的
        var id1 = "bd9f1e96-1111-1111-1111-aaaaaaaaaaaa"
        var id2 = "bd9f1e96-2222-2222-2222-bbbbbbbbbbbb"
        val result2 = cacheHandler.getGroupsByIds(listOf(id1, id2))
        val result3 = cacheHandler.getGroupsByIds(listOf(id1, id2))
        assert(result2.isNotEmpty())
        assert(result3 == result2)

        // 部分存在的
        id1 = "no_exist_id-1"
        var cacheItems = cacheHandler.getGroupsByIds(listOf(id1, id2))
        assertEquals(1, cacheItems.size)

        // 都不存在的
        id2 = "no_exist_id-2"
        cacheItems = cacheHandler.getGroupsByIds(listOf(id1, id2))
        assert(cacheItems.isEmpty())
    }

    @Test
    fun syncOnInsert() {
        // 插入新的记录到数据库
        val id = insertNewRecordToDb()

        // 同步缓存
        cacheHandler.syncOnInsert(id)

        // 验证新记录是否在缓存中
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), id)
        assertNotNull(cacheItem1)
        val cacheItem2 = cacheHandler.getGroupById(id)
        assertNotNull(cacheItem2)
        val cacheItem3 = cacheHandler.getGroupById(id)
        assert(cacheItem2 === cacheItem3)
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val id = "bd9f1e96-2222-2222-2222-bbbbbbbbbbbb"
        val success = authGroupDao.updateProperties(id, mapOf(AuthGroup::name.name to newGroupName))
        assert(success)

        // 同步缓存
        cacheHandler.syncOnUpdate(id)

        // 验证缓存中的记录
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), id) as AuthGroupCacheItem?
        assertNotNull(cacheItem1)
        assertEquals(newGroupName, cacheItem1.name)
        val cacheItem2 = cacheHandler.getGroupById(id)
        assertNotNull(cacheItem2)
        assertEquals(newGroupName, cacheItem2.name)
    }

    @Test
    fun syncOnDelete() {
        // 删除数据库中的记录
        val id = insertNewRecordToDb()
        val deleteSuccess = authGroupDao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(id)

        // 验证缓存中有没有
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), id)
        assertNull(cacheItem1)
        val cacheItem2 = cacheHandler.getGroupById(id)
        assertNull(cacheItem2)
    }

    @Test
    fun syncOnBatchDelete() {
        // 批量删除数据库中的记录
        val id1 = insertNewRecordToDb()
        val id2 = insertNewRecordToDb()
        val ids = listOf(id1, id2)
        val count = authGroupDao.batchDelete(ids)
        assert(count == 2)

        // 同步缓存
        cacheHandler.syncOnBatchDelete(ids)

        // 验证缓存中有没有
        var cacheItem = CacheKit.getValue(cacheHandler.cacheName(), id1)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getGroupById(id1)
        assertNull(cacheItem)
        cacheItem = CacheKit.getValue(cacheHandler.cacheName(), id2)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getGroupById(id2)
        assertNull(cacheItem)
    }

    private fun insertNewRecordToDb(): String {
        val timestamp = System.currentTimeMillis()
        val authGroup = AuthGroup.Companion().apply {
            code = "TEST_GROUP_${timestamp}"
            name = "测试用户组_${timestamp}"
            tenantId = "bd9f1e96-1111-1111-1111-111111111111"
            subsysCode = "default"
            active = true
            builtIn = false
        }
        return authGroupDao.insert(authGroup)
    }

}
