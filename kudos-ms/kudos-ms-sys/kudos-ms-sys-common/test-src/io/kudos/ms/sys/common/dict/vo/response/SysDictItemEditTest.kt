package io.kudos.ms.sys.common.dict.vo.response

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDictItemEdit
 *
 * Covers default values, full-args construction and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictItemEditTest {

    @Test
    fun defaults() {
        val e = SysDictItemEdit()
        assertEquals("", e.id)
        assertEquals("", e.itemCode)
        assertEquals("", e.itemName)
        assertEquals("", e.dictId)
        assertNull(e.orderNum)
        assertNull(e.parentId)
        assertNull(e.remark)
        assertTrue(e.active)
        assertTrue(e.builtIn)
        assertNull(e.createUserId)
        assertNull(e.createUserName)
        assertNull(e.createTime)
        assertNull(e.updateUserId)
        assertNull(e.updateUserName)
        assertNull(e.updateTime)
    }

    @Test
    fun fullArgsConstruction() {
        val now = LocalDateTime.of(2031, 3, 4, 5, 6)
        val e = SysDictItemEdit(
            id = "i-1",
            itemCode = "male",
            itemName = "Male",
            dictId = "d-1",
            orderNum = 7,
            parentId = "p-1",
            remark = "r",
            active = false,
            builtIn = true,
            createUserId = "u-1",
            createUserName = "alice",
            createTime = now,
            updateUserId = "u-2",
            updateUserName = "bob",
            updateTime = now,
        )
        assertEquals("i-1", e.id)
        assertEquals("male", e.itemCode)
        assertEquals("Male", e.itemName)
        assertEquals("d-1", e.dictId)
        assertEquals(7, e.orderNum)
        assertEquals("p-1", e.parentId)
        assertEquals("r", e.remark)
        assertEquals(false, e.active)
        assertEquals(true, e.builtIn)
        assertEquals("u-1", e.createUserId)
        assertEquals("alice", e.createUserName)
        assertEquals(now, e.createTime)
        assertEquals("u-2", e.updateUserId)
        assertEquals("bob", e.updateUserName)
        assertEquals(now, e.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysDictItemEdit(id = "i-1", dictId = "d-1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(dictId = "d-2"))
        assertTrue(a.toString().contains("i-1"))
    }

}
