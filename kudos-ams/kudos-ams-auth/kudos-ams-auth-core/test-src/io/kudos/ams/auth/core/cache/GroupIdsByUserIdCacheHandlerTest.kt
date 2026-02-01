package io.kudos.ams.auth.core.cache

import io.kudos.ams.auth.core.dao.AuthGroupUserDao
import io.kudos.ams.auth.core.model.po.AuthGroupUser
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for GroupIdsByUserIdCacheHandler
 *
 * 测试数据来源：`GroupIdsByUserIdCacheHandlerTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class GroupIdsByUserIdCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: GroupIdsByUserIdCacheHandler

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Test
    fun getGroupIds() {
        // 存在的用户ID，有一个用户组
        var userId = "88207878-1111-1111-1111-111111111111"
        val groupIds1 = cacheHandler.getGroupIds(userId)
        val groupIds2 = cacheHandler.getGroupIds(userId)
        assertTrue(groupIds1.isNotEmpty(), "用户${userId}应该有用户组ID列表")
        assertEquals(groupIds1, groupIds2, "两次调用应该返回相同的结果（缓存验证）")
        // 验证用户组ID：用户11111111有一个用户组GROUP_ADMIN
        assertEquals(1, groupIds1.size, "用户${userId}应该有1个用户组ID")
        assertTrue(groupIds1.contains("88307878-1111-1111-1111-111111111111"), "应该包含GROUP_ADMIN的用户组ID，实际返回：${groupIds1}")

        // 存在的用户ID，有多个用户组
        userId = "88207878-2222-2222-2222-222222222222"
        val groupIds3 = cacheHandler.getGroupIds(userId)
        val groupIds4 = cacheHandler.getGroupIds(userId)
        assertTrue(groupIds3.isNotEmpty(), "用户${userId}应该有用户组ID列表")
        assertEquals(groupIds3, groupIds4, "两次调用应该返回相同的结果（缓存验证）")
        // 用户22222222有两个用户组（GROUP_USER和GROUP_ADMIN）
        assertEquals(
            2,
            groupIds3.size,
            "用户${userId}应该有2个用户组ID，实际返回：${groupIds3}"
        )
        assertTrue(groupIds3.contains("88307878-1111-1111-1111-111111111111"), "应该包含GROUP_ADMIN的用户组ID")
        assertTrue(groupIds3.contains("88307878-2222-2222-2222-222222222222"), "应该包含GROUP_USER的用户组ID")

        // 存在的用户ID，但没有用户组
        userId = "88207878-3333-3333-3333-333333333333"
        val groupIds5 = cacheHandler.getGroupIds(userId)
        assertTrue(groupIds5.isEmpty(), "用户${userId}没有用户组，应该返回空列表")

        // 不存在的用户ID
        userId = "no_exist_user_id"
        val groupIds6 = cacheHandler.getGroupIds(userId)
        assertTrue(groupIds6.isEmpty(), "不存在的用户ID应该返回空列表")
    }

    @Test
    fun syncOnGroupUserChange() {
        val userId = "88207878-3333-3333-3333-333333333333"
        val groupId = "88307878-2222-2222-2222-222222222222" // GROUP_USER 的 ID

        // 先获取一次，记录初始用户组数量
        val groupIdsBefore = cacheHandler.getGroupIds(userId)
        val beforeSize = groupIdsBefore.size

        // 插入一条新的用户-用户组关系记录
        val authGroupUser = AuthGroupUser.Companion().apply {
            this.groupId = groupId
            this.userId = userId
        }
        val id = authGroupUserDao.insert(authGroupUser)

        // 同步缓存（模拟用户-用户组关系变更）
        cacheHandler.syncOnGroupUserChange(userId)

        // 验证缓存已被清除并重新加载，应该包含新插入的用户组
        val groupIdsAfter = cacheHandler.getGroupIds(userId)
        assertTrue(groupIdsAfter.size > beforeSize, "同步后应该包含新插入的用户组ID")
        assertTrue(groupIdsAfter.contains(groupId), "应该包含新插入的用户组ID")

        // 清理测试数据
        authGroupUserDao.deleteById(id)
    }

    @Test
    fun syncOnBatchGroupUserChange() {
        val userId1 = "88207878-3333-3333-3333-333333333333"
        val userId2 = "88207878-3333-3333-3333-333333333333"
        val groupId1 = "88307878-1111-1111-1111-111111111111" // GROUP_ADMIN 的 ID
        val groupId2 = "88307878-2222-2222-2222-222222222222" // GROUP_USER 的 ID
        val userIds = listOf(userId1, userId2)

        // 先获取一次，记录初始用户组数量
        val groupIds1Before = cacheHandler.getGroupIds(userId1)
        val beforeSize = groupIds1Before.size

        // 批量插入用户-用户组关系记录
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

        // 批量同步缓存（模拟批量用户-用户组关系变更）
        cacheHandler.syncOnBatchGroupUserChange(userIds)

        // 验证缓存已被清除并重新加载，应该包含新插入的用户组
        val groupIds1After = cacheHandler.getGroupIds(userId1)
        assertTrue(groupIds1After.size > beforeSize, "同步后应该包含新插入的用户组ID")

        // 清理测试数据
        authGroupUserDao.deleteById(id1)
        authGroupUserDao.deleteById(id2)
    }

    @Test
    fun syncOnUserDelete() {
        val userId = "88207878-3333-3333-3333-333333333333"
        val groupId = "88307878-1111-1111-1111-111111111111" // GROUP_ADMIN 的 ID

        // 先插入一条用户-用户组关系记录
        val authGroupUser = AuthGroupUser.Companion().apply {
            this.groupId = groupId
            this.userId = userId
        }
        val id = authGroupUserDao.insert(authGroupUser)

        // 先同步缓存，确保缓存中有新插入的数据
        cacheHandler.syncOnGroupUserChange(userId)

        // 获取一次，确保缓存中有数据
        val groupIdsBefore = cacheHandler.getGroupIds(userId)
        assertTrue(groupIdsBefore.contains(groupId), "新插入的用户组关系应该在缓存中")

        // 删除数据库记录（模拟用户删除或用户-用户组关系删除）
        val deleteSuccess = authGroupUserDao.deleteById(id)
        assertTrue(deleteSuccess, "删除应该成功")

        // 同步缓存（模拟用户删除）
        cacheHandler.syncOnUserDelete(userId)

        // 验证缓存已被清除，重新获取应该不包含已删除的用户组
        val groupIdsAfter = cacheHandler.getGroupIds(userId)
        assertTrue(!groupIdsAfter.contains(groupId), "删除后，缓存应该被清除，不应该包含已删除的用户组ID")
    }

}
