package io.kudos.ms.sys.core.service

import io.kudos.ms.sys.core.service.iservice.ISysI18nService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysI18NService
 *
 * 测试数据来源：`SysI18NServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysI18NServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysI18nService: ISysI18nService

    @Test
    fun getI18nValue() {
        val locale = "zh_CN"
        val i18nTypeDictCode = "label"
        val namespace = "label"
        val atomicServiceCode = "svc-as-i18n-test-1_8449"
        val key = "svc-i18n-key-1"
        val value = sysI18nService.getI18nValue(locale, i18nTypeDictCode, namespace, atomicServiceCode, key)
        assertNotNull(value)
        assertEquals("svc-i18n-value-1", value)
    }

    @Test
    fun getI18ns() {
        val locale = "zh_CN"
        val i18nTypeDictCode = "label"
        val namespace = "label"
        val atomicServiceCode = "svc-as-i18n-test-1_8449"
        val i18ns = sysI18nService.getI18ns(locale, i18nTypeDictCode, namespace, atomicServiceCode)
        assertTrue(i18ns.isNotEmpty())
        assertTrue(i18ns.containsKey("svc-i18n-key-1"))
        assertEquals("svc-i18n-value-1", i18ns["svc-i18n-key-1"])
    }

    @Test
    fun updateActive() {
        val id = "20000000-0000-0000-0000-000000008449"
        assertTrue(sysI18nService.updateActive(id, false))
        assertTrue(sysI18nService.updateActive(id, true))
    }
}
