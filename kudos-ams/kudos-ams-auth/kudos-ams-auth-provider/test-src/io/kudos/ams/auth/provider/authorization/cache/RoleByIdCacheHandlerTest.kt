package io.kudos.ams.auth.provider.authorization.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ams.auth.common.vo.role.AuthRoleCacheItem
import io.kudos.ams.auth.provider.authorization.dao.AuthRoleDao
import io.kudos.ams.auth.provider.authorization.model.po.AuthRole
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


/**
 * junit test for RoleByIdCacheHandler
 *
 * 测试数据来源：`RoleByIdCacheHandlerTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class RoleByIdCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: RoleByIdCacheHandler

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    private val newRoleName = "新角色名称"

    @Test
    fun getRoleById() {
        // 存在的
        var id = "bd9f1e96-1111-1111-1111-111111111111"
        val cacheItem2 = cacheHandler.getRoleById(id)
        val cacheItem3 = cacheHandler.getRoleById(id)
        assertNotNull(cacheItem2)
        assert(cacheItem3 === cacheItem2)

        // 不存在的
        id = "no_exist_id"
        assertNull(cacheHandler.getRoleById(id))
    }

    @Test
    fun getRolesByIds() {
        // 都存在的
        var id1 = "bd9f1e96-1111-1111-1111-111111111111"
        var id2 = "bd9f1e96-2222-2222-2222-222222222222"
        val result2 = cacheHandler.getRolesByIds(listOf(id1, id2))
        val result3 = cacheHandler.getRolesByIds(listOf(id1, id2))
        assert(result2.isNotEmpty())
        assert(result3 == result2)

        // 部分存在的
        id1 = "no_exist_id-1"
        var cacheItems = cacheHandler.getRolesByIds(listOf(id1, id2))
        assertEquals(1, cacheItems.size)

        // 都不存在的
        id2 = "no_exist_id-2"
        cacheItems = cacheHandler.getRolesByIds(listOf(id1, id2))
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
        val cacheItem2 = cacheHandler.getRoleById(id)
        assertNotNull(cacheItem2)
        val cacheItem3 = cacheHandler.getRoleById(id)
        assert(cacheItem2 === cacheItem3)
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val id = "bd9f1e96-2222-2222-2222-222222222222"
        val success = authRoleDao.updateProperties(id, mapOf(AuthRole::name.name to newRoleName))
        assert(success)

        // 同步缓存
        cacheHandler.syncOnUpdate(id)

        // 验证缓存中的记录
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), id) as AuthRoleCacheItem?
        assertNotNull(cacheItem1)
        assertEquals(newRoleName, cacheItem1.name)
        val cacheItem2 = cacheHandler.getRoleById(id)
        assertNotNull(cacheItem2)
        assertEquals(newRoleName, cacheItem2.name)
    }

    @Test
    fun syncOnDelete() {
        // 删除数据库中的记录
        val id = insertNewRecordToDb()
        val deleteSuccess = authRoleDao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(id)

        // 验证缓存中有没有
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), id)
        assertNull(cacheItem1)
        val cacheItem2 = cacheHandler.getRoleById(id)
        assertNull(cacheItem2)
    }

    @Test
    fun syncOnBatchDelete() {
        // 批量删除数据库中的记录
        val id1 = insertNewRecordToDb()
        val id2 = insertNewRecordToDb()
        val ids = listOf(id1, id2)
        val count = authRoleDao.batchDelete(ids)
        assert(count == 2)

        // 同步缓存
        cacheHandler.syncOnBatchDelete(ids)

        // 验证缓存中有没有
        var cacheItem = CacheKit.getValue(cacheHandler.cacheName(), id1)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getRoleById(id1)
        assertNull(cacheItem)
        cacheItem = CacheKit.getValue(cacheHandler.cacheName(), id2)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getRoleById(id2)
        assertNull(cacheItem)
    }

    private fun insertNewRecordToDb(): String {
        val timestamp = System.currentTimeMillis()
        val authRole = AuthRole().apply {
            code = "TEST_ROLE_${timestamp}"
            name = "测试角色_${timestamp}"
            tenantId = "bd9f1e96-1111-1111-1111-111111111111"
            subsysCode = "default"
            active = true
        }
        return authRoleDao.insert(authRole)
    }

}
