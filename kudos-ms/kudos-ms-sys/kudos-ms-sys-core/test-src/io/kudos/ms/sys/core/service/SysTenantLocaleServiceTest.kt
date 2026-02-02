package io.kudos.ms.sys.core.service

import io.kudos.ms.sys.core.service.iservice.ISysTenantLocaleService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for SysTenantLocaleService
 *
 * 测试数据来源：`SysTenantLocaleServiceTest.sql`
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
        
        // 重新绑定
        val bindCount = sysTenantLocaleService.batchBind(tenantId, listOf(localeCode))
        assertTrue(bindCount > 0)
    }
}
