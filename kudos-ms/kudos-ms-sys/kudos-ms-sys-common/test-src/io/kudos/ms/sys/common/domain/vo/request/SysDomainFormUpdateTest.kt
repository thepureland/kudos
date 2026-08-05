package io.kudos.ms.sys.common.domain.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDomainFormUpdate
 *
 * Covers id (IIdEntity) plus ISysDomainFormBase property accessors, nullable remark
 * and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDomainFormUpdateTest {

    private fun form() = SysDomainFormUpdate(
        id = "00000000-0000-0000-0000-000000000001",
        domain = "example.com",
        systemCode = "sys",
        tenantId = "t-1",
        remark = "r",
    )

    @Test
    fun properties() {
        val f = form()
        assertTrue(f is ISysDomainFormBase)
        assertTrue(f is IIdEntity<*>)
        assertEquals("00000000-0000-0000-0000-000000000001", f.id)
        assertEquals("example.com", f.domain)
        assertEquals("sys", f.systemCode)
        assertEquals("t-1", f.tenantId)
        assertEquals("r", f.remark)
    }

    @Test
    fun nullableRemark() {
        val f = form().copy(remark = null)
        assertNull(f.remark)
    }

    @Test
    fun dataClassContract() {
        val a = form()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "x"))
        assertTrue(a.toString().contains("example.com"))
    }

}
