package io.kudos.ams.auth.provider.cache

import io.kudos.test.container.cache.CacheHandlerTestBase
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for ResourceIdsByRoleIdCacheHandler
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ResourceIdsByRoleIdCacheHandlerTest : CacheHandlerTestBase() {

    @Resource
    private lateinit var cacheHandler: ResourceIdsByRoleIdCacheHandler

    @Test
    fun getResourceIds() {
        // 存在的角色ID，有多个资源
        var roleId = "11111111-1111-1111-1111-111111111111"
        val resourceIds1 = cacheHandler.getResourceIds(roleId)
        val resourceIds2 = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIds1.isNotEmpty(), "角色${roleId}应该有资源ID列表")
        assertEquals(resourceIds1, resourceIds2, "两次调用应该返回相同的结果（缓存验证）")
        // 验证资源ID：角色ROLE_ADMIN有resource-aaa和resource-bbb
        assertEquals(2, resourceIds1.size, "角色${roleId}应该有2个资源ID")
        assertTrue(resourceIds1.contains("resource-aaa"), "应该包含resource-aaa，实际返回：${resourceIds1}")
        assertTrue(resourceIds1.contains("resource-bbb"), "应该包含resource-bbb，实际返回：${resourceIds1}")

        // 存在的角色ID，有多个资源
        roleId = "22222222-2222-2222-2222-222222222222"
        val resourceIds3 = cacheHandler.getResourceIds(roleId)
        val resourceIds4 = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIds3.isNotEmpty(), "角色${roleId}应该有资源ID列表")
        assertEquals(resourceIds3, resourceIds4, "两次调用应该返回相同的结果（缓存验证）")
        // 角色ROLE_USER有resource-ccc和resource-ddd
        assertEquals(2, resourceIds3.size, "角色${roleId}应该有2个资源ID，实际返回：${resourceIds3}")
        assertTrue(resourceIds3.contains("resource-ccc"), "应该包含resource-ccc")
        assertTrue(resourceIds3.contains("resource-ddd"), "应该包含resource-ddd")

        // 存在的角色ID，但没有资源
        roleId = "33333333-3333-3333-3333-333333333333"
        val resourceIds5 = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIds5.isEmpty(), "角色${roleId}没有资源，应该返回空列表")

        // 不存在的角色ID
        roleId = "no_exist_role_id"
        val resourceIds6 = cacheHandler.getResourceIds(roleId)
        assertTrue(resourceIds6.isEmpty(), "不存在的角色ID应该返回空列表")
    }

}
