package io.kudos.ms.sys.common.param.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysParamEdit
 *
 * Covers default values, full-args construction, IIdEntity contract and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysParamEditTest {

    @Test
    fun defaults() {
        val e = SysParamEdit()
        assertEquals("", e.id)
        assertEquals("", e.paramName)
        assertEquals("", e.paramValue)
        assertNull(e.defaultValue)
        assertEquals("", e.atomicServiceCode)
        assertNull(e.orderNum)
        assertNull(e.remark)
        assertEquals(true, e.active)
        assertEquals(true, e.builtIn)
        assertNull(e.createTime)
        assertNull(e.updateTime)
    }

    @Test
    fun fullArgs() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val e = SysParamEdit(
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
        assertTrue(e is IIdEntity<*>)
        assertEquals("p-1", e.id)
        assertEquals(now, e.createTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysParamEdit(id = "p-1", paramName = "max.size")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(paramValue = "x"))
        assertTrue(a.toString().contains("p-1"))
    }

}
