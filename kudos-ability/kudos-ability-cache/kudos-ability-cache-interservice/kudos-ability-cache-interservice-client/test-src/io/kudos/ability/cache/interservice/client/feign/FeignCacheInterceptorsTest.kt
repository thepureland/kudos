package io.kudos.ability.cache.interservice.client.feign

import feign.Request
import feign.RequestTemplate
import feign.Response
import feign.Util
import feign.codec.Decoder
import io.kudos.ability.cache.interservice.client.core.ClientCacheHelper
import io.kudos.ability.cache.interservice.common.ClientCacheItem
import io.kudos.ability.cache.interservice.common.ClientCacheKey
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Protocol-contract unit tests for [FeignCacheRequestInterceptor] / [FeignCacheResponseInterceptor].
 *
 * Does not start Spring or issue real HTTP calls — directly constructs Feign's [RequestTemplate] /
 * [Response] objects and asserts:
 *  - **Request side**: local cache hit -> inject `cache-key` + `cache-uid` headers.
 *  - **Request side**: no local cache (hasLocalCache returns false) -> inject no headers.
 *  - **Response side 304**: read data from local cache, original decoder is not invoked.
 *  - **Response side 200**: original decoder decodes body + write to local cache (with new UID).
 *  - **Response side missing cache-uid/cache-status header**: delegate decoding as-is, do not enter cache path.
 *
 * Uses the in-memory [StubClientCacheHelper] in place of the real [ClientCacheHelper] — avoids
 * bringing up Spring + Caffeine.
 */
internal class FeignCacheInterceptorsTest {

    private lateinit var helper: StubClientCacheHelper

    @BeforeTest
    fun setup() {
        helper = StubClientCacheHelper()
        // KudosContextHolder is an InheritableThreadLocal independent per test; setting tenant stabilizes the computed key
        KudosContextHolder.get().tenantId = "tenant-X"
    }

    @AfterTest
    fun teardown() {
        KudosContextHolder.clear()
    }

    // region Request interceptor

