package io.kudos.ms.sys.common.accessrule.vo.response

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysAccessRuleDetail
 *
 * Covers default values, full-args construction and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAccessRuleDetailTest {

    @Test
    fun defaults() {
        val d = SysAccessRuleDetail()
        assertEquals("", d.id)
        assertNull(d.tenantId)
        assertNull(d.systemCode)
        assertNull(d.accessRuleTypeDictCode)
        assertNull(d.remark)
        assertNull(d.createUserId)
        assertNull(d.createUserName)
        assertNull(d.createTime)
        assertNull(d.updateUserId)
        assertNull(d.updateUserName)
        assertNull(d.updateTime)
    }

    @Test
    fun fullArgsConstruction() {
        val created = LocalDateTime.of(2030, 1, 1, 0, 0)
        val updated = LocalDateTime.of(2030, 2, 2, 0, 0)
        val d = SysAccessRuleDetail(
            id = "id-1",
            tenantId = "t-1",
            systemCode = "sys-a",
            accessRuleTypeDictCode = "2",
            remark = "r",
            createUserId = "u-1",
            createUserName = "alice",
            createTime = created,
            updateUserId = "u-2",
            updateUserName = "bob",
            updateTime = updated,
        )
        assertEquals("id-1", d.id)
        assertEquals("t-1", d.tenantId)
        assertEquals("sys-a", d.systemCode)
        assertEquals("2", d.accessRuleTypeDictCode)
        assertEquals("r", d.remark)
        assertEquals("u-1", d.createUserId)
        assertEquals("alice", d.createUserName)
        assertEquals(created, d.createTime)
        assertEquals("u-2", d.updateUserId)
        assertEquals("bob", d.updateUserName)
        assertEquals(updated, d.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysAccessRuleDetail(id = "id-1", systemCode = "sys-a")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "id-2"))
        assertTrue(a.toString().contains("sys-a"))
    }

}
