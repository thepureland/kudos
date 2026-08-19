package io.kudos.ability.cache.interservice.provider.web

import io.kudos.ability.cache.interservice.common.ClientCacheKey
import io.kudos.context.support.Consts
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Request-wrapping strategy tests for [ClientCacheWebFilter].
 *
 * Covers:
 * - default mode: no cache headers -> not wrapped
 * - cache-key header / cache-uid header -> wrapped, response bound to the wrapper
 * - wrapAllRequests compatibility mode
 * - null request / null response / non-http request pass-through
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class ClientCacheWebFilterTest {

    @Test
    fun doFilter_withoutCacheHeaders_doesNotWrapByDefault() {
        val chain = RecordingFilterChain()
        val request = MockHttpServletRequest()

        ClientCacheWebFilter().doFilter(request, MockHttpServletResponse(), chain)

        assertFalse(chain.request is CacheClientRequest)
        assertSame(request, chain.request)
    }

    @Test
    fun doFilter_withCacheKeyHeader_wrapsRequest() {
        val chain = RecordingFilterChain()
        val request = MockHttpServletRequest().apply {
            addHeader(ClientCacheKey.HEADER_KEY_CACHE_KEY, "cache-key")
        }
        val response = MockHttpServletResponse()

        ClientCacheWebFilter().doFilter(request, response, chain)

        val wrapped = chain.request
        assertTrue(wrapped is CacheClientRequest)
        assertSame(response, wrapped.getServletResponse())
    }

    // ---- requireRpcMarker ---------------------------------------------------

    @Test
    fun requireRpcMarker_defaultsOff_soExistingCallersKeepWorking() {
        // 默认必须保持现状。翻转默认值会在"标记未被发送"的部署里静默关掉整个 provider 端，
        // 而标记来自另一个模块（distributed-client-http），运行时并不保证存在。
        val chain = RecordingFilterChain()
        val request = MockHttpServletRequest().apply {
            addHeader(ClientCacheKey.HEADER_KEY_CACHE_KEY, "cache-key")
        }

        ClientCacheWebFilter().doFilter(request, MockHttpServletResponse(), chain)

        assertTrue(chain.request is CacheClientRequest, "默认不应要求内部调用标记")
    }

    @Test
    fun requireRpcMarker_enabled_skipsCallersWithoutTheMarker() {
        // 开启后，没有内部调用标记的请求完全不进入协商，响应里也就不会带上内容指纹。
        val chain = RecordingFilterChain()
        val request = MockHttpServletRequest().apply {
            addHeader(ClientCacheKey.HEADER_KEY_CACHE_KEY, "cache-key")
        }

        ClientCacheWebFilter(requireRpcMarker = true).doFilter(request, MockHttpServletResponse(), chain)

        assertFalse(chain.request is CacheClientRequest, "缺少内部调用标记时不应参与协商")
        assertSame(request, chain.request, "请求应原样传递下去")
    }

    @Test
    fun requireRpcMarker_enabled_allowsMarkedInternalCallers() {
        val chain = RecordingFilterChain()
        val request = MockHttpServletRequest().apply {
            addHeader(ClientCacheKey.HEADER_KEY_CACHE_KEY, "cache-key")
            addHeader(Consts.RequestHeader.RPC_REQUEST, "true")
        }

        ClientCacheWebFilter(requireRpcMarker = true).doFilter(request, MockHttpServletResponse(), chain)

        assertTrue(chain.request is CacheClientRequest, "带标记的内部调用应正常参与协商")
    }

    @Test
    fun requireRpcMarker_enabled_treatsBlankMarkerAsAbsent() {
        // 与 nacos 的 InternalRpcContextWebFilter 保持一致：空白标记等同于没有标记。
        val chain = RecordingFilterChain()
        val request = MockHttpServletRequest().apply {
            addHeader(ClientCacheKey.HEADER_KEY_CACHE_KEY, "cache-key")
            addHeader(Consts.RequestHeader.RPC_REQUEST, "   ")
        }

        ClientCacheWebFilter(requireRpcMarker = true).doFilter(request, MockHttpServletResponse(), chain)

        assertFalse(chain.request is CacheClientRequest)
    }

    @Test
    fun requireRpcMarker_enabled_alsoGatesWrapAllRequests() {
        // wrapAllRequests 是"包装一切"的兼容开关，但它不应绕过内部调用校验。
        val chain = RecordingFilterChain()
        val request = MockHttpServletRequest()

        ClientCacheWebFilter(wrapAllRequests = true, requireRpcMarker = true)
            .doFilter(request, MockHttpServletResponse(), chain)

        assertFalse(chain.request is CacheClientRequest, "requireRpcMarker 应先于 wrapAllRequests 判定")
    }

    @Test
    fun doFilter_withCacheUidHeader_wrapsRequest() {
        val chain = RecordingFilterChain()
        val request = MockHttpServletRequest().apply {
            addHeader(ClientCacheKey.HEADER_KEY_CACHE_UID, "some-uid")
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

    @Test
    fun doFilter_nullRequest_passesThroughUnwrapped() {
        val chain = RecordingFilterChain()

        ClientCacheWebFilter(wrapAllRequests = true).doFilter(null, MockHttpServletResponse(), chain)

        assertTrue(chain.invoked)
        assertNull(chain.request)
    }

    @Test
    fun doFilter_nullResponse_passesThroughUnwrapped() {
        val chain = RecordingFilterChain()
        val request = MockHttpServletRequest().apply {
            addHeader(ClientCacheKey.HEADER_KEY_CACHE_KEY, "cache-key")
        }

        ClientCacheWebFilter(wrapAllRequests = true).doFilter(request, null, chain)

        assertSame(request, chain.request)
        assertFalse(chain.request is CacheClientRequest)
    }

    @Test
    fun doFilter_nonHttpRequest_passesThroughUnwrapped() {
        val chain = RecordingFilterChain()
        val plainRequest = Proxy.newProxyInstance(
            ServletRequest::class.java.classLoader,
            arrayOf(ServletRequest::class.java),
            InvocationHandler { _, _, _ -> null }
        ) as ServletRequest

        ClientCacheWebFilter(wrapAllRequests = true).doFilter(plainRequest, MockHttpServletResponse(), chain)

        assertSame(plainRequest, chain.request)
        assertFalse(chain.request is CacheClientRequest)
    }

    /**
     * Filter chain stub that records the inbound request object.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class RecordingFilterChain : FilterChain {
        var request: ServletRequest? = null
            private set
        var invoked: Boolean = false
            private set

        override fun doFilter(request: ServletRequest?, response: ServletResponse?) {
            this.request = request
            this.invoked = true
        }
    }
}
