package io.kudos.ams.sys.provider.service

import io.kudos.ams.sys.provider.service.iservice.ISysDictItemI18nService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.cache.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysDictItemI18NService
 *
 * 测试数据来源：`V1.0.0.30__SysDictItemI18NServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDictItemI18NServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysDictItemI18nService: ISysDictItemI18nService

    @Test
    fun getI18nsByItemId() {
        val itemId = "20000000-0000-0000-0000-000000000031"
        val i18ns = sysDictItemI18nService.getI18nsByItemId(itemId)
        assertTrue(i18ns.isNotEmpty())
    }

    @Test
    fun getI18nValue() {
        val itemId = "20000000-0000-0000-0000-000000000031"
        val locale = "zh_CN"
        val value = sysDictItemI18nService.getI18nValue(itemId, locale)
        assertNotNull(value)
        assertEquals("中文值", value)
    }

    @Test
    fun updateActive() {
        val id = "20000000-0000-0000-0000-000000000031"
        assertTrue(sysDictItemI18nService.updateActive(id, false))
        assertTrue(sysDictItemI18nService.updateActive(id, true))
    }
}
