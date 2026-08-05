package io.kudos.ability.distributed.stream.common.init

import io.kudos.context.config.YamlPropertySourceFactory
import org.mockito.Mockito.mock
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.type.AnnotationMetadata
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [StreamConsumerEnvironRegistrar]:
 * - merging `spring.cloud.function.definition` values from the Environment and from YAML files
 *   registered in [YamlPropertySourceFactory]'s source map (semicolon/comma/whitespace separators,
 *   trimming, deduplication, semicolon-joined result)
 * - the aggregated property source being registered with highest priority (addFirst)
 * - skipped locations: blank keys, missing resources, YAML files without the definition key
 * - no aggregated property source when no definition exists anywhere, and blank Environment
 *   values being ignored
 *
 * The factory's global SOURCE_MAP is snapshotted and restored around each test via reflection,
 * keeping tests order-independent.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class StreamConsumerEnvironRegistrarTest {

    private lateinit var snapshot: Map<String?, String?>

    @BeforeTest
    fun saveSourceMap() {
        snapshot = HashMap(sourceMap())
        sourceMap().clear()
    }

    @AfterTest
    fun restoreSourceMap() {
        sourceMap().clear()
        sourceMap().putAll(snapshot)
    }

    @Test
    fun register_mergesEnvironmentAndYamlDefinitionsWithDedup() {
        sourceMap()[""] = ""                                           // blank location -> skipped
        sourceMap()["stream-registrar-missing-xyz.yml"] = ""           // missing resource -> skipped
        sourceMap()["classpath:stream-registrar-no-defs.yml"] = ""     // yml without the key -> skipped
        sourceMap()["classpath:stream-registrar-with-defs.yml"] = ""   // contributes yamlFuncA/yamlFuncB/envFuncA
        val env = StandardEnvironment()
        env.propertySources.addFirst(
            MapPropertySource("test-defs", mapOf(KEY to "envFuncA; envFuncB ,envFuncA"))
        )
        val registrar = StreamConsumerEnvironRegistrar().apply { setEnvironment(env) }

        registrar.registerBeanDefinitions(mock(AnnotationMetadata::class.java), mock(BeanDefinitionRegistry::class.java))

        assertTrue(env.propertySources.contains("aggregatedFunctionDefinition"))
        assertEquals("aggregatedFunctionDefinition", env.propertySources.iterator().next().name)
        assertEquals("envFuncA;envFuncB;yamlFuncA;yamlFuncB", env.getProperty(KEY))
    }

    @Test
    fun register_usesYamlDefinitionsWhenEnvironmentHasNone() {
        sourceMap()["classpath:stream-registrar-with-defs.yml"] = ""
        val env = StandardEnvironment()
        val registrar = StreamConsumerEnvironRegistrar().apply { setEnvironment(env) }

        registrar.registerBeanDefinitions(mock(AnnotationMetadata::class.java), mock(BeanDefinitionRegistry::class.java))

        assertEquals("yamlFuncA;yamlFuncB;envFuncA", env.getProperty(KEY))
    }

    @Test
    fun register_addsNothingWhenNoDefinitionsAnywhere() {
        sourceMap()["classpath:stream-registrar-no-defs.yml"] = ""
        val env = StandardEnvironment()
        val registrar = StreamConsumerEnvironRegistrar().apply { setEnvironment(env) }

        registrar.registerBeanDefinitions(mock(AnnotationMetadata::class.java), mock(BeanDefinitionRegistry::class.java))

        assertFalse(env.propertySources.contains("aggregatedFunctionDefinition"))
    }

    // NOTE: the `catch (e: IOException)` branch in registerBeanDefinitions is effectively
    // unreachable through YamlPropertySourceLoader — SnakeYAML wraps underlying IOExceptions
    // (verified empirically with a mandatory Windows file lock: org.yaml.snakeyaml.error.YAMLException
    // is thrown instead, a RuntimeException that bypasses the catch). Left uncovered intentionally.

    @Test
    fun register_ignoresBlankEnvironmentDefinition() {
        val env = StandardEnvironment()
        env.propertySources.addFirst(MapPropertySource("test-defs", mapOf(KEY to "   ")))
        val registrar = StreamConsumerEnvironRegistrar().apply { setEnvironment(env) }

        registrar.registerBeanDefinitions(mock(AnnotationMetadata::class.java), mock(BeanDefinitionRegistry::class.java))

        assertFalse(env.propertySources.contains("aggregatedFunctionDefinition"))
    }

    @Suppress("UNCHECKED_CAST")
    private fun sourceMap(): MutableMap<String?, String?> =
        YamlPropertySourceFactory::class.java.getDeclaredField("SOURCE_MAP").run {
            isAccessible = true
            get(null) as MutableMap<String?, String?>
        }

    companion object {
        private const val KEY = "spring.cloud.function.definition"
    }

}
