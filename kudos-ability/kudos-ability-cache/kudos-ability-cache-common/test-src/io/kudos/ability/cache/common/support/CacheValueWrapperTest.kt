package io.kudos.ability.cache.common.support

import java.util.function.Supplier
import kotlin.test.*

/**
 * test for CacheValueWrapper
 *
 * @author K
 * @since 1.0.0
 */
internal class CacheValueWrapperTest {

    @Test
    fun ofWithValueIsPresent() {
        val wrapper = CacheValueWrapper.of("hello")
        assertTrue(wrapper.isPresent)
        assertEquals("hello", wrapper.value)
        assertEquals("hello", wrapper.orElse("default"))
    }

    @Test
    fun ofWithNullIsAbsent() {
        val wrapper = CacheValueWrapper.of<String>(null)
        assertFalse(wrapper.isPresent)
        assertNull(wrapper.value)
        assertEquals("default", wrapper.orElse("default"))
    }

    @Test
    fun emptyIsAbsent() {
        val wrapper = CacheValueWrapper.empty<String>()
        assertFalse(wrapper.isPresent)
        assertNull(wrapper.value)
    }

    @Test
    fun orElseGetUsesSupplierOnlyWhenAbsent() {
        assertEquals("v", CacheValueWrapper.of("v").orElseGet(Supplier { "fallback" }))
        assertEquals("fallback", CacheValueWrapper.of<String>(null).orElseGet(Supplier { "fallback" }))
    }

    @Test
    fun orElseThrowReturnsValueWhenPresent() {
        assertEquals("v", CacheValueWrapper.of("v").orElseThrow(Supplier { IllegalStateException("absent") }))
    }

    @Test
    fun orElseThrowThrowsWhenAbsent() {
        val ex = assertFailsWith<IllegalStateException> {
            CacheValueWrapper.of<String>(null).orElseThrow(Supplier { IllegalStateException("absent") })
        }
        assertEquals("absent", ex.message)
    }
}
