package io.kudos.ms.user.core.platform.cache

import io.kudos.ms.user.core.account.dao.UserOrgUserDao
import io.kudos.ms.user.core.account.event.UserOrgUserRelationsChanged
import io.kudos.ms.user.core.account.model.po.UserOrgUser
import io.kudos.ms.user.core.org.cache.UserIdsByOrgIdCache
import io.kudos.ms.user.core.org.dao.UserOrgDao
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
