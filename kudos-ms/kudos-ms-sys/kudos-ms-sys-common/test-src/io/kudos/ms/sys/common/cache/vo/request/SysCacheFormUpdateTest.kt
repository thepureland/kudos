package io.kudos.ms.sys.common.cache.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysCacheFormUpdate
 *
 * Covers property accessors (id + ISysCacheFormBase overrides), the writeOnBoot/writeInTime
 * defaults (true), IIdEntity contract, nullable ttl/remark and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysCacheFormUpdateTest {

    @Test
    fun properties() {
        val f = SysCacheFormUpdate(
            id = "c-1",
            strategyDictCode = "REMOTE",
            writeOnBoot = false,
            writeInTime = false,
            ttl = 120,
            remark = "r",
        )
        assertTrue(f is IIdEntity<*>)
        assertEquals("c-1", f.id)
        assertEquals("REMOTE", f.strategyDictCode)
        assertEquals(false, f.writeOnBoot)
        assertEquals(false, f.writeInTime)
        assertEquals(120, f.ttl)
        assertEquals("r", f.remark)
    }

    @Test
    fun defaultsAndNullables() {
        val f = SysCacheFormUpdate(id = "c-1", strategyDictCode = "LOCAL", ttl = null, remark = null)
        assertEquals(true, f.writeOnBoot)
        assertEquals(true, f.writeInTime)
        assertNull(f.ttl)
        assertNull(f.remark)
    }

    @Test
    fun dataClassContract() {
        val a = SysCacheFormUpdate(id = "c-1", strategyDictCode = "LOCAL", ttl = 1, remark = null)
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "c-2"))
        assertTrue(a.toString().contains("c-1"))
    }

}
