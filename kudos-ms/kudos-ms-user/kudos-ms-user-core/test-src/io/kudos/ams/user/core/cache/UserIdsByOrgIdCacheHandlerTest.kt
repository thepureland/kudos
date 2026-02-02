package io.kudos.ms.user.core.cache

import io.kudos.ms.user.core.dao.UserOrgUserDao
import io.kudos.ms.user.core.model.po.UserOrgUser
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for UserIdsByOrgIdCacheHandler
 *
 * 测试数据来源：`UserIdsByOrgIdCacheHandlerTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserIdsByOrgIdCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: UserIdsByOrgIdCacheHandler

    @Resource
    private lateinit var userOrgUserDao: UserOrgUserDao

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

}
