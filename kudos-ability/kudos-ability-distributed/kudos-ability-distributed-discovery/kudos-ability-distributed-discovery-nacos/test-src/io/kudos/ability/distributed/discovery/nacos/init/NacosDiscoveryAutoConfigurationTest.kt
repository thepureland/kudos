package io.kudos.ability.distributed.discovery.nacos.init

import io.kudos.ability.distributed.discovery.nacos.filter.FeignContextWebFilter
import io.kudos.ability.distributed.discovery.nacos.init.properties.NacosDiscoveryProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Unit tests for [NacosDiscoveryAutoConfiguration] auto-configuration behavior.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class NacosDiscoveryAutoConfigurationTest {

    @Test
    fun feignContextWebFilterRegistration_registersFilterForAllPathsWithHighPrecedence() {
        val registration = NacosDiscoveryAutoConfiguration().feignContextWebFilterRegistration()

        assertIs<FeignContextWebFilter>(registration.filter)
        assertEquals("feignContextWebFilter", registration.filterName)
        assertEquals(NacosDiscoveryProperties.FILTER_ORDER, registration.order)
        assertEquals(setOf("/*"), registration.urlPatterns.toSet())
    }

}
