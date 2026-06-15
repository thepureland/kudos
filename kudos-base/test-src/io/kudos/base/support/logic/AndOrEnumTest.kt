package io.kudos.base.support.logic

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * AndOrEnum test cases.
 *
 * @author K
 * @since 1.0.0
 */
internal class AndOrEnumTest {

    @Test
    fun testEntries() {
        assertEquals(listOf(AndOrEnum.AND, AndOrEnum.OR), AndOrEnum.entries)
    }

    @Test
    fun testValueOf() {
        assertEquals(AndOrEnum.AND, AndOrEnum.valueOf("AND"))
        assertEquals(AndOrEnum.OR, AndOrEnum.valueOf("OR"))
    }

    @Test
    fun testNames() {
        assertEquals("AND", AndOrEnum.AND.name)
        assertEquals("OR", AndOrEnum.OR.name)
    }
}
