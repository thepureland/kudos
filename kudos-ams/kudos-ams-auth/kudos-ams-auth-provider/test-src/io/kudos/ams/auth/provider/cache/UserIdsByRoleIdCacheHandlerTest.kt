package io.kudos.ams.auth.provider.cache

import io.kudos.test.container.cache.CacheHandlerTestBase
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for UserIdsByRoleIdCacheHandler
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserIdsByRoleIdCacheHandlerTest : CacheHandlerTestBase() {

    @Resource
    private lateinit var cacheHandler: UserIdsByRoleIdCacheHandler

    @Test
    fun getUserIds() {
        // 存在的角色ID，有多个用户
        var roleId = "11111111-1111-1111-1111-111111111111"
        val userIds1 = cacheHandler.getUserIds(roleId)
        val userIds2 = cacheHandler.getUserIds(roleId)
        assertTrue(userIds1.isNotEmpty(), "角色${roleId}应该有用户ID列表")
        assertEquals(userIds1, userIds2, "两次调用应该返回相同的结果（缓存验证）")
        // 验证用户ID：角色ROLE_ADMIN有用户admin和zhangsan
        assertEquals(2, userIds1.size, "角色${roleId}应该有2个用户ID")
        assertTrue(userIds1.contains("11111111-1111-1111-1111-111111111111"), "应该包含admin的用户ID，实际返回：${userIds1}")
        assertTrue(userIds1.contains("22222222-2222-2222-2222-222222222222"), "应该包含zhangsan的用户ID，实际返回：${userIds1}")

        // 存在的角色ID，有一个用户
        roleId = "22222222-2222-2222-2222-222222222222"
        val userIds3 = cacheHandler.getUserIds(roleId)
        val userIds4 = cacheHandler.getUserIds(roleId)
        assertTrue(userIds3.isNotEmpty(), "角色${roleId}应该有用户ID列表")
        assertEquals(userIds3, userIds4, "两次调用应该返回相同的结果（缓存验证）")
        // 角色ROLE_USER只有用户zhangsan
        assertEquals(1, userIds3.size, "角色${roleId}应该有1个用户ID，实际返回：${userIds3}")
        assertTrue(userIds3.contains("22222222-2222-2222-2222-222222222222"), "应该包含zhangsan的用户ID")

        // 存在的角色ID，但没有用户
        roleId = "33333333-3333-3333-3333-333333333333"
        val userIds5 = cacheHandler.getUserIds(roleId)
        assertTrue(userIds5.isEmpty(), "角色${roleId}没有用户，应该返回空列表")

        // 不存在的角色ID
        roleId = "no_exist_role_id"
        val userIds6 = cacheHandler.getUserIds(roleId)
        assertTrue(userIds6.isEmpty(), "不存在的角色ID应该返回空列表")
    }

    @Test
    fun syncOnRoleUserChange() {
        val roleId = "11111111-1111-1111-1111-111111111111"
        
        // 先获取一次，确保缓存中有数据
        val userIdsBefore = cacheHandler.getUserIds(roleId)
        assertTrue(userIdsBefore.isNotEmpty(), "角色${roleId}应该有用户ID列表")
        
        // 同步缓存（模拟角色-用户关系变更）
        cacheHandler.syncOnRoleUserChange(roleId)
        
        // 验证缓存已被清除并重新加载
        val userIdsAfter = cacheHandler.getUserIds(roleId)
        assertTrue(userIdsAfter.isNotEmpty(), "同步后应该能重新获取到用户ID列表")
    }

    @Test
    fun syncOnBatchRoleUserChange() {
        val roleId1 = "11111111-1111-1111-1111-111111111111"
        val roleId2 = "22222222-2222-2222-2222-222222222222"
        val roleIds = listOf(roleId1, roleId2)
        
        // 先获取一次，确保缓存中有数据
        val userIds1Before = cacheHandler.getUserIds(roleId1)
        val userIds2Before = cacheHandler.getUserIds(roleId2)
        assertTrue(userIds1Before.isNotEmpty() || userIds2Before.isNotEmpty(), "至少一个角色应该有用户ID列表")
        
        // 批量同步缓存（模拟批量角色-用户关系变更）
        cacheHandler.syncOnBatchRoleUserChange(roleIds)
        
        // 验证缓存已被清除并重新加载
        val userIds1After = cacheHandler.getUserIds(roleId1)
        val userIds2After = cacheHandler.getUserIds(roleId2)
        assertTrue(userIds1After.isNotEmpty() || userIds2After.isNotEmpty(), "同步后应该能重新获取到用户ID列表")
    }

    @Test
    fun syncOnRoleDelete() {
        val roleId = "33333333-3333-3333-3333-333333333333"
        
        // 先获取一次，确保缓存中有数据（即使为空列表）
        val userIdsBefore = cacheHandler.getUserIds(roleId)
        
        // 同步缓存（模拟角色删除）
        cacheHandler.syncOnRoleDelete(roleId)
        
        // 验证缓存已被清除
        val userIdsAfter = cacheHandler.getUserIds(roleId)
        assertTrue(userIdsAfter.isEmpty(), "删除角色后，缓存应该被清除，重新获取应该返回空列表")
    }

}
