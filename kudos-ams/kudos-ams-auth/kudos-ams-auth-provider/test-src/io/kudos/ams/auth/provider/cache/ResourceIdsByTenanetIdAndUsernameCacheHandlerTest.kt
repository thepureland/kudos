package io.kudos.ams.auth.provider.cache

import io.kudos.test.container.cache.CacheHandlerTestBase
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for ResourceIdsByTenanetIdAndUsernameCacheHandler
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ResourceIdsByTenanetIdAndUsernameCacheHandlerTest : CacheHandlerTestBase() {

    @Resource
    private lateinit var cacheHandler: ResourceIdsByTenanetIdAndUsernameCacheHandler

    @Test
    fun getResourceIds() {
        // 存在的租户和用户
        var tenantId = "tenant-001"
        var username = "admin"
        val resourceIds2 = cacheHandler.getResourceIds(tenantId, username)
        val resourceIds3 = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIds2.isNotEmpty())
        assertEquals(resourceIds2, resourceIds3)

        // 不存在的用户名
        username = "no_exist_user"
        val resourceIds4 = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIds4.isEmpty())

        // 不存在的租户
        tenantId = "no_exist_tenant"
        username = "admin"
        val resourceIds5 = cacheHandler.getResourceIds(tenantId, username)
        assertTrue(resourceIds5.isEmpty())
    }

}
