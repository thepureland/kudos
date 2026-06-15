package io.kudos.base.bean.validation.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Test cases for [TerminalConstraint], focusing on its custom equals/hashCode
 * (which intentionally ignore the [TerminalConstraint.rule] array and key only on prop + constraint).
 *
 * @author K
 * @since 1.0.0
 */
internal class TerminalConstraintTest {

    private fun rule(vararg pairs: Pair<String, Any>): Array<Map<String, Any>> = arrayOf(mapOf(*pairs))

    @Test
    fun equalsSameInstance() {
        val tc = TerminalConstraint("name", "NotNull", rule("message" to "m"))
        @Suppress("KotlinConstantConditions")
        assertTrue(tc == tc)
    }

    @Test
    fun equalsIgnoresRule() {
        val a = TerminalConstraint("name", "NotNull", rule("message" to "x"))
        val b = TerminalConstraint("name", "NotNull", rule("message" to "y", "extra" to "z"))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun notEqualsWhenPropDiffers() {
        val a = TerminalConstraint("name", "NotNull", rule())
        val b = TerminalConstraint("age", "NotNull", rule())
        assertNotEquals(a, b)
    }

    @Test
    fun notEqualsWhenConstraintDiffers() {
        val a = TerminalConstraint("name", "NotNull", rule())
        val b = TerminalConstraint("name", "Size", rule())
        assertNotEquals(a, b)
    }

    @Test
    fun notEqualsWithNullOrOtherType() {
        val a = TerminalConstraint("name", "NotNull", rule())
        assertFalse(a.equals(null))
        assertFalse(a.equals("not a terminal constraint"))
    }

    @Test
    fun hashCodeIsPropAndConstraintBased() {
        val a = TerminalConstraint("name", "NotNull", rule("message" to "m"))
        val expected = "name".hashCode() * 31 + "NotNull".hashCode()
        // equivalent to: prop.hashCode()*31 + constraint.hashCode()
        assertEquals(expected, a.hashCode())
    }

    @Test
    fun copyRetainsEqualityKey() {
        val a = TerminalConstraint("name", "NotNull", rule("message" to "m"))
        val copy = a.copy(constraint = "Pattern")
        assertNotEquals(a, copy)
        assertEquals("name", copy.prop)
        assertEquals("Pattern", copy.constraint)
    }
}
