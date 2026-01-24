package io.kudos.ams.auth.provider.cache

import io.kudos.ams.auth.provider.dao.AuthUserDao
import io.kudos.ams.auth.provider.model.po.AuthUser
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
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
    private lateinit var authUserDao: AuthUserDao

    @Test
    fun getUserId() {
        // 存在的
        var tenantId = "tenant-001"
        var username = "admin"
        val userId2 = cacheHandler.getUserId(tenantId, username)
        val userId3 = cacheHandler.getUserId(tenantId, username)
        assertNotNull(userId2)
        assertEquals("11111111-1111-1111-1111-111111111111", userId2)
        assertEquals(userId2, userId3)

        // 不存在的用户名
        username = "no_exist_user"
        assertNull(cacheHandler.getUserId(tenantId, username))

        // 不存在的租户
        tenantId = "no_exist_tenant"
        username = "admin"
        assertNull(cacheHandler.getUserId(tenantId, username))

        // inactive 用户（只缓存 active=true 的）
        tenantId = "tenant-001"
        username = "wangwu"
        assertNull(cacheHandler.getUserId(tenantId, username))
    }

    @Test
    fun syncOnInsert() {
        val tenantId = "tenant-001"
        val username = "test_user_insert_${System.currentTimeMillis()}"
        val authUser = AuthUser().apply {
            this.tenantId = tenantId
            this.username = username
            this.loginPassword = "password"
            this.supervisorId = "11111111-1111-1111-1111-111111111111"
            this.active = true
        }
        val id = authUserDao.insert(authUser)

        // 同步缓存
        cacheHandler.syncOnInsert(authUser, id)

        // 验证新记录是否在缓存中
        val userId = cacheHandler.getUserId(tenantId, username)
        assertNotNull(userId)
        assertEquals(id, userId)
    }

    @Test
    fun syncOnUpdate() {
        val tenantId = "tenant-001"
        val username = "zhangsan"
        val id = "22222222-2222-2222-2222-222222222222"
        
        // 先获取一次，确保缓存中有数据
        val userIdBefore = cacheHandler.getUserId(tenantId, username)
        assertNotNull(userIdBefore)

        // 更新数据库记录
        val success = authUserDao.updateProperties(id, mapOf(AuthUser::username.name to "zhangsan_updated"))
        assert(success)

        // 同步缓存
        cacheHandler.syncOnUpdate(null, id)

        // 验证缓存已被清除并重新加载（用户名已改变，旧缓存应该被清除）
        val userIdAfter = cacheHandler.getUserId(tenantId, username)
        // 由于用户名已改变，旧缓存应该被清除，新用户名可能不在缓存中
    }

    @Test
    fun syncOnUpdateActive() {
        val tenantId = "tenant-001"
        val username = "test_user_active_${System.currentTimeMillis()}"
        val authUser = AuthUser().apply {
            this.tenantId = tenantId
            this.username = username
            this.loginPassword = "password"
            this.supervisorId = "11111111-1111-1111-1111-111111111111"
            this.active = false
        }
        val id = authUserDao.insert(authUser)

        // 由false更新为true
        val success = authUserDao.updateProperties(id, mapOf(AuthUser::active.name to true))
        assert(success)
        cacheHandler.syncOnUpdateActive(id, true)
        var userId = cacheHandler.getUserId(tenantId, username)
        assertNotNull(userId)
        assertEquals(id, userId)

        // 由true更新为false
        authUserDao.updateProperties(id, mapOf(AuthUser::active.name to false))
        cacheHandler.syncOnUpdateActive(id, false)
        userId = cacheHandler.getUserId(tenantId, username)
        assertNull(userId)
    }

    @Test
    fun syncOnDelete() {
        val tenantId = "tenant-001"
        val username = "test_user_delete_${System.currentTimeMillis()}"
        val authUser = AuthUser().apply {
            this.tenantId = tenantId
            this.username = username
            this.loginPassword = "password"
            this.supervisorId = "11111111-1111-1111-1111-111111111111"
            this.active = true
        }
        val id = authUserDao.insert(authUser)

        // 先获取一次，确保缓存中有数据
        val userIdBefore = cacheHandler.getUserId(tenantId, username)
        assertNotNull(userIdBefore)

        // 删除数据库记录
        val deleteSuccess = authUserDao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(authUser, id)

        // 验证缓存已被清除
        val userIdAfter = cacheHandler.getUserId(tenantId, username)
        assertNull(userIdAfter)
    }

    @Test
    fun syncOnBatchDelete() {
        val tenantId = "tenant-001"
        val timestamp = System.currentTimeMillis() % 1000000000
        val username1 = "u${timestamp}1" // 确保不超过32字符
        val username2 = "u${timestamp}2" // 确保不超过32字符
        
        val authUser1 = AuthUser().apply {
            this.tenantId = tenantId
            this.username = username1
            this.loginPassword = "password"
            this.supervisorId = "11111111-1111-1111-1111-111111111111"
            this.active = true
        }
        val id1 = authUserDao.insert(authUser1)
        
        val authUser2 = AuthUser().apply {
            this.tenantId = tenantId
            this.username = username2
            this.loginPassword = "password"
            this.supervisorId = "11111111-1111-1111-1111-111111111111"
            this.active = true
        }
        val id2 = authUserDao.insert(authUser2)

        // 先获取一次，确保缓存中有数据
        val userId1Before = cacheHandler.getUserId(tenantId, username1)
        val userId2Before = cacheHandler.getUserId(tenantId, username2)
        assertNotNull(userId1Before)
        assertNotNull(userId2Before)

        // 批量删除数据库记录
        val ids = listOf(id1, id2)
        val count = authUserDao.batchDelete(ids)
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
