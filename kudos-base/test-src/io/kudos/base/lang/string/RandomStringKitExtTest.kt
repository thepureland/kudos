package io.kudos.base.lang.string

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * RandomStringKit supplemental test cases (complements RandomStringKitTest).
 *
 * Targets the overloads not exercised elsewhere: random(count, start, end, letters, numbers, vararg chars)
 * and random(count, chars: String?) with an explicit null argument.
 *
 * @author K
 * @since 1.0.0
 */
internal class RandomStringKitExtTest {

    @Test
    fun random_withRangeAndVarargChars() {
        val pool = charArrayOf('A', 'B', 'C')
        // 6th overload: vararg chars
        val s = RandomStringKit.random(10, 0, 3, letters = true, numbers = false, *pool)
        assertEquals(10, s.length)
        s.forEach { assertTrue(it in pool, "Unexpected char: $it") }
    }

    @Test
    fun random_withCharsStringNull() {
        // chars = null path of random(count, chars: String?)
        val s = RandomStringKit.random(12, null as String?)
        assertEquals(12, s.length)
    }

}
