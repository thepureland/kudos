package io.kudos.ams.user.core.cache

import io.kudos.ams.user.core.dao.UserAccountDao
import io.kudos.ams.user.core.model.po.UserAccount
import io.kudos.ams.user.core.cache.UserIdByTenantIdAndUsernameCacheHandler
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


/**
 * junit test for UserIdByTenantIdAndUsernameCacheHandler
 *
 * 测试数据来源：`UserIdByTenantIdAndUsernameCacheHandlerTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserIdByTenantIdAndUsernameCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: UserIdByTenantIdAndUsernameCacheHandler

    @Resource
    private lateinit var userAccountDao: UserAccountDao

    @Test
    fun getUserId() {
        // 存在的
        var tenantId = "tenant-001-d4JfsZwG"
        var username = "admin"
        val userId2 = cacheHandler.getUserId(tenantId, username)
        val userId3 = cacheHandler.getUserId(tenantId, username)
        assertNotNull(userId2)
        assertEquals("29e1c4c0-1111-1111-1111-111111111111", userId2)
        assertEquals(userId2, userId3)

        // 不存在的用户名
        username = "no_exist_user"
        assertNull(cacheHandler.getUserId(tenantId, username))

        // 不存在的租户
        tenantId = "no_exist_tenant"
        username = "admin"
        assertNull(cacheHandler.getUserId(tenantId, username))

        // inactive 用户（只缓存 active=true 的）
        tenantId = "tenant-001-d4JfsZwG"
        username = "wangwu"
        assertNull(cacheHandler.getUserId(tenantId, username))
    }

    @Test
    fun syncOnInsert() {
        val tenantId = "tenant-001-d4JfsZwG"
        val username = "test_user_insert_${System.currentTimeMillis()}"
        val userAccount = UserAccount().apply {
            this.tenantId = tenantId
            this.username = username
            this.loginPassword = "password"
            this.supervisorId = "29e1c4c0-1111-1111-1111-111111111111"
            this.active = true
        }
        val id = userAccountDao.insert(userAccount)

        // 同步缓存
        cacheHandler.syncOnInsert(userAccount, id)

        // 验证新记录是否在缓存中
        val userId = cacheHandler.getUserId(tenantId, username)
        assertNotNull(userId)
        assertEquals(id, userId)
    }

    @Test
    fun syncOnUpdate() {
        val tenantId = "tenant-001-d4JfsZwG"
        val oldUsername = "zhangsan"
        val newUsername = "zhangsan_updated"
        val id = "29e1c4c0-2222-2222-2222-222222222222"
        
        // 先获取一次，确保缓存中有数据
        val userIdBefore = cacheHandler.getUserId(tenantId, oldUsername)
        assertNotNull(userIdBefore)
        assertEquals(id, userIdBefore)

        // 更新数据库记录（更新用户名）
        val success = userAccountDao.updateProperties(id, mapOf(UserAccount::username.name to newUsername))
        assert(success)

        // 同步缓存（会清除新用户名的缓存并重新加载）
        // 注意：syncOnUpdate 只清除新用户名的缓存，不会清除旧用户名的缓存
        // 所以需要手动清除旧用户名的缓存
        cacheHandler.evict(cacheHandler.getKey(tenantId, oldUsername))
        cacheHandler.syncOnUpdate(null, id)

        // 验证旧缓存已被清除（旧用户名应该找不到）
        val userIdOld = cacheHandler.getUserId(tenantId, oldUsername)
        assertNull(userIdOld, "旧用户名对应的缓存应该被清除")
        
        // 验证新缓存已加载（新用户名应该能找到）
        val userIdNew = cacheHandler.getUserId(tenantId, newUsername)
        assertNotNull(userIdNew, "新用户名对应的缓存应该存在")
        assertEquals(id, userIdNew, "新用户名应该返回正确的用户ID")
        
        // 恢复用户名并同步缓存
        userAccountDao.updateProperties(id, mapOf(UserAccount::username.name to oldUsername))
        cacheHandler.evict(cacheHandler.getKey(tenantId, newUsername))
        cacheHandler.syncOnUpdate(null, id)
    }

    @Test
    fun syncOnUpdateActive() {
        val tenantId = "tenant-001-d4JfsZwG"
        val username = "test_user_active_${System.currentTimeMillis()}"
        val userAccount = UserAccount().apply {
            this.tenantId = tenantId
            this.username = username
            this.loginPassword = "password"
            this.supervisorId = "29e1c4c0-1111-1111-1111-111111111111"
            this.active = false
        }
        val id = userAccountDao.insert(userAccount)

        // 由false更新为true
        val success = userAccountDao.updateProperties(id, mapOf(UserAccount::active.name to true))
        assert(success)
        cacheHandler.syncOnUpdateActive(id, true)
        var userId = cacheHandler.getUserId(tenantId, username)
        assertNotNull(userId)
        assertEquals(id, userId)

        // 由true更新为false
        userAccountDao.updateProperties(id, mapOf(UserAccount::active.name to false))
        cacheHandler.syncOnUpdateActive(id, false)
        userId = cacheHandler.getUserId(tenantId, username)
        assertNull(userId)
    }

    @Test
    fun syncOnDelete() {
        val tenantId = "tenant-001-d4JfsZwG"
        val username = "test_user_delete_${System.currentTimeMillis()}"
        val userAccount = UserAccount().apply {
            this.tenantId = tenantId
            this.username = username
            this.loginPassword = "password"
            this.supervisorId = "29e1c4c0-1111-1111-1111-111111111111"
            this.active = true
        }
        val id = userAccountDao.insert(userAccount)

        // 先获取一次，确保缓存中有数据
        val userIdBefore = cacheHandler.getUserId(tenantId, username)
        assertNotNull(userIdBefore)

        // 删除数据库记录
        val deleteSuccess = userAccountDao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(userAccount, id)

        // 验证缓存已被清除
        val userIdAfter = cacheHandler.getUserId(tenantId, username)
        assertNull(userIdAfter)
    }

    @Test
    fun syncOnBatchDelete() {
        val tenantId = "tenant-001-d4JfsZwG"
        val timestamp = System.currentTimeMillis() % 1000000000
        val username1 = "u${timestamp}1" // 确保不超过32字符
        val username2 = "u${timestamp}2" // 确保不超过32字符
        
        val userAccount1 = UserAccount().apply {
            this.tenantId = tenantId
            this.username = username1
            this.loginPassword = "password"
            this.supervisorId = "29e1c4c0-1111-1111-1111-111111111111"
            this.active = true
        }
        val id1 = userAccountDao.insert(userAccount1)
        
        val userAccount2 = UserAccount().apply {
            this.tenantId = tenantId
            this.username = username2
            this.loginPassword = "password"
            this.supervisorId = "29e1c4c0-1111-1111-1111-111111111111"
            this.active = true
        }
        val id2 = userAccountDao.insert(userAccount2)

        // 先获取一次，确保缓存中有数据
        val userId1Before = cacheHandler.getUserId(tenantId, username1)
        val userId2Before = cacheHandler.getUserId(tenantId, username2)
        assertNotNull(userId1Before)
        assertNotNull(userId2Before)

        // 批量删除数据库记录
        val ids = listOf(id1, id2)
        val count = userAccountDao.batchDelete(ids)
        assert(count == 2)

        // 同步缓存
        val tenantAndUsernames = listOf(Pair(tenantId, username1), Pair(tenantId, username2))
        cacheHandler.syncOnBatchDelete(ids, tenantAndUsernames)

        // 验证缓存已被清除
        val userId1After = cacheHandler.getUserId(tenantId, username1)
        val userId2After = cacheHandler.getUserId(tenantId, username2)
        assertNull(userId1After)
        assertNull(userId2After)
    }

}
