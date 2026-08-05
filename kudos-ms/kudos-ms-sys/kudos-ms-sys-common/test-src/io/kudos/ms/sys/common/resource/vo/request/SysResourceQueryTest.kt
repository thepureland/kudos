package io.kudos.ms.sys.common.resource.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.sys.common.resource.vo.response.SysResourceRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysResourceQuery
 *
 * Covers default values (active defaults to true, others null), full-args construction,
 * ListSearchPayload overrides and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysResourceQueryTest {

    @Test
    fun defaults() {
        val q = SysResourceQuery()
        assertNull(q.name)
        assertNull(q.url)
        assertNull(q.resourceTypeDictCode)
        assertNull(q.subSystemCode)
        assertEquals(true, q.active)
        assertNull(q.parentId)
        assertNull(q.level)
    }

    @Test
    fun fullArgs() {
        val q = SysResourceQuery(
            name = "menu",
            url = "/u",
            resourceTypeDictCode = "1",
            subSystemCode = "sys",
            active = false,
            parentId = "p-1",
            level = 2,
        )
        assertEquals("menu", q.name)
        assertEquals("/u", q.url)
        assertEquals("1", q.resourceTypeDictCode)
        assertEquals("sys", q.subSystemCode)
        assertEquals(false, q.active)
        assertEquals("p-1", q.parentId)
        assertEquals(2, q.level)
    }

    @Test
    fun activeIsMutable() {
        val q = SysResourceQuery()
        q.active = null
        assertNull(q.active)
    }

    @Test
    fun payloadOverrides() {
        val q = SysResourceQuery()
        assertEquals(SysResourceRow::class, q.getReturnEntityClass())
        assertTrue(q.isUnpagedSearchAllowed())

        val byName = q.getOperators().mapKeys { it.key.name }
        assertEquals(1, byName.size)
        assertEquals(OperatorEnum.ILIKE, byName["name"])
    }

    @Test
    fun dataClassContract() {
        val a = SysResourceQuery(name = "menu")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "other"))
        assertTrue(a.toString().contains("menu"))
    }

}
