package io.kudos.ms.auth.core.cache

import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.auth.core.dao.AuthRoleDao
import io.kudos.ms.auth.core.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.model.po.AuthRole
import io.kudos.ms.auth.core.model.po.AuthRoleResource
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for ResourceIdsByRoleCodeCacheHandler
 *
 * 测试数据来源：`ResourceIdsByTenantIdAndRoleCodeCacheTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ResourceIdsByTenantIdAndRoleCodeCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: ResourceIdsByTenantIdAndRoleCodeCache

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    @Resource
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Test
    fun getResourceIds() {
        // 存在的租户和角色
        var tenantId = "tenant-001-174d0234"
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
        val tenantId = "tenant-001-174d0234"
        val roleCode = "ROLE_USER"
        val roleId = "174d0234-2222-2222-2222-222222222222" // ROLE_USER 的 ID
        val resourceId = "resource-xxx" // 新的资源ID（使用不存在的资源ID，避免唯一约束冲突）
        
        // 先清除可能存在的缓存，确保测试环境干净
        cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))
        
        // 先获取一次，记录初始资源数量
        val resourceIdsBefore = cacheHandler.getResourceIds(tenantId, roleCode)
        val beforeSize = resourceIdsBefore.size
        
        // 检查关系是否已存在，如果存在则先删除（以防之前的测试没有清理）
        if (authRoleResourceDao.exists(roleId, resourceId)) {
            val criteria = Criteria.of(AuthRoleResource::roleId.name, OperatorEnum.EQ, roleId)
                .addAnd(AuthRoleResource::resourceId.name, OperatorEnum.EQ, resourceId)
            authRoleResourceDao.batchDeleteCriteria(criteria)
        }
        
        // 插入一条新的角色-资源关系记录
        val authRoleResource = AuthRoleResource.Companion().apply {
            this.roleId = roleId
            this.resourceId = resourceId
        }
        val id = authRoleResourceDao.insert(authRoleResource)
        
        // 同步缓存（模拟角色-资源关系新增）
        cacheHandler.syncOnRoleResourceInsert(tenantId, roleCode)
        
        // 验证缓存已被清除并重新加载，应该包含新插入的资源
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(resourceIdsAfter.size > beforeSize, "同步后应该包含新插入的资源ID，之前：${beforeSize}，之后：${resourceIdsAfter.size}")
        assertTrue(resourceIdsAfter.contains(resourceId), "应该包含新插入的资源ID：${resourceId}，实际返回：${resourceIdsAfter}")
        
        // 清理测试数据
        authRoleResourceDao.deleteById(id)
        // 清理缓存，避免影响其他测试
        cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))
    }

    @Test
    fun syncOnRoleResourceDelete() {
        val tenantId = "tenant-001-174d0234"
        val roleCode = "ROLE_USER"
        val roleId = "174d0234-2222-2222-2222-222222222222" // ROLE_USER 的 ID
        val resourceId = "resource-eee" // 新的资源ID
        
        // 先插入一条角色-资源关系记录
        val authRoleResource = AuthRoleResource.Companion().apply {
            this.roleId = roleId
            this.resourceId = resourceId
        }
        val id = authRoleResourceDao.insert(authRoleResource)
        
        // 先同步缓存，确保缓存中有新插入的数据
        cacheHandler.syncOnRoleResourceInsert(tenantId, roleCode)
        
        // 获取一次，确保缓存中有数据
        val resourceIdsBefore = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(resourceIdsBefore.contains(resourceId), "新插入的资源关系应该在缓存中")
        
        // 删除数据库记录
        val deleteSuccess = authRoleResourceDao.deleteById(id)
        assertTrue(deleteSuccess, "删除应该成功")
        
        // 同步缓存（模拟角色-资源关系删除）
        cacheHandler.syncOnRoleResourceDelete(tenantId, roleCode)
        
        // 验证缓存已被清除并重新加载，应该不包含已删除的资源
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(!resourceIdsAfter.contains(resourceId), "删除后，缓存应该被清除，不应该包含已删除的资源ID")
    }

    @Test
    fun syncOnRoleUpdate() {
        val oldTenantId = "tenant-001-174d0234"
        val oldRoleCode = "ROLE_USER"
        val newTenantId = "tenant-001-174d0234"
        val newRoleCode = "ROLE_USER_UPDATED"
        val roleId = "174d0234-2222-2222-2222-222222222222" // ROLE_USER 的 ID
        
        // 先获取一次，确保缓存中有数据
        val resourceIdsBefore = cacheHandler.getResourceIds(oldTenantId, oldRoleCode)
        
        // 更新角色编码
        val role = authRoleDao.getAs(roleId)
        assertTrue(role != null, "角色应该存在")
        val success = authRoleDao.updateProperties(roleId, mapOf(AuthRole::code.name to newRoleCode))
        assertTrue(success, "更新应该成功")
        
        // 同步缓存（模拟角色信息更新）
        cacheHandler.syncOnRoleUpdate(oldTenantId, oldRoleCode, newTenantId, newRoleCode)
        
        // 验证旧缓存已被清除，新缓存可以获取数据
//        val resourceIdsOld = cacheHandler.getResourceIds(oldTenantId, oldRoleCode)
        val resourceIdsNew = cacheHandler.getResourceIds(newTenantId, newRoleCode)
        // 旧缓存应该被清除，新缓存应该能获取到数据（资源关系不变，只是角色编码变了）
        assertEquals(
            resourceIdsBefore.size,
            resourceIdsNew.size,
            "新角色编码应该能获取到相同的资源列表"
        )
        
        // 恢复角色编码
        authRoleDao.updateProperties(roleId, mapOf(AuthRole::code.name to oldRoleCode))
    }

    @Test
    fun syncOnRoleDelete() {
        val tenantId = "tenant-001-174d0234"
        val roleCode = "ROLE_USER"
        
        // 先获取一次，确保缓存中有数据（即使为空列表）
        cacheHandler.getResourceIds(tenantId, roleCode)
        
        // 删除数据库中的角色记录
        val roleId = "174d0234-2222-2222-2222-222222222222" // ROLE_USER 的 ID
        authRoleDao.deleteById(roleId)
        
        // 同步缓存（模拟角色删除）
        cacheHandler.syncOnRoleDelete(tenantId, roleCode)
        
        // 验证缓存已被清除，重新获取应该返回空列表（因为角色已不存在）
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(resourceIdsAfter.isEmpty(), "删除角色后，缓存应该被清除，重新获取应该返回空列表")
    }

}
