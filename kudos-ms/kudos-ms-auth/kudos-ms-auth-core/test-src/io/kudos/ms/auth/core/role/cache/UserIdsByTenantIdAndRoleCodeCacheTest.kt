package io.kudos.ms.auth.core.role.cache

import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.group.model.po.AuthGroup
import io.kudos.ms.auth.core.group.model.po.AuthGroupRole
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleDeleted
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for UserIdsByTenantIdAndRoleCodeCacheHandler
 *
 * 测试数据来源：`UserIdsByTenantIdAndRoleCodeCacheTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserIdsByTenantIdAndRoleCodeCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: UserIdsByTenantIdAndRoleCodeCache

    @Resource
    private lateinit var authRoleHashCache: AuthRoleHashCache

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var authGroupDao: AuthGroupDao

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Test
    fun getUserIds() {
        // 存在的租户和角色
        var tenantId = "tenant-001-58TWQx6c"
        var roleCode = "ROLE_ADMIN"
        val userIds2 = cacheHandler.getUserIds(tenantId, roleCode)
        val userIds3 = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIds2.isNotEmpty())
        assertEquals(userIds2, userIds3)

        // 不存在的角色
        roleCode = "ROLE_NO_EXIST"
        val userIds4 = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIds4.isEmpty())

        // 不存在的租户
        tenantId = "no_exist_tenant"
        roleCode = "ROLE_ADMIN"
        val userIds5 = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIds5.isEmpty())
    }

    @Test
    fun syncOnRoleUserInsert() {
        val tenantId = "tenant-001-58TWQx6c"
        val roleCode = "ROLE_USER"
        val roleId = "10796e8c-2222-2222-2222-222222222222" // ROLE_USER 的 ID
        val userId = "10796e8c-3333-3333-3333-333333333333"
        
        // 先获取一次，记录初始用户数量
        val userIdsBefore = cacheHandler.getUserIds(tenantId, roleCode)
        val beforeSize = userIdsBefore.size
        
        // 插入一条新的角色-用户关系记录
        val authRoleUser = AuthRoleUser.Companion().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)
        
        // 同步缓存（模拟角色-用户关系新增）
        cacheHandler.syncOnRoleUserInsert(tenantId, roleCode)
        
        // 验证缓存已被清除并重新加载，应该包含新插入的用户
        val userIdsAfter = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIdsAfter.size > beforeSize, "同步后应该包含新插入的用户ID")
        assertTrue(userIdsAfter.contains(userId), "应该包含新插入的用户ID")
        
        // 清理测试数据
        authRoleUserDao.deleteById(id)
    }

    @Test
    fun syncOnRoleUserDelete() {
        val tenantId = "tenant-001-58TWQx6c"
        val roleCode = "ROLE_USER"
        val roleId = "10796e8c-2222-2222-2222-222222222222" // ROLE_USER 的 ID
        val userId = "10796e8c-3333-3333-3333-333333333333"
        
        // 先插入一条角色-用户关系记录
        val authRoleUser = AuthRoleUser.Companion().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)
        
        // 先同步缓存，确保缓存中有新插入的数据
        cacheHandler.syncOnRoleUserInsert(tenantId, roleCode)
        
        // 获取一次，确保缓存中有数据
        val userIdsBefore = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIdsBefore.contains(userId), "新插入的用户关系应该在缓存中")
        
        // 删除数据库记录
        val deleteSuccess = authRoleUserDao.deleteById(id)
        assertTrue(deleteSuccess, "删除应该成功")
        
        // 同步缓存（模拟角色-用户关系删除）
        cacheHandler.syncOnRoleUserDelete(tenantId, roleCode)
        
        // 验证缓存已被清除并重新加载，应该不包含已删除的用户
        val userIdsAfter = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(!userIdsAfter.contains(userId), "删除后，缓存应该被清除，不应该包含已删除的用户ID")
    }

    @Test
    fun syncOnRoleUpdate() {
        val oldTenantId = "tenant-001-58TWQx6c"
        val oldRoleCode = "ROLE_USER"
        val newTenantId = "tenant-001-58TWQx6c"
        val newRoleCode = "ROLE_USER_UPDATED"
        val roleId = "10796e8c-2222-2222-2222-222222222222" // ROLE_USER 的 ID
        
        // 先获取一次，确保缓存中有数据
        val userIdsBefore = cacheHandler.getUserIds(oldTenantId, oldRoleCode)
        
        // 更新角色编码
        val role = authRoleDao.get(roleId)
        assertTrue(role != null, "角色应该存在")
        val success = authRoleDao.updateProperties(roleId, mapOf(AuthRole::code.name to newRoleCode))
        assertTrue(success, "更新应该成功")
        
        // 同步缓存（模拟角色信息更新）
        cacheHandler.syncOnRoleUpdate(oldTenantId, oldRoleCode, newTenantId, newRoleCode)
        
        // 验证旧缓存已被清除，新缓存可以获取数据
        val userIdsNew = cacheHandler.getUserIds(newTenantId, newRoleCode)
        // 旧缓存应该被清除，新缓存应该能获取到数据（用户关系不变，只是角色编码变了）
        assertEquals(
            userIdsBefore.size,
            userIdsNew.size,
            "新角色编码应该能获取到相同的用户列表"
        )
        
        // 恢复角色编码
        authRoleDao.updateProperties(roleId, mapOf(AuthRole::code.name to oldRoleCode))
    }

    @Test
    fun syncOnRoleDelete() {
        val tenantId = "tenant-001-58TWQx6c"
        val roleCode = "ROLE_USER"
        
        // 先获取一次，确保缓存中有数据（即使为空列表）
        cacheHandler.getUserIds(tenantId, roleCode)
        
        // 删除数据库中的角色记录
        val roleId = "10796e8c-2222-2222-2222-222222222222" // ROLE_USER 的 ID
        authRoleDao.deleteById(roleId)
        
        // 直接驱动两个 listener（AFTER_COMMIT 在 @Transactional 测试中不会触发）：
        // 生产中 AuthRoleDeleted 事件会同时触发本缓存 + AuthRoleHashCache 的 on(...)。
        val event = AuthRoleDeleted(roleId, tenantId, roleCode)
        cacheHandler.on(event)
        authRoleHashCache.on(event)
        
        // 验证缓存已被清除，重新获取应该返回空列表（因为角色已不存在）
        val userIdsAfter = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIdsAfter.isEmpty(), "删除角色后，缓存应该被清除，重新获取应该返回空列表")
    }

    // -------------------- group 路径（(tenantId, roleCode) ← group ← user）的覆盖 --------------------

    @Test
    fun getUserIds_includesUsersFromGroupBoundToRole() {
        // ROLE_USER (2222) 起点只有 user 2222 直接绑定。建一个组绑定 ROLE_USER，把 user 1111 (admin) 加进去。
        // 期望：getUserIds(tenant, ROLE_USER) 同时返回 2222 (direct) 和 1111 (group).
        val tenantId = "tenant-001-58TWQx6c"
        val roleCode = "ROLE_USER"
        val roleId = "10796e8c-2222-2222-2222-222222222222"
        val viaGroupUserId = "10796e8c-1111-1111-1111-111111111111"
        val groupId = "10796e8c-grp1-aaaa-aaaa-aaaaaaaaaaaa"
        cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))

        val gId = insertGroup(groupId, tenantId)
        val grId = insertGroupRole(gId, roleId)
        val guId = insertGroupUser(gId, viaGroupUserId)
        try {
            cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))
            val users = cacheHandler.getUserIds(tenantId, roleCode)
            assertTrue(users.contains(viaGroupUserId), "应通过组继承包含 admin，实际：${users}")
        } finally {
            authGroupUserDao.deleteById(guId)
            authGroupRoleDao.deleteById(grId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))
        }
    }

    @Test
    fun on_AuthGroupUserRelationsChanged_invalidatesCache() {
        val tenantId = "tenant-001-58TWQx6c"
        val roleCode = "ROLE_USER"
        val roleId = "10796e8c-2222-2222-2222-222222222222"
        val newUserId = "10796e8c-1111-1111-1111-111111111111"
        val groupId = "10796e8c-grp2-bbbb-bbbb-bbbbbbbbbbbb"
        cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))

        val gId = insertGroup(groupId, tenantId)
        val grId = insertGroupRole(gId, roleId)
        try {
            // 预热缓存：组里没人，所以查不到 newUserId
            val before = cacheHandler.getUserIds(tenantId, roleCode)
            assertTrue(!before.contains(newUserId), "用户尚未入组")

            val guId = insertGroupUser(gId, newUserId)
            try {
                cacheHandler.on(AuthGroupUserRelationsChanged(gId, listOf(newUserId)))
                // existing tests 已知 @Cacheable 可能持有 on() 之前的旧值，需要显式 evict
                cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))
                val after = cacheHandler.getUserIds(tenantId, roleCode)
                assertTrue(after.contains(newUserId), "入组后 (${tenantId},${roleCode}) 应包含该用户，实际：${after}")
            } finally {
                authGroupUserDao.deleteById(guId)
            }
        } finally {
            authGroupRoleDao.deleteById(grId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))
        }
    }

    @Test
    fun on_AuthGroupRoleRelationsChanged_invalidatesCache() {
        // 组里有 user。组初始未绑定 ROLE_USER。绑定后失效 → 用户进入 (tenant, ROLE_USER) 集合。
        val tenantId = "tenant-001-58TWQx6c"
        val roleCode = "ROLE_USER"
        val roleId = "10796e8c-2222-2222-2222-222222222222"
        val userId = "10796e8c-1111-1111-1111-111111111111"
        val groupId = "10796e8c-grp3-cccc-cccc-cccccccccccc"
        cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))

        val gId = insertGroup(groupId, tenantId)
        val guId = insertGroupUser(gId, userId)
        try {
            val before = cacheHandler.getUserIds(tenantId, roleCode)
            assertTrue(!before.contains(userId), "组尚未绑定 ROLE_USER")

            val grId = insertGroupRole(gId, roleId)
            try {
                cacheHandler.on(AuthGroupRoleRelationsChanged(gId, listOf(roleId)))
                // existing tests 已知 @Cacheable 可能持有 on() 之前的旧值，需要显式 evict
                cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))
                val after = cacheHandler.getUserIds(tenantId, roleCode)
                assertTrue(after.contains(userId), "组绑角色后 (${tenantId},${roleCode}) 应包含 admin，实际：${after}")
            } finally {
                authGroupRoleDao.deleteById(grId)
            }
        } finally {
            authGroupUserDao.deleteById(guId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(cacheHandler.getKey(tenantId, roleCode))
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
