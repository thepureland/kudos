package io.kudos.ability.distributed.client.feign.init

import io.kudos.ability.distributed.client.feign.fallback.GlobalFeignFallBackFactory
import io.kudos.ability.distributed.client.feign.interceptor.GlobalHeaderRequestInterceptor
import io.kudos.ability.distributed.client.feign.init.properties.OpenFeignProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull

/**
 * Unit tests for [OpenFeignAutoConfiguration] bean factory methods.
 *
 * Coverage:
 *  - feignCacheRequestInterceptor() returns a [GlobalHeaderRequestInterceptor].
 *  - openFeignProperties() returns fresh defaults (signature secret unset).
 *  - globalFeignFallBackFactory() returns a [GlobalFeignFallBackFactory].
 *  - getComponentName() returns the module name.
 *
 * The bean wiring itself (@Bean / @ConditionalOnMissingBean) is exercised by the
 * Spring-container integration test [io.kudos.ability.distributed.client.feign.OpenFeignTest].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class OpenFeignAutoConfigurationTest {

    private val configuration = OpenFeignAutoConfiguration()

    @Test
    fun feignCacheRequestInterceptor_returnsGlobalHeaderRequestInterceptor() {
        val interceptor = configuration.feignCacheRequestInterceptor(OpenFeignProperties())
        assertIs<GlobalHeaderRequestInterceptor>(interceptor)
    }

    @Test
    fun openFeignProperties_returnsFreshInstanceWithDefaults() {
        val p1 = configuration.openFeignProperties()
        val p2 = configuration.openFeignProperties()
        assertNull(p1.contextSignatureSecret, "signing should be disabled by default")
        assertNotSame(p1, p2, "each factory call should create a new instance")
    }

    @Test
    fun globalFeignFallBackFactory_returnsFactory() {
        assertIs<GlobalFeignFallBackFactory>(configuration.globalFeignFallBackFactory())
    }

    @Test
    fun getComponentName_returnsModuleName() {
        assertEquals("kudos-ability-distributed-client-feign", configuration.getComponentName())
    }
}
