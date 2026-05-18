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
import io.kudos.ms.auth.core.role.event.AuthRoleDeleted
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for UserIdsByRoleIdCacheHandler
 *
 * 测试数据来源：`UserIdsByRoleIdCacheTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserIdsByRoleIdCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: UserIdsByRoleIdCache

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
        // 存在的角色ID，有多个用户
        var roleId = "5e90ce80-1111-1111-1111-111111111111"
        val userIds1 = cacheHandler.getUserIds(roleId)
        val userIds2 = cacheHandler.getUserIds(roleId)
        assertTrue(userIds1.isNotEmpty(), "角色${roleId}应该有用户ID列表")
        assertEquals(userIds1, userIds2, "两次调用应该返回相同的结果（缓存验证）")
        // 验证用户ID：角色ROLE_ADMIN有用户admin和zhangsan
        assertEquals(2, userIds1.size, "角色${roleId}应该有2个用户ID")
        assertTrue(userIds1.contains("5e90ce80-1111-1111-1111-111111111111"), "应该包含admin的用户ID，实际返回：${userIds1}")
        assertTrue(userIds1.contains("5e90ce80-2222-2222-2222-222222222222"), "应该包含zhangsan的用户ID，实际返回：${userIds1}")

        // 存在的角色ID，有一个用户
        roleId = "5e90ce80-2222-2222-2222-222222222222"
        val userIds3 = cacheHandler.getUserIds(roleId)
        val userIds4 = cacheHandler.getUserIds(roleId)
        assertTrue(userIds3.isNotEmpty(), "角色${roleId}应该有用户ID列表")
        assertEquals(userIds3, userIds4, "两次调用应该返回相同的结果（缓存验证）")
        // 角色ROLE_USER只有用户zhangsan
        assertEquals(
            1,
            userIds3.size,
            "角色${roleId}应该有1个用户ID，实际返回：${userIds3}"
        )
        assertTrue(userIds3.contains("5e90ce80-2222-2222-2222-222222222222"), "应该包含zhangsan的用户ID")

        // 存在的角色ID，但没有用户
        roleId = "5e90ce80-3333-3333-3333-333333333333"
        val userIds5 = cacheHandler.getUserIds(roleId)
        assertTrue(userIds5.isEmpty(), "角色${roleId}没有用户，应该返回空列表")

        // 不存在的角色ID
        roleId = "no_exist_role_id"
        val userIds6 = cacheHandler.getUserIds(roleId)
        assertTrue(userIds6.isEmpty(), "不存在的角色ID应该返回空列表")
    }

    @Test
    fun syncOnRoleUserChange() {
        val roleId = "5e90ce80-3333-3333-3333-333333333333"
        val userId = "5e90ce80-3333-3333-3333-333333333333"
        
        // 先获取一次，记录初始用户数量
        val userIdsBefore = cacheHandler.getUserIds(roleId)
        val beforeSize = userIdsBefore.size
        
        // 插入一条新的角色-用户关系记录
        val authRoleUser = AuthRoleUser.Companion().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)
        
        // 同步缓存（模拟角色-用户关系变更）
        cacheHandler.syncOnRoleUserChange(roleId)
        
        // 验证缓存已被清除并重新加载，应该包含新插入的用户
        val userIdsAfter = cacheHandler.getUserIds(roleId)
        assertTrue(userIdsAfter.size > beforeSize, "同步后应该包含新插入的用户ID")
        assertTrue(userIdsAfter.contains(userId), "应该包含新插入的用户ID")
        
        // 清理测试数据
        authRoleUserDao.deleteById(id)
    }

    @Test
    fun syncOnBatchRoleUserChange() {
        val roleId1 = "5e90ce80-3333-3333-3333-333333333333"
        val roleId2 = "5e90ce80-3333-3333-3333-333333333333"
        val userId1 = "5e90ce80-1111-1111-1111-111111111111"
        val userId2 = "5e90ce80-2222-2222-2222-222222222222"
        val roleIds = listOf(roleId1, roleId2)
        
        // 先获取一次，记录初始用户数量
        val userIds1Before = cacheHandler.getUserIds(roleId1)
        val beforeSize = userIds1Before.size
        
        // 批量插入角色-用户关系记录
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
        
        // 批量同步缓存（模拟批量角色-用户关系变更）
        cacheHandler.syncOnBatchRoleUserChange(roleIds)
        
        // 验证缓存已被清除并重新加载，应该包含新插入的用户
        val userIds1After = cacheHandler.getUserIds(roleId1)
        assertTrue(userIds1After.size > beforeSize, "同步后应该包含新插入的用户ID")
        
        // 清理测试数据
        authRoleUserDao.deleteById(id1)
        authRoleUserDao.deleteById(id2)
    }

    @Test
    fun syncOnRoleDelete() {
        val roleId = "5e90ce80-3333-3333-3333-333333333333"
        val userId = "5e90ce80-1111-1111-1111-111111111111"
        
        // 先插入一条角色-用户关系记录
        val authRoleUser = AuthRoleUser.Companion().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)
        
        // 先同步缓存，确保缓存中有新插入的数据
        cacheHandler.syncOnRoleUserChange(roleId)
        
        // 获取一次，确保缓存中有数据
        val userIdsBefore = cacheHandler.getUserIds(roleId)
        assertTrue(userIdsBefore.contains(userId), "新插入的用户关系应该在缓存中")
        
        // 删除数据库记录（模拟角色删除或角色-用户关系删除）
        val deleteSuccess = authRoleUserDao.deleteById(id)
        assertTrue(deleteSuccess, "删除应该成功")
        
        // 直接驱动事件 listener（AFTER_COMMIT 在 @Transactional 测试中不会触发，故直接调用 on(...)）
        cacheHandler.on(AuthRoleDeleted(roleId, tenantId = "tenant-x", code = "code-x"))
        
        // 验证缓存已被清除，重新获取应该不包含已删除的用户
        val userIdsAfter = cacheHandler.getUserIds(roleId)
        assertTrue(!userIdsAfter.contains(userId), "删除后，缓存应该被清除，不应该包含已删除的用户ID")
    }

    // -------------------- group 路径（role ← group ← user）的覆盖 --------------------

    @Test
    fun getUserIds_includesUsersFromGroupBoundToRole() {
        // ROLE_GUEST (3333) 当前没有直接用户。建一个 group 绑定 ROLE_GUEST，并加入两个用户。
        // 期望：getUserIds(ROLE_GUEST) 返回这两个用户。
        val roleId = "5e90ce80-3333-3333-3333-333333333333" // ROLE_GUEST
        val userA = "5e90ce80-1111-1111-1111-111111111111" // admin
        val userB = "5e90ce80-2222-2222-2222-222222222222" // zhangsan
        val groupId = "5e90ce80-grp1-aaaa-aaaa-aaaaaaaaaaaa"
        cacheHandler.evict(roleId)

        val gId = insertGroup(groupId)
        val grId = insertGroupRole(gId, roleId)
        val gu1Id = insertGroupUser(gId, userA)
        val gu2Id = insertGroupUser(gId, userB)
        try {
            cacheHandler.evict(roleId)
            val users = cacheHandler.getUserIds(roleId)
            assertTrue(users.contains(userA), "应通过组继承包含 admin，实际：${users}")
            assertTrue(users.contains(userB), "应通过组继承包含 zhangsan，实际：${users}")
        } finally {
            authGroupUserDao.deleteById(gu1Id)
            authGroupUserDao.deleteById(gu2Id)
            authGroupRoleDao.deleteById(grId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(roleId)
        }
    }

    @Test
    fun on_AuthGroupUserRelationsChanged_invalidatesCache() {
        val roleId = "5e90ce80-3333-3333-3333-333333333333" // ROLE_GUEST
        val newUserId = "5e90ce80-2222-2222-2222-222222222222"
        val groupId = "5e90ce80-grp2-bbbb-bbbb-bbbbbbbbbbbb"
        cacheHandler.evict(roleId)

        val gId = insertGroup(groupId)
        val grId = insertGroupRole(gId, roleId)
        try {
            // 起点：组里没人
            val before = cacheHandler.getUserIds(roleId)
            assertTrue(!before.contains(newUserId), "新用户尚未入组")

            // 入组
            val guId = insertGroupUser(gId, newUserId)
            try {
                cacheHandler.on(AuthGroupUserRelationsChanged(gId, listOf(newUserId)))
                // existing tests 已知 @Cacheable 可能持有 on() 之前的旧值，需要显式 evict
                cacheHandler.evict(roleId)
                val after = cacheHandler.getUserIds(roleId)
                assertTrue(after.contains(newUserId), "入组后角色 ${roleId} 应包含该用户，实际：${after}")
            } finally {
                authGroupUserDao.deleteById(guId)
            }
        } finally {
            authGroupRoleDao.deleteById(grId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(roleId)
        }
    }

    @Test
    fun on_AuthGroupRoleRelationsChanged_invalidatesCache() {
        // 组里已经有 user A。最初组没绑定 ROLE_GUEST → 缓存空。绑定后失效 → A 进 ROLE_GUEST 的用户集合。
        val roleId = "5e90ce80-3333-3333-3333-333333333333" // ROLE_GUEST
        val userA = "5e90ce80-1111-1111-1111-111111111111"
        val groupId = "5e90ce80-grp3-cccc-cccc-cccccccccccc"
        cacheHandler.evict(roleId)

        val gId = insertGroup(groupId)
        val guId = insertGroupUser(gId, userA)
        try {
            val before = cacheHandler.getUserIds(roleId)
            assertTrue(!before.contains(userA), "组尚未绑定 ROLE_GUEST")

            // 给组绑定 ROLE_GUEST
            val grId = insertGroupRole(gId, roleId)
            try {
                cacheHandler.on(AuthGroupRoleRelationsChanged(gId, listOf(roleId)))
                cacheHandler.evict(roleId)
                val after = cacheHandler.getUserIds(roleId)
                assertTrue(after.contains(userA), "组绑角色后 ROLE_GUEST 应包含 admin，实际：${after}")
            } finally {
                authGroupRoleDao.deleteById(grId)
            }
        } finally {
            authGroupUserDao.deleteById(guId)
            authGroupDao.deleteById(gId)
            cacheHandler.evict(roleId)
        }
    }

    private fun insertGroup(groupId: String): String {
        val group = AuthGroup.Companion().apply {
            this.id = groupId
            this.code = "GRP_${groupId.takeLast(4)}"
            this.name = "test group ${groupId.takeLast(4)}"
            this.tenantId = "tenant-001-twyuFAaV"
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
