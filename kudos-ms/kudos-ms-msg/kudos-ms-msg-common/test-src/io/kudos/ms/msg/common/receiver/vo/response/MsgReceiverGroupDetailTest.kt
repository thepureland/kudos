package io.kudos.ms.msg.common.receiver.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgReceiverGroupDetail
 *
 * Covers defaults, full construction (incl. audit fields and Boolean flags),
 * IIdEntity contract and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiverGroupDetailTest {

    @Test
    fun defaults() {
        val d = MsgReceiverGroupDetail()
        assertEquals("", d.id)
        assertNull(d.receiverGroupTypeDictCode)
        assertNull(d.active)
        assertNull(d.builtIn)
        assertNull(d.createUserId)
        assertNull(d.updateTime)
        assertTrue(d is IIdEntity<*>)
    }

    @Test
    fun fullConstruction() {
        val ct = LocalDateTime.of(2026, 1, 1, 0, 0)
        val ut = LocalDateTime.of(2026, 1, 2, 0, 0)
        val d = MsgReceiverGroupDetail(
            id = "g-1", receiverGroupTypeDictCode = "dept", defineTable = "tbl", nameColumn = "name",
            remark = "r", active = true, builtIn = false, createUserId = "cu", createUserName = "cn",
            createTime = ct, updateUserId = "uu", updateUserName = "un", updateTime = ut,
        )
        assertEquals("g-1", d.id)
        assertEquals(true, d.active)
        assertEquals(false, d.builtIn)
        assertEquals("cu", d.createUserId)
        assertEquals("un", d.updateUserName)
        assertEquals(ct, d.createTime)
        assertEquals(ut, d.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = MsgReceiverGroupDetail(id = "g-1", remark = "r")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(remark = "x"))
        assertTrue(a.toString().contains("g-1"))
    }

}
