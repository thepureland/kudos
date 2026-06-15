package io.kudos.ability.distributed.config.nacos.listener

import com.alibaba.nacos.api.PropertyKeyConst
import com.alibaba.nacos.api.exception.NacosException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pure unit tests for [NacosConfigServiceListener] — **without starting a Nacos server**.
 *
 * Coverage:
 *  - serverAddr constructor argument validation: null / empty / blank all fail `require`
 *    with the documented message, before any ConfigService is built
 *  - [NacosConfigServiceListener.PropertiesBuilder]: fluent chaining returns the same builder,
 *    accumulated properties are visible via `get()`, later `put` on the same key overwrites,
 *    null key/value behaviour of the backing [java.util.Properties] map, Unicode keys/values
 *  - Init-failure path of the companion's `obtainConfigService`: empty properties make the Nacos
 *    SDK constructor fail; the [NacosException] must be wrapped into a [RuntimeException]
 *    (covers the catch branch and `CacheKey.of`'s `orEmpty()` defaults)
 *
 * The happy path (real ConfigService creation, addListener / removeListener round-trip and
 * `(serverAddr, namespace)` cache reuse) needs a live Nacos and lives in
 * [io.kudos.ability.distributed.config.nacos.NacosConfigTest].
 *
 * @author K
 * @since 1.0.0
 */
internal class NacosConfigServiceListenerTest {

    @Test
    fun constructor_nullServerAddr_failsRequire() {
        val e = assertFailsWith<IllegalArgumentException> {
            NacosConfigServiceListener(null as String?)
        }
        assertEquals("serverAddr must not be blank", e.message)
    }

    @Test
    fun constructor_emptyServerAddr_failsRequire() {
        val e = assertFailsWith<IllegalArgumentException> {
            NacosConfigServiceListener("")
        }
        assertEquals("serverAddr must not be blank", e.message)
    }

    @Test
    fun constructor_blankServerAddr_failsRequire() {
        val e = assertFailsWith<IllegalArgumentException> {
            NacosConfigServiceListener("   \t ")
        }
        assertEquals("serverAddr must not be blank", e.message)
    }

    @Test
    fun constructor_emptyProperties_wrapsNacosInitFailureAsRuntimeException() {
        // No serverAddr / endpoint at all: the Nacos SDK cannot build a ConfigService.
        // obtainConfigService must surface this as RuntimeException(NacosException)
        // instead of swallowing it.
        val e = assertFailsWith<RuntimeException> {
            NacosConfigServiceListener(NacosConfigServiceListener.PropertiesBuilder())
        }
        assertIs<NacosException>(e.cause, "the cause must be the original NacosException")
    }

    @Test
    fun constructor_emptyPropertiesWithNamespace_alsoFails_andDoesNotPoisonCache() {
        // Same broken properties but a different namespace -> different cache key.
        // Both attempts must fail: a failed computeIfAbsent must not leave an entry behind.
        repeat(2) {
            val builder = NacosConfigServiceListener.PropertiesBuilder()
                .put(PropertyKeyConst.NAMESPACE, "unit-test-ns")
            val e = assertFailsWith<RuntimeException> {
                NacosConfigServiceListener(builder)
            }
            assertIs<NacosException>(e.cause)
        }
    }

    @Test
    fun propertiesBuilder_putReturnsSameInstance_forChaining() {
        val builder = NacosConfigServiceListener.PropertiesBuilder()
        val returned = builder.put("a", "1")
        assertSame(builder, returned, "put must return the builder itself for fluent chaining")
        assertSame(builder, builder.put("b", "2").put("c", "3"))
    }

    @Test
    fun propertiesBuilder_accumulatesAndOverwritesValues() {
        val props = NacosConfigServiceListener.PropertiesBuilder()
            .put(NacosConfigServiceListener.PRO_SERVER_ADDR_KEY, "host1:8848")
            .put("username", "u")
            .put("username", "u2") // overwrite
            .get()

        assertEquals("host1:8848", props.getProperty(NacosConfigServiceListener.PRO_SERVER_ADDR_KEY))
        assertEquals("u2", props.getProperty("username"))
        assertEquals(2, props.size)
    }

    @Test
    fun propertiesBuilder_emptyBuilder_returnsEmptyProperties() {
        val props = NacosConfigServiceListener.PropertiesBuilder().get()
        assertTrue(props.isEmpty)
    }

    @Test
    fun propertiesBuilder_nullKeyOrNullValue_throwsNpeFromBackingHashtable() {
        // Properties extends Hashtable, which rejects null keys and null values.
        // Pin that behaviour so a future switch of the backing map (e.g. to HashMap)
        // that silently starts accepting nulls is caught by the test suite.
        val builder = NacosConfigServiceListener.PropertiesBuilder()
        assertFailsWith<NullPointerException> { builder.put(null, "v") }
        assertFailsWith<NullPointerException> { builder.put("k", null) }
        assertTrue(builder.get().isEmpty, "failed puts must not leave partial entries behind")
    }

    @Test
    fun propertiesBuilder_supportsUnicodeAndNonStringValues() {
        val props = NacosConfigServiceListener.PropertiesBuilder()
            .put("中文键", "值-ÄÖÜ-😀")
            .put("timeout", 3000)
            .get()

        assertEquals("值-ÄÖÜ-😀", props["中文键"])
        assertEquals(3000, props["timeout"])
        // getProperty only returns String values; non-String yields null
        assertEquals(null, props.getProperty("timeout"))
    }

    @Test
    fun proServerAddrKey_matchesNacosSdkConstant() {
        assertEquals(PropertyKeyConst.SERVER_ADDR, NacosConfigServiceListener.PRO_SERVER_ADDR_KEY)
    }
}
