package io.kudos.ability.cache.common.support

import java.util.function.Supplier
import kotlin.test.*

/**
 * Tests for [CacheValueWrapper], whose whole reason to exist is telling a **cached null** apart from a
 * **cache miss**.
 *
 * The wrapper used to derive presence from `value != null`, which made `of(null)` and `empty()` identical and
 * left it unable to express that distinction; these tests pin the corrected three-state behaviour so it cannot
 * regress.
 *
 * @author K
 * @since 1.0.0
 */
internal class CacheValueWrapperTest {

    @Test
    fun ofWithValueIsPresent() {
        val wrapper = CacheValueWrapper.of("hello")
        assertTrue(wrapper.isPresent)
        assertFalse(wrapper.isEmpty)
        assertEquals("hello", wrapper.value)
        assertEquals("hello", wrapper.orElse("default"))
    }

    @Test
    fun ofWithNullIsPresentButNull() {
        // 负缓存：源端确实没有这条数据，"查过且为 null" 与 "没查过" 必须区分开
        val wrapper = CacheValueWrapper.of<String>(null)
        assertTrue(wrapper.isPresent, "of(null) 表示缓存里有这一项，只是值为 null")
        assertNull(wrapper.value)
        assertNull(wrapper.orElse("default"), "命中的 null 不应被兜底值替换，否则负缓存就失去意义")
    }

    @Test
    fun emptyIsAbsent() {
        val wrapper = CacheValueWrapper.empty<String>()
        assertFalse(wrapper.isPresent)
        assertTrue(wrapper.isEmpty)
        assertNull(wrapper.value)
        assertEquals("default", wrapper.orElse("default"), "未命中才回退到兜底值")
    }

    @Test
    fun ofNullAndEmptyAreDistinguishable() {
        val cachedNull = CacheValueWrapper.of<String>(null)
        val miss = CacheValueWrapper.empty<String>()
        assertNotEquals(cachedNull, miss, "两种状态必须可区分——这正是该类存在的理由")
        assertNotEquals(cachedNull.isPresent, miss.isPresent)
    }

    @Test
    fun orElseGetUsesSupplierOnlyWhenAbsent() {
        assertEquals("v", CacheValueWrapper.of("v").orElseGet(Supplier { "fallback" }))
        assertNull(CacheValueWrapper.of<String>(null).orElseGet(Supplier { "fallback" }))
        assertEquals("fallback", CacheValueWrapper.empty<String>().orElseGet(Supplier { "fallback" }))
    }

    @Test
    fun orElseThrowReturnsValueWhenPresent() {
        assertEquals("v", CacheValueWrapper.of("v").orElseThrow(Supplier { IllegalStateException("absent") }))
    }

    @Test
    fun orElseThrowReturnsNullForCachedNull() {
        assertNull(
            CacheValueWrapper.of<String>(null).orElseThrow(Supplier { IllegalStateException("absent") }),
            "条目存在（值为 null）不算缺失，不应抛异常"
        )
    }

    @Test
    fun orElseThrowThrowsWhenAbsent() {
        val ex = assertFailsWith<IllegalStateException> {
            CacheValueWrapper.empty<String>().orElseThrow(Supplier { IllegalStateException("absent") })
        }
        assertEquals("absent", ex.message)
    }

    @Test
    fun equalityAndHashCodeFollowBothFields() {
        assertEquals(CacheValueWrapper.of("a"), CacheValueWrapper.of("a"))
        assertEquals(CacheValueWrapper.of("a").hashCode(), CacheValueWrapper.of("a").hashCode())
        assertEquals(CacheValueWrapper.empty<String>(), CacheValueWrapper.empty<String>())
        assertNotEquals(CacheValueWrapper.of("a"), CacheValueWrapper.of("b"))
    }
}
