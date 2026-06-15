package io.kudos.ability.distributed.config.nacos.init

import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.Configuration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for [NacosConfigAutoConfiguration].
 *
 * Coverage:
 *  - [NacosConfigAutoConfiguration.getComponentName] returns the module's fixed component name
 *  - The class implements [IComponentInitializer] (so the kudos SPI scheduler can pick it up)
 *  - Class-level annotations: `@Configuration` present; `@AutoConfigureAfter` declares
 *    [ContextAutoConfiguration] (guards against accidental removal that would break init ordering)
 *
 * @author K
 * @since 1.0.0
 */
internal class NacosConfigAutoConfigurationTest {

    @Test
    fun getComponentName_returnsFixedModuleName() {
        val configuration = NacosConfigAutoConfiguration()
        assertEquals("kudos-ability-distributed-config-nacos", configuration.getComponentName())
    }

    @Test
    fun implementsComponentInitializer() {
        val configuration: IComponentInitializer = NacosConfigAutoConfiguration()
        assertTrue(configuration is IComponentInitializer)
    }

    @Test
    fun classAnnotations_declareConfigurationAndInitOrder() {
        val clazz = NacosConfigAutoConfiguration::class.java
        assertNotNull(clazz.getAnnotation(Configuration::class.java), "@Configuration must be present")
        val after = clazz.getAnnotation(AutoConfigureAfter::class.java)
        assertNotNull(after, "@AutoConfigureAfter must be present")
        assertTrue(
            after.value.contains(ContextAutoConfiguration::class),
            "@AutoConfigureAfter must declare ContextAutoConfiguration to keep init ordering",
        )
    }
}
