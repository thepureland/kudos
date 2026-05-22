package io.kudos.ability.cache.interservice.provider.web

import io.kudos.ability.cache.interservice.common.ClientCacheKey
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ClientCacheWebFilterTest {

    @Test
    fun doFilter_withoutCacheHeaders_doesNotWrapByDefault() {
        val chain = RecordingFilterChain()

        ClientCacheWebFilter().doFilter(MockHttpServletRequest(), MockHttpServletResponse(), chain)

        assertFalse(chain.request is CacheClientRequest)
    }

    @Test
    fun doFilter_withCacheKeyHeader_wrapsRequest() {
        val chain = RecordingFilterChain()
        val request = MockHttpServletRequest().apply {
            addHeader(ClientCacheKey.HEADER_KEY_CACHE_KEY, "cache-key")
        }

        ClientCacheWebFilter().doFilter(request, MockHttpServletResponse(), chain)

        assertTrue(chain.request is CacheClientRequest)
    }

    @Test
    fun doFilter_wrapAllRequests_keepsCompatibilityMode() {
        val chain = RecordingFilterChain()

        ClientCacheWebFilter(wrapAllRequests = true)
            .doFilter(MockHttpServletRequest(), MockHttpServletResponse(), chain)

        assertTrue(chain.request is CacheClientRequest)
    }

    private class RecordingFilterChain : FilterChain {
        var request: ServletRequest? = null
            private set

        override fun doFilter(request: ServletRequest?, response: ServletResponse?) {
            this.request = request
        }
    }
}
