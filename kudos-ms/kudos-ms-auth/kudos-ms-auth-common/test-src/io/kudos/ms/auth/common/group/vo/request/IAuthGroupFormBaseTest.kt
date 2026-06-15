package io.kudos.ms.auth.common.group.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * test for IAuthGroupFormBase
 *
 * Covers the interface contract via an anonymous implementation, exercising every declared
 * property getter (including the @get:MaxLength-annotated remark).
 *
 * @author K
 * @since 1.0.0
 */
internal class IAuthGroupFormBaseTest {

    private fun impl(remark: String?): IAuthGroupFormBase = object : IAuthGroupFormBase {
        override val code = "G1"
        override val name = "Group 1"
        override val tenantId = "t-1"
        override val subsysCode = "sys"
        override val remark = remark
    }

    @Test
    fun propertyGetters() {
        val f = impl("r")
        assertEquals("G1", f.code)
        assertEquals("Group 1", f.name)
        assertEquals("t-1", f.tenantId)
        assertEquals("sys", f.subsysCode)
        assertEquals("r", f.remark)
    }

    @Test
    fun nullRemark() {
        assertNull(impl(null).remark)
    }
}
