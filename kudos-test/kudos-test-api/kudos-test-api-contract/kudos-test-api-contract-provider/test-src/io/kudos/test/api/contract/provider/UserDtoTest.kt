package io.kudos.test.api.contract.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

/**
 * Test for [UserDto] data class (component access, equals/hashCode, copy, toString).
 *
 * @author K
 * @since 1.0.0
 */
internal class UserDtoTest {

    @Test
    fun properties_areExposedAsGiven() {
        val dto = UserDto(id = 7L, name = "Alice")
        assertEquals(7L, dto.id)
        assertEquals("Alice", dto.name)
    }

    @Test
    fun componentFunctions_followDeclarationOrder() {
        val (id, name) = UserDto(id = 9L, name = "Bob")
        assertEquals(9L, id)
        assertEquals("Bob", name)
    }

    @Test
    fun equals_isStructuralAcrossDistinctInstances() {
        val a = UserDto(id = 1L, name = "Tom")
        val b = UserDto(id = 1L, name = "Tom")
        assertNotSame(a, b)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_distinguishesEachField() {
        val base = UserDto(id = 1L, name = "Tom")
        assertNotEquals(base, base.copy(id = 2L))
        assertNotEquals(base, base.copy(name = "Jerry"))
    }

    @Test
    fun equals_reflexiveAndTypeSafe() {
        val dto = UserDto(id = 1L, name = "Tom")
        assertEquals(dto, dto)
        assertFalse(dto.equals(null))
        assertFalse(dto.equals("Tom"))
    }

    @Test
    fun copy_overridesSelectedFieldsOnly() {
        val original = UserDto(id = 1L, name = "Tom")
        assertEquals(UserDto(id = 5L, name = "Tom"), original.copy(id = 5L))
        assertEquals(UserDto(id = 1L, name = "Lee"), original.copy(name = "Lee"))
        assertEquals(original, original.copy())
    }

    @Test
    fun toString_containsClassNameAndFields() {
        val s = UserDto(id = 3L, name = "Zoe").toString()
        assertTrue(s.contains("UserDto"))
        assertTrue(s.contains("id=3"))
        assertTrue(s.contains("name=Zoe"))
    }

    @Test
    fun supportsUnicodeAndEmptyName() {
        val unicode = UserDto(id = 1L, name = "張三李四")
        assertEquals("張三李四", unicode.name)
        val empty = UserDto(id = 1L, name = "")
        assertEquals("", empty.name)
        assertNotEquals(unicode, empty)
    }
}
