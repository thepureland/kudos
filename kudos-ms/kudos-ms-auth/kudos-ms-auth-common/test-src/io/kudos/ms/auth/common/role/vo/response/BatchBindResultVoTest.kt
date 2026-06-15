package io.kudos.ms.auth.common.role.vo.response

import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for BatchBindResultVo
 *
 * Covers the [BatchBindResultVo.empty] factory, partial-success construction with the nested
 * [BatchBindResultVo.BatchBindFailure], Serializable markers and data-class contracts.
 *
 * @author K
 * @since 1.0.0
 */
internal class BatchBindResultVoTest {

    @Test
    fun emptyFactory() {
        val vo = BatchBindResultVo.empty()
        assertEquals(0, vo.ok)
        assertTrue(vo.failures.isEmpty())
    }

    @Test
    fun partialSuccess() {
        val f1 = BatchBindResultVo.BatchBindFailure(ownerId = "r-1", reason = "duplicate")
        val f2 = BatchBindResultVo.BatchBindFailure(ownerId = "r-2", reason = "not found")
        val vo = BatchBindResultVo(ok = 3, failures = listOf(f1, f2))
        assertEquals(3, vo.ok)
        assertEquals(2, vo.failures.size)
        assertEquals("r-1", vo.failures[0].ownerId)
        assertEquals("duplicate", vo.failures[0].reason)
        assertEquals("not found", vo.failures[1].reason)
    }

    @Test
    fun areSerializable() {
        assertTrue(BatchBindResultVo.empty() is Serializable)
        assertTrue(BatchBindResultVo.BatchBindFailure("r-1", "x") is Serializable)
    }

    @Test
    fun resultDataClassContract() {
        val a = BatchBindResultVo(ok = 1, failures = emptyList())
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(ok = 2))
        assertTrue(a.toString().contains("ok"))
    }

    @Test
    fun failureDataClassContract() {
        val a = BatchBindResultVo.BatchBindFailure(ownerId = "r-1", reason = "duplicate")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(reason = "other"))
        assertTrue(a.toString().contains("r-1"))
    }
}
