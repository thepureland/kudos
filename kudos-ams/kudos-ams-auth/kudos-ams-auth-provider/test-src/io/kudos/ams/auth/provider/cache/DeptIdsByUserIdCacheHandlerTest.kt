package io.kudos.ams.auth.provider.cache

import io.kudos.test.container.cache.CacheHandlerTestBase
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for DeptIdsByUserIdCacheHandler
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class DeptIdsByUserIdCacheHandlerTest : CacheHandlerTestBase() {

    @Resource
    private lateinit var cacheHandler: DeptIdsByUserIdCacheHandler

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

}
