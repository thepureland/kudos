package io.kudos.ms.sys.common.accessrule.vo.response

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysAccessRuleEdit
 *
 * Covers default values, full-args construction and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAccessRuleEditTest {

    @Test
    fun defaults() {
        val e = SysAccessRuleEdit()
        assertEquals("", e.id)
        assertNull(e.tenantId)
        assertNull(e.systemCode)
        assertNull(e.accessRuleTypeDictCode)
        assertNull(e.remark)
        assertNull(e.createUserId)
        assertNull(e.createUserName)
        assertNull(e.createTime)
        assertNull(e.updateUserId)
        assertNull(e.updateUserName)
        assertNull(e.updateTime)
    }

    @Test
    fun fullArgsConstruction() {
        val now = LocalDateTime.of(2030, 3, 4, 5, 6)
        val e = SysAccessRuleEdit(
            id = "id-1",
            tenantId = "t-1",
            systemCode = "sys-a",
            accessRuleTypeDictCode = "3",
            remark = "r",
            createUserId = "u-1",
            createUserName = "alice",
            createTime = now,
            updateUserId = "u-2",
            updateUserName = "bob",
            updateTime = now,
        )
        assertEquals("id-1", e.id)
        assertEquals("t-1", e.tenantId)
        assertEquals("sys-a", e.systemCode)
        assertEquals("3", e.accessRuleTypeDictCode)
        assertEquals("r", e.remark)
        assertEquals("u-1", e.createUserId)
        assertEquals("alice", e.createUserName)
        assertEquals(now, e.createTime)
        assertEquals("u-2", e.updateUserId)
        assertEquals("bob", e.updateUserName)
        assertEquals(now, e.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysAccessRuleEdit(id = "id-1", remark = "r")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(remark = "x"))
        assertTrue(a.toString().contains("id-1"))
    }

}
