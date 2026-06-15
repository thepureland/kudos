package io.kudos.ms.auth.common.role.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * test for IAuthRoleFormBase
 *
 * Covers the interface contract via an anonymous implementation, exercising every declared
 * property getter (including the @get:MaxLength-annotated remark).
 *
 * @author K
 * @since 1.0.0
 */
internal class IAuthRoleFormBaseTest {

    private fun impl(
        parentId: String?,
        approvalRequired: Boolean?,
        dataScope: String?,
        remark: String?,
    ): IAuthRoleFormBase = object : IAuthRoleFormBase {
        override val code = "ROLE1"
        override val name = "Role 1"
        override val tenantId = "t-1"
        override val subsysCode = "sys"
        override val parentId = parentId
        override val approvalRequired = approvalRequired
        override val dataScope = dataScope
        override val remark = remark
    }

    @Test
    fun propertyGetters() {
        val f = impl("p-1", true, "ORG", "r")
        assertEquals("ROLE1", f.code)
        assertEquals("Role 1", f.name)
        assertEquals("t-1", f.tenantId)
        assertEquals("sys", f.subsysCode)
        assertEquals("p-1", f.parentId)
        assertEquals(true, f.approvalRequired)
        assertEquals("ORG", f.dataScope)
        assertEquals("r", f.remark)
    }

    @Test
    fun nullableGetters() {
        val f = impl(null, null, null, null)
        assertNull(f.parentId)
        assertNull(f.approvalRequired)
        assertNull(f.dataScope)
        assertNull(f.remark)
    }
}
