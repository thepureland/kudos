package io.kudos.ms.sys.core.tenant.service

import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantLocaleService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for SysTenantLocaleService
 *
 * Test data source: `SysTenantLocaleServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysTenantLocaleServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysTenantLocaleService: ISysTenantLocaleService

    @Test
    fun getLocaleCodesByTenantId() {
        val tenantId = "20000000-0000-0000-0000-000000009132"
        val codes = sysTenantLocaleService.getLocaleCodesByTenantId(tenantId)
        assertTrue(codes.contains("zh_CN"))
    }

    @Test
    fun getTenantIdsByLocaleCode() {
        val localeCode = "zh_CN"
        val tenantIds = sysTenantLocaleService.getTenantIdsByLocaleCode(localeCode)
        assertTrue(tenantIds.contains("20000000-0000-0000-0000-000000009132"))
    }

    @Test
    fun exists() {
        val tenantId = "20000000-0000-0000-0000-000000009132"
        val localeCode = "zh_CN"
        assertTrue(sysTenantLocaleService.exists(tenantId, localeCode))
        assertFalse(sysTenantLocaleService.exists(tenantId, "en_US"))
    }

    @Test
    fun unbind() {
        val tenantId = "20000000-0000-0000-0000-000000009132"
        val localeCode = "zh_CN"
        val result = sysTenantLocaleService.unbind(tenantId, localeCode)
        assertTrue(result)
        
        // Rebind
        val bindCount = sysTenantLocaleService.batchBind(tenantId, listOf(localeCode))
        assertTrue(bindCount > 0)
    }

    /** batchBind with an empty collection short-circuits and inserts nothing. */
    @Test
    fun batchBind_emptyReturnsZero() {
        assertEquals(0, sysTenantLocaleService.batchBind("20000000-0000-0000-0000-000000009132", emptyList()))
    }

    /** batchBind where every requested locale already exists inserts nothing. */
    @Test
    fun batchBind_allExistReturnsZero() {
        val tenantId = "20000000-0000-0000-0000-000000009132"
        assertEquals(0, sysTenantLocaleService.batchBind(tenantId, listOf("zh_CN")))
    }

    /** unbind a non-existent relation returns false (warn branch). */
    @Test
    fun unbind_missingReturnsFalse() {
        val tenantId = "20000000-0000-0000-0000-000000009132"
        assertFalse(sysTenantLocaleService.unbind(tenantId, "no-such-locale"))
    }
}
