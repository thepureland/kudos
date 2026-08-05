package io.kudos.ms.sys.common.resource.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysResourceFormCreate
 *
 * Covers ISysResourceFormBase property accessors (including nullable fields),
 * data-class contract and Unicode handling.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysResourceFormCreateTest {

    private fun form() = SysResourceFormCreate(
        name = "菜单",
        url = "/sys/resource",
        resourceTypeDictCode = "1",
        parentId = "p-1",
        orderNum = 3,
        icon = "icon-menu",
        subSystemCode = "sys",
        remark = "r",
    )

    @Test
    fun properties() {
        val f = form()
        assertTrue(f is ISysResourceFormBase)
        assertEquals("菜单", f.name)
        assertEquals("/sys/resource", f.url)
        assertEquals("1", f.resourceTypeDictCode)
        assertEquals("p-1", f.parentId)
        assertEquals(3, f.orderNum)
        assertEquals("icon-menu", f.icon)
        assertEquals("sys", f.subSystemCode)
        assertEquals("r", f.remark)
    }

    @Test
    fun nullableFields() {
        val f = form().copy(url = null, parentId = null, orderNum = null, icon = null, remark = null)
        assertNull(f.url)
        assertNull(f.parentId)
        assertNull(f.orderNum)
        assertNull(f.icon)
        assertNull(f.remark)
    }

    @Test
    fun dataClassContract() {
        val a = form()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "other"))
        assertTrue(a.toString().contains("菜单"))
    }

}
