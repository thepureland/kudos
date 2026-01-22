package io.kudos.ams.auth.provider.cache

import io.kudos.test.container.cache.CacheHandlerTestBase
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for UserIdsByTenantIdAndRoleCodeCacheHandler
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserIdsByTenantIdAndRoleCodeCacheHandlerTest : CacheHandlerTestBase() {

    @Resource
    private lateinit var cacheHandler: UserIdsByTenantIdAndRoleCodeCacheHandler

    @Test
    fun getUserIds() {
        // 存在的租户和角色
        var tenantId = "tenant-001"
        var roleCode = "ROLE_ADMIN"
        val userIds2 = cacheHandler.getUserIds(tenantId, roleCode)
        val userIds3 = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIds2.isNotEmpty())
        assertEquals(userIds2, userIds3)

        // 不存在的角色
        roleCode = "ROLE_NO_EXIST"
        val userIds4 = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIds4.isEmpty())

        // 不存在的租户
        tenantId = "no_exist_tenant"
        roleCode = "ROLE_ADMIN"
        val userIds5 = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIds5.isEmpty())
    }

}
