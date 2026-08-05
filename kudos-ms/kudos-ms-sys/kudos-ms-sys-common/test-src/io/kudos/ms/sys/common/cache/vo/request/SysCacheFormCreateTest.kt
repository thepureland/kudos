package io.kudos.ms.sys.common.cache.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysCacheFormCreate
 *
 * Covers property accessors (own fields + ISysCacheFormBase overrides), nullable ttl/remark
 * and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysCacheFormCreateTest {

    private fun form(ttl: Int? = 60, remark: String? = "r") = SysCacheFormCreate(
        name = "userCache",
        atomicServiceCode = "sys",
        strategyDictCode = "LOCAL",
        writeOnBoot = true,
        writeInTime = false,
        ttl = ttl,
        remark = remark,
        hash = true,
    )

    @Test
    fun properties() {
        val f = form()
        assertTrue(f is ISysCacheFormBase)
        assertEquals("userCache", f.name)
        assertEquals("sys", f.atomicServiceCode)
        assertEquals("LOCAL", f.strategyDictCode)
        assertEquals(true, f.writeOnBoot)
        assertEquals(false, f.writeInTime)
        assertEquals(60, f.ttl)
        assertEquals("r", f.remark)
        assertEquals(true, f.hash)
    }

    @Test
    fun nullableFields() {
        val f = form(ttl = null, remark = null)
        assertNull(f.ttl)
        assertNull(f.remark)
    }

    @Test
    fun dataClassContract() {
        val a = form()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "other"))
        assertNotEquals(a, a.copy(hash = false))
        assertTrue(a.toString().contains("userCache"))
    }

}
