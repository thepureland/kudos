package io.kudos.ability.cache.interservice.aop

import io.kudos.ability.cache.interservice.common.ClientCacheItem
import io.kudos.ability.cache.interservice.common.ClientCacheKey
import io.kudos.ability.cache.interservice.common.RequestResult
import io.kudos.ability.cache.interservice.provider.web.CacheClientRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.junit.jupiter.api.AfterEach
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for ClientCacheableAspect
 *
 * Covers:
 * - exception propagation from the target method
 * - controller-class validation (RestController / Controller / non-controller)
 * - null result short-circuit
 * - missing RequestContextHolder attributes
 * - plain (non CacheClientRequest) http requests
 * - CacheClientRequest without a bound servlet response
 * - request uid absent / blank / matching / mismatching branches and response headers
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class ClientCacheableAspectTest {

    @AfterEach
    fun tearDown() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    fun around_rethrowsOriginalException() {
        val cause = IllegalStateException("boom")
        val aspect = ClientCacheableAspect()

        val thrown = assertFailsWith<IllegalStateException> {
            aspect.around(joinPoint(restControllerTarget) { throw cause })
        }

        assertSame(cause, thrown)
    }

    @Test
    fun around_rejectsNonControllerTarget() {
        val aspect = ClientCacheableAspect()

        val thrown = assertFailsWith<IllegalArgumentException> {
            aspect.around(joinPoint(NotAController()) { "ignored" })
        }

        assertTrue(thrown.message!!.contains("must be a Controller"))
    }

    @Test
    fun around_acceptsControllerAnnotatedTarget() {
        val aspect = ClientCacheableAspect()
        val result = RequestResult(1, "one")

        // No request attributes bound: result is returned untouched after validation passes.
        assertSame(result, aspect.around(joinPoint(mvcControllerTarget) { result }))
    }

    @Test
    fun around_nullResult_returnsNull() {
        val aspect = ClientCacheableAspect()

        assertNull(aspect.around(joinPoint(restControllerTarget) { null }))
    }

    @Test
    fun around_withoutRequestAttributes_returnsResult() {
        val aspect = ClientCacheableAspect()
        val result = RequestResult(1, "one")

        assertSame(result, aspect.around(joinPoint(restControllerTarget) { result }))
    }

    @Test
    fun around_plainHttpRequest_returnsResultWithoutCacheHeaders() {
        val aspect = ClientCacheableAspect()
        val result = RequestResult(1, "one")
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(MockHttpServletRequest()))

        assertSame(result, aspect.around(joinPoint(restControllerTarget) { result }))
    }

    @Test
    fun around_cacheClientRequestWithoutResponse_returnsResult() {
        val aspect = ClientCacheableAspect()
        val result = RequestResult(1, "one")
        val request = CacheClientRequest(MockHttpServletRequest(), null)
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

        assertSame(result, aspect.around(joinPoint(restControllerTarget) { result }))
    }

    @Test
    fun around_missingRequestUid_returnsResultAndSetsDoCacheHeaders() {
        val aspect = ClientCacheableAspect()
        val result = RequestResult(1, "one")
        val response = MockHttpServletResponse()
        bindCacheClientRequest(MockHttpServletRequest(), response)

        assertSame(result, aspect.around(joinPoint(restControllerTarget) { result }))
        assertEquals(ClientCacheItem.genUid(result), response.getHeader(ClientCacheKey.HEADER_KEY_CACHE_UID))
        assertEquals(ClientCacheKey.STATUS_DO_CACHE, response.getHeader(ClientCacheKey.HEADER_KEY_CACHE_STATUS))
    }

    @Test
    fun around_blankRequestUid_returnsResultAndSetsDoCacheHeaders() {
        val aspect = ClientCacheableAspect()
        val result = RequestResult(1, "one")
        val inner = MockHttpServletRequest().apply {
            addHeader(ClientCacheKey.HEADER_KEY_CACHE_UID, "   ")
        }
        val response = MockHttpServletResponse()
        bindCacheClientRequest(inner, response)

        assertSame(result, aspect.around(joinPoint(restControllerTarget) { result }))
        assertEquals(ClientCacheKey.STATUS_DO_CACHE, response.getHeader(ClientCacheKey.HEADER_KEY_CACHE_STATUS))
    }

    @Test
    fun around_matchingUid_returnsNullAndSetsUseCacheStatus() {
        val aspect = ClientCacheableAspect()
        val result = RequestResult(1, "one")
        val expectedUid = ClientCacheItem.genUid(result)
        val inner = MockHttpServletRequest().apply {
            addHeader(ClientCacheKey.HEADER_KEY_CACHE_UID, expectedUid)
        }
        val response = MockHttpServletResponse()
        bindCacheClientRequest(inner, response)

        assertNull(aspect.around(joinPoint(restControllerTarget) { result }))
        assertEquals(expectedUid, response.getHeader(ClientCacheKey.HEADER_KEY_CACHE_UID))
        assertEquals(ClientCacheKey.STATUS_USE_CACHE, response.getHeader(ClientCacheKey.HEADER_KEY_CACHE_STATUS))
    }

    @Test
    fun around_mismatchingUid_returnsResultAndSetsDoCacheStatus() {
        val aspect = ClientCacheableAspect()
        val result = RequestResult(1, "one")
        val inner = MockHttpServletRequest().apply {
            addHeader(ClientCacheKey.HEADER_KEY_CACHE_UID, "stale-uid")
        }
        val response = MockHttpServletResponse()
        bindCacheClientRequest(inner, response)

        assertSame(result, aspect.around(joinPoint(restControllerTarget) { result }))
        assertEquals(ClientCacheItem.genUid(result), response.getHeader(ClientCacheKey.HEADER_KEY_CACHE_UID))
        assertEquals(ClientCacheKey.STATUS_DO_CACHE, response.getHeader(ClientCacheKey.HEADER_KEY_CACHE_STATUS))
    }

    @Test
    fun cut_pointcutMarker_isInvokable() {
        // The @Pointcut method is a pure marker; invoke it reflectively so its body is exercised.
        val aspect = ClientCacheableAspect()
        val cut = ClientCacheableAspect::class.java.getDeclaredMethod("cut")
        cut.isAccessible = true
        assertNull(cut.invoke(aspect))
    }

    private fun bindCacheClientRequest(inner: MockHttpServletRequest, response: MockHttpServletResponse) {
        val request = CacheClientRequest(inner, response)
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    }

    companion object {
        private val restControllerTarget = RestTarget()
        private val mvcControllerTarget = MvcTarget()

        private fun joinPoint(target: Any, proceed: () -> Any?): ProceedingJoinPoint =
            proxy(ProceedingJoinPoint::class.java) { joinPointMethod, _ ->
                when (joinPointMethod.name) {
                    "getTarget" -> target
                    "proceed" -> proceed()
                    else -> defaultValue(joinPointMethod.returnType)
                }
            }

        private fun <T> proxy(type: Class<T>, handler: (Method, Array<Any?>?) -> Any?): T =
            type.cast(
                Proxy.newProxyInstance(
                    type.classLoader,
                    arrayOf(type),
                    InvocationHandler { _, method, args -> handler(method, args) }
                )
            )

        private fun defaultValue(returnType: Class<*>): Any? =
            when (returnType) {
                java.lang.Boolean.TYPE -> false
                java.lang.Byte.TYPE -> 0.toByte()
                java.lang.Short.TYPE -> 0.toShort()
                java.lang.Integer.TYPE -> 0
                java.lang.Long.TYPE -> 0L
                java.lang.Float.TYPE -> 0f
                java.lang.Double.TYPE -> 0.0
                java.lang.Character.TYPE -> 0.toChar()
                java.lang.Void.TYPE -> null
                else -> null
            }
    }

    /**
     * RestController-annotated aspect target.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    @RestController
    private class RestTarget

    /**
     * Controller-annotated aspect target.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    @Controller
    private class MvcTarget

    /**
     * Aspect target without any controller annotation.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class NotAController
}
