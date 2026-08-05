package io.kudos.ms.sys.common.cache.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysCacheEdit
 *
 * Covers default values, full-args construction, IIdEntity contract and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysCacheEditTest {

    @Test
    fun defaults() {
        val e = SysCacheEdit()
        assertEquals("", e.id)
        assertEquals("", e.name)
        assertEquals("", e.atomicServiceCode)
        assertEquals("", e.strategyDictCode)
        assertEquals(true, e.writeOnBoot)
        assertEquals(true, e.writeInTime)
        assertNull(e.ttl)
        assertNull(e.remark)
        assertEquals(false, e.hash)
    }

    @Test
    fun fullArgs() {
        val e = SysCacheEdit(
            id = "c-1",
            name = "userCache",
            atomicServiceCode = "sys",
            strategyDictCode = "LOCAL",
            writeOnBoot = false,
            writeInTime = false,
            ttl = 60,
            remark = "r",
            hash = true,
        )
        assertTrue(e is IIdEntity<*>)
        assertEquals("c-1", e.id)
        assertEquals(60, e.ttl)
        assertEquals(true, e.hash)
    }

    @Test
    fun dataClassContract() {
        val a = SysCacheEdit(id = "c-1", name = "userCache")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(hash = true))
        assertTrue(a.toString().contains("c-1"))
    }

}
