package io.kudos.ability.distributed.stream.rabbit.init

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
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for RabbitMqAutoConfiguration
 *
 * 覆盖场景：
 * 1. getComponentName() 返回唯一组件名（与构件名一致）
 * 2. 类可直接实例化，且同时是 StreamCommonConfiguration 与 IComponentInitializer
 * 3. @Import 引入 StreamConsumerEnvironRegistrar
 * 4. @Configuration / @AutoConfigureAfter(ContextAutoConfiguration) 注解元数据正确
 * 5. @PropertySource 引用 common 与 rabbit 两个 yml，使用 YamlPropertySourceFactory，
 *    且两个 yml 资源都真实存在于 classpath
 *
 * @author K
 * @since 1.0.0
 */
internal class RabbitMqAutoConfigurationTest {

    @Test
    fun getComponentName_returnsArtifactName() {
        val configuration = RabbitMqAutoConfiguration()
        assertEquals("kudos-ability-distributed-stream-rabbit", configuration.getComponentName())
    }

    @Test
    fun autoConfiguration_isStreamCommonConfigurationAndComponentInitializer() {
        val configuration = RabbitMqAutoConfiguration()
        assertIs<StreamCommonConfiguration>(configuration)
        assertIs<IComponentInitializer>(configuration)
    }

    @Test
    fun autoConfiguration_importsStreamConsumerEnvironRegistrar() {
        val importedClasses = RabbitMqAutoConfiguration::class.java
            .getAnnotation(Import::class.java)
            ?.value
            ?.toSet()
            .orEmpty()

        assertTrue(StreamConsumerEnvironRegistrar::class in importedClasses)
    }

    @Test
    fun autoConfiguration_isConfigurationAndOrderedAfterContextAutoConfiguration() {
        val clazz = RabbitMqAutoConfiguration::class.java
        assertNotNull(clazz.getAnnotation(Configuration::class.java))

        val after = clazz.getAnnotation(AutoConfigureAfter::class.java)
        assertNotNull(after)
        assertTrue(ContextAutoConfiguration::class in after.value)
    }

    @Test
    fun autoConfiguration_propertySourceReferencesCommonAndRabbitYmlWithYamlFactory() {
        val propertySource = RabbitMqAutoConfiguration::class.java
            .getAnnotation(PropertySource::class.java)
        assertNotNull(propertySource)

        val locations = propertySource.value.toList()
        assertEquals(
            listOf(
                "classpath:kudos-ability-distributed-stream-common.yml",
                "classpath:kudos-ability-distributed-stream-rabbit.yml"
            ),
            locations
        )
        assertEquals(YamlPropertySourceFactory::class, propertySource.factory)

        // @PropertySource 引用的两个 yml 都必须真实存在于 classpath，否则容器启动会失败
        val classLoader = RabbitMqAutoConfiguration::class.java.classLoader
        val rabbitYml = classLoader.getResource("kudos-ability-distributed-stream-rabbit.yml")
        assertNotNull(rabbitYml)
        val commonYml = classLoader.getResource("kudos-ability-distributed-stream-common.yml")
        assertNotNull(commonYml)
    }

}
