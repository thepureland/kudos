package io.kudos.ability.cache.interservice.client.http

import io.kudos.ability.cache.interservice.client.core.ClientCacheHelper
import io.kudos.ability.cache.interservice.common.ClientCacheItem
import io.kudos.ability.cache.interservice.common.ClientCacheKey
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpResponse
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.mock.http.client.MockClientHttpResponse
import java.net.URI
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Drives the inter-service cache protocol directly against [HttpCacheNegotiationInterceptor].
 *
 * This is the precise proof of the 304 path. `InterServiceCacheTest` in the provider module covers
 * that negotiation is *transparent* to callers, but it cannot show that a cache hit actually occurred
 * — a plain refetch looks identical from the outside. Here the "remote" is a stub, so what it did and
 * did not send is observable.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class HttpCacheNegotiationInterceptorTest {

    @Test
    fun withoutLocalCache_passesThroughUntouched() {
        val execution = StubExecution(body = FRESH, cacheStatus = ClientCacheKey.STATUS_DO_CACHE, uid = "u1")
        val interceptor = newInterceptor(RecordingCacheHelper(hasCache = false))

        val response = interceptor.intercept(newRequest(), ByteArray(0), execution)

        assertEquals(FRESH, response.body.readBytes().toString(StandardCharsets.UTF_8))
        assertNull(execution.seenHeaders?.getFirst(ClientCacheKey.HEADER_KEY_CACHE_KEY))
    }

    @Test
    fun firstCall_sendsKeyWithoutUid_andStoresTheFreshBody() {
        val helper = RecordingCacheHelper()
        val execution = StubExecution(body = FRESH, cacheStatus = ClientCacheKey.STATUS_DO_CACHE, uid = "u1")

        val response = newInterceptor(helper).intercept(newRequest(), ByteArray(0), execution)

        val sentKey = execution.seenHeaders?.getFirst(ClientCacheKey.HEADER_KEY_CACHE_KEY)
        assertNotNull(sentKey, "the cache key must be advertised on every negotiated request")
        assertNull(
            execution.seenHeaders?.getFirst(ClientCacheKey.HEADER_KEY_CACHE_UID),
            "with nothing cached there is no uid to advertise"
        )
        assertEquals(FRESH, response.body.readBytes().toString(StandardCharsets.UTF_8))

        val stored = helper.written[sentKey]
        assertEquals("u1", stored?.uuid)
        val cachedBody = stored?.cacheData as CachedResponseBody
        assertEquals(FRESH, cachedBody.bytes.toString(StandardCharsets.UTF_8))
        assertEquals(MediaType.APPLICATION_JSON_VALUE, cachedBody.contentType, "the content type must be cached with the bytes")
    }

    @Test
    fun secondCall_advertisesUid_and304IsServedFromTheLocalCopy() {
        val helper = RecordingCacheHelper()
        val request = newRequest()

        // First call primes the cache.
        newInterceptor(helper).intercept(
            request, ByteArray(0),
            StubExecution(body = FRESH, cacheStatus = ClientCacheKey.STATUS_DO_CACHE, uid = "u1")
        )

        // Second call: the provider answers 304 with an empty body.
        val second = StubExecution(body = "", cacheStatus = ClientCacheKey.STATUS_USE_CACHE, uid = "u1")
        val response = newInterceptor(helper).intercept(newRequest(), ByteArray(0), second)

        assertEquals(
            "u1", second.seenHeaders?.getFirst(ClientCacheKey.HEADER_KEY_CACHE_UID),
            "the cached uid must be advertised so the provider can answer 304"
        )
        // The body the converters will see is the locally cached copy, not the empty 304 body...
        assertEquals(FRESH, response.body.readBytes().toString(StandardCharsets.UTF_8))
        // ...and the status is normalised so a 304 is not mistaken for "no content".
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(FRESH.length.toLong(), response.headers.contentLength)
        // Regression: replaying under the 304 own headers gave the converters application/octet-stream
        // and they refused the body with UnknownContentTypeException.
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
    }

    @Test
    fun keyIsStableAcrossCalls_butDiffersByPath() {
        val helper = RecordingCacheHelper()
        val a1 = StubExecution(body = FRESH, cacheStatus = ClientCacheKey.STATUS_DO_CACHE, uid = "u1")
        val a2 = StubExecution(body = FRESH, cacheStatus = ClientCacheKey.STATUS_DO_CACHE, uid = "u1")
        val b = StubExecution(body = FRESH, cacheStatus = ClientCacheKey.STATUS_DO_CACHE, uid = "u1")

        newInterceptor(helper).intercept(newRequest("/same"), ByteArray(0), a1)
        newInterceptor(helper).intercept(newRequest("/same"), ByteArray(0), a2)
        newInterceptor(helper).intercept(newRequest("/other"), ByteArray(0), b)

        val keyA1 = a1.seenHeaders?.getFirst(ClientCacheKey.HEADER_KEY_CACHE_KEY)
        val keyA2 = a2.seenHeaders?.getFirst(ClientCacheKey.HEADER_KEY_CACHE_KEY)
        val keyB = b.seenHeaders?.getFirst(ClientCacheKey.HEADER_KEY_CACHE_KEY)
        assertEquals(keyA1, keyA2, "the same request must always produce the same key")
        assertTrue(keyA1 != keyB, "different paths must not share a cache entry")
    }

    @Test
    fun differentRequestBodies_produceDifferentKeys() {
        val helper = RecordingCacheHelper()
        val first = StubExecution(body = FRESH, cacheStatus = ClientCacheKey.STATUS_DO_CACHE, uid = "u1")
        val second = StubExecution(body = FRESH, cacheStatus = ClientCacheKey.STATUS_DO_CACHE, uid = "u1")

        newInterceptor(helper).intercept(newRequest(), "one".toByteArray(), first)
        newInterceptor(helper).intercept(newRequest(), "two".toByteArray(), second)

        assertTrue(
            first.seenHeaders?.getFirst(ClientCacheKey.HEADER_KEY_CACHE_KEY) !=
                second.seenHeaders?.getFirst(ClientCacheKey.HEADER_KEY_CACHE_KEY)
        )
    }

    @Test
    fun responseWithoutNegotiationHeaders_isPassedThroughAndNotCached() {
        val helper = RecordingCacheHelper()
        val execution = StubExecution(body = FRESH, cacheStatus = null, uid = null)

        val response = newInterceptor(helper).intercept(newRequest(), ByteArray(0), execution)

        assertEquals(FRESH, response.body.readBytes().toString(StandardCharsets.UTF_8))
        assertTrue(helper.written.isEmpty(), "an endpoint without @ClientCacheable must not populate the cache")
    }

    @Test
    fun a304WithNoLocalEntry_fallsBackInsteadOfFailing() {
        // The provider answers 304 on the strength of a uid we advertised, but the entry is gone
        // (bounded region, evicted between send and receive). Must not blow up.
        val helper = RecordingCacheHelper(evictOnRead = true)
        val execution = StubExecution(body = "", cacheStatus = ClientCacheKey.STATUS_USE_CACHE, uid = "u1")

        val response = newInterceptor(helper).intercept(newRequest(), ByteArray(0), execution)

        assertEquals("", response.body.readBytes().toString(StandardCharsets.UTF_8))
    }

    @Test
    fun excludedGroup_isNotNegotiated() {
        val helper = RecordingCacheHelper()
        val execution = StubExecution(body = FRESH, cacheStatus = ClientCacheKey.STATUS_DO_CACHE, uid = "u1")
        val interceptor = HttpCacheNegotiationInterceptor(
            cacheHelper = helper, applicationName = "app", excludeGroups = setOf("sys"), group = "sys"
        )

        interceptor.intercept(newRequest(), ByteArray(0), execution)

        assertNull(execution.seenHeaders?.getFirst(ClientCacheKey.HEADER_KEY_CACHE_KEY))
        assertTrue(helper.written.isEmpty())
    }

    @Test
    fun includedGroup_isNegotiated() {
        val helper = RecordingCacheHelper()
        val execution = StubExecution(body = FRESH, cacheStatus = ClientCacheKey.STATUS_DO_CACHE, uid = "u1")
        val interceptor = HttpCacheNegotiationInterceptor(
            cacheHelper = helper, applicationName = "app", includeGroups = setOf("sys"), group = "sys"
        )

        interceptor.intercept(newRequest(), ByteArray(0), execution)

        assertNotNull(execution.seenHeaders?.getFirst(ClientCacheKey.HEADER_KEY_CACHE_KEY))
    }

    private fun newInterceptor(helper: ClientCacheHelper) =
        HttpCacheNegotiationInterceptor(cacheHelper = helper, applicationName = "app", group = "sys")

    private fun newRequest(path: String = "/same") =
        MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost:13578$path"))

    /** Stub remote: records the headers it was handed, replies with a configurable negotiated response. */
    private class StubExecution(
        private val body: String,
        private val cacheStatus: String?,
        private val uid: String?,
    ) : ClientHttpRequestExecution {
        var seenHeaders: HttpHeaders? = null

        override fun execute(request: HttpRequest, body: ByteArray): ClientHttpResponse {
            seenHeaders = HttpHeaders(request.headers)
            val response = MockClientHttpResponse(this.body.toByteArray(StandardCharsets.UTF_8), HttpStatus.OK)
            // A real 200 declares its type; a real 304 does not — the interceptor has to restore it.
            if (cacheStatus != ClientCacheKey.STATUS_USE_CACHE) {
                response.headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            }
            cacheStatus?.let { response.headers.set(ClientCacheKey.HEADER_KEY_CACHE_STATUS, it) }
            uid?.let { response.headers.set(ClientCacheKey.HEADER_KEY_CACHE_UID, it) }
            return response
        }
    }

    /** In-memory stand-in for the local cache region. */
    private class RecordingCacheHelper(
        private val hasCache: Boolean = true,
        private val evictOnRead: Boolean = false,
    ) : ClientCacheHelper() {
        val written = mutableMapOf<String, ClientCacheItem>()

        override fun hasLocalCache(): Boolean = hasCache

        override fun loadFromLocalCache(cacheKey: String): ClientCacheItem? =
            if (evictOnRead) null else written[cacheKey]

        override fun writeToLocalCache(cacheKey: String, data: ClientCacheItem?) {
            data?.let { written[cacheKey] = it }
        }
    }

    private companion object {
        const val FRESH = """{"id":1,"name":"one"}"""
    }
}
