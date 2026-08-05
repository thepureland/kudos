package io.kudos.ms.auth.common.exclusion.vo.response

import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for AuthRoleExclusionViolationVo
 *
 * Covers construction, empty/non-empty violatingUserIds with order preservation, Serializable
 * marker and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleExclusionViolationVoTest {

    @Test
    fun construction() {
        val vo = AuthRoleExclusionViolationVo(
            exclusionId = "e-1", roleAId = "a", roleBId = "b",
            violatingUserIds = listOf("u3", "u1", "u2"),
        )
        assertEquals("e-1", vo.exclusionId)
        assertEquals("a", vo.roleAId)
        assertEquals("b", vo.roleBId)
        assertEquals(listOf("u3", "u1", "u2"), vo.violatingUserIds)
    }

    @Test
    fun emptyViolatingUsers() {
        val vo = AuthRoleExclusionViolationVo("e-1", "a", "b", emptyList())
        assertTrue(vo.violatingUserIds.isEmpty())
    }

    @Test
    fun isSerializable() {
        assertTrue(AuthRoleExclusionViolationVo("e-1", "a", "b", emptyList()) is Serializable)
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleExclusionViolationVo("e-1", "a", "b", listOf("u1"))
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(violatingUserIds = listOf("u2")))
        assertTrue(a.toString().contains("e-1"))
    }
}
