package io.kudos.ms.sys.core.tenant.service

import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantSystemService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for SysTenantSystemService
 *
 * Test data source: `SysTenantSystemServiceTest.sql`
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
        val tenantId = "20000000-0000-0000-0000-000000003675"
        val codes = sysTenantSystemService.searchSystemCodesByTenantId(tenantId)
        assertTrue(codes.contains("svc-subsys-ts-test-1_7901"))
    }

    @Test
    fun searchTenantIdsBySystemCode() {
        val systemCode = "svc-subsys-ts-test-1_7901"
        val tenantIds = sysTenantSystemService.searchTenantIdsBySystemCode(systemCode)
        assertTrue(tenantIds.contains("20000000-0000-0000-0000-000000003675"))
    }

    @Test
    fun exists() {
        val tenantId = "20000000-0000-0000-0000-000000003675"
        val systemCode = "svc-subsys-ts-test-1_7901"
        assertTrue(sysTenantSystemService.exists(tenantId, systemCode))
        assertFalse(sysTenantSystemService.exists(tenantId, "non-existent"))
    }

    @Test
    fun batchBind_and_unbind() {
        val tenantId = "20000000-0000-0000-0000-000000003675"
        val systemCode = "svc-subsys-ts-test-1_7901"
        
        // Create the new system first
        // Note: this assumes the system already exists; in real tests it may need to be created
        // For simplicity, we test unbind here
        val systemCodeToUnbind = "svc-subsys-ts-test-1_7901"
        val unbindResult = sysTenantSystemService.unbind(tenantId, systemCodeToUnbind)
        assertTrue(unbindResult)

        // Rebind
        val bindCount = sysTenantSystemService.batchBind(tenantId, listOf(systemCode))
        assertTrue(bindCount > 0)
    }
}
