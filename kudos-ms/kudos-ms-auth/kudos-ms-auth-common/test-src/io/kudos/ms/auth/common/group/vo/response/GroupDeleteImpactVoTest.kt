package io.kudos.ms.auth.common.group.vo.response

import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for GroupDeleteImpactVo
 *
 * Covers the [GroupDeleteImpactVo.zero] factory, explicit construction, Serializable marker
 * and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class GroupDeleteImpactVoTest {

    @Test
    fun zeroFactory() {
        val vo = GroupDeleteImpactVo.zero()
        assertEquals(0, vo.users)
        assertEquals(0, vo.roles)
    }

    @Test
    fun construction() {
        val vo = GroupDeleteImpactVo(users = 5, roles = 3)
        assertEquals(5, vo.users)
        assertEquals(3, vo.roles)
    }

    @Test
    fun isSerializable() {
        assertTrue(GroupDeleteImpactVo.zero() is Serializable)
    }

    @Test
    fun dataClassContract() {
        val a = GroupDeleteImpactVo(users = 5, roles = 3)
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(roles = 4))
        assertNotEquals(GroupDeleteImpactVo.zero(), a)
        assertTrue(a.toString().contains("5"))
    }
}
