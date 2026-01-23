package io.kudos.ams.auth.provider.cache

import io.kudos.test.container.cache.CacheHandlerTestBase
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for RoleIdsByUserIdCacheHandler
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class RoleIdsByUserIdCacheHandlerTest : CacheHandlerTestBase() {

    @Resource
    private lateinit var cacheHandler: RoleIdsByUserIdCacheHandler

    @Test
    fun getRoleIds() {
        // 存在的用户ID，有一个角色
        var userId = "11111111-1111-1111-1111-111111111111"
        val roleIds1 = cacheHandler.getRoleIds(userId)
        val roleIds2 = cacheHandler.getRoleIds(userId)
        assertTrue(roleIds1.isNotEmpty(), "用户${userId}应该有角色ID列表")
        assertEquals(roleIds1, roleIds2, "两次调用应该返回相同的结果（缓存验证）")
        // 验证角色ID：用户11111111有角色ROLE_ADMIN
        assertEquals(1, roleIds1.size, "用户${userId}应该有1个角色ID")
        assertTrue(roleIds1.contains("11111111-1111-1111-1111-111111111111"), "应该包含ROLE_ADMIN的角色ID，实际返回：${roleIds1}")

        // 存在的用户ID，有多个角色
        userId = "22222222-2222-2222-2222-222222222222"
        val roleIds3 = cacheHandler.getRoleIds(userId)
        val roleIds4 = cacheHandler.getRoleIds(userId)
        assertTrue(roleIds3.isNotEmpty(), "用户${userId}应该有角色ID列表")
        assertEquals(roleIds3, roleIds4, "两次调用应该返回相同的结果（缓存验证）")
        // 用户22222222有两个角色（ROLE_USER和ROLE_ADMIN）
        assertEquals(2, roleIds3.size, "用户${userId}应该有2个角色ID，实际返回：${roleIds3}")
        assertTrue(roleIds3.contains("11111111-1111-1111-1111-111111111111"), "应该包含ROLE_ADMIN的角色ID")
        assertTrue(roleIds3.contains("22222222-2222-2222-2222-222222222222"), "应该包含ROLE_USER的角色ID")

        // 存在的用户ID，但没有角色
        userId = "33333333-3333-3333-3333-333333333333"
        val roleIds5 = cacheHandler.getRoleIds(userId)
        assertTrue(roleIds5.isEmpty(), "用户${userId}没有角色，应该返回空列表")

        // 不存在的用户ID
        userId = "no_exist_user_id"
        val roleIds6 = cacheHandler.getRoleIds(userId)
        assertTrue(roleIds6.isEmpty(), "不存在的用户ID应该返回空列表")
    }

    @Test
    fun syncOnRoleUserChange() {
        val userId = "11111111-1111-1111-1111-111111111111"
        
        // 先获取一次，确保缓存中有数据
        val roleIdsBefore = cacheHandler.getRoleIds(userId)
        assertTrue(roleIdsBefore.isNotEmpty(), "用户${userId}应该有角色ID列表")
        
        // 同步缓存（模拟用户-角色关系变更）
        cacheHandler.syncOnRoleUserChange(userId)
        
        // 验证缓存已被清除并重新加载
        val roleIdsAfter = cacheHandler.getRoleIds(userId)
        assertTrue(roleIdsAfter.isNotEmpty(), "同步后应该能重新获取到角色ID列表")
    }

    @Test
    fun syncOnBatchRoleUserChange() {
        val userId1 = "11111111-1111-1111-1111-111111111111"
        val userId2 = "22222222-2222-2222-2222-222222222222"
        val userIds = listOf(userId1, userId2)
        
        // 先获取一次，确保缓存中有数据
        val roleIds1Before = cacheHandler.getRoleIds(userId1)
        val roleIds2Before = cacheHandler.getRoleIds(userId2)
        assertTrue(roleIds1Before.isNotEmpty() || roleIds2Before.isNotEmpty(), "至少一个用户应该有角色ID列表")
        
        // 批量同步缓存（模拟批量用户-角色关系变更）
        cacheHandler.syncOnBatchRoleUserChange(userIds)
        
        // 验证缓存已被清除并重新加载
        val roleIds1After = cacheHandler.getRoleIds(userId1)
        val roleIds2After = cacheHandler.getRoleIds(userId2)
        assertTrue(roleIds1After.isNotEmpty() || roleIds2After.isNotEmpty(), "同步后应该能重新获取到角色ID列表")
    }

    @Test
    fun syncOnUserDelete() {
        val userId = "33333333-3333-3333-3333-333333333333"
        
        // 先获取一次，确保缓存中有数据（即使为空列表）
        val roleIdsBefore = cacheHandler.getRoleIds(userId)
        
        // 同步缓存（模拟用户删除）
        cacheHandler.syncOnUserDelete(userId)
        
        // 验证缓存已被清除
        val roleIdsAfter = cacheHandler.getRoleIds(userId)
        assertTrue(roleIdsAfter.isEmpty(), "删除用户后，缓存应该被清除，重新获取应该返回空列表")
    }

}
