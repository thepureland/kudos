package io.kudos.ms.user.common.org.vo.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for UserOrgDetail / UserOrgEdit / UserOrgRow / UserOrgTreeRow
 *
 * UserOrgTreeRow additionally has a mutable, recursive `children` list which is exercised here.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserOrgResponseVosTest {

    @Test
    fun detail() {
        val d = UserOrgDetail()
        assertEquals("", d.id)
        assertNull(d.name)
        val full = d.copy(id = "o-1", name = "HQ", sortNum = 1, active = true)
        assertEquals("HQ", full.name)
        assertEquals(1, full.sortNum)
        assertEquals(full, full.copy())
        assertNotEquals(full, full.copy(name = "Branch"))
        assertTrue(full.toString().contains("HQ"))
    }

    @Test
    fun edit() {
        val e = UserOrgEdit(id = "o-1", name = "HQ", parentId = "p-1")
        assertEquals("p-1", e.parentId)
        assertEquals(e, e.copy())
        assertNotEquals(e, e.copy(id = "o-2"))
    }

    @Test
    fun row() {
        val r = UserOrgRow()
        assertEquals("", r.id)
        val full = r.copy(id = "o-1", name = "HQ", builtIn = true)
        assertEquals(true, full.builtIn)
        assertEquals(full, full.copy())
        assertNotEquals(full, full.copy(builtIn = false))
    }

    @Test
    fun treeRowDefaultsAndChildren() {
        val node = UserOrgTreeRow()
        assertEquals("", node.id)
        assertNull(node.children)

        val child = UserOrgTreeRow(id = "c-1", name = "Child", parentId = "o-1")
        val parent = UserOrgTreeRow(id = "o-1", name = "HQ", children = mutableListOf(child))
        assertEquals(1, parent.children!!.size)
        assertSame(child, parent.children!![0])

        // children is a var: can append after construction
        val grandChild = UserOrgTreeRow(id = "gc-1", name = "GrandChild", parentId = "c-1")
        child.children = mutableListOf(grandChild)
        assertEquals("gc-1", parent.children!![0].children!![0].id)
    }

    @Test
    fun treeRowDataClassContract() {
        val a = UserOrgTreeRow(id = "o-1", name = "HQ")
        assertEquals(a, a.copy())
        assertEquals(a.hashCode(), a.copy().hashCode())
        assertNotEquals(a, a.copy(name = "Branch"))
        assertTrue(a.toString().contains("HQ"))
    }

}
