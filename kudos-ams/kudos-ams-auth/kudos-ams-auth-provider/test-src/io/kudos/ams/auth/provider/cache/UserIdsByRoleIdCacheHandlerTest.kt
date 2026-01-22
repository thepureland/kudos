package io.kudos.ams.auth.provider.cache

import io.kudos.test.container.cache.CacheHandlerTestBase
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for UserIdsByRoleIdCacheHandler
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserIdsByRoleIdCacheHandlerTest : CacheHandlerTestBase() {

    @Resource
    private lateinit var cacheHandler: UserIdsByRoleIdCacheHandler

    @Test
    fun getUserIds() {
        // 存在的角色ID，有多个用户
        var roleId = "11111111-1111-1111-1111-111111111111"
        val userIds1 = cacheHandler.getUserIds(roleId)
        val userIds2 = cacheHandler.getUserIds(roleId)
        assertTrue(userIds1.isNotEmpty(), "角色${roleId}应该有用户ID列表")
        assertEquals(userIds1, userIds2, "两次调用应该返回相同的结果（缓存验证）")
        // 验证用户ID：角色ROLE_ADMIN有用户admin和zhangsan
        assertEquals(2, userIds1.size, "角色${roleId}应该有2个用户ID")
        assertTrue(userIds1.contains("11111111-1111-1111-1111-111111111111"), "应该包含admin的用户ID，实际返回：${userIds1}")
        assertTrue(userIds1.contains("22222222-2222-2222-2222-222222222222"), "应该包含zhangsan的用户ID，实际返回：${userIds1}")

        // 存在的角色ID，有一个用户
        roleId = "22222222-2222-2222-2222-222222222222"
        val userIds3 = cacheHandler.getUserIds(roleId)
        val userIds4 = cacheHandler.getUserIds(roleId)
        assertTrue(userIds3.isNotEmpty(), "角色${roleId}应该有用户ID列表")
        assertEquals(userIds3, userIds4, "两次调用应该返回相同的结果（缓存验证）")
        // 角色ROLE_USER只有用户zhangsan
        assertEquals(1, userIds3.size, "角色${roleId}应该有1个用户ID，实际返回：${userIds3}")
        assertTrue(userIds3.contains("22222222-2222-2222-2222-222222222222"), "应该包含zhangsan的用户ID")

        // 存在的角色ID，但没有用户
        roleId = "33333333-3333-3333-3333-333333333333"
        val userIds5 = cacheHandler.getUserIds(roleId)
        assertTrue(userIds5.isEmpty(), "角色${roleId}没有用户，应该返回空列表")

        // 不存在的角色ID
        roleId = "no_exist_role_id"
        val userIds6 = cacheHandler.getUserIds(roleId)
        assertTrue(userIds6.isEmpty(), "不存在的角色ID应该返回空列表")
    }

}
