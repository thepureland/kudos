package io.kudos.ams.sys.provider.service

import io.kudos.ams.sys.provider.service.iservice.ISysTenantService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysTenantService
 *
 * 测试数据来源：`SysTenantServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysTenantServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysTenantService: ISysTenantService

    @Test
    fun getTenant_and_getTenantRecord() {
        val id = "20000000-0000-0000-0000-000000000024"
        val cacheItem = sysTenantService.getTenant(id)
        assertNotNull(cacheItem)
        
        val record = sysTenantService.getTenantRecord(id)
        assertNotNull(record)
    }

    @Test
    fun getTenantByName() {
        val name = "svc-tenant-test-1"
        val record = sysTenantService.getTenantByName(name)
        assertNotNull(record)
    }

    @Test
    fun getSubSystemCodesByTenantId() {
        val tenantId = "20000000-0000-0000-0000-000000000024"
        val codes = sysTenantService.getSubSystemCodesByTenantId(tenantId)
        assertTrue(codes.contains("svc-subsys-tenant-test-1"))
    }

    @Test
    fun updateActive() {
        val id = "20000000-0000-0000-0000-000000000024"
        assertTrue(sysTenantService.updateActive(id, false))
        assertTrue(sysTenantService.updateActive(id, true))
    }
}
