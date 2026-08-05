package io.kudos.ms.sys.common.cache.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysCacheDetail
 *
 * Covers default values, full-args construction, IIdEntity contract and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysCacheDetailTest {

    @Test
    fun defaults() {
        val d = SysCacheDetail()
        assertEquals("", d.id)
        assertEquals("", d.name)
        assertEquals("", d.atomicServiceCode)
        assertEquals("", d.strategyDictCode)
        assertEquals(true, d.writeOnBoot)
        assertEquals(true, d.writeInTime)
        assertNull(d.ttl)
        assertNull(d.remark)
        assertEquals(true, d.active)
        assertEquals(true, d.builtIn)
        assertEquals(false, d.hash)
        assertNull(d.createTime)
        assertNull(d.updateTime)
    }

    @Test
    fun fullArgs() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val d = SysCacheDetail(
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
            createUserId = "u1",
            createUserName = "creator",
            createTime = now,
            updateUserId = "u2",
            updateUserName = "updater",
            updateTime = now,
        )
        assertTrue(d is IIdEntity<*>)
        assertEquals("c-1", d.id)
        assertEquals(60, d.ttl)
        assertEquals(true, d.hash)
        assertEquals(now, d.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysCacheDetail(id = "c-1", name = "userCache")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(hash = true))
        assertTrue(a.toString().contains("c-1"))
    }

}
