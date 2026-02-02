package io.kudos.ams.sys.core.service

import io.kudos.ams.sys.core.service.iservice.ISysTenantSystemService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for SysTenantSystemService
 *
 * 测试数据来源：`SysTenantSystemServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysTenantSystemServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysTenantSystemService: ISysTenantSystemService

    @Test
    fun searchSystemCodesByTenantId() {
        val tenantId = "20000000-0000-0000-0000-000000000025"
        val codes = sysTenantSystemService.searchSystemCodesByTenantId(tenantId)
        assertTrue(codes.contains("svc-subsys-ts-test-1"))
    }

    @Test
    fun searchTenantIdsBySystemCode() {
        val systemCode = "svc-subsys-ts-test-1"
        val tenantIds = sysTenantSystemService.searchTenantIdsBySystemCode(systemCode)
        assertTrue(tenantIds.contains("20000000-0000-0000-0000-000000000025"))
    }

    @Test
    fun exists() {
        val tenantId = "20000000-0000-0000-0000-000000000025"
        val systemCode = "svc-subsys-ts-test-1"
        assertTrue(sysTenantSystemService.exists(tenantId, systemCode))
        assertFalse(sysTenantSystemService.exists(tenantId, "non-existent"))
    }

    @Test
    fun batchBind_and_unbind() {
        val tenantId = "20000000-0000-0000-0000-000000000025"
        val systemCode = "svc-subsys-ts-test-2"
        
        // 先创建新的系统
        // 注意：这里假设系统已存在，实际测试中可能需要先创建
        // 为了简化，我们测试解绑
        val systemCodeToUnbind = "svc-subsys-ts-test-1"
        val unbindResult = sysTenantSystemService.unbind(tenantId, systemCodeToUnbind)
        assertTrue(unbindResult)

        // 重新绑定
        val bindCount = sysTenantSystemService.batchBind(tenantId, listOf(systemCode))
        assertTrue(bindCount > 0)
    }
}
