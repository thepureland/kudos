package io.kudos.ms.sys.common.resource.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysResourceDetail
 *
 * Covers default values, full-args construction, the mutable parentIds extra field,
 * IIdEntity contract and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysResourceDetailTest {

    @Test
    fun defaults() {
        val d = SysResourceDetail()
        assertEquals("", d.id)
        assertEquals("", d.name)
        assertNull(d.url)
        assertEquals("", d.resourceTypeDictCode)
        assertNull(d.parentId)
        assertNull(d.orderNum)
        assertNull(d.icon)
        assertEquals("", d.subSystemCode)
        assertNull(d.remark)
        assertTrue(d.active)
        assertEquals(false, d.builtIn)
        assertNull(d.createTime)
        assertNull(d.parentIds)
        assertTrue(d is IIdEntity<*>)
    }

    @Test
    fun fullArgsAndParentIds() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val d = SysResourceDetail(
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
            createUserId = "u1",
            createUserName = "User1",
            createTime = now,
            updateUserId = "u2",
            updateUserName = "User2",
            updateTime = now,
        )
        d.parentIds = listOf("a", "b")
        assertEquals("r-1", d.id)
        assertEquals("Menu", d.name)
        assertEquals(false, d.active)
        assertEquals(true, d.builtIn)
        assertEquals(now, d.createTime)
        assertEquals(listOf("a", "b"), d.parentIds)
    }

    @Test
    fun dataClassContract() {
        val a = SysResourceDetail(id = "r-1", name = "Menu")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "other"))
        assertTrue(a.toString().contains("r-1"))
    }

    @Test
    fun parentIdsNotPartOfDataClassEquality() {
        val a = SysResourceDetail(id = "r-1")
        val b = a.copy()
        a.parentIds = listOf("x")
        // parentIds is declared in the class body, not the primary constructor,
        // so it is excluded from equals/hashCode.
        assertEquals(a, b)
    }

}
