package io.kudos.ams.auth.provider.authorization.cache

import io.kudos.ams.auth.provider.authorization.dao.AuthRoleResourceDao
import io.kudos.ams.auth.provider.authorization.dao.AuthRoleUserDao
import io.kudos.ams.auth.provider.authorization.model.po.AuthRoleResource
import io.kudos.ams.auth.provider.authorization.model.po.AuthRoleUser
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
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

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Test
    fun getResourceIds() {
        // 存在的用户ID，有一个角色，该角色有多个资源
        var userId = "165f7094-1111-1111-1111-111111111111"
        val resourceIds1 = cacheHandler.getResourceIds(userId)
        val resourceIds2 = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIds1.isNotEmpty(), "用户${userId}应该有资源ID列表")
        assertEquals(resourceIds1, resourceIds2, "两次调用应该返回相同的结果（缓存验证）")
        // 验证资源ID：用户11111111有角色ROLE_ADMIN，应该包含resource-aaa和resource-bbb
        assertEquals(2, resourceIds1.size, "用户${userId}应该有2个资源ID")
        assertTrue(resourceIds1.contains("resource-aaa-6Z55FylV"), "应该包含resource-aaa，实际返回：${resourceIds1}")
        assertTrue(resourceIds1.contains("resource-bbb-6Z55FylV"), "应该包含resource-bbb，实际返回：${resourceIds1}")

        // 存在的用户ID，有多个角色，每个角色有多个资源
        userId = "165f7094-2222-2222-2222-222222222222"
        // 先清除可能存在的缓存，确保测试环境干净
        cacheHandler.evict(userId)
        val resourceIds3 = cacheHandler.getResourceIds(userId)
        val resourceIds4 = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIds3.isNotEmpty(), "用户${userId}应该有资源ID列表")
        assertEquals(resourceIds3, resourceIds4, "两次调用应该返回相同的结果（缓存验证）")
        // 用户22222222有两个角色（ROLE_USER和ROLE_ADMIN），应该包含所有角色的资源（去重后）
        // ROLE_ADMIN的资源：resource-aaa, resource-bbb
        // ROLE_USER的资源：resource-ccc, resource-ddd
        // 总共应该是4个资源
        assertEquals(4, resourceIds3.size, "用户${userId}应该有4个资源ID，实际返回：${resourceIds3}")
        assertTrue(resourceIds3.contains("resource-aaa-6Z55FylV"), "应该包含resource-aaa")
        assertTrue(resourceIds3.contains("resource-bbb-6Z55FylV"), "应该包含resource-bbb")
        assertTrue(resourceIds3.contains("resource-ccc-6Z55FylV"), "应该包含resource-ccc")
        assertTrue(resourceIds3.contains("resource-ddd-6Z55FylV"), "应该包含resource-ddd")

        // 存在的用户ID，但没有角色
        userId = "165f7094-3333-3333-3333-333333333333"
        // 先清除可能存在的缓存，确保测试环境干净
        cacheHandler.evict(userId)
        // 清理可能存在的用户-角色关系记录（以防之前的测试没有清理干净）
        val roleUserCriteria = Criteria(AuthRoleUser::userId.name, OperatorEnum.EQ, userId)
        val existingRoleUsers = authRoleUserDao.search(roleUserCriteria)
        existingRoleUsers.forEach { authRoleUserDao.deleteById(it.id!!) }
        // 再次清除缓存，确保从数据库重新加载
        cacheHandler.evict(userId)
        val resourceIds5 = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIds5.isEmpty(), "用户${userId}没有角色，应该返回空列表，实际返回：${resourceIds5}")

        // 不存在的用户ID
        userId = "no_exist_user_id"
        val resourceIds6 = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIds6.isEmpty(), "不存在的用户ID应该返回空列表")
    }

    @Test
    fun syncOnRoleUserChange() {
        val userId = "165f7094-3333-3333-3333-333333333333"
        val roleId = "165f7094-2222-2222-2222-222222222222" // ROLE_USER 的 ID
        
        // 先获取一次，记录初始资源数量
        val resourceIdsBefore = cacheHandler.getResourceIds(userId)
        val beforeSize = resourceIdsBefore.size
        
        // 插入一条新的用户-角色关系记录
        val authRoleUser = AuthRoleUser().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)
        
        // 同步缓存（模拟用户-角色关系变更）
        cacheHandler.syncOnRoleUserChange(userId)
        
        // 验证缓存已被清除并重新加载，应该包含新角色的资源
        val resourceIdsAfter = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIdsAfter.size >= beforeSize, "同步后应该包含新角色的资源ID")
        
        // 清理测试数据
        authRoleUserDao.deleteById(id)
    }

    @Test
    fun syncOnRoleResourceChange() {
        val roleId = "165f7094-2222-2222-2222-222222222222" // ROLE_USER 的 ID
        val userId = "165f7094-2222-2222-2222-222222222222" // 拥有该角色的用户
        val resourceId = "resource-jjj"
        
        // 先清除可能存在的缓存，确保测试环境干净
        cacheHandler.evict(userId)
        
        // 先获取一次，记录初始资源数量
        val resourceIdsBefore = cacheHandler.getResourceIds(userId)
        val beforeSize = resourceIdsBefore.size
        
        // 插入一条新的角色-资源关系记录
        val authRoleResource = AuthRoleResource().apply {
            this.roleId = roleId
            this.resourceId = resourceId
        }
        val id = authRoleResourceDao.insert(authRoleResource)
        
        // 同步缓存（模拟角色-资源关系变更，会影响拥有该角色的所有用户）
        cacheHandler.syncOnRoleResourceChange(roleId)
        
        // 验证缓存已被清除并重新加载，应该包含新插入的资源
        val resourceIdsAfter = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIdsAfter.size > beforeSize, "同步后应该包含新插入的资源ID，之前：${beforeSize}，之后：${resourceIdsAfter.size}")
        assertTrue(resourceIdsAfter.contains(resourceId), "应该包含新插入的资源ID")
        
        // 清理测试数据
        authRoleResourceDao.deleteById(id)
        // 清理缓存，避免影响其他测试
        cacheHandler.syncOnRoleResourceChange(roleId)
    }

    @Test
    fun syncOnUserDelete() {
        val userId = "165f7094-3333-3333-3333-333333333333"
        val roleId = "165f7094-1111-1111-1111-111111111111" // ROLE_ADMIN 的 ID
        
        // 先插入一条用户-角色关系记录
        val authRoleUser = AuthRoleUser().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)
        
        // 先同步缓存，确保缓存中有新插入的数据
        cacheHandler.syncOnRoleUserChange(userId)
        
        // 获取一次，确保缓存中有数据
        val resourceIdsBefore = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIdsBefore.isNotEmpty(), "新插入的角色关系应该在缓存中")
        
        // 删除数据库记录（模拟用户删除或用户-角色关系删除）
        val deleteSuccess = authRoleUserDao.deleteById(id)
        assertTrue(deleteSuccess, "删除应该成功")
        
        // 同步缓存（模拟用户删除）
        cacheHandler.syncOnUserDelete(userId)
        
        // 验证缓存已被清除，重新获取应该不包含已删除的资源
        val resourceIdsAfter = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIdsAfter.isEmpty(), "删除后，缓存应该被清除，不应该包含已删除的资源ID")
    }

    @Test
    fun syncOnBatchRoleUserChange() {
        val userId1 = "165f7094-3333-3333-3333-333333333333"
        val userId2 = "165f7094-3333-3333-3333-333333333333"
        val roleId1 = "165f7094-1111-1111-1111-111111111111" // ROLE_ADMIN 的 ID
        val roleId2 = "165f7094-2222-2222-2222-222222222222" // ROLE_USER 的 ID
        val userIds = listOf(userId1, userId2)
        
        // 先获取一次，记录初始资源数量
        val resourceIds1Before = cacheHandler.getResourceIds(userId1)
        val beforeSize = resourceIds1Before.size
        
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
        
        // 验证缓存已被清除并重新加载，应该包含新角色的资源
        val resourceIds1After = cacheHandler.getResourceIds(userId1)
        assertTrue(resourceIds1After.size >= beforeSize, "同步后应该包含新角色的资源ID")
        
        // 清理测试数据
        authRoleUserDao.deleteById(id1)
        authRoleUserDao.deleteById(id2)
    }

}
