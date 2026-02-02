package io.kudos.ms.sys.core.service

import io.kudos.ms.sys.core.service.iservice.ISysTenantLanguageService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for SysTenantLanguageService
 *
 * 测试数据来源：`SysTenantLanguageServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysTenantLanguageServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysTenantLanguageService: ISysTenantLanguageService

    @Test
    fun getLanguageCodesByTenantId() {
        val tenantId = "20000000-0000-0000-0000-000000000039"
        val codes = sysTenantLanguageService.getLanguageCodesByTenantId(tenantId)
        assertTrue(codes.contains("zh_CN"))
    }

    @Test
    fun getTenantIdsByLanguageCode() {
        val languageCode = "zh_CN"
        val tenantIds = sysTenantLanguageService.getTenantIdsByLanguageCode(languageCode)
        assertTrue(tenantIds.contains("20000000-0000-0000-0000-000000000039"))
    }

    @Test
    fun exists() {
        val tenantId = "20000000-0000-0000-0000-000000000039"
        val languageCode = "zh_CN"
        assertTrue(sysTenantLanguageService.exists(tenantId, languageCode))
        assertFalse(sysTenantLanguageService.exists(tenantId, "en_US"))
    }

    @Test
    fun unbind() {
        val tenantId = "20000000-0000-0000-0000-000000000039"
        val languageCode = "zh_CN"
        val result = sysTenantLanguageService.unbind(tenantId, languageCode)
        assertTrue(result)
        
        // 重新绑定
        val bindCount = sysTenantLanguageService.batchBind(tenantId, listOf(languageCode))
        assertTrue(bindCount > 0)
    }
}
