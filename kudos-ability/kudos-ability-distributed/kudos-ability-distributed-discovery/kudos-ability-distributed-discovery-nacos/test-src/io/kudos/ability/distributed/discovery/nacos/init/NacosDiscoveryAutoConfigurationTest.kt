package io.kudos.ability.distributed.discovery.nacos.init

import io.kudos.ability.distributed.discovery.nacos.filter.FeignContextWebFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs


internal class NacosDiscoveryAutoConfigurationTest {

    @Test
    fun feignContextWebFilterRegistration_registersFilterForAllPathsWithHighPrecedence() {
        val registration = NacosDiscoveryAutoConfiguration().feignContextWebFilterRegistration()

        assertIs<FeignContextWebFilter>(registration.filter)
        assertEquals("feignContextWebFilter", registration.filterName)
        assertEquals(FilterRegistrationBean.HIGHEST_PRECEDENCE + 1, registration.order)
        assertEquals(setOf("/*"), registration.urlPatterns.toSet())
    }

}
