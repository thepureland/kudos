package io.kudos.ms.sys.common.resource.vo.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysResourceRow
 *
 * Covers default values, full-args construction and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysResourceRowTest {

    @Test
    fun defaults() {
        val r = SysResourceRow()
        assertEquals("", r.id)
        assertEquals("", r.name)
        assertNull(r.url)
        assertEquals("", r.resourceTypeDictCode)
        assertNull(r.parentId)
        assertNull(r.orderNum)
        assertNull(r.icon)
        assertEquals("", r.subSystemCode)
        assertNull(r.remark)
        assertTrue(r.active)
        assertEquals(false, r.builtIn)
    }

    @Test
    fun fullArgs() {
        val r = SysResourceRow(
            id = "r-1",
            name = "Menu",
            url = "/u",
            resourceTypeDictCode = "1",
            parentId = "p-1",
            orderNum = 2,
            icon = "ic",
            subSystemCode = "sys",
            remark = "r",
            active = false,
            builtIn = true,
        )
        assertEquals("r-1", r.id)
        assertEquals(false, r.active)
        assertEquals(true, r.builtIn)
    }

    @Test
    fun dataClassContract() {
        val a = SysResourceRow(id = "r-1", name = "Menu")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "other"))
        assertTrue(a.toString().contains("r-1"))
    }

}
