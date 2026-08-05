package io.kudos.ability.log.audit.common.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [AuditLogPage].
 *
 * Coverage:
 *  - Constructor + accessors carry the slice, total and echoed page coordinates verbatim.
 *  - [AuditLogPage.empty] returns a zero-total page with an empty item list while echoing pageNo/pageSize.
 *  - data-class semantics: equals/hashCode/copy/toString (structural equality on all four components).
 *
 * @author K
 * @since 1.0.0
 */
internal class AuditLogPageTest {

    @Test
    fun constructor_carriesAllComponents() {
        val row = SysAuditLogVo().apply { id = "a-1" }
        val page = AuditLogPage(items = listOf(row), total = 101L, pageNo = 3, pageSize = 20)

        assertEquals(1, page.items.size)
        assertEquals("a-1", page.items[0].id)
        assertEquals(101L, page.total)
        assertEquals(3, page.pageNo)
        assertEquals(20, page.pageSize)
    }

    @Test
    fun empty_returnsZeroTotalAndEchoesPageCoordinates() {
        val page = AuditLogPage.empty(pageNo = 7, pageSize = 50)
        assertTrue(page.items.isEmpty())
        assertEquals(0L, page.total)
        assertEquals(7, page.pageNo)
        assertEquals(50, page.pageSize)
    }

    @Test
    fun dataClassSemantics_equalsHashCodeCopyToString() {
        val a = AuditLogPage(emptyList(), 0L, 1, 10)
        val b = AuditLogPage(emptyList(), 0L, 1, 10)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())

        val c = a.copy(total = 5L)
        assertEquals(5L, c.total)
        assertEquals(1, c.pageNo)
        assertNotEquals(a, c)

        assertTrue(a.toString().contains("total=0"), "toString should expose components: ${a}")
    }
}
