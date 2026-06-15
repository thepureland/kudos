package io.kudos.ms.sys.common.dict.vo.response

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDictEdit
 *
 * Covers default values, full-args construction and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictEditTest {

    @Test
    fun defaults() {
        val e = SysDictEdit()
        assertEquals("", e.id)
        assertEquals("", e.dictType)
        assertEquals("", e.dictName)
        assertEquals("", e.atomicServiceCode)
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
        val now = LocalDateTime.of(2030, 6, 7, 8, 9)
        val e = SysDictEdit(
            id = "d-1",
            dictType = "gender",
            dictName = "Gender",
            atomicServiceCode = "sys",
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
        assertEquals("d-1", e.id)
        assertEquals("gender", e.dictType)
        assertEquals("Gender", e.dictName)
        assertEquals("sys", e.atomicServiceCode)
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
        val a = SysDictEdit(id = "d-1", dictName = "Gender")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(dictName = "Other"))
        assertTrue(a.toString().contains("d-1"))
    }

}
