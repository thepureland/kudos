package io.kudos.ms.auth.common.exclusion.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthRoleExclusionFormUpdate
 *
 * Covers the IIdEntity id override, default null description and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleExclusionFormUpdateTest {

    @Test
    fun idAndDefaultDescription() {
        val f = AuthRoleExclusionFormUpdate(id = "e-1")
        assertTrue(f is IIdEntity<*>)
        assertEquals("e-1", f.id)
        assertNull(f.description)
    }

    @Test
    fun withDescription() {
        val f = AuthRoleExclusionFormUpdate(id = "e-1", description = "updated reason")
        assertEquals("updated reason", f.description)
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleExclusionFormUpdate(id = "e-1", description = "d")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "e-2"))
        assertNotEquals(a, a.copy(description = null))
        assertTrue(a.toString().contains("e-1"))
    }
}
