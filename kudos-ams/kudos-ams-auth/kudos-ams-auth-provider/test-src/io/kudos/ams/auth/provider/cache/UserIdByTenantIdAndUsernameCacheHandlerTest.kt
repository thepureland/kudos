package io.kudos.ams.auth.provider.cache

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


/**
 * junit test for UserIdByTenantIdAndUsernameCacheHandler
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserIdByTenantIdAndUsernameCacheHandlerTest : CacheHandlerTestBase() {

    @Autowired
    private lateinit var cacheHandler: UserIdByTenantIdAndUsernameCacheHandler

    @Test
    fun getUserId() {
        // 存在的
        var tenantId = "tenant-001"
        var username = "admin"
        val userId2 = cacheHandler.getUserId(tenantId, username)
        val userId3 = cacheHandler.getUserId(tenantId, username)
        assertNotNull(userId2)
        assertEquals("11111111-1111-1111-1111-111111111111", userId2)
        assertEquals(userId2, userId3)

        // 不存在的用户名
        username = "no_exist_user"
        assertNull(cacheHandler.getUserId(tenantId, username))

        // 不存在的租户
        tenantId = "no_exist_tenant"
        username = "admin"
        assertNull(cacheHandler.getUserId(tenantId, username))

        // inactive 用户（只缓存 active=true 的）
        tenantId = "tenant-001"
        username = "wangwu"
        assertNull(cacheHandler.getUserId(tenantId, username))
    }

}
