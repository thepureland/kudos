package io.kudos.ams.auth.provider.cache

import io.kudos.test.container.cache.RdbAndRedisCacheTestBase
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for ResourceIdsByRoleIdCacheHandler
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ResourceIdsByRoleIdCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: ResourceIdsByRoleIdCacheHandler

    @Test
    fun getResourceIds() {
        // 存在的角色ID，有多个资源
        var roleId = "11111111-1111-1111-1111-111111111111"
        val resourceIds1 = cacheHandler.getResourceIds(roleId)
        val resourceIds2 = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIds1.isNotEmpty(), "角色${roleId}应该有资源ID列表")
        assertEquals(resourceIds1, resourceIds2, "两次调用应该返回相同的结果（缓存验证）")
        // 验证资源ID：角色ROLE_ADMIN有resource-aaa和resource-bbb
        assertEquals(2, resourceIds1.size, "角色${roleId}应该有2个资源ID")
        assertTrue(resourceIds1.contains("resource-aaa"), "应该包含resource-aaa，实际返回：${resourceIds1}")
        assertTrue(resourceIds1.contains("resource-bbb"), "应该包含resource-bbb，实际返回：${resourceIds1}")

        // 存在的角色ID，有多个资源
        roleId = "22222222-2222-2222-2222-222222222222"
        val resourceIds3 = cacheHandler.getResourceIds(roleId)
        val resourceIds4 = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIds3.isNotEmpty(), "角色${roleId}应该有资源ID列表")
        assertEquals(resourceIds3, resourceIds4, "两次调用应该返回相同的结果（缓存验证）")
        // 角色ROLE_USER有resource-ccc和resource-ddd
        assertEquals(2, resourceIds3.size, "角色${roleId}应该有2个资源ID，实际返回：${resourceIds3}")
        assertTrue(resourceIds3.contains("resource-ccc"), "应该包含resource-ccc")
        assertTrue(resourceIds3.contains("resource-ddd"), "应该包含resource-ddd")

        // 存在的角色ID，但没有资源
        roleId = "33333333-3333-3333-3333-333333333333"
        val resourceIds5 = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIds5.isEmpty(), "角色${roleId}没有资源，应该返回空列表")

        // 不存在的角色ID
        roleId = "no_exist_role_id"
        val resourceIds6 = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIds6.isEmpty(), "不存在的角色ID应该返回空列表")
    }

    @Test
    fun syncOnRoleResourceChange() {
        val roleId = "11111111-1111-1111-1111-111111111111"
        
        // 先获取一次，确保缓存中有数据
        val resourceIdsBefore = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIdsBefore.isNotEmpty(), "角色${roleId}应该有资源ID列表")
        
        // 同步缓存（模拟角色-资源关系变更）
        cacheHandler.syncOnRoleResourceChange(roleId)
        
        // 验证缓存已被清除并重新加载
        val resourceIdsAfter = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIdsAfter.isNotEmpty(), "同步后应该能重新获取到资源ID列表")
    }

    @Test
    fun syncOnBatchRoleResourceChange() {
        val roleId1 = "11111111-1111-1111-1111-111111111111"
        val roleId2 = "22222222-2222-2222-2222-222222222222"
        val roleIds = listOf(roleId1, roleId2)
        
        // 先获取一次，确保缓存中有数据
        val resourceIds1Before = cacheHandler.getResourceIds(roleId1)
        val resourceIds2Before = cacheHandler.getResourceIds(roleId2)
        assertTrue(resourceIds1Before.isNotEmpty() || resourceIds2Before.isNotEmpty(), "至少一个角色应该有资源ID列表")
        
        // 批量同步缓存（模拟批量角色-资源关系变更）
        cacheHandler.syncOnBatchRoleResourceChange(roleIds)
        
        // 验证缓存已被清除并重新加载
        val resourceIds1After = cacheHandler.getResourceIds(roleId1)
        val resourceIds2After = cacheHandler.getResourceIds(roleId2)
        assertTrue(resourceIds1After.isNotEmpty() || resourceIds2After.isNotEmpty(), "同步后应该能重新获取到资源ID列表")
    }

    @Test
    fun syncOnRoleDelete() {
        val roleId = "33333333-3333-3333-3333-333333333333"
        
        // 先获取一次，确保缓存中有数据（即使为空列表）
        val resourceIdsBefore = cacheHandler.getResourceIds(roleId)
        
        // 同步缓存（模拟角色删除）
        cacheHandler.syncOnRoleDelete(roleId)
        
        // 验证缓存已被清除
        val resourceIdsAfter = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIdsAfter.isEmpty(), "删除角色后，缓存应该被清除，重新获取应该返回空列表")
    }

}
