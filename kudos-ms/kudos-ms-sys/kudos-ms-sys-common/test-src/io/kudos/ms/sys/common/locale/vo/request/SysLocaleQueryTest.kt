package io.kudos.ms.sys.common.locale.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.sys.common.locale.vo.response.SysLocaleRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysLocaleQuery
 *
 * Covers default values (all null), full-args construction, ListSearchPayload overrides
 * and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysLocaleQueryTest {

    @Test
    fun defaults() {
        val q = SysLocaleQuery()
        assertNull(q.code)
        assertNull(q.displayName)
        assertNull(q.active)
    }

    @Test
    fun fullArgs() {
        val q = SysLocaleQuery(code = "zh_CN", displayName = "简体中文", active = true)
        assertEquals("zh_CN", q.code)
        assertEquals("简体中文", q.displayName)
        assertEquals(true, q.active)
    }

    @Test
    fun payloadOverrides() {
        val q = SysLocaleQuery()
        assertEquals(SysLocaleRow::class, q.getReturnEntityClass())
        assertTrue(q.isUnpagedSearchAllowed())

        val byName = q.getOperators().mapKeys { it.key.name }
        assertEquals(2, byName.size)
        assertEquals(OperatorEnum.ILIKE, byName["code"])
        assertEquals(OperatorEnum.ILIKE, byName["displayName"])
    }

    @Test
    fun dataClassContract() {
        val a = SysLocaleQuery(code = "zh_CN")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = true))
        assertTrue(a.toString().contains("zh_CN"))
    }

}
