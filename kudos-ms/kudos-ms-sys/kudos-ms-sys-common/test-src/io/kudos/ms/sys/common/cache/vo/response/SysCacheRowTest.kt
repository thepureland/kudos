package io.kudos.ms.sys.common.cache.vo.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysCacheRow
 *
 * Covers default values, full-args construction and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysCacheRowTest {

    @Test
    fun defaults() {
        val r = SysCacheRow()
        assertEquals("", r.id)
        assertEquals("", r.name)
        assertEquals("", r.atomicServiceCode)
        assertEquals("", r.strategyDictCode)
        assertEquals(true, r.writeOnBoot)
        assertEquals(true, r.writeInTime)
        assertNull(r.ttl)
        assertNull(r.remark)
        assertEquals(true, r.active)
        assertEquals(true, r.builtIn)
        assertEquals(false, r.hash)
    }

    @Test
    fun fullArgs() {
        val r = SysCacheRow(
            id = "c-1",
            name = "userCache",
            atomicServiceCode = "sys",
            strategyDictCode = "LOCAL",
            writeOnBoot = false,
            writeInTime = false,
            ttl = 60,
            remark = "r",
            active = false,
            builtIn = false,
            hash = true,
        )
        assertEquals("c-1", r.id)
        assertEquals(60, r.ttl)
        assertEquals(true, r.hash)
    }

    @Test
    fun dataClassContract() {
        val a = SysCacheRow(id = "c-1", name = "userCache")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = false))
        assertTrue(a.toString().contains("c-1"))
    }

}
