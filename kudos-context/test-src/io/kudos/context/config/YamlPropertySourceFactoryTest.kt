package io.kudos.context.config

import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.EncodedResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for YamlPropertySourceFactory
 *
 * Coverage:
 * - Plain yaml loading when no config-center data exists (the ServiceLoader finder returns null)
 * - Source-name resolution priority: explicit name > resource filename > "application"
 * - Config-center merge branch: a MutableMap-backed property source is merged over the local yaml
 *   (center values win on conflict)
 * - Config-center passthrough branch: a non-map property source is returned as-is
 * - SOURCE_MAP bookkeeping: successful URI recording, the warn-and-continue fallback for resources
 *   without a URI, getSourceMap() read-only view, allSourcePath() listing
 *
 * The config-center branches are driven by [TestConfigDataFinder], registered through
 * META-INF/services and scoped to the "yaml-test-" name prefix.
 *
 * @author K
 * @since 1.0.0
 */
internal class YamlPropertySourceFactoryTest {

    private val factory = YamlPropertySourceFactory()

    private fun yamlResource(yaml: String) = EncodedResource(ByteArrayResource(yaml.toByteArray()))

    // ============================================================
    // Plain yaml loading (no config-center data)
    // ============================================================

    @Test
    fun loadsPlainYamlWhenNoConfigCenterData() {
        val ps = factory.createPropertySource(
            "yaml-test-plain",
            yamlResource("kudos:\n  alpha: 1\n  beta: two\n")
        )
        assertEquals("yaml-test-plain", ps.name)
        assertEquals(1, ps.getProperty("kudos.alpha"))
        assertEquals("two", ps.getProperty("kudos.beta"))
        assertNull(ps.getProperty("kudos.missing"))
    }

    @Test
    fun fallsBackToResourceFilenameWhenNameIsNull() {
        // ClassPathResource has a filename ("application.yml") and a resolvable URI
        val ps = factory.createPropertySource(null, EncodedResource(ClassPathResource("application.yml")))
        assertEquals("application.yml", ps.name, "should fall back to the resource filename")
        assertEquals("true", ps.getProperty("kudos.context.test")?.toString())
        // The URI of a classpath resource is resolvable -> the jar map records a real location
        assertTrue(
            YamlPropertySourceFactory.getSourceMap()["application.yml"]!!.isNotEmpty(),
            "a resolvable resource should record its URI in the source map"
        )
    }

    @Test
    fun fallsBackToApplicationWhenNameAndFilenameAreNull() {
        // ByteArrayResource has no filename -> the final "application" fallback
        val ps = factory.createPropertySource(null, yamlResource("x: 1\n"))
        assertEquals("application", ps.name)
    }

    // ============================================================
    // Config-center branches (via TestConfigDataFinder)
    // ============================================================

    @Test
    fun mergesMapBackedConfigCenterSourceOverLocalYaml() {
        val ps = factory.createPropertySource(
            "yaml-test-map",
            yamlResource("kudos:\n  conflict: local-loses\n  local.only: from-local\n")
        )
        assertEquals("yaml-test-map", ps.name)
        assertEquals("from-center", ps.getProperty("kudos.center.only"), "center-only keys should be merged in")
        assertEquals("center-wins", ps.getProperty("kudos.conflict"), "the config center must win on conflicts")
        assertEquals("from-local", ps.getProperty("kudos.local.only"), "local-only keys must be preserved")
    }

    @Test
    fun returnsNonMapConfigCenterSourceDirectly() {
        val ps = factory.createPropertySource(
            "yaml-test-raw",
            yamlResource("ignored: true\n")
        )
        assertEquals("yaml-test-raw", ps.name)
        assertEquals("raw-value", ps.getProperty("raw.key"))
        assertSame("raw-source", ps.source, "the non-map property source should be returned unchanged")
        assertNull(ps.getProperty("ignored"), "the local yaml must NOT be merged in this branch")
    }

    // ============================================================
    // SOURCE_MAP bookkeeping
    // ============================================================

    @Test
    fun recordsEmptyUriWhenResourceHasNoUri() {
        // ByteArrayResource cannot resolve a URI -> the warn fallback writes ""
        factory.createPropertySource("yaml-test-no-uri", yamlResource("a: 1\n"))
        assertEquals("", YamlPropertySourceFactory.getSourceMap()["yaml-test-no-uri"])
        assertTrue(YamlPropertySourceFactory.allSourcePath().contains("yaml-test-no-uri"))
    }

    @Test
    fun sourceMapViewIsReadOnly() {
        factory.createPropertySource("yaml-test-readonly", yamlResource("a: 1\n"))
        val view = YamlPropertySourceFactory.getSourceMap()
        assertFailsWith<UnsupportedOperationException> {
            (view as MutableMap<String?, String?>)["hacked"] = "nope"
        }
    }
}
