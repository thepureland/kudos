package io.kudos.ability.distributed.stream.rocketmq.init

import io.kudos.ability.distributed.stream.common.init.StreamCommonConfiguration
import io.kudos.ability.distributed.stream.common.init.StreamConsumerEnvironRegistrar
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * [RocketMqAutoConfiguration] unit tests (annotation metadata + component name; container behavior
 * is covered by the dockerized [io.kudos.ability.distributed.stream.rocketmq.RocketMqTest]).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class RocketMqAutoConfigurationTest {

    @Test
    fun autoConfiguration_importsStreamConsumerEnvironRegistrar() {
        val importedClasses = RocketMqAutoConfiguration::class.java
            .getAnnotation(Import::class.java)
            ?.value
            ?.toSet()
            .orEmpty()

        assertTrue(StreamConsumerEnvironRegistrar::class in importedClasses)
    }

    @Test
    fun componentName_matchesArtifactName() {
        assertEquals("kudos-ability-distributed-stream-rocketmq", RocketMqAutoConfiguration().getComponentName())
    }

    @Test
    fun autoConfiguration_extendsStreamCommonConfigurationAndIsComponentInitializer() {
        val config = RocketMqAutoConfiguration()
        assertTrue(config is StreamCommonConfiguration)
        assertTrue(config is IComponentInitializer)
    }

    @Test
    fun autoConfiguration_runsAfterContextAutoConfiguration() {
        val after = RocketMqAutoConfiguration::class.java.getAnnotation(AutoConfigureAfter::class.java)
        assertNotNull(after)
        assertTrue(ContextAutoConfiguration::class in after.value)
    }

    @Test
    fun propertySource_loadsCommonAndRocketMqYamlsViaYamlFactory() {
        val propertySource = RocketMqAutoConfiguration::class.java.getAnnotation(PropertySource::class.java)
        assertNotNull(propertySource)
        assertContentEquals(
            arrayOf(
                "classpath:kudos-ability-distributed-stream-common.yml",
                "classpath:kudos-ability-distributed-stream-rocketmq.yml"
            ),
            propertySource.value
        )
        assertEquals(YamlPropertySourceFactory::class, propertySource.factory)
    }

}
