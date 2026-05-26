package io.kudos.ability.distributed.stream.kafka.init

import io.kudos.ability.distributed.stream.common.init.StreamConsumerEnvironRegistrar
import org.springframework.context.annotation.Import
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

/**
 * Unit tests for [KafkaAutoConfiguration] auto-configuration imports and defaults.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class KafkaAutoConfigurationTest {

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
