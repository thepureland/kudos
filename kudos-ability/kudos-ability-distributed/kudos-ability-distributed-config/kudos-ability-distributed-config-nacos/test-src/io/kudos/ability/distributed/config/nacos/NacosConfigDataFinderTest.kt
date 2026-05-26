package io.kudos.ability.distributed.config.nacos

import com.alibaba.cloud.nacos.NacosPropertySourceRepository
import com.alibaba.cloud.nacos.client.NacosPropertySource
import io.kudos.ability.distributed.config.nacos.decrypt.NacosConfigValueDecryptor
import io.kudos.context.config.IConfigDataFinder
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertySource
import java.util.Date
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [NacosConfigDataFinder] — **without starting Nacos**; test data is injected
 * directly into the static map of [NacosPropertySourceRepository].
 *
 * Coverage:
 *  - Hit by `dataId`: finder returns the corresponding PropertySource
 *  - Miss: returns null
 *  - Multiple nacos sources registered: the one matching the dataId wins
 *  - **SPI registration**: `ServiceLoader.load(IConfigDataFinder::class.java)` finds [NacosConfigDataFinder]
 *    (guards against accidental removal of `resources/META-INF/services/...`)
 *
 * The static map in `NacosPropertySourceRepository` is shared JVM-wide and has no clear API —
 * this test clears it via reflection before/after each case to avoid leaking into other tests
 * sharing the same JVM.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class NacosConfigDataFinderTest {

    private val finder = NacosConfigDataFinder()

    @BeforeTest
    fun clearRepoBefore() = clearRepoViaReflection()

    @AfterTest
    fun clearRepoAfter() = clearRepoViaReflection()

    @Test
    fun findConfigData_returnsRegisteredSource_byDataId() {
        register(dataId = "app.yaml", group = "DEFAULT_GROUP", payload = mapOf("k" to "v"))

        val source = finder.findConfigData("app.yaml")

        assertNotNull(source)
        assertEquals("v", source.getProperty("k"))
    }

    @Test
    fun findConfigData_returnsNull_whenDataIdMissing() {
        register(dataId = "app.yaml", group = "DEFAULT_GROUP", payload = mapOf("k" to "v"))

        val source = finder.findConfigData("not-registered.yaml")

        assertNull(source, "Looking up a non-existent dataId should return null")
    }

    @Test
    fun findConfigData_nullName_returnsNull() {
        register(dataId = "app.yaml", group = "DEFAULT_GROUP", payload = mapOf("k" to "v"))
        // The dataId field is non-null; passing null should not match any entry
        assertNull(finder.findConfigData(null))
    }

    @Test
    fun findConfigData_multipleSources_picksByDataId() {
        register(dataId = "service-a.yaml", group = "DEFAULT_GROUP", payload = mapOf("svc" to "A"))
        register(dataId = "service-b.yaml", group = "DEFAULT_GROUP", payload = mapOf("svc" to "B"))

        val a = finder.findConfigData("service-a.yaml")
        val b = finder.findConfigData("service-b.yaml")

        assertEquals("A", a?.getProperty("svc"))
        assertEquals("B", b?.getProperty("svc"))
    }

    @Test
    fun serviceLoader_resolvesNacosConfigDataFinder() {
        val loaded = ServiceLoader.load(IConfigDataFinder::class.java).toList()
        assertTrue(
            loaded.any { it::class == NacosConfigDataFinder::class },
            "META-INF/services/io.kudos.context.config.IConfigDataFinder should register NacosConfigDataFinder; actually loaded: ${loaded.map { it::class.qualifiedName }}",
        )
    }

    @Test
    fun findConfigData_appliesConfiguredDecryptorToStringValues() {
        register(dataId = "secret.yaml", group = "DEFAULT_GROUP", payload = mapOf("password" to "ENC(cipher)"))
        val finder = NacosConfigDataFinder(listOf(TestDecryptor()))

        val source = finder.findConfigData("secret.yaml")

        assertEquals("plain:cipher", source?.getProperty("password"))
    }

    /**
     * Test decryptor that converts `ENC(...)` strings into a plain-text marker.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class TestDecryptor : NacosConfigValueDecryptor {
        override fun supports(value: String): Boolean = value.startsWith("ENC(") && value.endsWith(")")

        override fun decrypt(value: String): String = "plain:" + value.removePrefix("ENC(").removeSuffix(")")
    }

    /**
     * Clear the static ConcurrentHashMap of [NacosPropertySourceRepository] via reflection.
     *
     * The public API exposes no clear method — in production Nacos assumes PropertySources are
     * only added and align with the spring config-context lifecycle. Tests must clean up between
     * cases, otherwise execution order affects assertions.
     */
    private fun clearRepoViaReflection() {
        val field = NacosPropertySourceRepository::class.java
            .getDeclaredField("NACOS_PROPERTY_SOURCE_REPOSITORY")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val map = field.get(null) as ConcurrentHashMap<String, NacosPropertySource>
        map.clear()
    }

    /** Build a [NacosPropertySource] and push it into the repository. */
    private fun register(dataId: String, group: String, payload: Map<String, Any>) {
        val inner: PropertySource<*> = MapPropertySource(dataId, payload)
        val src = NacosPropertySource(listOf(inner), group, dataId, Date(), /* refreshable = */ false)
        NacosPropertySourceRepository.collectNacosPropertySource(src)
    }
}
