package io.kudos.ams.auth.provider.cache

import io.kudos.ams.auth.provider.dao.AuthDeptUserDao
import io.kudos.ams.auth.provider.model.po.AuthDeptUser
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for DeptIdsByUserIdCacheHandler
 *
 * 测试数据来源：`DeptIdsByUserIdCacheHandlerTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class DeptIdsByUserIdCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: DeptIdsByUserIdCacheHandler

    @Resource
    private lateinit var authDeptUserDao: AuthDeptUserDao

    @Test
    fun getDeptIds() {
        // 存在的用户ID，有一个部门
        var userId = "11111111-1111-1111-1111-111111111111"
        val deptIds1 = cacheHandler.getDeptIds(userId)
        val deptIds2 = cacheHandler.getDeptIds(userId)
        assertTrue(deptIds1.isNotEmpty(), "用户${userId}应该有部门ID列表")
        assertEquals(deptIds1, deptIds2, "两次调用应该返回相同的结果（缓存验证）")
        // 验证部门ID：用户11111111属于技术部
        assertEquals(1, deptIds1.size, "用户${userId}应该有1个部门ID")
        assertTrue(deptIds1.contains("11111111-1111-1111-1111-111111111111"), "应该包含技术部的部门ID，实际返回：${deptIds1}")

        // 存在的用户ID，有多个部门
        userId = "22222222-2222-2222-2222-222222222222"
        val deptIds3 = cacheHandler.getDeptIds(userId)
        val deptIds4 = cacheHandler.getDeptIds(userId)
        assertTrue(deptIds3.isNotEmpty(), "用户${userId}应该有部门ID列表")
        assertEquals(deptIds3, deptIds4, "两次调用应该返回相同的结果（缓存验证）")
        // 用户22222222属于技术部和产品部
        assertEquals(2, deptIds3.size, "用户${userId}应该有2个部门ID，实际返回：${deptIds3}")
        assertTrue(deptIds3.contains("11111111-1111-1111-1111-111111111111"), "应该包含技术部的部门ID")
        assertTrue(deptIds3.contains("22222222-2222-2222-2222-222222222222"), "应该包含产品部的部门ID")

        // 存在的用户ID，但没有部门
        userId = "33333333-3333-3333-3333-333333333333"
        val deptIds5 = cacheHandler.getDeptIds(userId)
        assertTrue(deptIds5.isEmpty(), "用户${userId}没有部门，应该返回空列表")

        // 不存在的用户ID
        userId = "no_exist_user_id"
        val deptIds6 = cacheHandler.getDeptIds(userId)
        assertTrue(deptIds6.isEmpty(), "不存在的用户ID应该返回空列表")
    }

    @Test
    fun syncOnDeptUserChange() {
        val userId = "33333333-3333-3333-3333-333333333333"
        val deptId = "22222222-2222-2222-2222-222222222222"
        
        // 先获取一次，记录初始部门数量
        val deptIdsBefore = cacheHandler.getDeptIds(userId)
        val beforeSize = deptIdsBefore.size
        
        // 插入一条新的用户-部门关系记录
        val authDeptUser = AuthDeptUser().apply {
            this.deptId = deptId
            this.userId = userId
            this.deptAdmin = false
        }
        val id = authDeptUserDao.insert(authDeptUser)
        
        // 同步缓存（模拟用户-部门关系变更）
        cacheHandler.syncOnDeptUserChange(userId)
        
        // 验证缓存已被清除并重新加载，应该包含新插入的部门
        val deptIdsAfter = cacheHandler.getDeptIds(userId)
        assertTrue(deptIdsAfter.size > beforeSize, "同步后应该包含新插入的部门ID")
        assertTrue(deptIdsAfter.contains(deptId), "应该包含新插入的部门ID")
        
        // 清理测试数据
        authDeptUserDao.deleteById(id)
    }

    @Test
    fun syncOnBatchDeptUserChange() {
        val userId1 = "33333333-3333-3333-3333-333333333333"
        val userId2 = "33333333-3333-3333-3333-333333333333"
        val deptId1 = "11111111-1111-1111-1111-111111111111"
        val deptId2 = "22222222-2222-2222-2222-222222222222"
        val userIds = listOf(userId1, userId2)
        
        // 先获取一次，记录初始部门数量
        val deptIds1Before = cacheHandler.getDeptIds(userId1)
        val deptIds2Before = cacheHandler.getDeptIds(userId2)
        val beforeSize1 = deptIds1Before.size
        val beforeSize2 = deptIds2Before.size
        
        // 批量插入用户-部门关系记录
        val authDeptUser1 = AuthDeptUser().apply {
            this.deptId = deptId1
            this.userId = userId1
            this.deptAdmin = false
        }
        val id1 = authDeptUserDao.insert(authDeptUser1)
        
        val authDeptUser2 = AuthDeptUser().apply {
            this.deptId = deptId2
            this.userId = userId2
            this.deptAdmin = false
        }
        val id2 = authDeptUserDao.insert(authDeptUser2)
        
        // 批量同步缓存（模拟批量用户-部门关系变更）
        cacheHandler.syncOnBatchDeptUserChange(userIds)
        
        // 验证缓存已被清除并重新加载，应该包含新插入的部门
        val deptIds1After = cacheHandler.getDeptIds(userId1)
        val deptIds2After = cacheHandler.getDeptIds(userId2)
        assertTrue(deptIds1After.size > beforeSize1 || deptIds2After.size > beforeSize2, "同步后应该包含新插入的部门ID")
        
        // 清理测试数据
        authDeptUserDao.deleteById(id1)
        authDeptUserDao.deleteById(id2)
    }

    @Test
    fun syncOnUserDelete() {
        val userId = "33333333-3333-3333-3333-333333333333"
        val deptId = "11111111-1111-1111-1111-111111111111"
        
        // 先插入一条用户-部门关系记录
        val authDeptUser = AuthDeptUser().apply {
            this.deptId = deptId
            this.userId = userId
            this.deptAdmin = false
        }
        val id = authDeptUserDao.insert(authDeptUser)
        
        // 先同步缓存，确保缓存中有新插入的数据
        cacheHandler.syncOnDeptUserChange(userId)
        
        // 获取一次，确保缓存中有数据
        val deptIdsBefore = cacheHandler.getDeptIds(userId)
        assertTrue(deptIdsBefore.contains(deptId), "新插入的部门关系应该在缓存中")
        
        // 删除数据库记录（模拟用户删除或用户-部门关系删除）
        val deleteSuccess = authDeptUserDao.deleteById(id)
        assertTrue(deleteSuccess, "删除应该成功")
        
        // 同步缓存（模拟用户删除）
        cacheHandler.syncOnUserDelete(userId)
        
        // 验证缓存已被清除，重新获取应该不包含已删除的部门
        val deptIdsAfter = cacheHandler.getDeptIds(userId)
        assertTrue(!deptIdsAfter.contains(deptId), "删除后，缓存应该被清除，不应该包含已删除的部门ID")
    }

}
