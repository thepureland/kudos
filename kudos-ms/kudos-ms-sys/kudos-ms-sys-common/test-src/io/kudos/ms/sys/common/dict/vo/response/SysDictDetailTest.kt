package io.kudos.ms.sys.common.dict.vo.response

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDictDetail
 *
 * Covers default values, full-args construction and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictDetailTest {

    @Test
    fun defaults() {
        val d = SysDictDetail()
        assertEquals("", d.id)
        assertEquals("", d.dictType)
        assertEquals("", d.dictName)
        assertEquals("", d.atomicServiceCode)
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
        val now = LocalDateTime.of(2030, 1, 1, 8, 0)
        val d = SysDictDetail(
            id = "d-1",
            dictType = "gender",
            dictName = "Gender",
            atomicServiceCode = "sys",
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
        assertEquals("d-1", d.id)
        assertEquals("gender", d.dictType)
        assertEquals("Gender", d.dictName)
        assertEquals("sys", d.atomicServiceCode)
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
        val a = SysDictDetail(id = "d-1", dictType = "gender")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = false))
        assertTrue(a.toString().contains("gender"))
    }

}
