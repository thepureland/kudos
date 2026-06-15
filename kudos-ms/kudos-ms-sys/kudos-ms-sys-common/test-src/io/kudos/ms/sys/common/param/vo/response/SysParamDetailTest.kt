package io.kudos.ms.sys.common.param.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysParamDetail
 *
 * Covers default values, full-args construction, IIdEntity contract and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysParamDetailTest {

    @Test
    fun defaults() {
        val d = SysParamDetail()
        assertEquals("", d.id)
        assertEquals("", d.paramName)
        assertEquals("", d.paramValue)
        assertNull(d.defaultValue)
        assertEquals("", d.atomicServiceCode)
        assertNull(d.orderNum)
        assertNull(d.remark)
        assertEquals(true, d.active)
        assertEquals(true, d.builtIn)
        assertNull(d.createTime)
        assertNull(d.updateTime)
    }

    @Test
    fun fullArgs() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val d = SysParamDetail(
            id = "p-1",
            paramName = "max.size",
            paramValue = "100",
            defaultValue = "50",
            atomicServiceCode = "sys",
            orderNum = 1,
            remark = "r",
            active = false,
            builtIn = false,
            createUserId = "u1",
            createUserName = "creator",
            createTime = now,
            updateUserId = "u2",
            updateUserName = "updater",
            updateTime = now,
        )
        assertTrue(d is IIdEntity<*>)
        assertEquals("p-1", d.id)
        assertEquals(1, d.orderNum)
        assertEquals(false, d.active)
        assertEquals(now, d.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysParamDetail(id = "p-1", paramName = "max.size")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = false))
        assertTrue(a.toString().contains("p-1"))
    }

}
