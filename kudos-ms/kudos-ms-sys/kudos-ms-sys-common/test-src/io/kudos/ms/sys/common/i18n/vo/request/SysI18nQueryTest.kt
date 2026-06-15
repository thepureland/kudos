package io.kudos.ms.sys.common.i18n.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.sys.common.i18n.vo.response.SysI18nRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysI18nQuery
 *
 * Covers default values (active defaults to true, others null), full-args construction,
 * ListSearchPayload overrides (getReturnEntityClass / getOperators / isUnpagedSearchAllowed)
 * and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysI18nQueryTest {

    @Test
    fun defaults() {
        val q = SysI18nQuery()
        assertNull(q.locale)
        assertNull(q.atomicServiceCode)
        assertNull(q.i18nTypeDictCode)
        assertNull(q.namespace)
        assertNull(q.key)
        assertNull(q.value)
        assertEquals(true, q.active)
    }

    @Test
    fun fullArgs() {
        val q = SysI18nQuery(
            locale = "zh_CN",
            atomicServiceCode = "sys",
            i18nTypeDictCode = "ui",
            namespace = "common",
            key = "ok",
            value = "确定",
            active = false,
        )
        assertEquals("zh_CN", q.locale)
        assertEquals("sys", q.atomicServiceCode)
        assertEquals("ui", q.i18nTypeDictCode)
        assertEquals("common", q.namespace)
        assertEquals("ok", q.key)
        assertEquals("确定", q.value)
        assertEquals(false, q.active)
    }

    @Test
    fun payloadOverrides() {
        val q = SysI18nQuery()
        assertEquals(SysI18nRow::class, q.getReturnEntityClass())
        assertTrue(q.isUnpagedSearchAllowed())

        val byName = q.getOperators().mapKeys { it.key.name }
        assertEquals(2, byName.size)
        assertEquals(OperatorEnum.ILIKE, byName["key"])
        assertEquals(OperatorEnum.ILIKE, byName["namespace"])
    }

    @Test
    fun dataClassContract() {
        val a = SysI18nQuery(key = "ok")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = false))
        assertTrue(a.toString().contains("ok"))
    }

}
