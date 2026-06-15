package io.kudos.ms.user.common.contact.vo.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for UserContactWayDetail / UserContactWayEdit / UserContactWayRow
 *
 * @author K
 * @since 1.0.0
 */
internal class UserContactWayResponseVosTest {

    @Test
    fun detail() {
        val d = UserContactWayDetail()
        assertEquals("", d.id)
        assertNull(d.priority)
        val full = d.copy(id = "cw-1", contactWayValue = "a@b.com", priority = 10.toShort(), active = true)
        assertEquals("a@b.com", full.contactWayValue)
        assertEquals(10.toShort(), full.priority)
        assertEquals(full, full.copy())
        assertNotEquals(full, full.copy(priority = 20.toShort()))
        assertTrue(full.toString().contains("cw-1"))
    }

    @Test
    fun edit() {
        val e = UserContactWayEdit(id = "cw-1", contactWayDictCode = "email", priority = 5.toShort())
        assertEquals("email", e.contactWayDictCode)
        assertEquals(5.toShort(), e.priority)
        assertEquals(e, e.copy())
        assertNotEquals(e, e.copy(id = "cw-2"))
        assertTrue(e.toString().contains("email"))
    }

    @Test
    fun row() {
        val r = UserContactWayRow()
        assertEquals("", r.id)
        val full = r.copy(id = "cw-1", contactWayValue = "a@b.com", active = false)
        assertEquals(false, full.active)
        assertEquals(full, full.copy())
        assertNotEquals(full, full.copy(active = true))
        assertTrue(full.toString().contains("cw-1"))
    }

}
