package io.kudos.ams.auth.provider.cache

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for ResourceIdsByUserIdCacheHandler
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ResourceIdsByUserIdCacheHandlerTest : CacheHandlerTestBase() {

    @Resource
    private lateinit var cacheHandler: ResourceIdsByUserIdCacheHandler

    @Test
    fun getResourceIds() {
        // 存在的用户ID，有一个角色，该角色有多个资源
        var userId = "11111111-1111-1111-1111-111111111111"
        val resourceIds1 = cacheHandler.getResourceIds(userId)
        val resourceIds2 = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIds1.isNotEmpty(), "用户${userId}应该有资源ID列表")
        assertEquals(resourceIds1, resourceIds2, "两次调用应该返回相同的结果（缓存验证）")
        // 验证资源ID：用户11111111有角色ROLE_ADMIN，应该包含resource-aaa和resource-bbb
        assertEquals(2, resourceIds1.size, "用户${userId}应该有2个资源ID")
        assertTrue(resourceIds1.contains("resource-aaa"), "应该包含resource-aaa，实际返回：${resourceIds1}")
        assertTrue(resourceIds1.contains("resource-bbb"), "应该包含resource-bbb，实际返回：${resourceIds1}")

        // 存在的用户ID，有多个角色，每个角色有多个资源
        userId = "22222222-2222-2222-2222-222222222222"
        val resourceIds3 = cacheHandler.getResourceIds(userId)
        val resourceIds4 = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIds3.isNotEmpty(), "用户${userId}应该有资源ID列表")
        assertEquals(resourceIds3, resourceIds4, "两次调用应该返回相同的结果（缓存验证）")
        // 用户22222222有两个角色（ROLE_USER和ROLE_ADMIN），应该包含所有角色的资源（去重后）
        // ROLE_ADMIN的资源：resource-aaa, resource-bbb
        // ROLE_USER的资源：resource-ccc, resource-ddd
        // 总共应该是4个资源
        assertEquals(4, resourceIds3.size, "用户${userId}应该有4个资源ID，实际返回：${resourceIds3}")
        assertTrue(resourceIds3.contains("resource-aaa"), "应该包含resource-aaa")
        assertTrue(resourceIds3.contains("resource-bbb"), "应该包含resource-bbb")
        assertTrue(resourceIds3.contains("resource-ccc"), "应该包含resource-ccc")
        assertTrue(resourceIds3.contains("resource-ddd"), "应该包含resource-ddd")

        // 存在的用户ID，但没有角色
        userId = "33333333-3333-3333-3333-333333333333"
        val resourceIds5 = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIds5.isEmpty(), "用户${userId}没有角色，应该返回空列表")

        // 不存在的用户ID
        userId = "no_exist_user_id"
        val resourceIds6 = cacheHandler.getResourceIds(userId)
        assertTrue(resourceIds6.isEmpty(), "不存在的用户ID应该返回空列表")
    }

}
