package io.kudos.ms.auth.core.cache

import io.kudos.ms.auth.core.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.model.po.AuthRoleResource
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for ResourceIdsByRoleIdCacheHandler
 *
 * 测试数据来源：`ResourceIdsByRoleIdCacheTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ResourceIdsByRoleIdCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: ResourceIdsByRoleIdCache

    @Resource
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Test
    fun getResourceIds() {
        // 存在的角色ID，有多个资源
        var roleId = "699180cb-1111-1111-1111-111111111111"
        val resourceIds1 = cacheHandler.getResourceIds(roleId)
        val resourceIds2 = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIds1.isNotEmpty(), "角色${roleId}应该有资源ID列表")
        assertEquals(resourceIds1, resourceIds2, "两次调用应该返回相同的结果（缓存验证）")
        // 验证资源ID：角色ROLE_ADMIN有resource-aaa和resource-bbb
        assertEquals(2, resourceIds1.size, "角色${roleId}应该有2个资源ID")
        assertTrue(resourceIds1.contains("resource-aaa-wXxAqLrp"), "应该包含resource-aaa，实际返回：${resourceIds1}")
        assertTrue(resourceIds1.contains("resource-bbb-wXxAqLrp"), "应该包含resource-bbb，实际返回：${resourceIds1}")

        // 存在的角色ID，有多个资源
        roleId = "699180cb-2222-2222-2222-222222222222"
        val resourceIds3 = cacheHandler.getResourceIds(roleId)
        val resourceIds4 = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIds3.isNotEmpty(), "角色${roleId}应该有资源ID列表")
        assertEquals(resourceIds3, resourceIds4, "两次调用应该返回相同的结果（缓存验证）")
        // 角色ROLE_USER有resource-ccc和resource-ddd
        assertEquals(
            2,
            resourceIds3.size,
            "角色${roleId}应该有2个资源ID，实际返回：${resourceIds3}"
        )
        assertTrue(resourceIds3.contains("resource-ccc-wXxAqLrp"), "应该包含resource-ccc")
        assertTrue(resourceIds3.contains("resource-ddd-wXxAqLrp"), "应该包含resource-ddd")

        // 存在的角色ID，但没有资源（先清除可能存在的缓存）
        roleId = "699180cb-3333-3333-3333-333333333333"
        cacheHandler.evict(roleId) // 清除可能存在的缓存
        val resourceIds5 = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIds5.isEmpty(), "角色${roleId}没有资源，应该返回空列表，实际返回：${resourceIds5}")

        // 不存在的角色ID
        roleId = "no_exist_role_id"
        val resourceIds6 = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIds6.isEmpty(), "不存在的角色ID应该返回空列表")
    }

    @Test
    fun syncOnRoleResourceChange() {
        val roleId = "699180cb-3333-3333-3333-333333333333"
        val resourceId = "resource-fff"
        
        // 先清除可能存在的缓存，确保测试环境干净
        cacheHandler.evict(roleId)
        
        // 先获取一次，记录初始资源数量
        val resourceIdsBefore = cacheHandler.getResourceIds(roleId)
        val beforeSize = resourceIdsBefore.size
        
        // 插入一条新的角色-资源关系记录
        val authRoleResource = AuthRoleResource.Companion().apply {
            this.roleId = roleId
            this.resourceId = resourceId
        }
        val id = authRoleResourceDao.insert(authRoleResource)
        
        // 同步缓存（模拟角色-资源关系变更）
        cacheHandler.syncOnRoleResourceChange(roleId)
        
        // 验证缓存已被清除并重新加载，应该包含新插入的资源
        val resourceIdsAfter = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIdsAfter.size > beforeSize, "同步后应该包含新插入的资源ID，之前：${beforeSize}，之后：${resourceIdsAfter.size}")
        assertTrue(resourceIdsAfter.contains(resourceId), "应该包含新插入的资源ID")
        
        // 清理测试数据
        authRoleResourceDao.deleteById(id)
        // 清理缓存，避免影响其他测试
        cacheHandler.syncOnRoleResourceChange(roleId)
    }

    @Test
    fun syncOnBatchRoleResourceChange() {
        val roleId1 = "699180cb-3333-3333-3333-333333333333"
        val roleId2 = "699180cb-3333-3333-3333-333333333333"
        val resourceId1 = "resource-ggg"
        val resourceId2 = "resource-hhh"
        val roleIds = listOf(roleId1, roleId2)
        
        // 先获取一次，记录初始资源数量
        val resourceIds1Before = cacheHandler.getResourceIds(roleId1)
        val beforeSize = resourceIds1Before.size
        
        // 批量插入角色-资源关系记录
        val authRoleResource1 = AuthRoleResource.Companion().apply {
            this.roleId = roleId1
            this.resourceId = resourceId1
        }
        val id1 = authRoleResourceDao.insert(authRoleResource1)
        
        val authRoleResource2 = AuthRoleResource.Companion().apply {
            this.roleId = roleId2
            this.resourceId = resourceId2
        }
        val id2 = authRoleResourceDao.insert(authRoleResource2)
        
        // 批量同步缓存（模拟批量角色-资源关系变更）
        cacheHandler.syncOnBatchRoleResourceChange(roleIds)
        
        // 验证缓存已被清除并重新加载，应该包含新插入的资源
        val resourceIds1After = cacheHandler.getResourceIds(roleId1)
        assertTrue(resourceIds1After.size > beforeSize, "同步后应该包含新插入的资源ID")
        
        // 清理测试数据
        authRoleResourceDao.deleteById(id1)
        authRoleResourceDao.deleteById(id2)
    }

    @Test
    fun syncOnRoleDelete() {
        val roleId = "699180cb-3333-3333-3333-333333333333"
        val resourceId = "resource-iii"
        
        // 先插入一条角色-资源关系记录
        val authRoleResource = AuthRoleResource.Companion().apply {
            this.roleId = roleId
            this.resourceId = resourceId
        }
        val id = authRoleResourceDao.insert(authRoleResource)
        
        // 先同步缓存，确保缓存中有新插入的数据
        cacheHandler.syncOnRoleResourceChange(roleId)
        
        // 获取一次，确保缓存中有数据
        val resourceIdsBefore = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIdsBefore.contains(resourceId), "新插入的资源关系应该在缓存中")
        
        // 删除数据库记录（模拟角色删除或角色-资源关系删除）
        val deleteSuccess = authRoleResourceDao.deleteById(id)
        assertTrue(deleteSuccess, "删除应该成功")
        
        // 同步缓存（模拟角色删除）
        cacheHandler.syncOnRoleDelete(roleId)
        
        // 验证缓存已被清除，重新获取应该不包含已删除的资源
        val resourceIdsAfter = cacheHandler.getResourceIds(roleId)
        assertTrue(!resourceIdsAfter.contains(resourceId), "删除后，缓存应该被清除，不应该包含已删除的资源ID")
    }

}
