package io.kudos.ability.distributed.discovery.nacos.init

import io.kudos.ability.distributed.discovery.nacos.filter.FeignContextWebFilter
import io.kudos.ability.distributed.discovery.nacos.init.properties.NacosDiscoveryProperties
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import io.kudos.context.support.Consts
import org.springframework.context.support.StaticApplicationContext
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for [NacosDiscoveryAutoConfiguration] auto-configuration behavior.
 *
 * Coverage:
 *  - filter registration: filter type, name, order, url patterns (default properties)
 *  - component name reported to the kudos component initializer SPI
 *  - secret configured -> a signature verifier is wired (unsigned marked requests get 401)
 *  - blank secret -> legacy unauthenticated behavior is preserved
 *  - nacosDiscoveryProperties() bean returns default property values
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

    @Test
    fun getComponentName_returnsModuleName() {
        assertEquals(
            "kudos-ability-distributed-discovery-nacos",
            NacosDiscoveryAutoConfiguration().getComponentName()
        )
    }

    @Test
    fun registration_withSecretConfigured_wiresVerifier_unsignedRequestRejectedWith401() {
        val properties = NacosDiscoveryProperties().apply {
            feignContextFilter.contextSignatureSecret = "shared-secret"
            feignContextFilter.contextSignatureTimestampWindowMillis = 60_000L
            feignContextFilter.contextSignatureNonceCacheMaxSize = 16
        }
        val registration = NacosDiscoveryAutoConfiguration().feignContextWebFilterRegistration(properties)

        val request = MockHttpServletRequest("POST", "/api").apply {
            addHeader(Consts.RequestHeader.FEIGN_REQUEST, "true")
            addHeader(Consts.RequestHeader.TENANT_ID, "tenant-a")
        }
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        registration.filter!!.doFilter(request, response, chain)

        assertNull(chain.request, "an unsigned marked request must be rejected when a secret is configured")
        assertEquals(401, response.status)
    }

    @Test
    fun registration_withBlankSecret_keepsLegacyUnauthenticatedBehavior() {
        val ctx = StaticApplicationContext().apply { refresh() }
        SpringKit.applicationContext = ctx
        KudosContextHolder.clear()
        try {
            val properties = NacosDiscoveryProperties().apply {
                feignContextFilter.contextSignatureSecret = "   "
            }
            val registration = NacosDiscoveryAutoConfiguration().feignContextWebFilterRegistration(properties)

            val request = MockHttpServletRequest("POST", "/api").apply {
                addHeader(Consts.RequestHeader.FEIGN_REQUEST, "true")
                addHeader(Consts.RequestHeader.TENANT_ID, "tenant-legacy")
            }
            val response = MockHttpServletResponse()
            val chain = MockFilterChain()
            registration.filter!!.doFilter(request, response, chain)

            assertNotNull(chain.request, "blank secret must keep the legacy unauthenticated behavior")
            assertEquals(200, response.status)
            assertEquals("tenant-legacy", KudosContextHolder.get().tenantId)
        } finally {
            KudosContextHolder.clear()
            ctx.close()
        }
    }

    @Test
    fun nacosDiscoveryProperties_beanReturnsDefaults() {
        val properties = NacosDiscoveryAutoConfiguration().nacosDiscoveryProperties()

        assertEquals(false, properties.feignContextFilter.allowUnmarkedContextHeaders)
        assertNull(properties.feignContextFilter.contextSignatureSecret)
    }

}