    @Test
    fun request_noLocalCache_skipsHeaderInjection() {
        helper.localCacheEnabled = false
        val interceptor = newRequestInterceptor(appName = "svc-A")

        val template = newRequestTemplate(method = "GET", url = "/users/1")
        interceptor.apply(template)

        assertNull(template.headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY],
            "When there is no local cache, cache-key must not be added to the request headers")
        assertNull(template.headers()[ClientCacheKey.HEADER_KEY_CACHE_UID],
            "When there is no local cache, cache-uid must not be added to the request headers")
    }

    @Test
    fun request_localCacheEmpty_setsCacheKeyHeader_butNoUid() {
        helper.localCacheEnabled = true
        val interceptor = newRequestInterceptor(appName = "svc-A")
        val template = newRequestTemplate(method = "GET", url = "/users/1")

        interceptor.apply(template)

        // Cache is empty -- cache-key should be added (so the server can identify it), but cache-uid should not
        assertNotNull(template.headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY])
        assertNull(template.headers()[ClientCacheKey.HEADER_KEY_CACHE_UID])
    }

    @Test
    fun request_localCacheHit_setsBothCacheKeyAndUidHeader() {
        helper.localCacheEnabled = true
        val interceptor = newRequestInterceptor(appName = "svc-A")

        // Call apply once first to obtain the generated cacheKey
        val firstTemplate = newRequestTemplate(method = "GET", url = "/users/1")
        interceptor.apply(firstTemplate)
        val cacheKey = firstTemplate.headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY]!!.first()
        // Simulate that a cache entry for this key already exists locally
        helper.writeToLocalCache(cacheKey, ClientCacheItem("uid-existing", "cached-payload"))

        val template = newRequestTemplate(method = "GET", url = "/users/1")
        interceptor.apply(template)

        assertEquals(listOf(cacheKey), template.headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY]!!.toList())
        assertEquals(listOf("uid-existing"), template.headers()[ClientCacheKey.HEADER_KEY_CACHE_UID]!!.toList())
    }

    // endregion

    // region Response decoder

    @Test
    fun response_noLocalCache_fallsThroughToDelegate() {
        helper.localCacheEnabled = false
        val delegate = RecordingDecoder("decoded-value")
        val interceptor = FeignCacheResponseInterceptor(delegate, helper)

        val response = newResponse(200, headers = emptyMap(), body = "body")
        val result = interceptor.decode(response, String::class.java)

        assertEquals("decoded-value", result)
        assertEquals(1, delegate.callCount, "Without a local cache the original decoder should be used directly")
    }

    @Test
    fun response_missingCacheHeaders_fallsThroughToDelegate() {
        helper.localCacheEnabled = true
        val delegate = RecordingDecoder("decoded-value")
        val interceptor = FeignCacheResponseInterceptor(delegate, helper)

        // Response is missing cache-uid/cache-status headers -> treat as "server has not enabled @ClientCacheable" and go through delegate
        val response = newResponse(200, headers = emptyMap(), body = "body")
        val result = interceptor.decode(response, String::class.java)

        assertEquals("decoded-value", result)
        assertEquals(1, delegate.callCount)
    }

    @Test
    fun response_status304_returnsCachedItemWithoutCallingDelegate() {
        helper.localCacheEnabled = true
        helper.writeToLocalCache("cache-1", ClientCacheItem("uid-A", "cached-data"))
        val delegate = RecordingDecoder("should-not-be-called")
        val interceptor = FeignCacheResponseInterceptor(delegate, helper)

        val response = newResponse(
            200,
            headers = mapOf(
                ClientCacheKey.HEADER_KEY_CACHE_UID to listOf("uid-A"),
                ClientCacheKey.HEADER_KEY_CACHE_STATUS to listOf(ClientCacheKey.STATUS_USE_CACHE),
            ),
            body = "ignored",
            requestHeaders = mapOf(ClientCacheKey.HEADER_KEY_CACHE_KEY to listOf("cache-1")),
        )
        val result = interceptor.decode(response, String::class.java)

        assertEquals("cached-data", result)
        assertEquals(0, delegate.callCount, "304 path must not invoke the original decoder")
    }

    @Test
    fun response_status200_decodesAndWritesLocalCache() {
        helper.localCacheEnabled = true
        val delegate = RecordingDecoder("freshly-decoded")
        val interceptor = FeignCacheResponseInterceptor(delegate, helper)

        val response = newResponse(
            200,
            headers = mapOf(
                ClientCacheKey.HEADER_KEY_CACHE_UID to listOf("uid-NEW"),
                ClientCacheKey.HEADER_KEY_CACHE_STATUS to listOf(ClientCacheKey.STATUS_DO_CACHE),
            ),
            body = "body",
            requestHeaders = mapOf(ClientCacheKey.HEADER_KEY_CACHE_KEY to listOf("cache-1")),
        )
        val result = interceptor.decode(response, String::class.java)

        assertEquals("freshly-decoded", result)
        assertEquals(1, delegate.callCount)
        val cached = helper.loadFromLocalCache("cache-1")
        assertNotNull(cached, "200 path must write the decoded value into local cache")
        assertEquals("uid-NEW", cached.uuid)
        assertEquals("freshly-decoded", cached.cacheData)
    }

    @Test
    fun response_missingCacheKeyHeader_fallsThroughToDelegate() {
        helper.localCacheEnabled = true
        val delegate = RecordingDecoder("decoded")
        val interceptor = FeignCacheResponseInterceptor(delegate, helper)

        // Server returned cache-uid/cache-status but the client's request headers lack cache-key (should not happen, defensive)
        val response = newResponse(
            200,
            headers = mapOf(
                ClientCacheKey.HEADER_KEY_CACHE_UID to listOf("uid-X"),
                ClientCacheKey.HEADER_KEY_CACHE_STATUS to listOf(ClientCacheKey.STATUS_DO_CACHE),
            ),
            body = "body",
            requestHeaders = emptyMap(),
        )
        val result = interceptor.decode(response, String::class.java)

        assertEquals("decoded", result, "decoding still works when cache-key is missing")
        assertEquals(1, delegate.callCount)
    }

    // endregion

    // region helpers

    private fun newRequestInterceptor(appName: String): FeignCacheRequestInterceptor {
        return FeignCacheRequestInterceptor(helper, appName)
    }

    private fun newRequestTemplate(method: String, url: String, body: ByteArray? = null): RequestTemplate {
        // In production Feign calls `resolve` itself before invoking interceptors; simulate that here manually.
        // [RequestTemplate.resolve] returns a **new object** — the original's resolved flag stays unchanged, so capture the return value.
        val template = RequestTemplate()
        template.method(Request.HttpMethod.valueOf(method))
        template.uri(url)
        template.target("http://localhost")
        if (body != null) template.body(body, Util.UTF_8)
        return template.resolve(emptyMap<String, Any>())
    }

    private fun newResponse(
        status: Int,
        headers: Map<String, Collection<String>>,
        body: String,
        requestHeaders: Map<String, Collection<String>> = emptyMap(),
    ): Response {
        val req = Request.create(
            Request.HttpMethod.GET,
            "http://localhost/x",
            requestHeaders,
            Request.Body.empty(),
            RequestTemplate(),
        )
        return Response.builder()
            .status(status)
            .headers(headers)
            .body(body.toByteArray(StandardCharsets.UTF_8))
            .request(req)
            .build()
    }

    /** Records each decode invocation so tests can assert "was it called". */
    private class RecordingDecoder(private val toReturn: Any?) : Decoder {
        var callCount: Int = 0
            private set
        override fun decode(response: Response, type: Type): Any? {
            callCount++
            return toReturn
        }
    }

    /** In-memory Map stub for [ClientCacheHelper] — bypasses IKeyValueCacheManager wiring. */
    open class StubClientCacheHelper : ClientCacheHelper() {
        @Volatile var localCacheEnabled: Boolean = true
        private val store = ConcurrentHashMap<String, ClientCacheItem>()
        override fun hasLocalCache(): Boolean = localCacheEnabled
        override fun loadFromLocalCache(cacheKey: String): ClientCacheItem? = store[cacheKey]
        override fun writeToLocalCache(cacheKey: String, data: ClientCacheItem?) {
            if (data != null) store[cacheKey] = data else store.remove(cacheKey)
        }
        override fun afterPropertiesSet() { /* no-op */ }
    }
    // endregion
}
