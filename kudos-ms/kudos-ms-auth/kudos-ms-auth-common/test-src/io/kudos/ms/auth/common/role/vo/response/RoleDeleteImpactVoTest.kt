package io.kudos.ms.auth.common.role.vo.response

import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for RoleDeleteImpactVo
 *
 * Covers the [RoleDeleteImpactVo.zero] factory, explicit construction, Serializable marker
 * and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class RoleDeleteImpactVoTest {

    @Test
    fun zeroFactory() {
        val vo = RoleDeleteImpactVo.zero()
        assertEquals(0, vo.users)
        assertEquals(0, vo.groups)
    }

    @Test
    fun construction() {
        val vo = RoleDeleteImpactVo(users = 7, groups = 2)
        assertEquals(7, vo.users)
        assertEquals(2, vo.groups)
    }

    @Test
    fun isSerializable() {
        assertTrue(RoleDeleteImpactVo.zero() is Serializable)
    }

    @Test
    fun dataClassContract() {
        val a = RoleDeleteImpactVo(users = 7, groups = 2)
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(groups = 3))
        assertNotEquals(RoleDeleteImpactVo.zero(), a)
        assertTrue(a.toString().contains("7"))
    }
}
