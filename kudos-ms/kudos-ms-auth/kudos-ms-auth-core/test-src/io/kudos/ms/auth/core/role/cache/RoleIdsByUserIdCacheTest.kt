package io.kudos.ms.auth.core.role.cache

import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.group.model.po.AuthGroup
import io.kudos.ms.auth.core.group.model.po.AuthGroupRole
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for RoleIdsByUserIdCacheHandler
 *
 * 测试数据来源：`RoleIdsByUserIdCacheTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class RoleIdsByUserIdCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: RoleIdsByUserIdCache

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var authGroupDao: AuthGroupDao

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Test
    fun getRoleIds() {
        // 存在的用户ID，有一个角色
        var userId = "88207878-1111-1111-1111-111111111111"
        val roleIds1 = cacheHandler.getRoleIds(userId)
        val roleIds2 = cacheHandler.getRoleIds(userId)
        assertTrue(roleIds1.isNotEmpty(), "用户${userId}应该有角色ID列表")
        assertEquals(roleIds1, roleIds2, "两次调用应该返回相同的结果（缓存验证）")
        // 验证角色ID：用户11111111有角色ROLE_ADMIN
        assertEquals(1, roleIds1.size, "用户${userId}应该有1个角色ID")
        assertTrue(roleIds1.contains("88207878-1111-1111-1111-111111111111"), "应该包含ROLE_ADMIN的角色ID，实际返回：${roleIds1}")

        // 存在的用户ID，有多个角色
        userId = "88207878-2222-2222-2222-222222222222"
        val roleIds3 = cacheHandler.getRoleIds(userId)
        val roleIds4 = cacheHandler.getRoleIds(userId)
        assertTrue(roleIds3.isNotEmpty(), "用户${userId}应该有角色ID列表")
        assertEquals(roleIds3, roleIds4, "两次调用应该返回相同的结果（缓存验证）")
        // 用户22222222有两个角色（ROLE_USER和ROLE_ADMIN）
        assertEquals(
            2,
            roleIds3.size,
            "用户${userId}应该有2个角色ID，实际返回：${roleIds3}"
        )
        assertTrue(roleIds3.contains("88207878-1111-1111-1111-111111111111"), "应该包含ROLE_ADMIN的角色ID")
        assertTrue(roleIds3.contains("88207878-2222-2222-2222-222222222222"), "应该包含ROLE_USER的角色ID")

        // 存在的用户ID，但没有角色
        userId = "88207878-3333-3333-3333-333333333333"
        val roleIds5 = cacheHandler.getRoleIds(userId)
        assertTrue(roleIds5.isEmpty(), "用户${userId}没有角色，应该返回空列表")

        // 不存在的用户ID
        userId = "no_exist_user_id"
        val roleIds6 = cacheHandler.getRoleIds(userId)
        assertTrue(roleIds6.isEmpty(), "不存在的用户ID应该返回空列表")
    }

    @Test
    fun syncOnRoleUserChange() {
        val userId = "88207878-3333-3333-3333-333333333333"
        val roleId = "88207878-2222-2222-2222-222222222222" // ROLE_USER 的 ID
        
        // 先获取一次，记录初始角色数量
        val roleIdsBefore = cacheHandler.getRoleIds(userId)
        val beforeSize = roleIdsBefore.size
        
        // 插入一条新的用户-角色关系记录
        val authRoleUser = AuthRoleUser.Companion().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)
        
        // 同步缓存（模拟用户-角色关系变更）
        cacheHandler.syncOnRoleUserChange(userId)
        
        // 验证缓存已被清除并重新加载，应该包含新插入的角色
        val roleIdsAfter = cacheHandler.getRoleIds(userId)
        assertTrue(roleIdsAfter.size > beforeSize, "同步后应该包含新插入的角色ID")
        assertTrue(roleIdsAfter.contains(roleId), "应该包含新插入的角色ID")
        
        // 清理测试数据
        authRoleUserDao.deleteById(id)
    }

    @Test
    fun syncOnBatchRoleUserChange() {
        val userId1 = "88207878-3333-3333-3333-333333333333"
        val userId2 = "88207878-3333-3333-3333-333333333333"
        val roleId1 = "88207878-1111-1111-1111-111111111111" // ROLE_ADMIN 的 ID
        val roleId2 = "88207878-2222-2222-2222-222222222222" // ROLE_USER 的 ID
        val userIds = listOf(userId1, userId2)
        
        // 先获取一次，记录初始角色数量
        val roleIds1Before = cacheHandler.getRoleIds(userId1)
        val beforeSize = roleIds1Before.size
        
        // 批量插入用户-角色关系记录
        val authRoleUser1 = AuthRoleUser.Companion().apply {
            this.roleId = roleId1
            this.userId = userId1
        }
        val id1 = authRoleUserDao.insert(authRoleUser1)
        
        val authRoleUser2 = AuthRoleUser.Companion().apply {
            this.roleId = roleId2
            this.userId = userId2
        }
        val id2 = authRoleUserDao.insert(authRoleUser2)
        
        // 批量同步缓存（模拟批量用户-角色关系变更）
        cacheHandler.syncOnBatchRoleUserChange(userIds)
        
        // 验证缓存已被清除并重新加载，应该包含新插入的角色
        val roleIds1After = cacheHandler.getRoleIds(userId1)
        assertTrue(roleIds1After.size > beforeSize, "同步后应该包含新插入的角色ID")
        
        // 清理测试数据
        authRoleUserDao.deleteById(id1)
        authRoleUserDao.deleteById(id2)
    }

    @Test
    fun syncOnUserDelete() {
        val userId = "88207878-3333-3333-3333-333333333333"
        val roleId = "88207878-1111-1111-1111-111111111111" // ROLE_ADMIN 的 ID
        
        // 先插入一条用户-角色关系记录
        val authRoleUser = AuthRoleUser.Companion().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)
        
        // 先同步缓存，确保缓存中有新插入的数据
        cacheHandler.syncOnRoleUserChange(userId)
        
        // 获取一次，确保缓存中有数据
        val roleIdsBefore = cacheHandler.getRoleIds(userId)
        assertTrue(roleIdsBefore.contains(roleId), "新插入的角色关系应该在缓存中")
        
        // 删除数据库记录（模拟用户删除或用户-角色关系删除）
        val deleteSuccess = authRoleUserDao.deleteById(id)
        assertTrue(deleteSuccess, "删除应该成功")
        
        // 直接驱动事件 listener（AFTER_COMMIT 在 @Transactional 测试中不会触发，故直接调用 on(...)）
        cacheHandler.on(UserAccountDeleted(userId, tenantId = "tenant-x", username = "user-x"))
        
        // 验证缓存已被清除，重新获取应该不包含已删除的角色
        val roleIdsAfter = cacheHandler.getRoleIds(userId)
        assertTrue(!roleIdsAfter.contains(roleId), "删除后，缓存应该被清除，不应该包含已删除的角色ID")
    }

    // -------------------- group 路径（user → group → role）的覆盖 --------------------

    @Test
    fun getRoleIds_includesGroupInheritedRoles() {
        // lisi（用户 3333）没有直接绑定任何角色，但加入 group → group 关联 ROLE_USER (2222)。
        // 期望：getRoleIds 返回包含 ROLE_USER。
        val userId = "88207878-3333-3333-3333-333333333333"
        val roleId = "88207878-2222-2222-2222-222222222222" // ROLE_USER
        val groupId = "88207878-grp1-aaaa-aaaa-aaaaaaaaaaaa"
        cacheHandler.evict(userId)
        val (gId, guId, grId) = bindUserToRoleViaGroup(groupId, userId, roleId)
        try {
            cacheHandler.evict(userId)
            val roleIds = cacheHandler.getRoleIds(userId)
            assertTrue(
                roleIds.contains(roleId),
                "用户${userId}通过组${groupId}应继承角色${roleId}，实际返回：${roleIds}",
            )
        } finally {
            authGroupRoleDao.deleteById(grId)
            authGroupUserDao.deleteById(guId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(userId)
        }
    }

    @Test
    fun on_AuthGroupUserRelationsChanged_invalidatesCache() {
        // 起点：lisi 直接绑定 ROLE_USER → 缓存包含 ROLE_USER。
        // 触发：lisi 被加入一个绑定了 ROLE_ADMIN 的 group，并 fire AuthGroupUserRelationsChanged。
        // 预期：缓存重算后包含 ROLE_USER (direct) 和 ROLE_ADMIN (via group)。
        val userId = "88207878-3333-3333-3333-333333333333"
        val directRoleId = "88207878-2222-2222-2222-222222222222" // ROLE_USER
        val groupRoleId = "88207878-1111-1111-1111-111111111111" // ROLE_ADMIN
        val groupId = "88207878-grp2-bbbb-bbbb-bbbbbbbbbbbb"

        val directRel = AuthRoleUser.Companion().apply {
            this.roleId = directRoleId
            this.userId = userId
        }
        val directRelId = authRoleUserDao.insert(directRel)
        cacheHandler.evict(userId)
        try {
            // 预热缓存：只看到直接角色
            val before = cacheHandler.getRoleIds(userId)
            assertTrue(before.contains(directRoleId), "起点缓存应包含直接角色")
            assertTrue(!before.contains(groupRoleId), "此时组继承尚未生效")

            // 加入 group → role 绑定
            val (gId, guId, grId) = bindUserToRoleViaGroup(groupId, userId, groupRoleId)
            try {
                // 直接驱动 listener
                cacheHandler.on(AuthGroupUserRelationsChanged(groupId, listOf(userId)))
                val after = cacheHandler.getRoleIds(userId)
                assertTrue(after.contains(directRoleId), "失效重算后仍应包含直接角色")
                assertTrue(after.contains(groupRoleId), "失效重算后应包含组继承角色")
            } finally {
                authGroupRoleDao.deleteById(grId)
                authGroupUserDao.deleteById(guId)
                authGroupDao.deleteById(gId)
            }
        } finally {
            authRoleUserDao.deleteById(directRelId)
            cacheHandler.evict(userId)
        }
    }

    @Test
    fun on_AuthGroupRoleRelationsChanged_invalidatesCache() {
        // user 已经在组里，组先绑定 ROLE_USER；缓存里看到 ROLE_USER。
        // 组再加绑 ROLE_ADMIN，fire AuthGroupRoleRelationsChanged → 缓存应同时含两个角色。
        val userId = "88207878-3333-3333-3333-333333333333"
        val initialRoleId = "88207878-2222-2222-2222-222222222222" // ROLE_USER
        val addedRoleId = "88207878-1111-1111-1111-111111111111" // ROLE_ADMIN
        val groupId = "88207878-grp3-cccc-cccc-cccccccccccc"

        // 创建 group + 入组 + group 关联 ROLE_USER
        val (gId, guId, grId) = bindUserToRoleViaGroup(groupId, userId, initialRoleId)
        cacheHandler.evict(userId)
        try {
            val before = cacheHandler.getRoleIds(userId)
            assertTrue(before.contains(initialRoleId), "起点缓存应通过组继承到 ROLE_USER")
            assertTrue(!before.contains(addedRoleId), "ROLE_ADMIN 尚未绑定到组")

            // 给同组加绑 ROLE_ADMIN —— 注意 bindUserToRoleViaGroup 返回的 gId 才是真实 DB id；
            // dao.insert 不接受手设 id，必须用 gId 而不是本地预设的 groupId 字符串。
            val newGr = AuthGroupRole.Companion().apply {
                this.groupId = gId
                this.roleId = addedRoleId
            }
            val newGrId = authGroupRoleDao.insert(newGr)
            try {
                cacheHandler.on(AuthGroupRoleRelationsChanged(gId, listOf(addedRoleId)))
                cacheHandler.evict(userId)
                val after = cacheHandler.getRoleIds(userId)
                assertTrue(after.contains(initialRoleId), "失效重算后仍应包含原组角色")
                assertTrue(after.contains(addedRoleId), "失效重算后应包含新加的组角色")
            } finally {
                authGroupRoleDao.deleteById(newGrId)
            }
        } finally {
            authGroupRoleDao.deleteById(grId)
            authGroupUserDao.deleteById(guId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(userId)
        }
    }

    /**
     * 测试辅助：创建一个 group，把 user 加进去，把 role 绑到组上。返回 (groupId, groupUserId, groupRoleId)
     * 用于 finally 清理。tenantId / subsysCode 用与 SQL fixture 同租户 + ams 子系统保持现实感。
     */
    private fun bindUserToRoleViaGroup(
        groupId: String,
        userId: String,
        roleId: String,
    ): Triple<String, String, String> {
        val group = AuthGroup.Companion().apply {
            this.id = groupId
            this.code = "GRP_${groupId.takeLast(4)}"
            this.name = "test group ${groupId.takeLast(4)}"
            this.tenantId = "tenant-001-Gv4Pb40w"
            this.subsysCode = "ams"
            this.active = true
            this.builtIn = false
        }
        val gId = authGroupDao.insert(group)
        val gu = AuthGroupUser.Companion().apply {
            this.groupId = gId
            this.userId = userId
        }
        val guId = authGroupUserDao.insert(gu)
        val gr = AuthGroupRole.Companion().apply {
            this.groupId = gId
            this.roleId = roleId
        }
        val grId = authGroupRoleDao.insert(gr)
        return Triple(gId, guId, grId)
    }

}
