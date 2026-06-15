package io.kudos.ability.distributed.stream.kafka.init

import io.kudos.ability.distributed.stream.common.init.StreamCommonConfiguration
import io.kudos.ability.distributed.stream.common.init.StreamConsumerEnvironRegistrar
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for [KafkaAutoConfiguration] auto-configuration imports and defaults.
 *
 * Covers: component name returned by [IComponentInitializer.getComponentName], class hierarchy
 * (extends [StreamCommonConfiguration]), and annotation wiring (@Configuration,
 * @AutoConfigureAfter, @PropertySource with yaml factory, @Import).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class KafkaAutoConfigurationTest {

    @Test
    fun getComponentName_returnsModuleArtifactName() {
        val configuration = KafkaAutoConfiguration()
        assertEquals("kudos-ability-distributed-stream-kafka", configuration.getComponentName())
    }

    @Test
    fun autoConfiguration_extendsStreamCommonConfigurationAndImplementsInitializer() {
        val configuration = KafkaAutoConfiguration()
        assertTrue(configuration is StreamCommonConfiguration)
        assertTrue(configuration is IComponentInitializer)
    }

    @Test
    fun autoConfiguration_isConfigurationOrderedAfterContextAutoConfiguration() {
        val clazz = KafkaAutoConfiguration::class.java
        assertNotNull(clazz.getAnnotation(Configuration::class.java))

        val after = clazz.getAnnotation(AutoConfigureAfter::class.java)
        assertNotNull(after)
        assertTrue(ContextAutoConfiguration::class in after.value)
    }

    @Test
    fun autoConfiguration_mergesCommonAndKafkaYamlViaYamlFactory() {
        val propertySource = KafkaAutoConfiguration::class.java
            .getAnnotation(PropertySource::class.java)
        assertNotNull(propertySource)
        assertEquals(
            listOf(
                "classpath:kudos-ability-distributed-stream-common.yml",
                "classpath:kudos-ability-distributed-stream-kafka.yml"
            ),
            propertySource.value.toList()
        )
        assertEquals(YamlPropertySourceFactory::class, propertySource.factory)
    }

    @Test
    fun autoConfiguration_importsStreamConsumerEnvironRegistrar() {
        val importedClasses = KafkaAutoConfiguration::class.java
            .getAnnotation(Import::class.java)
            ?.value
            ?.toSet()
            .orEmpty()

        assertTrue(StreamConsumerEnvironRegistrar::class in importedClasses)
    }

    @Test
    fun kafkaDefaults_areExternalizedForProduction() {
        val content = requireNotNull(
            javaClass.classLoader.getResource("kudos-ability-distributed-stream-kafka.yml")
        ).readText()

        assertContains(content, "\${KAFKA_BROKERS:localhost:9092}")
        assertContains(content, "\${KAFKA_BINDER_HEADERS:}")
        assertContains(content, "\${KAFKA_SECURITY_PROTOCOL:PLAINTEXT}")
    }

}
