package io.kudos.ams.auth.provider.authorization.cache

import io.kudos.ams.auth.provider.authorization.dao.AuthRoleUserDao
import io.kudos.ams.auth.provider.authorization.model.po.AuthRoleUser
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for RoleIdsByUserIdCacheHandler
 *
 * 测试数据来源：`RoleIdsByUserIdCacheHandlerTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class RoleIdsByUserIdCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: RoleIdsByUserIdCacheHandler

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Test
    fun getRoleIds() {
        // 存在的用户ID，有一个角色
        var userId = "88207878-1111-1111-1111-111111111111"
        val roleIds1 = cacheHandler.getRoleIds(userId)
        val roleIds2 = cacheHandler.getRoleIds(userId)
        assertTrue(roleIds1.isNotEmpty(), "用户${userId}应该有角色ID列表")
        assertEquals(roleIds1, roleIds2, "两次调用应该返回相同的结果（缓存验证）")
        // 验证角色ID：用户11111111有角色ROLE_ADMIN
        assertEquals(1, roleIds1.size, "用户${userId}应该有1个角色ID")
        assertTrue(roleIds1.contains("88207878-1111-1111-1111-111111111111"), "应该包含ROLE_ADMIN的角色ID，实际返回：${roleIds1}")

        // 存在的用户ID，有多个角色
        userId = "88207878-2222-2222-2222-222222222222"
        val roleIds3 = cacheHandler.getRoleIds(userId)
        val roleIds4 = cacheHandler.getRoleIds(userId)
        assertTrue(roleIds3.isNotEmpty(), "用户${userId}应该有角色ID列表")
        assertEquals(roleIds3, roleIds4, "两次调用应该返回相同的结果（缓存验证）")
        // 用户22222222有两个角色（ROLE_USER和ROLE_ADMIN）
        assertEquals(2, roleIds3.size, "用户${userId}应该有2个角色ID，实际返回：${roleIds3}")
        assertTrue(roleIds3.contains("88207878-1111-1111-1111-111111111111"), "应该包含ROLE_ADMIN的角色ID")
        assertTrue(roleIds3.contains("88207878-2222-2222-2222-222222222222"), "应该包含ROLE_USER的角色ID")

        // 存在的用户ID，但没有角色
        userId = "88207878-3333-3333-3333-333333333333"
        val roleIds5 = cacheHandler.getRoleIds(userId)
        assertTrue(roleIds5.isEmpty(), "用户${userId}没有角色，应该返回空列表")

        // 不存在的用户ID
        userId = "no_exist_user_id"
        val roleIds6 = cacheHandler.getRoleIds(userId)
        assertTrue(roleIds6.isEmpty(), "不存在的用户ID应该返回空列表")
    }

    @Test
    fun syncOnRoleUserChange() {
        val userId = "88207878-3333-3333-3333-333333333333"
        val roleId = "88207878-2222-2222-2222-222222222222" // ROLE_USER 的 ID
        
        // 先获取一次，记录初始角色数量
        val roleIdsBefore = cacheHandler.getRoleIds(userId)
        val beforeSize = roleIdsBefore.size
        
        // 插入一条新的用户-角色关系记录
        val authRoleUser = AuthRoleUser().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)
        
        // 同步缓存（模拟用户-角色关系变更）
        cacheHandler.syncOnRoleUserChange(userId)
        
        // 验证缓存已被清除并重新加载，应该包含新插入的角色
        val roleIdsAfter = cacheHandler.getRoleIds(userId)
        assertTrue(roleIdsAfter.size > beforeSize, "同步后应该包含新插入的角色ID")
        assertTrue(roleIdsAfter.contains(roleId), "应该包含新插入的角色ID")
        
        // 清理测试数据
        authRoleUserDao.deleteById(id)
    }

    @Test
    fun syncOnBatchRoleUserChange() {
        val userId1 = "88207878-3333-3333-3333-333333333333"
        val userId2 = "88207878-3333-3333-3333-333333333333"
        val roleId1 = "88207878-1111-1111-1111-111111111111" // ROLE_ADMIN 的 ID
        val roleId2 = "88207878-2222-2222-2222-222222222222" // ROLE_USER 的 ID
        val userIds = listOf(userId1, userId2)
        
        // 先获取一次，记录初始角色数量
        val roleIds1Before = cacheHandler.getRoleIds(userId1)
        val beforeSize = roleIds1Before.size
        
        // 批量插入用户-角色关系记录
        val authRoleUser1 = AuthRoleUser().apply {
            this.roleId = roleId1
            this.userId = userId1
        }
        val id1 = authRoleUserDao.insert(authRoleUser1)
        
        val authRoleUser2 = AuthRoleUser().apply {
            this.roleId = roleId2
            this.userId = userId2
        }
        val id2 = authRoleUserDao.insert(authRoleUser2)
        
        // 批量同步缓存（模拟批量用户-角色关系变更）
        cacheHandler.syncOnBatchRoleUserChange(userIds)
        
        // 验证缓存已被清除并重新加载，应该包含新插入的角色
        val roleIds1After = cacheHandler.getRoleIds(userId1)
        assertTrue(roleIds1After.size > beforeSize, "同步后应该包含新插入的角色ID")
        
        // 清理测试数据
        authRoleUserDao.deleteById(id1)
        authRoleUserDao.deleteById(id2)
    }

    @Test
    fun syncOnUserDelete() {
        val userId = "88207878-3333-3333-3333-333333333333"
        val roleId = "88207878-1111-1111-1111-111111111111" // ROLE_ADMIN 的 ID
        
        // 先插入一条用户-角色关系记录
        val authRoleUser = AuthRoleUser().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)
        
        // 先同步缓存，确保缓存中有新插入的数据
        cacheHandler.syncOnRoleUserChange(userId)
        
        // 获取一次，确保缓存中有数据
        val roleIdsBefore = cacheHandler.getRoleIds(userId)
        assertTrue(roleIdsBefore.contains(roleId), "新插入的角色关系应该在缓存中")
        
        // 删除数据库记录（模拟用户删除或用户-角色关系删除）
        val deleteSuccess = authRoleUserDao.deleteById(id)
        assertTrue(deleteSuccess, "删除应该成功")
        
        // 同步缓存（模拟用户删除）
        cacheHandler.syncOnUserDelete(userId)
        
        // 验证缓存已被清除，重新获取应该不包含已删除的角色
        val roleIdsAfter = cacheHandler.getRoleIds(userId)
        assertTrue(!roleIdsAfter.contains(roleId), "删除后，缓存应该被清除，不应该包含已删除的角色ID")
    }

}
