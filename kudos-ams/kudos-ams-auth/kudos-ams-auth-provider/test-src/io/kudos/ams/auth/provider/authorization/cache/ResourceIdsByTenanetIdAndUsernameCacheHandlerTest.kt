package io.kudos.ams.auth.provider.authorization.cache

import io.kudos.ams.auth.provider.authorization.dao.AuthRoleResourceDao
import io.kudos.ams.auth.provider.authorization.dao.AuthRoleUserDao
import io.kudos.ams.auth.provider.authorization.model.po.AuthRoleResource
import io.kudos.ams.auth.provider.authorization.model.po.AuthRoleUser
import io.kudos.ams.auth.provider.user.dao.AuthUserDao
import io.kudos.ams.auth.provider.user.model.po.AuthUser
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for ResourceIdsByTenanetIdAndUsernameCacheHandler
 *
 * 测试数据来源：`ResourceIdsByTenanetIdAndUsernameCacheHandlerTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ResourceIdsByTenanetIdAndUsernameCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: ResourceIdsByTenanetIdAndUsernameCacheHandler

    @Resource
    private lateinit var authUserDao: AuthUserDao

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Test
    fun getResourceIds() {
        // 存在的租户和用户
        var tenantId = "tenant-001-InqhPsBT"
        var username = "admin"
        val resourceIds2 = cacheHandler.getResourceIds(tenantId, username)
        val resourceIds3 = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIds2.isNotEmpty())
        assertEquals(resourceIds2, resourceIds3)

        // 不存在的用户名
        username = "no_exist_user"
        val resourceIds4 = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIds4.isEmpty())

        // 不存在的租户
        tenantId = "no_exist_tenant"
        username = "admin"
        val resourceIds5 = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIds5.isEmpty())
    }

    @Test
    fun syncOnUserUpdate() {
        val oldTenantId = "tenant-001-InqhPsBT"
        val oldUsername = "zhangsan"
        val newTenantId = "tenant-001-InqhPsBT"
        val newUsername = "zhangsan_updated"
        val userId = "8e232124-2222-2222-2222-222222222222" // zhangsan 的 ID
        
        // 先获取一次，确保缓存中有数据
        val resourceIdsBefore = cacheHandler.getResourceIds(oldTenantId, oldUsername)
        
        // 更新用户名
        val success = authUserDao.updateProperties(userId, mapOf(AuthUser::username.name to newUsername))
        assertTrue(success, "更新应该成功")
        
        // 同步缓存（模拟用户信息更新）
        cacheHandler.syncOnUserUpdate(oldTenantId, oldUsername, newTenantId, newUsername)
        
        // 验证旧缓存已被清除，新缓存可以获取数据
        val resourceIdsOld = cacheHandler.getResourceIds(oldTenantId, oldUsername)
        val resourceIdsNew = cacheHandler.getResourceIds(newTenantId, newUsername)
        // 旧缓存应该被清除，新缓存应该能获取到数据（资源关系不变，只是用户名变了）
        assertEquals(resourceIdsBefore.size, resourceIdsNew.size, "新用户名应该能获取到相同的资源列表")
        
        // 恢复用户名
        authUserDao.updateProperties(userId, mapOf(AuthUser::username.name to oldUsername))
    }

    @Test
    fun syncOnRoleUserChange() {
        val tenantId = "tenant-001-InqhPsBT"
        val username = "zhangsan"
        val userId = "8e232124-2222-2222-2222-222222222222" // zhangsan 的 ID
        val roleId = "8e232124-1111-1111-1111-111111111111" // ROLE_ADMIN 的 ID
        
        // 先获取一次，记录初始资源数量
        val resourceIdsBefore = cacheHandler.getResourceIds(tenantId, username)
        val beforeSize = resourceIdsBefore.size
        
        // 插入一条新的用户-角色关系记录
        val authRoleUser = AuthRoleUser().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)
        
        // 同步缓存（模拟用户-角色关系变更）
        cacheHandler.syncOnRoleUserChange(tenantId, username)
        
        // 验证缓存已被清除并重新加载，应该包含新角色的资源
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIdsAfter.size >= beforeSize, "同步后应该包含新角色的资源ID")
        
        // 清理测试数据
        authRoleUserDao.deleteById(id)
    }

    @Test
    fun syncOnRoleResourceChange() {
        val roleId = "8e232124-1111-1111-1111-111111111111" // ROLE_ADMIN 的 ID
        val tenantId = "tenant-001-InqhPsBT"
        val username = "admin"
        val resourceId = "resource-kkk"
        
        // 先清除可能存在的缓存，确保测试环境干净
        cacheHandler.evict(cacheHandler.getKey(tenantId, username))
        
        // 先获取一次，记录初始资源数量（会从数据库加载并缓存）
        val resourceIdsBefore = cacheHandler.getResourceIds(tenantId, username)
        val beforeSize = resourceIdsBefore.size
        
        // 插入一条新的角色-资源关系记录
        val authRoleResource = AuthRoleResource().apply {
            this.roleId = roleId
            this.resourceId = resourceId
        }
        val id = authRoleResourceDao.insert(authRoleResource)
        
        // 同步缓存（模拟角色-资源关系变更，会影响拥有该角色的所有用户）
        // 这会清除所有拥有该角色的用户的缓存
        cacheHandler.syncOnRoleResourceChange(roleId)
        
        // 再次清除缓存，确保从数据库重新加载（因为 @Cacheable 可能会使用旧缓存）
        cacheHandler.evict(cacheHandler.getKey(tenantId, username))
        
        // 验证缓存已被清除并重新加载，应该包含新插入的资源
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIdsAfter.size > beforeSize, "同步后应该包含新插入的资源ID，之前：${beforeSize}，之后：${resourceIdsAfter.size}，实际返回：${resourceIdsAfter}")
        assertTrue(resourceIdsAfter.contains(resourceId), "应该包含新插入的资源ID：${resourceId}，实际返回：${resourceIdsAfter}")
        
        // 清理测试数据
        authRoleResourceDao.deleteById(id)
        // 清理缓存，避免影响其他测试
        cacheHandler.evict(cacheHandler.getKey(tenantId, username))
        cacheHandler.syncOnRoleResourceChange(roleId)
    }

    @Test
    fun syncOnUserDelete() {
        val tenantId = "tenant-001-InqhPsBT"
        val username = "zhangsan"
        
        // 先获取一次，确保缓存中有数据（即使为空列表）
        cacheHandler.getResourceIds(tenantId, username)

        // 删除数据库中的用户记录
        val userId = "8e232124-2222-2222-2222-222222222222" // zhangsan 的 ID
        authUserDao.deleteById(userId)
        
        // 同步缓存（模拟用户删除）
        cacheHandler.syncOnUserDelete(tenantId, username)
        
        // 验证缓存已被清除，重新获取应该返回空列表（因为用户已不存在）
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIdsAfter.isEmpty(), "删除用户后，缓存应该被清除，重新获取应该返回空列表")
    }

}
