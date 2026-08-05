package io.kudos.ability.distributed.discovery.nacos.init.properties

import io.kudos.ability.distributed.discovery.nacos.filter.FeignContextSignatureVerifier
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
 *  - default values of every FeignContextFilter property (and their constant sources)
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
        val filter = properties.feignContextFilter

        assertFalse(filter.allowUnmarkedContextHeaders)
        assertNull(filter.contextSignatureSecret)
        assertEquals(
            FeignContextSignatureVerifier.DEFAULT_TIMESTAMP_WINDOW_MILLIS,
            filter.contextSignatureTimestampWindowMillis
        )
        assertEquals(5 * 60 * 1000L, filter.contextSignatureTimestampWindowMillis)
        assertEquals(
            FeignContextSignatureVerifier.DEFAULT_NONCE_CACHE_MAX_SIZE,
            filter.contextSignatureNonceCacheMaxSize
        )
        assertEquals(100_000, filter.contextSignatureNonceCacheMaxSize)
    }

    @Test
    fun settersRoundTrip() {
        val properties = NacosDiscoveryProperties()
        val original = properties.feignContextFilter
        val replacement = NacosDiscoveryProperties.FeignContextFilter().apply {
            allowUnmarkedContextHeaders = true
            contextSignatureSecret = "s3cret"
            contextSignatureTimestampWindowMillis = 1234L
            contextSignatureNonceCacheMaxSize = 99
        }
        properties.feignContextFilter = replacement

        assertNotSame(original, properties.feignContextFilter)
        assertTrue(properties.feignContextFilter.allowUnmarkedContextHeaders)
        assertEquals("s3cret", properties.feignContextFilter.contextSignatureSecret)
        assertEquals(1234L, properties.feignContextFilter.contextSignatureTimestampWindowMillis)
        assertEquals(99, properties.feignContextFilter.contextSignatureNonceCacheMaxSize)
    }

    @Test
    fun filterOrder_isOneAboveHighestPrecedence() {
        assertEquals(
            FilterRegistrationBean.HIGHEST_PRECEDENCE + 1,
            NacosDiscoveryProperties.FILTER_ORDER
        )
    }

}
