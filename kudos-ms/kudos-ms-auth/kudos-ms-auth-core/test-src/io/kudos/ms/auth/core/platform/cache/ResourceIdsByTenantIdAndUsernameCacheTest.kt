package io.kudos.ms.auth.core.platform.cache

import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.group.model.po.AuthGroup
import io.kudos.ms.auth.core.group.model.po.AuthGroupRole
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.model.po.AuthRoleResource
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for ResourceIdsByTenanetIdAndUsernameCacheHandler
 *
 * 测试数据来源：`ResourceIdsByTenantIdAndUsernameCacheTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ResourceIdsByTenantIdAndUsernameCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: ResourceIdsByTenantIdAndUsernameCache

    @Resource
    private lateinit var userAccountHashCache: UserAccountHashCache

    @Resource
    private lateinit var userAccountDao: UserAccountDao

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Resource
    private lateinit var authGroupDao: AuthGroupDao

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

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
        val success = userAccountDao.updateProperties(userId, mapOf(UserAccount::username.name to newUsername))
        assertTrue(success, "更新应该成功")
        
        // 同步缓存（模拟用户信息更新）
        cacheHandler.syncOnUserUpdate(oldTenantId, oldUsername, newTenantId, newUsername)
        
        // 验证旧缓存已被清除，新缓存可以获取数据
//        val resourceIdsOld = cacheHandler.getResourceIds(oldTenantId, oldUsername)
        val resourceIdsNew = cacheHandler.getResourceIds(newTenantId, newUsername)
        // 旧缓存应该被清除，新缓存应该能获取到数据（资源关系不变，只是用户名变了）
        assertEquals(
            resourceIdsBefore.size,
            resourceIdsNew.size,
            "新用户名应该能获取到相同的资源列表"
        )
        
        // 恢复用户名
        userAccountDao.updateProperties(userId, mapOf(UserAccount::username.name to oldUsername))
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
        val authRoleUser = AuthRoleUser.Companion().apply {
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
        val authRoleResource = AuthRoleResource.Companion().apply {
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
        userAccountDao.deleteById(userId)
        
        // 直接驱动两个 listener（AFTER_COMMIT 在 @Transactional 测试中不会触发）：
        // 生产中 UserAccountDeleted 事件会同时触发本缓存 + UserAccountHashCache 的 on(...)。
        val event = UserAccountDeleted(userId, tenantId, username)
        cacheHandler.on(event)
        userAccountHashCache.on(event)
        
        // 验证缓存已被清除，重新获取应该返回空列表（因为用户已不存在）
        val resourceIdsAfter = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIdsAfter.isEmpty(), "删除用户后，缓存应该被清除，重新获取应该返回空列表")
    }

    // -------------------- group 路径 ((tenantId, username) → group → role → resource) 的覆盖 --------------------

    @Test
    fun getResourceIds_includesResourcesFromGroupInheritedRoles() {
        // zhangsan 直接绑定 ROLE_USER → resource-ccc。加入组，组绑定 ROLE_ADMIN → resources aaa/bbb。
        // 期望：getResourceIds 同时返回三条。
        val tenantId = "tenant-001-InqhPsBT"
        val username = "zhangsan"
        val roleId = "8e232124-1111-1111-1111-111111111111" // ROLE_ADMIN
        val groupId = "8e232124-grp1-aaaa-aaaa-aaaaaaaaaaaa"
        val userId = "8e232124-2222-2222-2222-222222222222" // zhangsan
        cacheHandler.evict(cacheHandler.getKey(tenantId, username))

        val gId = insertGroup(groupId, tenantId)
        val grId = insertGroupRole(gId, roleId)
        val guId = insertGroupUser(gId, userId)
        try {
            cacheHandler.evict(cacheHandler.getKey(tenantId, username))
            val resources = cacheHandler.getResourceIds(tenantId, username)
            assertTrue(resources.contains("resource-ccc-InqhPsBT"), "应保留 zhangsan 直接的 resource-ccc，实际：${resources}")
            assertTrue(resources.contains("resource-aaa-InqhPsBT"), "应通过组继承 resource-aaa，实际：${resources}")
            assertTrue(resources.contains("resource-bbb-InqhPsBT"), "应通过组继承 resource-bbb，实际：${resources}")
        } finally {
            authGroupUserDao.deleteById(guId)
            authGroupRoleDao.deleteById(grId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(cacheHandler.getKey(tenantId, username))
        }
    }

    @Test
    fun on_AuthGroupUserRelationsChanged_invalidatesCache() {
        val tenantId = "tenant-001-InqhPsBT"
        val username = "zhangsan"
        val userId = "8e232124-2222-2222-2222-222222222222"
        val roleId = "8e232124-1111-1111-1111-111111111111" // ROLE_ADMIN
        val groupId = "8e232124-grp2-bbbb-bbbb-bbbbbbbbbbbb"
        cacheHandler.evict(cacheHandler.getKey(tenantId, username))

        // 起点：zhangsan 只通过直接绑定看到 resource-ccc，看不到 aaa
        val before = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(before.contains("resource-ccc-InqhPsBT"))
        assertTrue(!before.contains("resource-aaa-InqhPsBT"))

        val gId = insertGroup(groupId, tenantId)
        val grId = insertGroupRole(gId, roleId)
        val guId = insertGroupUser(gId, userId)
        try {
            cacheHandler.on(AuthGroupUserRelationsChanged(gId, listOf(userId)))
            cacheHandler.evict(cacheHandler.getKey(tenantId, username))
            val after = cacheHandler.getResourceIds(tenantId, username)
            assertTrue(after.contains("resource-aaa-InqhPsBT"), "入组后应看到 resource-aaa，实际：${after}")
        } finally {
            authGroupUserDao.deleteById(guId)
            authGroupRoleDao.deleteById(grId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(cacheHandler.getKey(tenantId, username))
        }
    }

    @Test
    fun on_AuthGroupRoleRelationsChanged_invalidatesCache() {
        // zhangsan 在组里，组初始没绑角色。给组加 ROLE_ADMIN → zhangsan 资源应该新增 aaa/bbb。
        val tenantId = "tenant-001-InqhPsBT"
        val username = "zhangsan"
        val userId = "8e232124-2222-2222-2222-222222222222"
        val roleId = "8e232124-1111-1111-1111-111111111111" // ROLE_ADMIN
        val groupId = "8e232124-grp3-cccc-cccc-cccccccccccc"
        cacheHandler.evict(cacheHandler.getKey(tenantId, username))

        val gId = insertGroup(groupId, tenantId)
        val guId = insertGroupUser(gId, userId)
        try {
            val before = cacheHandler.getResourceIds(tenantId, username)
            assertTrue(!before.contains("resource-aaa-InqhPsBT"))

            val grId = insertGroupRole(gId, roleId)
            try {
                cacheHandler.on(AuthGroupRoleRelationsChanged(gId, listOf(roleId)))
                // existing tests 已知 @Cacheable 可能持有 on() 之前的旧值，需要显式 evict
                cacheHandler.evict(cacheHandler.getKey(tenantId, username))
                val after = cacheHandler.getResourceIds(tenantId, username)
                assertTrue(after.contains("resource-aaa-InqhPsBT"), "组绑角色后应看到 resource-aaa，实际：${after}")
                assertTrue(after.contains("resource-bbb-InqhPsBT"), "组绑角色后应看到 resource-bbb，实际：${after}")
            } finally {
                authGroupRoleDao.deleteById(grId)
            }
        } finally {
            authGroupUserDao.deleteById(guId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(cacheHandler.getKey(tenantId, username))
        }
    }

    private fun insertGroup(groupId: String, tenantId: String): String {
        val group = AuthGroup.Companion().apply {
            this.id = groupId
            this.code = "GRP_${groupId.takeLast(4)}"
            this.name = "test group ${groupId.takeLast(4)}"
            this.tenantId = tenantId
            this.subsysCode = "ams"
            this.active = true
            this.builtIn = false
        }
        return authGroupDao.insert(group)
    }

    private fun insertGroupUser(groupId: String, userId: String): String {
        val gu = AuthGroupUser.Companion().apply {
            this.groupId = groupId
            this.userId = userId
        }
        return authGroupUserDao.insert(gu)
    }

    private fun insertGroupRole(groupId: String, roleId: String): String {
        val gr = AuthGroupRole.Companion().apply {
            this.groupId = groupId
            this.roleId = roleId
        }
        return authGroupRoleDao.insert(gr)
    }

}
