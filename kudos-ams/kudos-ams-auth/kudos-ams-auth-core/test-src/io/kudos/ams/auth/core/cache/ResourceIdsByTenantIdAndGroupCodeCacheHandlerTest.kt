package io.kudos.ams.auth.core.cache

import io.kudos.ams.auth.core.dao.AuthGroupDao
import io.kudos.ams.auth.core.dao.AuthGroupRoleDao
import io.kudos.ams.auth.core.dao.AuthRoleDao
import io.kudos.ams.auth.core.dao.AuthRoleResourceDao
import io.kudos.ams.auth.core.model.po.AuthGroup
import io.kudos.ams.auth.core.model.po.AuthGroupRole
import io.kudos.ams.auth.core.model.po.AuthRole
import io.kudos.ams.auth.core.model.po.AuthRoleResource
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for ResourceIdsByTenantIdAndGroupCodeCacheHandler
 *
 * 测试数据来源：`ResourceIdsByTenantIdAndGroupCodeCacheHandlerTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ResourceIdsByTenantIdAndGroupCodeCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: ResourceIdsByTenantIdAndGroupCodeCacheHandler

    @Resource
    private lateinit var authGroupDao: AuthGroupDao

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    @Resource
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Test
    fun getResourceIds() {
        // 存在的租户和用户组
        var tenantId = "tenant-001-7h2QGcPi"
        var groupCode = "GROUP_ADMIN"
        val resourceIds2 = cacheHandler.getResourceIds(tenantId, groupCode)
        val resourceIds3 = cacheHandler.getResourceIds(tenantId, groupCode)
        assertTrue(resourceIds2.isNotEmpty())
        assertEquals(resourceIds2, resourceIds3)

        // 不存在的用户组
        groupCode = "GROUP_NO_EXIST"
        val resourceIds4 = cacheHandler.getResourceIds(tenantId, groupCode)
        assertTrue(resourceIds4.isEmpty())

        // 不存在的租户
        tenantId = "no_exist_tenant"
        groupCode = "GROUP_ADMIN"
        val resourceIds5 = cacheHandler.getResourceIds(tenantId, groupCode)
        assertTrue(resourceIds5.isEmpty())
    }

    @Test
    fun syncOnGroupRoleInsert() {
        val tenantId = "tenant-001-7h2QGcPi"
        val groupCode = "GROUP_USER"
        val groupId = "274d0234-2222-2222-2222-222222222222" // GROUP_USER 的 ID
        val roleId = "274d0234-4444-4444-4444-444444444444" // 新角色ID
        val roleCode = "ROLE_TEST_GROUP_INSERT"
        val resourceId = "resource-new-group-role"

        // 先清除可能存在的缓存，确保测试环境干净
        cacheHandler.evict(cacheHandler.getKey(tenantId, groupCode))

        // 先获取一次，记录初始资源数量
        val resourceIdsBefore = cacheHandler.getResourceIds(tenantId, groupCode)
        val beforeSize = resourceIdsBefore.size

        // 检查关系是否已存在，如果存在则先删除
        if (authGroupRoleDao.exists(groupId, roleId)) {
            val criteria = Criteria.of(AuthGroupRole::groupId.name, OperatorEnum.EQ, groupId)
                .addAnd(AuthGroupRole::roleId.name, OperatorEnum.EQ, roleId)
            authGroupRoleDao.batchDeleteCriteria(criteria)
        }
        if (authRoleResourceDao.exists(roleId, resourceId)) {
            val criteria = Criteria.of(AuthRoleResource::roleId.name, OperatorEnum.EQ, roleId)
                .addAnd(AuthRoleResource::resourceId.name, OperatorEnum.EQ, resourceId)
            authRoleResourceDao.batchDeleteCriteria(criteria)
        }
        val authRole = AuthRole.Companion().apply {
            this.id = roleId
            this.code = roleCode
            this.name = "测试角色_${roleCode}"
            this.tenantId = tenantId
            this.subsysCode = "ams"
            this.active = true
        }
        authRoleDao.insert(authRole)
        val authRoleResource = AuthRoleResource.Companion().apply {
            this.roleId = roleId
            this.resourceId = resourceId
        }
        val roleResourceId = authRoleResourceDao.insert(authRoleResource)

        // 插入一条新的组-角色关系记录
        val authGroupRole = AuthGroupRole.Companion().apply {
            this.groupId = groupId
            this.roleId = roleId
        }
        val id = authGroupRoleDao.insert(authGroupRole)

        // 同步缓存（模拟组-角色关系新增）
        cacheHandler.syncOnGroupRoleInsert(tenantId, groupCode)

        // 验证缓存已被清除并重新加载，应该包含新角色的资源
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, groupCode).map { it.trim() }
        assertTrue(resourceIdsAfter.size > beforeSize, "同步后应该包含新插入的资源ID，之前：${beforeSize}，之后：${resourceIdsAfter.size}")
        assertTrue(resourceIdsAfter.contains(resourceId), "应该包含新插入的资源ID：${resourceId}，实际返回：${resourceIdsAfter}")

        // 清理测试数据
        authGroupRoleDao.deleteById(id)
        authRoleResourceDao.deleteById(roleResourceId)
        authRoleDao.deleteById(roleId)
        // 清理缓存，避免影响其他测试
        cacheHandler.evict(cacheHandler.getKey(tenantId, groupCode))
    }

    @Test
    fun syncOnGroupRoleDelete() {
        val tenantId = "tenant-001-7h2QGcPi"
        val groupCode = "GROUP_USER"
        val groupId = "274d0234-2222-2222-2222-222222222222" // GROUP_USER 的 ID
        val roleId = "274d0234-3333-3333-3333-333333333333" // ROLE_GUEST 的 ID

        if (authGroupRoleDao.exists(groupId, roleId)) {
            val criteria = Criteria.of(AuthGroupRole::groupId.name, OperatorEnum.EQ, groupId)
                .addAnd(AuthGroupRole::roleId.name, OperatorEnum.EQ, roleId)
            authGroupRoleDao.batchDeleteCriteria(criteria)
        }

        // 先插入一条组-角色关系记录
        val authGroupRole = AuthGroupRole.Companion().apply {
            this.groupId = groupId
            this.roleId = roleId
        }
        val id = authGroupRoleDao.insert(authGroupRole)

        // 先同步缓存，确保缓存中有新插入的数据
        cacheHandler.syncOnGroupRoleInsert(tenantId, groupCode)

        // 获取一次，确保缓存中有数据
        val resourceIdsBefore = cacheHandler.getResourceIds(tenantId, groupCode)
        assertTrue(resourceIdsBefore.isNotEmpty(), "新插入的组-角色关系应该使缓存有数据")

        // 删除数据库记录
        val deleteSuccess = authGroupRoleDao.deleteById(id)
        assertTrue(deleteSuccess, "删除应该成功")

        // 同步缓存（模拟组-角色关系删除）
        cacheHandler.syncOnGroupRoleDelete(tenantId, groupCode)

        // 验证缓存已被清除并重新加载
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, groupCode)
        assertTrue(resourceIdsAfter.size <= resourceIdsBefore.size, "删除后，资源数量不应增加")
    }

    @Test
    fun syncOnRoleResourceChange() {
        val roleId = "274d0234-1111-1111-1111-111111111111" // ROLE_ADMIN 的 ID
        val tenantId = "tenant-001-7h2QGcPi"
        val groupCode = "GROUP_ADMIN"
        val resourceId = "resource-ggg"

        // 先清除可能存在的缓存，确保测试环境干净
        cacheHandler.evict(cacheHandler.getKey(tenantId, groupCode))

        // 先获取一次，记录初始资源数量
        val resourceIdsBefore = cacheHandler.getResourceIds(tenantId, groupCode)
        val beforeSize = resourceIdsBefore.size

        // 检查关系是否已存在，如果存在则先删除
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

        // 同步缓存（模拟角色-资源关系变更，会影响包含该角色的用户组）
        cacheHandler.syncOnRoleResourceChange(roleId)

        // 再次清除缓存，确保从数据库重新加载
        cacheHandler.evict(cacheHandler.getKey(tenantId, groupCode))

        // 验证缓存已被清除并重新加载，应该包含新插入的资源
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, groupCode).map { it.trim() }
        assertTrue(resourceIdsAfter.size > beforeSize, "同步后应该包含新插入的资源ID，之前：${beforeSize}，之后：${resourceIdsAfter.size}")
        assertTrue(resourceIdsAfter.contains(resourceId), "应该包含新插入的资源ID：${resourceId}，实际返回：${resourceIdsAfter}")

        // 清理测试数据
        authRoleResourceDao.deleteById(id)
        // 清理缓存，避免影响其他测试
        cacheHandler.evict(cacheHandler.getKey(tenantId, groupCode))
        cacheHandler.syncOnRoleResourceChange(roleId)
    }

    @Test
    fun syncOnGroupUpdate() {
        val oldTenantId = "tenant-001-7h2QGcPi"
        val oldGroupCode = "GROUP_USER"
        val newTenantId = "tenant-001-7h2QGcPi"
        val newGroupCode = "GROUP_USER_UPDATED"
        val groupId = "274d0234-2222-2222-2222-222222222222" // GROUP_USER 的 ID

        // 先获取一次，确保缓存中有数据
        val resourceIdsBefore = cacheHandler.getResourceIds(oldTenantId, oldGroupCode)

        // 更新用户组编码
        val group = authGroupDao.get(groupId)
        assertTrue(group != null, "用户组应该存在")
        val success = authGroupDao.updateProperties(groupId, mapOf(AuthGroup::code.name to newGroupCode))
        assertTrue(success, "更新应该成功")

        // 同步缓存（模拟用户组信息更新）
        cacheHandler.syncOnGroupUpdate(oldTenantId, oldGroupCode, newTenantId, newGroupCode)

        // 验证旧缓存已被清除，新缓存可以获取数据
        val resourceIdsNew = cacheHandler.getResourceIds(newTenantId, newGroupCode)
        assertEquals(
            resourceIdsBefore.size,
            resourceIdsNew.size,
            "新用户组编码应该能获取到相同的资源列表"
        )

        // 恢复用户组编码
        authGroupDao.updateProperties(groupId, mapOf(AuthGroup::code.name to oldGroupCode))
    }

    @Test
    fun syncOnGroupDelete() {
        val tenantId = "tenant-001-7h2QGcPi"
        val groupCode = "GROUP_USER"

        // 先获取一次，确保缓存中有数据（即使为空列表）
        cacheHandler.getResourceIds(tenantId, groupCode)

        // 删除数据库中的用户组记录
        val groupId = "274d0234-2222-2222-2222-222222222222" // GROUP_USER 的 ID
        authGroupDao.deleteById(groupId)

        // 同步缓存（模拟用户组删除）
        cacheHandler.syncOnGroupDelete(tenantId, groupCode)

        // 验证缓存已被清除，重新获取应该返回空列表（因为用户组已不存在）
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, groupCode)
        assertTrue(resourceIdsAfter.isEmpty(), "删除用户组后，缓存应该被清除，重新获取应该返回空列表")
    }

}
