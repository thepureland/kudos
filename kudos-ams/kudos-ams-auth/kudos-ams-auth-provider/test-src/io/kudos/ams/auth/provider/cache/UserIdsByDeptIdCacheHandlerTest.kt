package io.kudos.ams.auth.provider.cache

import io.kudos.ams.auth.provider.dao.AuthDeptUserDao
import io.kudos.ams.auth.provider.model.po.AuthDeptUser
import io.kudos.test.container.cache.RdbAndRedisCacheTestBase
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for UserIdsByDeptIdCacheHandler
 *
 * 测试数据来源：`V1.0.0.4__UserIdsByDeptIdCacheHandlerTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserIdsByDeptIdCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: UserIdsByDeptIdCacheHandler

    @Resource
    private lateinit var authDeptUserDao: AuthDeptUserDao

    @Test
    fun getUserIds() {
        // 存在的部门（有多个用户）
        var deptId = "11111111-1111-1111-1111-111111111111"
        val userIds2 = cacheHandler.getUserIds(deptId)
        val userIds3 = cacheHandler.getUserIds(deptId)
        assertTrue(userIds2.isNotEmpty())
        assertEquals(userIds2, userIds3)

        // 不存在的部门
        deptId = "no_exist_dept"
        val userIds4 = cacheHandler.getUserIds(deptId)
        assertTrue(userIds4.isEmpty())
    }

    @Test
    fun syncOnInsert() {
        val deptId = "11111111-1111-1111-1111-111111111111"
        val authDeptUser = AuthDeptUser().apply {
            this.deptId = deptId
            this.userId = "33333333-3333-3333-3333-333333333333"
            this.deptAdmin = false
        }
        val id = authDeptUserDao.insert(authDeptUser)

        // 先获取一次，记录用户数量
        val userIdsBefore = cacheHandler.getUserIds(deptId)
        val beforeSize = userIdsBefore.size

        // 同步缓存
        cacheHandler.syncOnInsert(authDeptUser, id)

        // 验证缓存已被清除并重新加载
        val userIdsAfter = cacheHandler.getUserIds(deptId)
        assertTrue(userIdsAfter.size >= beforeSize, "用户数量应该增加或保持不变")
    }

    @Test
    fun syncOnUpdate() {
        val deptId = "11111111-1111-1111-1111-111111111111"
        val authDeptUser = AuthDeptUser().apply {
            this.deptId = deptId
            this.userId = "33333333-3333-3333-3333-333333333333"
            this.deptAdmin = false
        }
        val id = authDeptUserDao.insert(authDeptUser)

        // 先同步插入缓存
        cacheHandler.syncOnInsert(authDeptUser, id)

        // 先获取一次，确保缓存中有数据
        val userIdsBefore = cacheHandler.getUserIds(deptId)
        assertTrue(userIdsBefore.contains(authDeptUser.userId), "新用户应该在缓存中")

        // 更新数据库记录
        val success = authDeptUserDao.updateProperties(id, mapOf(AuthDeptUser::deptAdmin.name to true))
        assert(success)

        // 同步缓存
        cacheHandler.syncOnUpdate(null, id)

        // 验证缓存已被清除并重新加载
        val userIdsAfter = cacheHandler.getUserIds(deptId)
        assertTrue(userIdsAfter.contains(authDeptUser.userId), "更新后的用户应该仍在缓存中")
    }

    @Test
    fun syncOnDelete() {
        val deptId = "11111111-1111-1111-1111-111111111111"
        val authDeptUser = AuthDeptUser().apply {
            this.deptId = deptId
            this.userId = "33333333-3333-3333-3333-333333333333"
            this.deptAdmin = false
        }
        val id = authDeptUserDao.insert(authDeptUser)

        // 先同步插入缓存
        cacheHandler.syncOnInsert(authDeptUser, id)

        // 先获取一次，确保缓存中有数据
        val userIdsBefore = cacheHandler.getUserIds(deptId)
        assertTrue(userIdsBefore.contains(authDeptUser.userId), "新用户应该在缓存中")

        // 删除数据库记录
        val deleteSuccess = authDeptUserDao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(authDeptUser, id)

        // 验证缓存已被清除并重新加载
        val userIdsAfter = cacheHandler.getUserIds(deptId)
        assertTrue(!userIdsAfter.contains(authDeptUser.userId), "删除后的用户不应该包含在缓存中")
    }

    @Test
    fun syncOnBatchDelete() {
        val deptId = "11111111-1111-1111-1111-111111111111"
        
        val authDeptUser1 = AuthDeptUser().apply {
            this.deptId = deptId
            this.userId = "33333333-3333-3333-3333-333333333333"
            this.deptAdmin = false
        }
        val id1 = authDeptUserDao.insert(authDeptUser1)
        
        val authDeptUser2 = AuthDeptUser().apply {
            this.deptId = deptId
            this.userId = "44444444-4444-4444-4444-444444444444"
            this.deptAdmin = false
        }
        val id2 = authDeptUserDao.insert(authDeptUser2)

        // 先同步插入缓存
        cacheHandler.syncOnInsert(authDeptUser1, id1)
        cacheHandler.syncOnInsert(authDeptUser2, id2)

        // 先获取一次，确保缓存中有数据
        val userIdsBefore = cacheHandler.getUserIds(deptId)
        assertTrue(userIdsBefore.contains(authDeptUser1.userId), "新用户1应该在缓存中")
        assertTrue(userIdsBefore.contains(authDeptUser2.userId), "新用户2应该在缓存中")

        // 批量删除数据库记录
        val ids = listOf(id1, id2)
        val count = authDeptUserDao.batchDelete(ids)
        assert(count == 2)

        // 同步缓存
        cacheHandler.syncOnBatchDelete(ids, listOf(deptId, deptId))

        // 验证缓存已被清除并重新加载
        val userIdsAfter = cacheHandler.getUserIds(deptId)
        assertTrue(!userIdsAfter.contains(authDeptUser1.userId), "删除后的用户1不应该包含在缓存中")
        assertTrue(!userIdsAfter.contains(authDeptUser2.userId), "删除后的用户2不应该包含在缓存中")
    }

}
