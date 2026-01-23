package io.kudos.ams.sys.provider.service

import io.kudos.ams.sys.provider.service.iservice.ISysI18nService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.cache.CacheHandlerTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysI18NService
 *
 * 测试数据来源：`V1.0.0.40__SysI18NServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysI18NServiceTest : CacheHandlerTestBase() {

    @Resource
    private lateinit var sysI18nService: ISysI18nService

    @Test
    fun getI18nValue() {
        val locale = "zh_CN"
        val moduleCode = "svc-module-i18n-test-1"
        val i18nTypeDictCode = "label"
        val key = "svc-i18n-key-1"
        val value = sysI18nService.getI18nValue(locale, moduleCode, i18nTypeDictCode, key)
        assertNotNull(value)
        assertEquals("svc-i18n-value-1", value)
    }

    @Test
    fun getI18nsByModuleAndType() {
        val moduleCode = "svc-module-i18n-test-1"
        val i18nTypeDictCode = "label"
        val i18ns = sysI18nService.getI18nsByModuleAndType(moduleCode, i18nTypeDictCode, null)
        assertTrue(i18ns.isNotEmpty())
        
        val zhI18ns = sysI18nService.getI18nsByModuleAndType(moduleCode, i18nTypeDictCode, "zh_CN")
        assertTrue(zhI18ns.any { it.locale == "zh_CN" })
    }

    @Test
    fun updateActive() {
        val id = "20000000-0000-0000-0000-000000000040"
        assertTrue(sysI18nService.updateActive(id, false))
        assertTrue(sysI18nService.updateActive(id, true))
    }
}
