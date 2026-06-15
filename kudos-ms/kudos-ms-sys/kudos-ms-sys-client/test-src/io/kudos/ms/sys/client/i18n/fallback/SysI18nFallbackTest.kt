package io.kudos.ms.sys.client.i18n.fallback

import io.kudos.ms.sys.common.i18n.vo.request.SysI18nFormUpdate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [SysI18nFallback].
 *
 * Coverage:
 *  - getI18nValue returns null.
 *  - getI18ns returns empty map.
 *  - batchSaveOrUpdate returns 0 for both empty and non-empty input lists (the size
 *    branch in the error log message is exercised by a non-empty list).
 *  - updateActive returns false for both flag values.
 *  - Edge inputs: empty strings, Unicode.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysI18nFallbackTest {

    private val fallback = SysI18nFallback()

    @Test
    fun getI18nValue_returnsNull() {
        assertNull(fallback.getI18nValue("zh_CN", "type", "ns", "atomic", "key"))
        assertNull(fallback.getI18nValue("", "", "", "", ""))
        assertNull(fallback.getI18nValue("語言", "類型", "命名空間", "服務", "鍵-ünï"))
    }

    @Test
    fun getI18ns_returnsEmptyMap() {
        assertTrue(fallback.getI18ns("zh_CN", "type", "ns", "atomic").isEmpty())
        assertTrue(fallback.getI18ns("", "", "", "").isEmpty())
    }

    @Test
    fun batchSaveOrUpdate_returnsZero() {
        assertEquals(0, fallback.batchSaveOrUpdate(emptyList()))
        val item = SysI18nFormUpdate(
            id = "id-1",
            locale = "zh_CN",
            atomicServiceCode = "atomic",
            i18nTypeDictCode = "type",
            namespace = "ns",
            key = "k",
            value = "v",
            remark = null,
        )
        assertEquals(0, fallback.batchSaveOrUpdate(listOf(item, item)))
    }

    @Test
    fun updateActive_returnsFalse_forBothFlagValues() {
        assertFalse(fallback.updateActive("id-1", true))
        assertFalse(fallback.updateActive("id-1", false))
        assertFalse(fallback.updateActive("", true))
    }
}
