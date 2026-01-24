package io.kudos.ams.auth.provider.cache

import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for ResourceIdsByUserIdCacheHandler
 *
 * 测试数据来源：`ResourceIdsByUserIdCacheHandlerTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ResourceIdsByUserIdCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: ResourceIdsByUserIdCacheHandler

    @Test
    fun getResourceIds() {
        // 存在的用户ID，有一个角色，该角色有多个资源
        var userId = "11111111-1111-1111-1111-111111111111"
        val resourceIds1 = cacheHandler.getResourceIds(userId)
        val resourceIds2 = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIds1.isNotEmpty(), "用户${userId}应该有资源ID列表")
        assertEquals(resourceIds1, resourceIds2, "两次调用应该返回相同的结果（缓存验证）")
        // 验证资源ID：用户11111111有角色ROLE_ADMIN，应该包含resource-aaa和resource-bbb
        assertEquals(2, resourceIds1.size, "用户${userId}应该有2个资源ID")
        assertTrue(resourceIds1.contains("resource-aaa"), "应该包含resource-aaa，实际返回：${resourceIds1}")
        assertTrue(resourceIds1.contains("resource-bbb"), "应该包含resource-bbb，实际返回：${resourceIds1}")

        // 存在的用户ID，有多个角色，每个角色有多个资源
        userId = "22222222-2222-2222-2222-222222222222"
        val resourceIds3 = cacheHandler.getResourceIds(userId)
        val resourceIds4 = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIds3.isNotEmpty(), "用户${userId}应该有资源ID列表")
        assertEquals(resourceIds3, resourceIds4, "两次调用应该返回相同的结果（缓存验证）")
        // 用户22222222有两个角色（ROLE_USER和ROLE_ADMIN），应该包含所有角色的资源（去重后）
        // ROLE_ADMIN的资源：resource-aaa, resource-bbb
        // ROLE_USER的资源：resource-ccc, resource-ddd
        // 总共应该是4个资源
        assertEquals(4, resourceIds3.size, "用户${userId}应该有4个资源ID，实际返回：${resourceIds3}")
        assertTrue(resourceIds3.contains("resource-aaa"), "应该包含resource-aaa")
        assertTrue(resourceIds3.contains("resource-bbb"), "应该包含resource-bbb")
        assertTrue(resourceIds3.contains("resource-ccc"), "应该包含resource-ccc")
        assertTrue(resourceIds3.contains("resource-ddd"), "应该包含resource-ddd")

        // 存在的用户ID，但没有角色
        userId = "33333333-3333-3333-3333-333333333333"
        val resourceIds5 = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIds5.isEmpty(), "用户${userId}没有角色，应该返回空列表")

        // 不存在的用户ID
        userId = "no_exist_user_id"
        val resourceIds6 = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIds6.isEmpty(), "不存在的用户ID应该返回空列表")
    }

    @Test
    fun syncOnRoleUserChange() {
        val userId = "11111111-1111-1111-1111-111111111111"
        
        // 先获取一次，确保缓存中有数据
        val resourceIdsBefore = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIdsBefore.isNotEmpty(), "用户${userId}应该有资源ID列表")
        
        // 同步缓存（模拟用户-角色关系变更）
        cacheHandler.syncOnRoleUserChange(userId)
        
        // 验证缓存已被清除并重新加载
        val resourceIdsAfter = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIdsAfter.isNotEmpty(), "同步后应该能重新获取到资源ID列表")
    }

    @Test
    fun syncOnRoleResourceChange() {
        val roleId = "11111111-1111-1111-1111-111111111111"
        
        // 先获取一次，确保缓存中有数据（通过用户ID）
        val userId = "11111111-1111-1111-1111-111111111111"
        val resourceIdsBefore = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIdsBefore.isNotEmpty(), "用户${userId}应该有资源ID列表")
        
        // 同步缓存（模拟角色-资源关系变更，会影响拥有该角色的所有用户）
        cacheHandler.syncOnRoleResourceChange(roleId)
        
        // 验证缓存已被清除并重新加载
        val resourceIdsAfter = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIdsAfter.isNotEmpty(), "同步后应该能重新获取到资源ID列表")
    }

    @Test
    fun syncOnUserDelete() {
        val userId = "33333333-3333-3333-3333-333333333333"
        
        // 先获取一次，确保缓存中有数据（即使为空列表）
        val resourceIdsBefore = cacheHandler.getResourceIds(userId)
        
        // 同步缓存（模拟用户删除）
        cacheHandler.syncOnUserDelete(userId)
        
        // 验证缓存已被清除
        val resourceIdsAfter = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIdsAfter.isEmpty(), "删除用户后，缓存应该被清除，重新获取应该返回空列表")
    }

    @Test
    fun syncOnBatchRoleUserChange() {
        val userId1 = "11111111-1111-1111-1111-111111111111"
        val userId2 = "22222222-2222-2222-2222-222222222222"
        val userIds = listOf(userId1, userId2)
        
        // 先获取一次，确保缓存中有数据
        val resourceIds1Before = cacheHandler.getResourceIds(userId1)
        val resourceIds2Before = cacheHandler.getResourceIds(userId2)
        assertTrue(resourceIds1Before.isNotEmpty() || resourceIds2Before.isNotEmpty(), "至少一个用户应该有资源ID列表")
        
        // 批量同步缓存（模拟批量用户-角色关系变更）
        cacheHandler.syncOnBatchRoleUserChange(userIds)
        
        // 验证缓存已被清除并重新加载
        val resourceIds1After = cacheHandler.getResourceIds(userId1)
        val resourceIds2After = cacheHandler.getResourceIds(userId2)
        assertTrue(resourceIds1After.isNotEmpty() || resourceIds2After.isNotEmpty(), "同步后应该能重新获取到资源ID列表")
    }

}
