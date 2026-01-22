package io.kudos.ams.auth.provider.cache

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


/**
 * junit test for RoleIdByTenantIdAndRoleCodeCacheHandler
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class RoleIdByTenantIdAndRoleCodeCacheHandlerTest : CacheHandlerTestBase() {

    @Resource
    private lateinit var cacheHandler: RoleIdByTenantIdAndRoleCodeCacheHandler

    @Test
    fun getRoleId() {
        // 存在的
        var tenantId = "tenant-001"
        var code = "ROLE_ADMIN"
        val roleId2 = cacheHandler.getRoleId(tenantId, code)
        val roleId3 = cacheHandler.getRoleId(tenantId, code)
        assertNotNull(roleId2)
        assertEquals("11111111-1111-1111-1111-111111111111", roleId2)
        assertEquals(roleId2, roleId3)

        // 不存在的角色编码
        code = "ROLE_NO_EXIST"
        assertNull(cacheHandler.getRoleId(tenantId, code))

        // 不存在的租户
        tenantId = "no_exist_tenant"
        code = "ROLE_ADMIN"
        assertNull(cacheHandler.getRoleId(tenantId, code))

        // inactive 角色（只缓存 active=true 的）
        tenantId = "tenant-001"
        code = "ROLE_TEST"
        assertNull(cacheHandler.getRoleId(tenantId, code))
    }

}
