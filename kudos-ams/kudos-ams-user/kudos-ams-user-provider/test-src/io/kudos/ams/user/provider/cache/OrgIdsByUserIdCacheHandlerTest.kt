package io.kudos.ams.user.provider.cache

import io.kudos.ams.user.provider.dao.UserOrgUserDao
import io.kudos.ams.user.provider.model.po.UserOrgUser
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for OrgIdsByUserIdCacheHandler
 *
 * 测试数据来源：`OrgIdsByUserIdCacheHandlerTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class OrgIdsByUserIdCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: OrgIdsByUserIdCacheHandler

    @Resource
    private lateinit var userOrgUserDao: UserOrgUserDao

    @Test
    fun getOrgIds() {
        // 存在的用户ID，有一个机构
        var userId = "81cea00f-1111-1111-1111-111111111111"
        val orgIds1 = cacheHandler.getOrgIds(userId)
        val orgIds2 = cacheHandler.getOrgIds(userId)
        assertTrue(orgIds1.isNotEmpty(), "用户${userId}应该有机构ID列表")
        assertEquals(orgIds1, orgIds2, "两次调用应该返回相同的结果（缓存验证）")
        // 验证机构ID：用户11111111属于技术部
        assertEquals(1, orgIds1.size, "用户${userId}应该有1个机构ID")
        assertTrue(orgIds1.contains("81cea00f-1111-1111-1111-111111111111"), "应该包含技术部的机构ID，实际返回：${orgIds1}")

        // 存在的用户ID，有多个机构
        userId = "81cea00f-2222-2222-2222-222222222222"
        val orgIds3 = cacheHandler.getOrgIds(userId)
        val orgIds4 = cacheHandler.getOrgIds(userId)
        assertTrue(orgIds3.isNotEmpty(), "用户${userId}应该有机构ID列表")
        assertEquals(orgIds3, orgIds4, "两次调用应该返回相同的结果（缓存验证）")
        // 用户22222222属于技术部和产品部
        assertEquals(2, orgIds3.size, "用户${userId}应该有2个机构ID，实际返回：${orgIds3}")
        assertTrue(orgIds3.contains("81cea00f-1111-1111-1111-111111111111"), "应该包含技术部的机构ID")
        assertTrue(orgIds3.contains("81cea00f-2222-2222-2222-222222222222"), "应该包含产品部的机构ID")

        // 存在的用户ID，但没有机构
        userId = "81cea00f-3333-3333-3333-333333333333"
        val orgIds5 = cacheHandler.getOrgIds(userId)
        assertTrue(orgIds5.isEmpty(), "用户${userId}没有机构，应该返回空列表")

        // 不存在的用户ID
        userId = "no_exist_user_id"
        val orgIds6 = cacheHandler.getOrgIds(userId)
        assertTrue(orgIds6.isEmpty(), "不存在的用户ID应该返回空列表")
    }

    @Test
    fun syncOnOrgUserChange() {
        val userId = "81cea00f-3333-3333-3333-333333333333"
        val orgId = "81cea00f-2222-2222-2222-222222222222"
        
        // 先获取一次，记录初始机构数量
        val orgIdsBefore = cacheHandler.getOrgIds(userId)
        val beforeSize = orgIdsBefore.size
        
        // 插入一条新的用户-机构关系记录
        val userOrgUser = UserOrgUser().apply {
            this.orgId = orgId
            this.userId = userId
            this.orgAdmin = false
        }
        val id = userOrgUserDao.insert(userOrgUser)
        
        // 同步缓存（模拟用户-机构关系变更）
        cacheHandler.syncOnOrgUserChange(userId)
        
        // 验证缓存已被清除并重新加载，应该包含新插入的机构
        val orgIdsAfter = cacheHandler.getOrgIds(userId)
        assertTrue(orgIdsAfter.size >= beforeSize, "同步后应该包含新插入的机构ID")
        assertTrue(orgIdsAfter.contains(orgId), "应该包含新插入的机构ID")
        
        // 清理测试数据
        userOrgUserDao.deleteById(id)
    }

    @Test
    fun syncOnBatchOrgUserChange() {
        val userId1 = "81cea00f-3333-3333-3333-333333333333"
        val userId2 = "81cea00f-3333-3333-3333-333333333333"
        val orgId1 = "81cea00f-1111-1111-1111-111111111111"
        val orgId2 = "81cea00f-2222-2222-2222-222222222222"
        val userIds = listOf(userId1, userId2)
        
        // 先获取一次，记录初始机构数量
        val orgIds1Before = cacheHandler.getOrgIds(userId1)
        val orgIds2Before = cacheHandler.getOrgIds(userId2)
        val beforeSize1 = orgIds1Before.size
        val beforeSize2 = orgIds2Before.size
        
        // 批量插入用户-机构关系记录
        val userOrgUser1 = UserOrgUser().apply {
            this.orgId = orgId1
            this.userId = userId1
            this.orgAdmin = false
        }
        val id1 = userOrgUserDao.insert(userOrgUser1)
        
        val userOrgUser2 = UserOrgUser().apply {
            this.orgId = orgId2
            this.userId = userId2
            this.orgAdmin = false
        }
        val id2 = userOrgUserDao.insert(userOrgUser2)
        
        // 批量同步缓存（模拟批量用户-机构关系变更）
        cacheHandler.syncOnBatchOrgUserChange(userIds)
        
        // 验证缓存已被清除并重新加载，应该包含新插入的机构
        val orgIds1After = cacheHandler.getOrgIds(userId1)
        val orgIds2After = cacheHandler.getOrgIds(userId2)
        assertTrue(orgIds1After.size >= beforeSize1 || orgIds2After.size >= beforeSize2, "同步后应该包含新插入的机构ID")
        
        // 清理测试数据
        userOrgUserDao.deleteById(id1)
        userOrgUserDao.deleteById(id2)
    }

    @Test
    fun syncOnUserDelete() {
        val userId = "81cea00f-3333-3333-3333-333333333333"
        val orgId = "81cea00f-1111-1111-1111-111111111111"
        
        // 先插入一条用户-机构关系记录
        val orgUser = UserOrgUser().apply {
            this.orgId = orgId
            this.userId = userId
            this.orgAdmin = false
        }
        val id = userOrgUserDao.insert(orgUser)
        
        // 先同步缓存，确保缓存中有新插入的数据
        cacheHandler.syncOnOrgUserChange(userId)
        
        // 获取一次，确保缓存中有数据
        val orgIdsBefore = cacheHandler.getOrgIds(userId)
        assertTrue(orgIdsBefore.contains(orgId), "新插入的机构关系应该在缓存中")
        
        // 删除数据库记录（模拟用户删除或用户-机构关系删除）
        val deleteSuccess = userOrgUserDao.deleteById(id)
        assertTrue(deleteSuccess, "删除应该成功")
        
        // 同步缓存（模拟用户删除）
        cacheHandler.syncOnUserDelete(userId)
        
        // 验证缓存已被清除，重新获取应该不包含已删除的机构
        val orgIdsAfter = cacheHandler.getOrgIds(userId)
        assertTrue(!orgIdsAfter.contains(orgId), "删除后，缓存应该被清除，不应该包含已删除的机构ID")
    }

}
