package io.kudos.ms.sys.common.i18n.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for SysI18nBatchPayload
 *
 * Covers default values (empty locale / empty map / empty set), full-args construction
 * and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysI18nBatchPayloadTest {

    @Test
    fun defaults() {
        val p = SysI18nBatchPayload()
        assertEquals("", p.locale)
        assertTrue(p.namespacesByI18nTypeDictCode.isEmpty())
        assertTrue(p.atomicServiceCodes.isEmpty())
    }

    @Test
    fun fullArgs() {
        val p = SysI18nBatchPayload(
            locale = "zh_CN",
            namespacesByI18nTypeDictCode = mapOf("ui" to listOf("common", "form")),
            atomicServiceCodes = setOf("sys", "auth"),
        )
        assertEquals("zh_CN", p.locale)
        assertEquals(listOf("common", "form"), p.namespacesByI18nTypeDictCode["ui"])
        assertEquals(setOf("sys", "auth"), p.atomicServiceCodes)
    }

    @Test
    fun dataClassContract() {
        val a = SysI18nBatchPayload(locale = "en_US")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(locale = "zh_CN"))
        assertTrue(a.toString().contains("en_US"))
    }

}
