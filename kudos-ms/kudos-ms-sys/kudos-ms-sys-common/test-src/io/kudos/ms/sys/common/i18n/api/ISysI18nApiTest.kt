package io.kudos.ms.sys.common.i18n.api

import io.kudos.ms.sys.common.i18n.vo.request.SysI18nFormUpdate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for ISysI18nApi
 *
 * Covers the abstract method contracts via a fake implementation
 * (getI18nValue / getI18ns / batchSaveOrUpdate / updateActive dispatch).
 *
 * @author K
 * @since 1.0.0
 */
internal class ISysI18nApiTest {

    private class FakeApi : ISysI18nApi {
        var lastBatch: List<SysI18nFormUpdate>? = null

        override fun getI18nValue(
            locale: String,
            i18nTypeDictCode: String,
            namespace: String,
            atomicServiceCode: String,
            key: String
        ): String? = if (key == "ok") "确定" else null

        override fun getI18ns(
            locale: String,
            i18nTypeDictCode: String,
            namespace: String,
            atomicServiceCode: String
        ): Map<String, String> = mapOf("ok" to "确定")

        override fun batchSaveOrUpdate(i18ns: List<SysI18nFormUpdate>): Int {
            lastBatch = i18ns
            return i18ns.size
        }

        override fun updateActive(id: String, active: Boolean): Boolean = active
    }

    @Test
    fun getI18nValueDispatch() {
        val api = FakeApi()
        assertEquals("确定", api.getI18nValue("zh_CN", "ui", "common", "sys", "ok"))
        assertNull(api.getI18nValue("zh_CN", "ui", "common", "sys", "missing"))
    }

    @Test
    fun getI18nsDispatch() {
        val api = FakeApi()
        assertEquals(mapOf("ok" to "确定"), api.getI18ns("zh_CN", "ui", "common", "sys"))
    }

    @Test
    fun batchSaveOrUpdateDispatch() {
        val api = FakeApi()
        val list = listOf(
            SysI18nFormUpdate("i-1", "zh_CN", "sys", "ui", "common", "ok", "确定", null),
        )
        assertEquals(1, api.batchSaveOrUpdate(list))
        assertEquals(list, api.lastBatch)
        assertEquals(0, api.batchSaveOrUpdate(emptyList()))
    }

    @Test
    fun updateActiveDispatch() {
        val api = FakeApi()
        assertTrue(api.updateActive("i-1", true))
        assertFalse(api.updateActive("i-1", false))
    }

}
