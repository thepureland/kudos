package io.kudos.ms.sys.common.resource.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysResourceFormUpdate
 *
 * Covers id (IIdEntity) plus ISysResourceFormBase property accessors,
 * nullable fields and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysResourceFormUpdateTest {

    private fun form() = SysResourceFormUpdate(
        id = "00000000-0000-0000-0000-000000000001",
        name = "Menu",
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
        assertTrue(f is IIdEntity<*>)
        assertEquals("00000000-0000-0000-0000-000000000001", f.id)
        assertEquals("Menu", f.name)
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
        assertNotEquals(a, a.copy(id = "x"))
        assertTrue(a.toString().contains("Menu"))
    }

}
