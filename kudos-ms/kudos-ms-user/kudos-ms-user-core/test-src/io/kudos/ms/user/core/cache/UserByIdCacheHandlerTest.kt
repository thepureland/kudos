package io.kudos.ms.user.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ms.user.common.vo.user.UserAccountCacheItem
import io.kudos.ms.user.core.dao.UserAccountDao
import io.kudos.ms.user.core.model.po.UserAccount
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


/**
 * junit test for UserByIdCacheHandler
 *
 * 测试数据来源：`UserByIdCacheHandlerTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserByIdCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: UserByIdCacheHandler

    @Resource
    private lateinit var userAccountDao: UserAccountDao

    private val newUsername = "new_test_user_${System.currentTimeMillis()}"

    @Test
    fun getUserById() {
        // 存在的
        var id = "61146119-1111-1111-1111-111111111111"
        val cacheItem2 = cacheHandler.getUserById(id)
        val cacheItem3 = cacheHandler.getUserById(id)
        assertNotNull(cacheItem2)
        assert(cacheItem3 === cacheItem2)

        // 不存在的
        id = "no_exist_id"
        assertNull(cacheHandler.getUserById(id))
    }

    @Test
    fun getUsersByIds() {
        // 都存在的
        var id1 = "61146119-1111-1111-1111-111111111111"
        var id2 = "61146119-2222-2222-2222-222222222222"
        val result2 = cacheHandler.getUsersByIds(listOf(id1, id2))
        val result3 = cacheHandler.getUsersByIds(listOf(id1, id2))
        assert(result2.isNotEmpty())
        assert(result3 == result2)

        // 部分存在的
        id1 = "no_exist_id-1"
        var cacheItems = cacheHandler.getUsersByIds(listOf(id1, id2))
        assertEquals(1, cacheItems.size)

        // 都不存在的
        id2 = "no_exist_id-2"
        cacheItems = cacheHandler.getUsersByIds(listOf(id1, id2))
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
        val cacheItem2 = cacheHandler.getUserById(id)
        assertNotNull(cacheItem2)
        val cacheItem3 = cacheHandler.getUserById(id)
        assert(cacheItem2 === cacheItem3)
    }

    @Test
    fun syncOnUpdate() {
        // 更新数据库中已存在的记录
        val id = "61146119-2222-2222-2222-222222222222"
        val success = userAccountDao.updateProperties(id, mapOf(UserAccount::username.name to newUsername))
        assert(success)

        // 同步缓存
        cacheHandler.syncOnUpdate(id)

        // 验证缓存中的记录
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), id) as UserAccountCacheItem?
        assertNotNull(cacheItem1)
        assertEquals(newUsername, cacheItem1.username)
        val cacheItem2 = cacheHandler.getUserById(id)
        assertNotNull(cacheItem2)
        assertEquals(newUsername, cacheItem2.username)
    }

    @Test
    fun syncOnDelete() {
        // 删除数据库中的记录
        val id = insertNewRecordToDb()
        val deleteSuccess = userAccountDao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(id)

        // 验证缓存中有没有
        val cacheItem1 = CacheKit.getValue(cacheHandler.cacheName(), id)
        assertNull(cacheItem1)
        val cacheItem2 = cacheHandler.getUserById(id)
        assertNull(cacheItem2)
    }

    @Test
    fun syncOnBatchDelete() {
        // 批量删除数据库中的记录
        val id1 = insertNewRecordToDb()
        val id2 = insertNewRecordToDb()
        val ids = listOf(id1, id2)
        val count = userAccountDao.batchDelete(ids)
        assert(count == 2)

        // 同步缓存
        cacheHandler.syncOnBatchDelete(ids)

        // 验证缓存中有没有
        var cacheItem = CacheKit.getValue(cacheHandler.cacheName(), id1)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getUserById(id1)
        assertNull(cacheItem)
        cacheItem = CacheKit.getValue(cacheHandler.cacheName(), id2)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getUserById(id2)
        assertNull(cacheItem)
    }

    private fun insertNewRecordToDb(): String {
        val timestamp = System.currentTimeMillis()
        val userAccount = UserAccount().apply {
            username = "u${timestamp % 1000000000}" // 确保不超过32字符
            tenantId = "tenant-001-ujdERXYn"
            loginPassword = "password"
            supervisorId = "61146119-1111-1111-1111-111111111111"
            active = true
        }
        return userAccountDao.insert(userAccount)
    }

}
