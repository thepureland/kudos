package io.kudos.ams.auth.provider.cache

import io.kudos.ams.auth.provider.dao.AuthRoleResourceDao
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for ResourceIdsByRoleCodeCacheHandler
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ResourceIdsByRoleCodeCacheHandlerTest : CacheHandlerTestBase() {

    @Autowired
    private lateinit var cacheHandler: ResourceIdsByRoleCodeCacheHandler

    @Autowired
    private lateinit var dao: AuthRoleResourceDao

    @Test
    fun getResourceIds() {
        // 存在的租户和角色
        var tenantId = "tenant-001"
        var roleCode = "ROLE_ADMIN"
        val resourceIds2 = cacheHandler.getResourceIds(tenantId, roleCode)
        val resourceIds3 = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(resourceIds2.isNotEmpty())
        assertEquals(resourceIds2, resourceIds3)

        // 不存在的角色
        roleCode = "ROLE_NO_EXIST"
        val resourceIds4 = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(resourceIds4.isEmpty())

        // 不存在的租户
        tenantId = "no_exist_tenant"
        roleCode = "ROLE_ADMIN"
        val resourceIds5 = cacheHandler.getResourceIds(tenantId, roleCode)
        assertTrue(resourceIds5.isEmpty())
    }

}
