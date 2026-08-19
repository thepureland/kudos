package io.kudos.ability.distributed.discovery.nacos.init.properties

import io.kudos.ability.distributed.discovery.nacos.filter.InternalRpcContextSignatureVerifier
import org.springframework.boot.web.servlet.FilterRegistrationBean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [NacosDiscoveryProperties] default values and mutability.
 *
 * Coverage:
 *  - default values of every RpcContextFilter property (and their constant sources)
 *  - setter round-trips for all mutable properties
 *  - FILTER_ORDER constant value
 *
 * @author K
 * @since 1.0.0
 */
internal class NacosDiscoveryPropertiesTest {

    @Test
    fun defaults() {
        val properties = NacosDiscoveryProperties()
        val filter = properties.rpcContextFilter

        assertFalse(filter.allowUnmarkedContextHeaders)
        assertNull(filter.contextSignatureSecret)
        assertEquals(
            InternalRpcContextSignatureVerifier.DEFAULT_TIMESTAMP_WINDOW_MILLIS,
            filter.contextSignatureTimestampWindowMillis
        )
        assertEquals(5 * 60 * 1000L, filter.contextSignatureTimestampWindowMillis)
        assertEquals(
            InternalRpcContextSignatureVerifier.DEFAULT_NONCE_CACHE_MAX_SIZE,
            filter.contextSignatureNonceCacheMaxSize
        )
        assertEquals(100_000, filter.contextSignatureNonceCacheMaxSize)
    }

    @Test
    fun settersRoundTrip() {
        val properties = NacosDiscoveryProperties()
        val original = properties.rpcContextFilter
        val replacement = NacosDiscoveryProperties.RpcContextFilter().apply {
            allowUnmarkedContextHeaders = true
            contextSignatureSecret = "s3cret"
            contextSignatureTimestampWindowMillis = 1234L
            contextSignatureNonceCacheMaxSize = 99
        }
        properties.rpcContextFilter = replacement

        assertNotSame(original, properties.rpcContextFilter)
        assertTrue(properties.rpcContextFilter.allowUnmarkedContextHeaders)
        assertEquals("s3cret", properties.rpcContextFilter.contextSignatureSecret)
        assertEquals(1234L, properties.rpcContextFilter.contextSignatureTimestampWindowMillis)
        assertEquals(99, properties.rpcContextFilter.contextSignatureNonceCacheMaxSize)
    }

    @Test
    fun filterOrder_isOneAboveHighestPrecedence() {
        assertEquals(
            FilterRegistrationBean.HIGHEST_PRECEDENCE + 1,
            NacosDiscoveryProperties.FILTER_ORDER
        )
    }

}
