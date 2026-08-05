package io.kudos.ms.sys.common.resource.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysResourceEdit
 *
 * Covers default values, full-args construction, the mutable parentIds extra field,
 * IIdEntity contract and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysResourceEditTest {

    @Test
    fun defaults() {
        val e = SysResourceEdit()
        assertEquals("", e.id)
        assertEquals("", e.name)
        assertNull(e.url)
        assertEquals("", e.resourceTypeDictCode)
        assertNull(e.parentId)
        assertNull(e.orderNum)
        assertNull(e.icon)
        assertEquals("", e.subSystemCode)
        assertNull(e.remark)
        assertTrue(e.active)
        assertEquals(false, e.builtIn)
        assertNull(e.parentIds)
        assertTrue(e is IIdEntity<*>)
    }

    @Test
    fun fullArgsAndParentIds() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val e = SysResourceEdit(
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
            createTime = now,
            updateTime = now,
        )
        e.parentIds = listOf("a")
        assertEquals("r-1", e.id)
        assertEquals(false, e.active)
        assertEquals(true, e.builtIn)
        assertEquals(now, e.updateTime)
        assertEquals(listOf("a"), e.parentIds)
    }

    @Test
    fun dataClassContract() {
        val a = SysResourceEdit(id = "r-1", name = "Menu")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "other"))
        assertTrue(a.toString().contains("r-1"))
    }

}
