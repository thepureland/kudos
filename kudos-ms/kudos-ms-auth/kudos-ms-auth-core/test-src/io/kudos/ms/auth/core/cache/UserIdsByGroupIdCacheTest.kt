package io.kudos.ms.auth.core.cache

import io.kudos.ms.auth.core.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.model.po.AuthGroupUser
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for UserIdsByGroupIdCacheHandler
 *
 * 测试数据来源：`UserIdsByGroupIdCacheTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserIdsByGroupIdCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: UserIdsByGroupIdCache

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Test
    fun getUserIds() {
        // 存在的用户组ID，有多个用户
        var groupId = "6e90ce80-1111-1111-1111-111111111111"
        val userIds1 = cacheHandler.getUserIds(groupId)
        val userIds2 = cacheHandler.getUserIds(groupId)
        assertTrue(userIds1.isNotEmpty(), "用户组${groupId}应该有用户ID列表")
        assertEquals(userIds1, userIds2, "两次调用应该返回相同的结果（缓存验证）")
        // 验证用户ID：用户组GROUP_ADMIN有用户admin和zhangsan
        assertEquals(2, userIds1.size, "用户组${groupId}应该有2个用户ID")
        assertTrue(userIds1.contains("5e90ce80-1111-1111-1111-111111111111"), "应该包含admin的用户ID，实际返回：${userIds1}")
        assertTrue(userIds1.contains("5e90ce80-2222-2222-2222-222222222222"), "应该包含zhangsan的用户ID，实际返回：${userIds1}")

        // 存在的用户组ID，有一个用户
        groupId = "6e90ce80-2222-2222-2222-222222222222"
        val userIds3 = cacheHandler.getUserIds(groupId)
        val userIds4 = cacheHandler.getUserIds(groupId)
        assertTrue(userIds3.isNotEmpty(), "用户组${groupId}应该有用户ID列表")
        assertEquals(userIds3, userIds4, "两次调用应该返回相同的结果（缓存验证）")
        // 用户组GROUP_USER只有用户zhangsan
        assertEquals(
            1,
            userIds3.size,
            "用户组${groupId}应该有1个用户ID，实际返回：${userIds3}"
        )
        assertTrue(userIds3.contains("5e90ce80-2222-2222-2222-222222222222"), "应该包含zhangsan的用户ID")

        // 存在的用户组ID，但没有用户
        groupId = "6e90ce80-3333-3333-3333-333333333333"
        val userIds5 = cacheHandler.getUserIds(groupId)
        assertTrue(userIds5.isEmpty(), "用户组${groupId}没有用户，应该返回空列表")

        // 不存在的用户组ID
        groupId = "no_exist_group_id"
        val userIds6 = cacheHandler.getUserIds(groupId)
        assertTrue(userIds6.isEmpty(), "不存在的用户组ID应该返回空列表")
    }

    @Test
    fun syncOnGroupUserChange() {
        val groupId = "6e90ce80-3333-3333-3333-333333333333"
        val userId = "5e90ce80-3333-3333-3333-333333333333"

        // 先获取一次，记录初始用户数量
        val userIdsBefore = cacheHandler.getUserIds(groupId)
        val beforeSize = userIdsBefore.size

        // 插入一条新的用户组-用户关系记录
        val authGroupUser = AuthGroupUser.Companion().apply {
            this.groupId = groupId
            this.userId = userId
        }
        val id = authGroupUserDao.insert(authGroupUser)

        // 同步缓存（模拟用户组-用户关系变更）
        cacheHandler.syncOnGroupUserChange(groupId)

        // 验证缓存已被清除并重新加载，应该包含新插入的用户
        val userIdsAfter = cacheHandler.getUserIds(groupId)
        assertTrue(userIdsAfter.size > beforeSize, "同步后应该包含新插入的用户ID")
        assertTrue(userIdsAfter.contains(userId), "应该包含新插入的用户ID")

        // 清理测试数据
        authGroupUserDao.deleteById(id)
    }

    @Test
    fun syncOnBatchGroupUserChange() {
        val groupId1 = "6e90ce80-3333-3333-3333-333333333333"
        val groupId2 = "6e90ce80-3333-3333-3333-333333333333"
        val userId1 = "5e90ce80-1111-1111-1111-111111111111"
        val userId2 = "5e90ce80-2222-2222-2222-222222222222"
        val groupIds = listOf(groupId1, groupId2)

        // 先获取一次，记录初始用户数量
        val userIds1Before = cacheHandler.getUserIds(groupId1)
        val beforeSize = userIds1Before.size

        // 批量插入用户组-用户关系记录
        val authGroupUser1 = AuthGroupUser.Companion().apply {
            this.groupId = groupId1
            this.userId = userId1
        }
        val id1 = authGroupUserDao.insert(authGroupUser1)

        val authGroupUser2 = AuthGroupUser.Companion().apply {
            this.groupId = groupId2
            this.userId = userId2
        }
        val id2 = authGroupUserDao.insert(authGroupUser2)

        // 批量同步缓存（模拟批量用户组-用户关系变更）
        cacheHandler.syncOnBatchGroupUserChange(groupIds)

        // 验证缓存已被清除并重新加载，应该包含新插入的用户
        val userIds1After = cacheHandler.getUserIds(groupId1)
        assertTrue(userIds1After.size > beforeSize, "同步后应该包含新插入的用户ID")

        // 清理测试数据
        authGroupUserDao.deleteById(id1)
        authGroupUserDao.deleteById(id2)
    }

    @Test
    fun syncOnGroupDelete() {
        val groupId = "6e90ce80-3333-3333-3333-333333333333"
        val userId = "5e90ce80-1111-1111-1111-111111111111"

        // 先插入一条用户组-用户关系记录
        val authGroupUser = AuthGroupUser.Companion().apply {
            this.groupId = groupId
            this.userId = userId
        }
        val id = authGroupUserDao.insert(authGroupUser)

        // 先同步缓存，确保缓存中有新插入的数据
        cacheHandler.syncOnGroupUserChange(groupId)

        // 获取一次，确保缓存中有数据
        val userIdsBefore = cacheHandler.getUserIds(groupId)
        assertTrue(userIdsBefore.contains(userId), "新插入的用户关系应该在缓存中")

        // 删除数据库记录（模拟用户组删除或用户组-用户关系删除）
        val deleteSuccess = authGroupUserDao.deleteById(id)
        assertTrue(deleteSuccess, "删除应该成功")

        // 同步缓存（模拟用户组删除）
        cacheHandler.syncOnGroupDelete(groupId)

        // 验证缓存已被清除，重新获取应该不包含已删除的用户
        val userIdsAfter = cacheHandler.getUserIds(groupId)
        assertTrue(!userIdsAfter.contains(userId), "删除后，缓存应该被清除，不应该包含已删除的用户ID")
    }

}
