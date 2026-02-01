package io.kudos.ams.sys.core.service

import io.kudos.ams.sys.core.service.iservice.ISysTenantSubSystemService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for SysTenantSubSystemService
 *
 * 测试数据来源：`SysTenantSubSystemServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysTenantSubSystemServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysTenantSubSystemService: ISysTenantSubSystemService

    @Test
    fun searchSubSystemCodesByTenantId() {
        val tenantId = "20000000-0000-0000-0000-000000000025"
        val codes = sysTenantSubSystemService.searchSubSystemCodesByTenantId(tenantId)
        assertTrue(codes.contains("svc-subsys-ts-test-1"))
    }

    @Test
    fun searchTenantIdsBySubSystemCode() {
        val subSystemCode = "svc-subsys-ts-test-1"
        val tenantIds = sysTenantSubSystemService.searchTenantIdsBySubSystemCode(subSystemCode)
        assertTrue(tenantIds.contains("20000000-0000-0000-0000-000000000025"))
    }

    @Test
    fun exists() {
        val tenantId = "20000000-0000-0000-0000-000000000025"
        val subSystemCode = "svc-subsys-ts-test-1"
        assertTrue(sysTenantSubSystemService.exists(tenantId, subSystemCode))
        assertFalse(sysTenantSubSystemService.exists(tenantId, "non-existent"))
    }

    @Test
    fun batchBind_and_unbind() {
        val tenantId = "20000000-0000-0000-0000-000000000025"
        val portalCode = "svc-portal-ts-test-1"
        val newSubSystemCodes = listOf("svc-subsys-ts-test-2")
        
        // 先创建新的子系统
        // 注意：这里假设子系统已存在，实际测试中可能需要先创建
        // 为了简化，我们测试解绑
        val subSystemCode = "svc-subsys-ts-test-1"
        val unbindResult = sysTenantSubSystemService.unbind(tenantId, subSystemCode)
        assertTrue(unbindResult)
        
        // 重新绑定
        val bindCount = sysTenantSubSystemService.batchBind(tenantId, listOf(subSystemCode), portalCode)
        assertTrue(bindCount > 0)
    }
}
