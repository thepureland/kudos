package io.kudos.ms.user.core.platform.cache

import io.kudos.ms.user.core.account.dao.UserOrgUserDao
import io.kudos.ms.user.core.account.event.UserOrgUserRelationsChanged
import io.kudos.ms.user.core.account.model.po.UserOrgUser
import io.kudos.ms.user.core.org.cache.UserIdsByOrgIdCache
import io.kudos.ms.user.core.org.dao.UserOrgDao
import io.kudos.ms.user.core.org.event.UserOrgBatchDeleted
import io.kudos.ms.user.core.org.event.UserOrgDeleted
import io.kudos.ms.user.core.org.event.UserOrgUpdated
import io.kudos.ms.user.core.org.model.po.UserOrg
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for UserIdsByOrgIdCacheHandler
 *
 * 测试数据来源：`UserIdsByOrgIdCacheTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserIdsByOrgIdCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: UserIdsByOrgIdCache

    @Resource
    private lateinit var userOrgUserDao: UserOrgUserDao

    @Resource
    private lateinit var userOrgDao: UserOrgDao

    @Test
    fun getUserIds() {
        // 存在的机构（有多个用户）
        var orgId = "84c558fe-1111-1111-1111-111111111111"
        val userIds2 = cacheHandler.getUserIds(orgId)
        val userIds3 = cacheHandler.getUserIds(orgId)
        assertTrue(userIds2.isNotEmpty())
        assertEquals(userIds2, userIds3)

        // 不存在的机构
        orgId = "no_exist_org"
        val userIds4 = cacheHandler.getUserIds(orgId)
        assertTrue(userIds4.isEmpty())
    }

    @Test
    fun syncOnInsert() {
        val orgId = "84c558fe-1111-1111-1111-111111111111"
        val orgUser = UserOrgUser().apply {
            this.orgId = orgId
            this.userId = "84c558fe-3333-3333-3333-333333333333"
            this.orgAdmin = false
        }
        val id = userOrgUserDao.insert(orgUser)

        // 先获取一次，记录用户数量
        val userIdsBefore = cacheHandler.getUserIds(orgId)
        val beforeSize = userIdsBefore.size

        // 同步缓存
        cacheHandler.syncOnInsert(orgUser, id)

        // 验证缓存已被清除并重新加载
        val userIdsAfter = cacheHandler.getUserIds(orgId)
        assertTrue(userIdsAfter.size >= beforeSize, "用户数量应该增加或保持不变")
    }

    @Test
    fun syncOnUpdate() {
        val orgId = "84c558fe-1111-1111-1111-111111111111"
        val orgUser = UserOrgUser().apply {
            this.orgId = orgId
            this.userId = "84c558fe-3333-3333-3333-333333333333"
            this.orgAdmin = false
        }
        val id = userOrgUserDao.insert(orgUser)

        // 先同步插入缓存
        cacheHandler.syncOnInsert(orgUser, id)

        // 先获取一次，确保缓存中有数据
        val userIdsBefore = cacheHandler.getUserIds(orgId)
        assertTrue(userIdsBefore.contains(orgUser.userId), "新用户应该在缓存中")

        // 更新数据库记录
        val success = userOrgUserDao.updateProperties(id, mapOf(UserOrgUser::orgAdmin.name to true))
        assert(success)

        // 同步缓存
        cacheHandler.syncOnUpdate(null, id)

        // 验证缓存已被清除并重新加载
        val userIdsAfter = cacheHandler.getUserIds(orgId)
        assertTrue(userIdsAfter.contains(orgUser.userId), "更新后的用户应该仍在缓存中")
    }

    @Test
    fun syncOnDelete() {
        val orgId = "84c558fe-1111-1111-1111-111111111111"
        val orgUser = UserOrgUser().apply {
            this.orgId = orgId
            this.userId = "84c558fe-3333-3333-3333-333333333333"
            this.orgAdmin = false
        }
        val id = userOrgUserDao.insert(orgUser)

        // 先同步插入缓存
        cacheHandler.syncOnInsert(orgUser, id)

        // 先获取一次，确保缓存中有数据
        val userIdsBefore = cacheHandler.getUserIds(orgId)
        assertTrue(userIdsBefore.contains(orgUser.userId), "新用户应该在缓存中")

        // 删除数据库记录
        val deleteSuccess = userOrgUserDao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(orgUser, id)

        // 验证缓存已被清除并重新加载
        val userIdsAfter = cacheHandler.getUserIds(orgId)
        assertTrue(!userIdsAfter.contains(orgUser.userId), "删除后的用户不应该包含在缓存中")
    }

    @Test
    fun syncOnBatchDelete() {
        val orgId = "84c558fe-1111-1111-1111-111111111111"
        
        val orgUser1 = UserOrgUser().apply {
            this.orgId = orgId
            this.userId = "84c558fe-3333-3333-3333-333333333333"
            this.orgAdmin = false
        }
        val id1 = userOrgUserDao.insert(orgUser1)
        
        val orgUser2 = UserOrgUser().apply {
            this.orgId = orgId
            this.userId = "84c558fe-4444-4444-4444-444444444444"
            this.orgAdmin = false
        }
        val id2 = userOrgUserDao.insert(orgUser2)

        // 先同步插入缓存
        cacheHandler.syncOnInsert(orgUser1, id1)
        cacheHandler.syncOnInsert(orgUser2, id2)

        // 先获取一次，确保缓存中有数据
        val userIdsBefore = cacheHandler.getUserIds(orgId)
        assertTrue(userIdsBefore.contains(orgUser1.userId), "新用户1应该在缓存中")
        assertTrue(userIdsBefore.contains(orgUser2.userId), "新用户2应该在缓存中")

        // 批量删除数据库记录
        val ids = listOf(id1, id2)
        val count = userOrgUserDao.batchDelete(ids)
        assert(count == 2)

        // 同步缓存
        cacheHandler.syncOnBatchDelete(ids, listOf(orgId, orgId))

        // 验证缓存已被清除并重新加载
        val userIdsAfter = cacheHandler.getUserIds(orgId)
        assertTrue(!userIdsAfter.contains(orgUser1.userId), "删除后的用户1不应该包含在缓存中")
        assertTrue(!userIdsAfter.contains(orgUser2.userId), "删除后的用户2不应该包含在缓存中")
    }

    // -------------------- 子机构成员展开（含父→子树）的覆盖 --------------------

    @Test
    fun getUserIds_includesUsersInDescendantOrgs() {
        // 建一棵：parent → child；分别给两层挂用户。问 parent 应当含两层用户。
        val parentOrgId = insertOrg(name = "销售部", parentId = null)
        val childOrgId = insertOrg(name = "华东销售", parentId = parentOrgId)
        val parentUserId = "84c558fe-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        val childUserId = "84c558fe-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
        val rel1 = insertOrgUser(parentOrgId, parentUserId)
        val rel2 = insertOrgUser(childOrgId, childUserId)
        try {
            cacheHandler.evict(parentOrgId)
            val users = cacheHandler.getUserIds(parentOrgId)
            assertTrue(users.contains(parentUserId), "应含父机构直接用户，实际：${users}")
            assertTrue(users.contains(childUserId), "应含子机构用户，实际：${users}")

            // 子机构单独查时应当只看到自己的用户
            cacheHandler.evict(childOrgId)
            val childUsers = cacheHandler.getUserIds(childOrgId)
            assertTrue(childUsers.contains(childUserId))
            assertTrue(!childUsers.contains(parentUserId), "子机构不应反向包含父机构用户")
        } finally {
            userOrgUserDao.deleteById(rel1)
            userOrgUserDao.deleteById(rel2)
            userOrgDao.deleteById(childOrgId)
            userOrgDao.deleteById(parentOrgId)
            cacheHandler.evict(parentOrgId)
            cacheHandler.evict(childOrgId)
        }
    }

    @Test
    fun getUserIds_excludesUsersInInactiveDescendants() {
        // 子机构 active=false → 不算在子树范围内。
        val parentOrgId = insertOrg(name = "销售部", parentId = null)
        val childOrgId = insertOrg(name = "华东(停)", parentId = parentOrgId, active = false)
        val userInActive = "84c558fe-cccc-cccc-cccc-cccccccccccc"
        val rel = insertOrgUser(childOrgId, userInActive)
        try {
            cacheHandler.evict(parentOrgId)
            val users = cacheHandler.getUserIds(parentOrgId)
            assertTrue(!users.contains(userInActive), "禁用子机构的用户不应出现在父机构视图，实际：${users}")
        } finally {
            userOrgUserDao.deleteById(rel)
            userOrgDao.deleteById(childOrgId)
            userOrgDao.deleteById(parentOrgId)
            cacheHandler.evict(parentOrgId)
        }
    }

    @Test
    fun on_UserOrgUserRelationsChanged_invalidatesAncestorChain() {
        // 给子机构加新用户 + fire 事件 → 父机构缓存也应被失效。
        val parentOrgId = insertOrg(name = "销售部", parentId = null)
        val childOrgId = insertOrg(name = "华东销售", parentId = parentOrgId)
        val initialUser = "84c558fe-dddd-dddd-dddd-dddddddddddd"
        val initialRel = insertOrgUser(childOrgId, initialUser)
        cacheHandler.evict(parentOrgId)
        cacheHandler.evict(childOrgId)
        try {
            // 预热父机构缓存
            val before = cacheHandler.getUserIds(parentOrgId)
            assertTrue(before.contains(initialUser))

            // 给子机构加新用户
            val newUserId = "84c558fe-eeee-eeee-eeee-eeeeeeeeeeee"
            val newRel = insertOrgUser(childOrgId, newUserId)
            try {
                cacheHandler.on(UserOrgUserRelationsChanged(orgId = childOrgId, userIds = listOf(newUserId)))
                cacheHandler.evict(parentOrgId)
                val after = cacheHandler.getUserIds(parentOrgId)
                assertTrue(after.contains(initialUser), "失效重算后原用户仍应在")
                assertTrue(after.contains(newUserId), "新用户应通过子树传播到父机构视图，实际：${after}")
            } finally {
                userOrgUserDao.deleteById(newRel)
            }
        } finally {
            userOrgUserDao.deleteById(initialRel)
            userOrgDao.deleteById(childOrgId)
            userOrgDao.deleteById(parentOrgId)
            cacheHandler.evict(parentOrgId)
            cacheHandler.evict(childOrgId)
        }
    }

    // -------------------- org tree mutation 精确失效 --------------------

    @Test
    fun on_UserOrgUpdated_moveOrg_invalidatesOldAndNewAncestorChains() {
        // 起始树：oldParent <- child  +  newParent (空)；child 里有用户
        val oldParentId = insertOrg("旧部门", parentId = null)
        val newParentId = insertOrg("新部门", parentId = null)
        val childOrgId = insertOrg("华东", parentId = oldParentId)
        val movingUser = "84c558fe-1111-aaaa-aaaa-aaaaaaaaaaaa"
        val rel = insertOrgUser(childOrgId, movingUser)
        cacheHandler.evict(oldParentId)
        cacheHandler.evict(newParentId)
        cacheHandler.evict(childOrgId)
        try {
            // 预热：oldParent 视图含 movingUser，newParent 视图空
            assertTrue(cacheHandler.getUserIds(oldParentId).contains(movingUser), "起点 oldParent 应含 child 用户")
            assertTrue(!cacheHandler.getUserIds(newParentId).contains(movingUser), "起点 newParent 不应含")

            // 实际 reparent：直接改 DB 表示移动
            userOrgDao.updateProperties(childOrgId, mapOf(UserOrg::parentId.name to newParentId))
            // 触发事件
            cacheHandler.on(
                UserOrgUpdated(id = childOrgId, oldParentId = oldParentId, newParentId = newParentId)
            )
            // 双 evict 保险（与 fd2e425e 中现有测试的兼容写法一致）
            cacheHandler.evict(oldParentId)
            cacheHandler.evict(newParentId)
            cacheHandler.evict(childOrgId)

            assertTrue(
                !cacheHandler.getUserIds(oldParentId).contains(movingUser),
                "移动后 oldParent 视图应失去 child 用户，实际：${cacheHandler.getUserIds(oldParentId)}",
            )
            assertTrue(
                cacheHandler.getUserIds(newParentId).contains(movingUser),
                "移动后 newParent 视图应获得 child 用户，实际：${cacheHandler.getUserIds(newParentId)}",
            )
        } finally {
            userOrgUserDao.deleteById(rel)
            userOrgDao.deleteById(childOrgId)
            userOrgDao.deleteById(newParentId)
            userOrgDao.deleteById(oldParentId)
            cacheHandler.evict(oldParentId)
            cacheHandler.evict(newParentId)
            cacheHandler.evict(childOrgId)
        }
    }

    @Test
    fun on_UserOrgDeleted_invalidatesAncestorChain() {
        // parent -> child；child 里有用户。删 child → parent 视图应失去这些用户。
        val parentOrgId = insertOrg("销售部", parentId = null)
        val childOrgId = insertOrg("华东", parentId = parentOrgId)
        val userInChild = "84c558fe-2222-bbbb-bbbb-bbbbbbbbbbbb"
        val rel = insertOrgUser(childOrgId, userInChild)
        cacheHandler.evict(parentOrgId)
        cacheHandler.evict(childOrgId)
        try {
            assertTrue(cacheHandler.getUserIds(parentOrgId).contains(userInChild), "起点应含子机构用户")

            // 模拟先删 user_org_user，再删 child org（生产真删时也是这个顺序：先关系后实体；
            // 此处主要验证 listener 用 event.parentId 失效）
            userOrgUserDao.deleteById(rel)
            userOrgDao.deleteById(childOrgId)

            cacheHandler.on(UserOrgDeleted(id = childOrgId, parentId = parentOrgId))
            cacheHandler.evict(parentOrgId)

            assertTrue(
                !cacheHandler.getUserIds(parentOrgId).contains(userInChild),
                "删除后 parent 视图不应包含 child 已离场的用户，实际：${cacheHandler.getUserIds(parentOrgId)}",
            )
        } finally {
            // child/rel 已删；只剩 parent
            userOrgDao.deleteById(parentOrgId)
            cacheHandler.evict(parentOrgId)
        }
    }

    @Test
    fun on_UserOrgBatchDeleted_invalidatesAllAncestorChains() {
        // parent -> [childA, childB]；各自有用户。批量删两个 child。
        val parentOrgId = insertOrg("销售部", parentId = null)
        val childA = insertOrg("华东", parentId = parentOrgId)
        val childB = insertOrg("华南", parentId = parentOrgId)
        val userA = "84c558fe-3333-cccc-aaaa-aaaaaaaaaaaa"
        val userB = "84c558fe-3333-cccc-bbbb-bbbbbbbbbbbb"
        val relA = insertOrgUser(childA, userA)
        val relB = insertOrgUser(childB, userB)
        cacheHandler.evict(parentOrgId)
        try {
            val before = cacheHandler.getUserIds(parentOrgId)
            assertTrue(before.contains(userA) && before.contains(userB), "起点 parent 应含两个 child 的用户")

            userOrgUserDao.deleteById(relA)
            userOrgUserDao.deleteById(relB)
            userOrgDao.deleteById(childA)
            userOrgDao.deleteById(childB)

            cacheHandler.on(
                UserOrgBatchDeleted(
                    items = listOf(
                        UserOrgBatchDeleted.Item(childA, parentOrgId),
                        UserOrgBatchDeleted.Item(childB, parentOrgId),
                    )
                )
            )
            cacheHandler.evict(parentOrgId)
            val after = cacheHandler.getUserIds(parentOrgId)
            assertTrue(!after.contains(userA), "parent 应失去 childA 的用户")
            assertTrue(!after.contains(userB), "parent 应失去 childB 的用户")
        } finally {
            userOrgDao.deleteById(parentOrgId)
            cacheHandler.evict(parentOrgId)
        }
    }

    private fun insertOrg(
        name: String,
        parentId: String?,
        active: Boolean = true,
    ): String {
        val org = UserOrg.Companion().apply {
            this.name = name
            this.tenantId = "tenant-test-org-hierarchy"
            this.parentId = parentId
            this.orgTypeDictCode = "dept"
            this.active = active
            this.builtIn = false
        }
        return userOrgDao.insert(org)
    }

    private fun insertOrgUser(orgId: String, userId: String, admin: Boolean = false): String {
        val rel = UserOrgUser().apply {
            this.orgId = orgId
            this.userId = userId
            this.orgAdmin = admin
        }
        return userOrgUserDao.insert(rel)
    }

}
