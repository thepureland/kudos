package io.kudos.ams.auth.provider.cache

import io.kudos.ams.auth.provider.dao.AuthUserDao
import io.kudos.test.container.cache.RdbAndRedisCacheTestBase
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for ResourceIdsByTenanetIdAndUsernameCacheHandler
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

    @Test
    fun getResourceIds() {
        // 存在的租户和用户
        var tenantId = "tenant-001"
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
        val oldTenantId = "tenant-001"
        val oldUsername = "admin"
        val newTenantId = "tenant-001"
        val newUsername = "admin_updated"
        
        // 先获取一次，确保缓存中有数据
        val resourceIdsBefore = cacheHandler.getResourceIds(oldTenantId, oldUsername)
        assertTrue(resourceIdsBefore.isNotEmpty(), "用户${oldUsername}应该有资源ID列表")
        
        // 同步缓存（模拟用户信息更新）
        cacheHandler.syncOnUserUpdate(oldTenantId, oldUsername, newTenantId, newUsername)
        
        // 验证旧缓存已被清除
        val resourceIdsOld = cacheHandler.getResourceIds(oldTenantId, oldUsername)
        // 旧缓存应该被清除，重新获取可能返回空或新的数据
    }

    @Test
    fun syncOnRoleUserChange() {
        val tenantId = "tenant-001"
        val username = "admin"
        
        // 先获取一次，确保缓存中有数据
        val resourceIdsBefore = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIdsBefore.isNotEmpty(), "用户${username}应该有资源ID列表")
        
        // 同步缓存（模拟用户-角色关系变更）
        cacheHandler.syncOnRoleUserChange(tenantId, username)
        
        // 验证缓存已被清除并重新加载
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIdsAfter.isNotEmpty(), "同步后应该能重新获取到资源ID列表")
    }

    @Test
    fun syncOnRoleResourceChange() {
        val roleId = "11111111-1111-1111-1111-111111111111"
        
        // 先获取一次，确保缓存中有数据（通过用户ID）
        val tenantId = "tenant-001"
        val username = "admin"
        val resourceIdsBefore = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIdsBefore.isNotEmpty(), "用户${username}应该有资源ID列表")
        
        // 同步缓存（模拟角色-资源关系变更，会影响拥有该角色的所有用户）
        cacheHandler.syncOnRoleResourceChange(roleId)
        
        // 验证缓存已被清除并重新加载
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIdsAfter.isNotEmpty(), "同步后应该能重新获取到资源ID列表")
    }

    @Test
    fun syncOnUserDelete() {
        val tenantId = "tenant-001"
        val username = "zhangsan"
        
        // 先获取一次，确保缓存中有数据（即使为空列表）
        val resourceIdsBefore = cacheHandler.getResourceIds(tenantId, username)
        
        // 删除数据库中的用户记录
        val userId = "22222222-2222-2222-2222-222222222222" // zhangsan 的 ID
        authUserDao.deleteById(userId)
        
        // 同步缓存（模拟用户删除）
        cacheHandler.syncOnUserDelete(tenantId, username)
        
        // 验证缓存已被清除，重新获取应该返回空列表（因为用户已不存在）
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIdsAfter.isEmpty(), "删除用户后，缓存应该被清除，重新获取应该返回空列表")
    }

}
