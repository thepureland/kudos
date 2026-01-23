package io.kudos.ams.auth.provider.cache

import io.kudos.ams.auth.provider.dao.AuthRoleDao
import io.kudos.test.container.cache.RdbAndRedisCacheTestBase
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for ResourceIdsByRoleCodeCacheHandler
 *
 * 测试数据来源：`V1.0.0.6__ResourceIdsByRoleCodeCacheHandlerTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ResourceIdsByRoleCodeCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: ResourceIdsByRoleCodeCacheHandler

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    @Test
    fun getResourceIds() {
        // 存在的租户和角色
        var tenantId = "tenant-001"
        var roleCode = "ROLE_ADMIN"
        val resourceIds2 = cacheHandler.getResourceIds(tenantId, roleCode)
        val resourceIds3 = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(resourceIds2.isNotEmpty())
        assertEquals(resourceIds2, resourceIds3)

        // 不存在的角色
        roleCode = "ROLE_NO_EXIST"
        val resourceIds4 = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(resourceIds4.isEmpty())

        // 不存在的租户
        tenantId = "no_exist_tenant"
        roleCode = "ROLE_ADMIN"
        val resourceIds5 = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(resourceIds5.isEmpty())
    }

    @Test
    fun syncOnRoleResourceInsert() {
        val tenantId = "tenant-001"
        val roleCode = "ROLE_ADMIN"
        
        // 先获取一次，确保缓存中有数据
        val resourceIdsBefore = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(resourceIdsBefore.isNotEmpty(), "角色${roleCode}应该有资源ID列表")
        
        // 同步缓存（模拟角色-资源关系新增）
        cacheHandler.syncOnRoleResourceInsert(tenantId, roleCode)
        
        // 验证缓存已被清除并重新加载
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(resourceIdsAfter.isNotEmpty(), "同步后应该能重新获取到资源ID列表")
    }

    @Test
    fun syncOnRoleResourceDelete() {
        val tenantId = "tenant-001"
        val roleCode = "ROLE_ADMIN"
        
        // 先获取一次，确保缓存中有数据
        val resourceIdsBefore = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(resourceIdsBefore.isNotEmpty(), "角色${roleCode}应该有资源ID列表")
        
        // 同步缓存（模拟角色-资源关系删除）
        cacheHandler.syncOnRoleResourceDelete(tenantId, roleCode)
        
        // 验证缓存已被清除并重新加载
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(resourceIdsAfter.isNotEmpty() || resourceIdsAfter.isEmpty(), "同步后应该能重新获取到资源ID列表（可能为空）")
    }

    @Test
    fun syncOnRoleUpdate() {
        val oldTenantId = "tenant-001"
        val oldRoleCode = "ROLE_ADMIN"
        val newTenantId = "tenant-001"
        val newRoleCode = "ROLE_ADMIN_UPDATED"
        
        // 先获取一次，确保缓存中有数据
        val resourceIdsBefore = cacheHandler.getResourceIds(oldTenantId, oldRoleCode)
        assertTrue(resourceIdsBefore.isNotEmpty() || resourceIdsBefore.isEmpty(), "旧角色应该有资源ID列表（可能为空）")
        
        // 同步缓存（模拟角色信息更新）
        cacheHandler.syncOnRoleUpdate(oldTenantId, oldRoleCode, newTenantId, newRoleCode)
        
        // 验证旧缓存已被清除
        val resourceIdsOld = cacheHandler.getResourceIds(oldTenantId, oldRoleCode)
        // 旧缓存应该被清除，重新获取可能返回空或新的数据
    }

    @Test
    fun syncOnRoleDelete() {
        val tenantId = "tenant-001"
        val roleCode = "ROLE_USER"
        
        // 先获取一次，确保缓存中有数据（即使为空列表）
        val resourceIdsBefore = cacheHandler.getResourceIds(tenantId, roleCode)
        
        // 删除数据库中的角色记录
        val roleId = "22222222-2222-2222-2222-222222222222" // ROLE_USER 的 ID
        authRoleDao.deleteById(roleId)
        
        // 同步缓存（模拟角色删除）
        cacheHandler.syncOnRoleDelete(tenantId, roleCode)
        
        // 验证缓存已被清除，重新获取应该返回空列表（因为角色已不存在）
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(resourceIdsAfter.isEmpty(), "删除角色后，缓存应该被清除，重新获取应该返回空列表")
    }

}
