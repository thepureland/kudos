package io.kudos.ability.distributed.stream.rabbit.init

import io.kudos.ability.distributed.stream.common.init.StreamConsumerEnvironRegistrar
import org.springframework.context.annotation.Import
import kotlin.test.Test
import kotlin.test.assertTrue


internal class RabbitMqAutoConfigurationTest {

    @Test
    fun autoConfiguration_importsStreamConsumerEnvironRegistrar() {
        val importedClasses = RabbitMqAutoConfiguration::class.java
            .getAnnotation(Import::class.java)
            ?.value
            ?.toSet()
            .orEmpty()

        assertTrue(StreamConsumerEnvironRegistrar::class in importedClasses)
    }

}
