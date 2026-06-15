package io.kudos.ms.sys.core.tenant.service

import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantResourceService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for SysTenantResourceService
 *
 * Test data source: `SysTenantResourceServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysTenantResourceServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysTenantResourceService: ISysTenantResourceService

    @Test
    fun getResourceIdsByTenantId() {
        val tenantId = "20000000-0000-0000-0000-000000005370"
        val resourceIds = sysTenantResourceService.getResourceIdsByTenantId(tenantId)
        assertTrue(resourceIds.contains("20000000-0000-0000-0000-000000005370"))
    }

    @Test
    fun getTenantIdsByResourceId() {
        val resourceId = "20000000-0000-0000-0000-000000005370"
        val tenantIds = sysTenantResourceService.getTenantIdsByResourceId(resourceId)
        assertTrue(tenantIds.contains("20000000-0000-0000-0000-000000005370"))
    }

    @Test
    fun exists() {
        val tenantId = "20000000-0000-0000-0000-000000005370"
        val resourceId = "20000000-0000-0000-0000-000000005370"
        assertTrue(sysTenantResourceService.exists(tenantId, resourceId))
        assertFalse(sysTenantResourceService.exists(tenantId, "non-existent"))
    }

    @Test
    fun unbind() {
        val tenantId = "20000000-0000-0000-0000-000000005370"
        val resourceId = "20000000-0000-0000-0000-000000005370"
        val result = sysTenantResourceService.unbind(tenantId, resourceId)
        assertTrue(result)
        
        // Rebind
        val bindCount = sysTenantResourceService.batchBind(tenantId, listOf(resourceId))
        assertTrue(bindCount > 0)
    }

    /** batchBind with an empty collection short-circuits and inserts nothing. */
    @Test
    fun batchBind_emptyReturnsZero() {
        assertEquals(0, sysTenantResourceService.batchBind("20000000-0000-0000-0000-000000005370", emptyList()))
    }

    /** batchBind where every requested resource already exists inserts nothing. */
    @Test
    fun batchBind_allExistReturnsZero() {
        val tenantId = "20000000-0000-0000-0000-000000005370"
        assertEquals(0, sysTenantResourceService.batchBind(tenantId, listOf("20000000-0000-0000-0000-000000005370")))
    }

    /** unbind a non-existent relation returns false (warn branch). */
    @Test
    fun unbind_missingReturnsFalse() {
        val tenantId = "20000000-0000-0000-0000-000000005370"
        assertFalse(sysTenantResourceService.unbind(tenantId, "no-such-resource"))
    }
}
