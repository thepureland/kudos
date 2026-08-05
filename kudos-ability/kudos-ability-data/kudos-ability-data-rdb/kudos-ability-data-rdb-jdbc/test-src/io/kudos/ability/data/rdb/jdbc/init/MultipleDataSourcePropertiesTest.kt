package io.kudos.ability.data.rdb.jdbc.init

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [MultipleDataSourceProperties]: exact lookup, prefix-matching resolution with
 * caching, the empty-string "no match" contract, hot-update via forceChangeDataSource (cache
 * invalidation), and the afterPropertiesSet logging callback.
 *
 * @author K
 * @since 1.0.0
 */
internal class MultipleDataSourcePropertiesTest {

    private fun newProps(vararg pairs: Pair<String, String>): MultipleDataSourceProperties {
        val props = MultipleDataSourceProperties()
        pairs.forEach { (k, v) -> props.packageDataSource[k] = v }
        return props
    }

    @Test
    fun lookUpKey_exactMatchOnly() {
        val props = newProps("com.example.biz" to "ds1")
        assertEquals("ds1", props.lookUpKey("com.example.biz"))
        assertNull(props.lookUpKey("com.example.biz.sub"), "lookUpKey does no prefix matching")
        assertNull(props.lookUpKey("other"))
        // NOTE: lookUpKey(null) currently throws NPE (ConcurrentHashMap.containsKey(null))
        // despite the nullable signature — recorded as a suspected main-code bug, not asserted here.
    }

    @Test
    fun lookDataSourceKey_prefixMatchAndCache() {
        val props = newProps("io.kudos.ability.data" to "dsA")
        // this test class's package starts with the configured prefix
        assertEquals("dsA", props.lookDataSourceKey(this::class.java))
        // second lookup is served from the serviceDataSource cache (same result)
        assertEquals("dsA", props.lookDataSourceKey(this::class.java))
    }

    @Test
    fun lookDataSourceKey_noMatchReturnsEmptyString() {
        val props = newProps("com.acme.never" to "dsB")
        assertEquals("", props.lookDataSourceKey(this::class.java))
        // and the empty result is re-resolved (isNullOrBlank cache miss), still empty
        assertEquals("", props.lookDataSourceKey(this::class.java))
    }

    @Test
    fun lookDataSourceKey_noConfigAtAll() {
        val props = MultipleDataSourceProperties()
        assertEquals("", props.lookDataSourceKey(String::class.java))
    }

    @Test
    fun forceChangeDataSource_overridesAndClearsCache() {
        val pkg = this::class.java.packageName
        val props = newProps(pkg to "before")
        assertEquals("before", props.lookDataSourceKey(this::class.java))

        props.forceChangeDataSource(pkg, "after")

        assertEquals("after", props.lookUpKey(pkg))
        assertEquals(
            "after", props.lookDataSourceKey(this::class.java),
            "the resolution cache must be cleared so the next lookup sees the new mapping"
        )
    }

    @Test
    fun afterPropertiesSet_printsConfigurationWithoutThrowing() {
        val props = newProps("a.b" to "ds1", "c.d" to "ds2")
        props.afterPropertiesSet() // DEBUG logging only — must not throw
    }
}
