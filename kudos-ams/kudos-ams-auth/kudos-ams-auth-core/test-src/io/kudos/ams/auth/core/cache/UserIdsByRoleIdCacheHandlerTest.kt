package io.kudos.ams.auth.core.cache

import io.kudos.ams.auth.core.dao.AuthRoleUserDao
import io.kudos.ams.auth.core.model.po.AuthRoleUser
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for UserIdsByRoleIdCacheHandler
 *
 * 测试数据来源：`UserIdsByRoleIdCacheHandlerTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserIdsByRoleIdCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: UserIdsByRoleIdCacheHandler

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Test
    fun getUserIds() {
        // 存在的角色ID，有多个用户
        var roleId = "5e90ce80-1111-1111-1111-111111111111"
        val userIds1 = cacheHandler.getUserIds(roleId)
        val userIds2 = cacheHandler.getUserIds(roleId)
        assertTrue(userIds1.isNotEmpty(), "角色${roleId}应该有用户ID列表")
        assertEquals(userIds1, userIds2, "两次调用应该返回相同的结果（缓存验证）")
        // 验证用户ID：角色ROLE_ADMIN有用户admin和zhangsan
        assertEquals(2, userIds1.size, "角色${roleId}应该有2个用户ID")
        assertTrue(userIds1.contains("5e90ce80-1111-1111-1111-111111111111"), "应该包含admin的用户ID，实际返回：${userIds1}")
        assertTrue(userIds1.contains("5e90ce80-2222-2222-2222-222222222222"), "应该包含zhangsan的用户ID，实际返回：${userIds1}")

        // 存在的角色ID，有一个用户
        roleId = "5e90ce80-2222-2222-2222-222222222222"
        val userIds3 = cacheHandler.getUserIds(roleId)
        val userIds4 = cacheHandler.getUserIds(roleId)
        assertTrue(userIds3.isNotEmpty(), "角色${roleId}应该有用户ID列表")
        assertEquals(userIds3, userIds4, "两次调用应该返回相同的结果（缓存验证）")
        // 角色ROLE_USER只有用户zhangsan
        assertEquals(
            1,
            userIds3.size,
            "角色${roleId}应该有1个用户ID，实际返回：${userIds3}"
        )
        assertTrue(userIds3.contains("5e90ce80-2222-2222-2222-222222222222"), "应该包含zhangsan的用户ID")

        // 存在的角色ID，但没有用户
        roleId = "5e90ce80-3333-3333-3333-333333333333"
        val userIds5 = cacheHandler.getUserIds(roleId)
        assertTrue(userIds5.isEmpty(), "角色${roleId}没有用户，应该返回空列表")

        // 不存在的角色ID
        roleId = "no_exist_role_id"
        val userIds6 = cacheHandler.getUserIds(roleId)
        assertTrue(userIds6.isEmpty(), "不存在的角色ID应该返回空列表")
    }

    @Test
    fun syncOnRoleUserChange() {
        val roleId = "5e90ce80-3333-3333-3333-333333333333"
        val userId = "5e90ce80-3333-3333-3333-333333333333"
        
        // 先获取一次，记录初始用户数量
        val userIdsBefore = cacheHandler.getUserIds(roleId)
        val beforeSize = userIdsBefore.size
        
        // 插入一条新的角色-用户关系记录
        val authRoleUser = AuthRoleUser.Companion().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)
        
        // 同步缓存（模拟角色-用户关系变更）
        cacheHandler.syncOnRoleUserChange(roleId)
        
        // 验证缓存已被清除并重新加载，应该包含新插入的用户
        val userIdsAfter = cacheHandler.getUserIds(roleId)
        assertTrue(userIdsAfter.size > beforeSize, "同步后应该包含新插入的用户ID")
        assertTrue(userIdsAfter.contains(userId), "应该包含新插入的用户ID")
        
        // 清理测试数据
        authRoleUserDao.deleteById(id)
    }

    @Test
    fun syncOnBatchRoleUserChange() {
        val roleId1 = "5e90ce80-3333-3333-3333-333333333333"
        val roleId2 = "5e90ce80-3333-3333-3333-333333333333"
        val userId1 = "5e90ce80-1111-1111-1111-111111111111"
        val userId2 = "5e90ce80-2222-2222-2222-222222222222"
        val roleIds = listOf(roleId1, roleId2)
        
        // 先获取一次，记录初始用户数量
        val userIds1Before = cacheHandler.getUserIds(roleId1)
        val beforeSize = userIds1Before.size
        
        // 批量插入角色-用户关系记录
        val authRoleUser1 = AuthRoleUser.Companion().apply {
            this.roleId = roleId1
            this.userId = userId1
        }
        val id1 = authRoleUserDao.insert(authRoleUser1)
        
        val authRoleUser2 = AuthRoleUser.Companion().apply {
            this.roleId = roleId2
            this.userId = userId2
        }
        val id2 = authRoleUserDao.insert(authRoleUser2)
        
        // 批量同步缓存（模拟批量角色-用户关系变更）
        cacheHandler.syncOnBatchRoleUserChange(roleIds)
        
        // 验证缓存已被清除并重新加载，应该包含新插入的用户
        val userIds1After = cacheHandler.getUserIds(roleId1)
        assertTrue(userIds1After.size > beforeSize, "同步后应该包含新插入的用户ID")
        
        // 清理测试数据
        authRoleUserDao.deleteById(id1)
        authRoleUserDao.deleteById(id2)
    }

    @Test
    fun syncOnRoleDelete() {
        val roleId = "5e90ce80-3333-3333-3333-333333333333"
        val userId = "5e90ce80-1111-1111-1111-111111111111"
        
        // 先插入一条角色-用户关系记录
        val authRoleUser = AuthRoleUser.Companion().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)
        
        // 先同步缓存，确保缓存中有新插入的数据
        cacheHandler.syncOnRoleUserChange(roleId)
        
        // 获取一次，确保缓存中有数据
        val userIdsBefore = cacheHandler.getUserIds(roleId)
        assertTrue(userIdsBefore.contains(userId), "新插入的用户关系应该在缓存中")
        
        // 删除数据库记录（模拟角色删除或角色-用户关系删除）
        val deleteSuccess = authRoleUserDao.deleteById(id)
        assertTrue(deleteSuccess, "删除应该成功")
        
        // 同步缓存（模拟角色删除）
        cacheHandler.syncOnRoleDelete(roleId)
        
        // 验证缓存已被清除，重新获取应该不包含已删除的用户
        val userIdsAfter = cacheHandler.getUserIds(roleId)
        assertTrue(!userIdsAfter.contains(userId), "删除后，缓存应该被清除，不应该包含已删除的用户ID")
    }

}
