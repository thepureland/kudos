package io.kudos.ms.sys.common.dict.vo.response

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDictItemDetail
 *
 * Covers default values, full-args construction and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictItemDetailTest {

    @Test
    fun defaults() {
        val d = SysDictItemDetail()
        assertEquals("", d.id)
        assertEquals("", d.itemCode)
        assertEquals("", d.itemName)
        assertEquals("", d.dictId)
        assertNull(d.orderNum)
        assertNull(d.parentId)
        assertNull(d.remark)
        assertTrue(d.active)
        assertTrue(d.builtIn)
        assertNull(d.createUserId)
        assertNull(d.createUserName)
        assertNull(d.createTime)
        assertNull(d.updateUserId)
        assertNull(d.updateUserName)
        assertNull(d.updateTime)
    }

    @Test
    fun fullArgsConstruction() {
        val now = LocalDateTime.of(2030, 10, 11, 12, 13)
        val d = SysDictItemDetail(
            id = "i-1",
            itemCode = "male",
            itemName = "Male",
            dictId = "d-1",
            orderNum = 5,
            parentId = "p-1",
            remark = "r",
            active = false,
            builtIn = false,
            createUserId = "u-1",
            createUserName = "alice",
            createTime = now,
            updateUserId = "u-2",
            updateUserName = "bob",
            updateTime = now,
        )
        assertEquals("i-1", d.id)
        assertEquals("male", d.itemCode)
        assertEquals("Male", d.itemName)
        assertEquals("d-1", d.dictId)
        assertEquals(5, d.orderNum)
        assertEquals("p-1", d.parentId)
        assertEquals("r", d.remark)
        assertEquals(false, d.active)
        assertEquals(false, d.builtIn)
        assertEquals("u-1", d.createUserId)
        assertEquals("alice", d.createUserName)
        assertEquals(now, d.createTime)
        assertEquals("u-2", d.updateUserId)
        assertEquals("bob", d.updateUserName)
        assertEquals(now, d.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysDictItemDetail(id = "i-1", itemCode = "male")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(orderNum = 9))
        assertTrue(a.toString().contains("male"))
    }

}
