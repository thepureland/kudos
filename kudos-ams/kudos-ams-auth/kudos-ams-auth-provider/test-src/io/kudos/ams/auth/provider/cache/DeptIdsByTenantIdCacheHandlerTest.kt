package io.kudos.ams.auth.provider.cache

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for DeptIdsByTenantIdCacheHandler
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class DeptIdsByTenantIdCacheHandlerTest : CacheHandlerTestBase() {

    @Autowired
    private lateinit var cacheHandler: DeptIdsByTenantIdCacheHandler

    @Test
    fun getDeptIds() {
        // 存在的租户（有多个部门）- 复用 V1.0.0.1 的数据
        var tenantId = "tenant-001"
        val deptIds2 = cacheHandler.getDeptIds(tenantId)
        val deptIds3 = cacheHandler.getDeptIds(tenantId)
        assertEquals(7, deptIds2.size) // tenant-001 有 7 个 active=true 的部门（技术部、产品部、运营部、前端组、后端组、测试组、人事部）
        assertTrue(deptIds2.contains("11111111-1111-1111-1111-111111111111")) // 技术部
        assertTrue(deptIds2.contains("22222222-2222-2222-2222-222222222222")) // 产品部
        assertTrue(deptIds2.contains("88888888-8888-8888-8888-888888888888")) // 人事部
        assertEquals(deptIds2, deptIds3)

        // 存在的租户（只有 1 个部门）
        tenantId = "tenant-002"
        val deptIds4 = cacheHandler.getDeptIds(tenantId)
        assertEquals(1, deptIds4.size) // tenant-002 有 1 个 active=true 的部门（总部）
        assertTrue(deptIds4.contains("99999999-9999-9999-9999-999999999999")) // 总部

        // 不存在的租户
        tenantId = "no_exist_tenant"
        val deptIds5 = cacheHandler.getDeptIds(tenantId)
        assertTrue(deptIds5.isEmpty())
    }

}
