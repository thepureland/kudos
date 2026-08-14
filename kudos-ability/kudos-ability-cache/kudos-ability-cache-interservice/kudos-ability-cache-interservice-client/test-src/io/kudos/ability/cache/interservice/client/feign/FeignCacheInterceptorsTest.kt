package io.kudos.ability.cache.interservice.client.feign

import feign.Request
import feign.RequestTemplate
import feign.Response
import feign.Target
import feign.Util
import feign.codec.DecodeException
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
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
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
    fun request_cacheKeyDistinguishesTargetServices() {
        // 同一路径、不同被调服务必须得到不同的 cacheKey。
        // RequestInterceptor 在 Target.apply(template) 之前运行，此时 request.url() 只有路径没有 host；
        // 而 applicationName 是"调用方自己"的名字，同一 JVM 内所有 Feign client 都一样。
        // 于是 serviceA.get("/config") 与 serviceB.get("/config") 会算出同一个 key：
        // 一方的响应会覆盖另一方，304 判定还会让 B 读到 A 的数据。
        helper.localCacheEnabled = true
        val interceptor = newRequestInterceptor(appName = "caller-app")

        val toA = newRequestTemplate(method = "GET", url = "/config")
            .apply { feignTarget(Target.HardCodedTarget(Any::class.java, "service-a", "http://service-a")) }
        val toB = newRequestTemplate(method = "GET", url = "/config")
            .apply { feignTarget(Target.HardCodedTarget(Any::class.java, "service-b", "http://service-b")) }

        interceptor.apply(toA)
        interceptor.apply(toB)

        val keyA = toA.headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY]!!.first()
        val keyB = toB.headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY]!!.first()
        assertNotEquals(keyA, keyB, "被调服务不同时 cacheKey 必须不同，否则会跨服务串数据")
    }

    @Test
    fun request_binaryBodiesThatDifferDoNotShareACacheKey() {
        // body 以原始字节参与摘要。旧实现走 String(body)：用平台字符集解码任意字节既不跨机器确定，
        // 对二进制载荷（protobuf、文件上传）还是有损的——无法映射的字节统统塌缩成替换字符，
        // 于是两个不同的 body 会算出同一个 key，一个请求可能拿到另一个请求的缓存响应。
        helper.localCacheEnabled = true
        val interceptor = newRequestInterceptor(appName = "svc-A")

        // 两串各自都不是合法 UTF-8 的字节，且互不相同
        val bodyA = byteArrayOf(0xC3.toByte(), 0x28)
        val bodyB = byteArrayOf(0xA0.toByte(), 0xA1.toByte())

        val templateA = newRequestTemplate(method = "POST", url = "/upload", body = bodyA)
        val templateB = newRequestTemplate(method = "POST", url = "/upload", body = bodyB)
        interceptor.apply(templateA)
        interceptor.apply(templateB)

        assertNotEquals(
            templateA.headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY]!!.first(),
            templateB.headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY]!!.first(),
            "二进制 body 不同则 cacheKey 必须不同，否则会串用彼此的缓存响应"
        )
    }

    @Test
    fun request_sameRequestProducesStableCacheKey() {
        helper.localCacheEnabled = true
        val interceptor = newRequestInterceptor(appName = "svc-A")

        val first = newRequestTemplate(method = "GET", url = "/users/1")
        val second = newRequestTemplate(method = "GET", url = "/users/1")
        interceptor.apply(first)
        interceptor.apply(second)

        assertEquals(
            first.headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY]!!.first(),
            second.headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY]!!.first(),
            "同一请求必须稳定地算出同一个 key"
        )
    }

    // ---- per-client 开关 ------------------------------------------------------

    @Test
    fun request_excludedClientGetsNoCacheHeaders() {
        // Spring Cloud 会把每个 RequestInterceptor 应用到所有 @FeignClient 上，
        // 包括调用第三方 API 的那些——那会把内部内容指纹发给外部。
        helper.localCacheEnabled = true
        val interceptor = newRequestInterceptor(appName = "svc-A", excludeClients = setOf("third-party"))

        val template = newRequestTemplate(method = "GET", url = "/x")
            .apply { feignTarget(Target.HardCodedTarget(Any::class.java, "third-party", "http://third-party")) }
        interceptor.apply(template)

        assertNull(
            template.headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY],
            "被排除的 client 不应带上任何缓存头"
        )
    }

    @Test
    fun request_allowListRestrictsToListedClients() {
        helper.localCacheEnabled = true
        val interceptor = newRequestInterceptor(appName = "svc-A", includeClients = setOf("internal-svc"))

        val allowed = newRequestTemplate(method = "GET", url = "/x")
            .apply { feignTarget(Target.HardCodedTarget(Any::class.java, "internal-svc", "http://internal-svc")) }
        val other = newRequestTemplate(method = "GET", url = "/x")
            .apply { feignTarget(Target.HardCodedTarget(Any::class.java, "other-svc", "http://other-svc")) }
        interceptor.apply(allowed)
        interceptor.apply(other)

        assertNotNull(allowed.headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY], "白名单内的 client 应参与")
        assertNull(other.headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY], "白名单外的 client 不应参与")
    }

    @Test
    fun request_bothListsEmpty_appliesEverywhere() {
        // 默认不配置时保持原有的"处处生效"行为。
        helper.localCacheEnabled = true
        val interceptor = newRequestInterceptor(appName = "svc-A")

        val template = newRequestTemplate(method = "GET", url = "/x")
            .apply { feignTarget(Target.HardCodedTarget(Any::class.java, "any-svc", "http://any-svc")) }
        interceptor.apply(template)

        assertNotNull(template.headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY])
    }

    @Test
    fun request_doesNotCreateAKudosContextOnTheCallingThread() {
        // Feign 跑在 HTTP 客户端 / @Async / 舱壁线程池上。KudosContextHolder.get() 在没有绑定时会新建并 set 进
        // InheritableThreadLocal —— 既在池线程上留下永不回收的上下文（该类自己的 KDoc 就警告了这点），
        // 又让 tenantId 退化为空，把不同租户的请求算到同一个 cacheKey 上。
        KudosContextHolder.clear()
        helper.localCacheEnabled = true
        val interceptor = newRequestInterceptor(appName = "caller-app")

        interceptor.apply(newRequestTemplate(method = "GET", url = "/users/1"))

        assertNull(
            KudosContextHolder.getOrNull(),
            "拦截器不应在调用线程上创建并绑定 KudosContext"
        )
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

    @Test
    fun request_nullTenantId_stillGeneratesCacheKey_distinctFromTenantKey() {
        helper.localCacheEnabled = true
        val interceptor = newRequestInterceptor(appName = "svc-A")

        // Key for tenant-X (set in setup)
        val tenantTemplate = newRequestTemplate(method = "GET", url = "/users/1")
        interceptor.apply(tenantTemplate)
        val tenantKey = tenantTemplate.headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY]!!.first()

        // Null tenant falls back to "" and must still produce a key — but a different one (tenant isolation)
        KudosContextHolder.get().tenantId = null
        val template = newRequestTemplate(method = "GET", url = "/users/1")
        interceptor.apply(template)
        val nullTenantKey = template.headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY]?.firstOrNull()

        assertNotNull(nullTenantKey, "A cache key must still be generated when the tenant id is null")
        assertNotEquals(tenantKey, nullTenantKey, "Different tenants must not share cache keys")
    }

    @Test
    fun request_sameRequest_generatesStableKey() {
        helper.localCacheEnabled = true
        val interceptor = newRequestInterceptor(appName = "svc-A")

        val t1 = newRequestTemplate(method = "POST", url = "/orders", body = "{\"id\":1}".toByteArray())
        val t2 = newRequestTemplate(method = "POST", url = "/orders", body = "{\"id\":1}".toByteArray())
        interceptor.apply(t1)
        interceptor.apply(t2)

        assertEquals(
            t1.headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY]!!.first(),
            t2.headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY]!!.first(),
            "The same request must always map to the same cache key"
        )
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

    @Test
    fun response_missingStatusHeader_fallsThroughToDelegate() {
        helper.localCacheEnabled = true
        val delegate = RecordingDecoder("decoded")
        val interceptor = FeignCacheResponseInterceptor(delegate, helper)

        // cache-uid present but cache-status missing -> do not enter the cache path
        val response = newResponse(
            200,
            headers = mapOf(ClientCacheKey.HEADER_KEY_CACHE_UID to listOf("uid-X")),
            body = "body",
            requestHeaders = mapOf(ClientCacheKey.HEADER_KEY_CACHE_KEY to listOf("cache-1")),
        )
        val result = interceptor.decode(response, String::class.java)

        assertEquals("decoded", result)
        assertEquals(1, delegate.callCount)
        assertNull(helper.loadFromLocalCache("cache-1"), "nothing must be cached outside the cache path")
    }

    @Test
    fun response_emptyCacheKeyHeaderValue_fallsThroughToDelegate() {
        helper.localCacheEnabled = true
        val delegate = RecordingDecoder("decoded")
        val interceptor = FeignCacheResponseInterceptor(delegate, helper)

        // The request carries a cache-key header whose value is an empty string (defensive branch)
        val response = newResponse(
            200,
            headers = mapOf(
                ClientCacheKey.HEADER_KEY_CACHE_UID to listOf("uid-X"),
                ClientCacheKey.HEADER_KEY_CACHE_STATUS to listOf(ClientCacheKey.STATUS_DO_CACHE),
            ),
            body = "body",
            requestHeaders = mapOf(ClientCacheKey.HEADER_KEY_CACHE_KEY to listOf("")),
        )
        val result = interceptor.decode(response, String::class.java)

        assertEquals("decoded", result)
        assertEquals(1, delegate.callCount)
    }

    @Test
    fun response_emptyCacheKeyHeaderValueList_fallsThroughToDelegate() {
        helper.localCacheEnabled = true
        val delegate = RecordingDecoder("decoded")
        val interceptor = FeignCacheResponseInterceptor(delegate, helper)

        // The request header map contains the cache-key entry but with an EMPTY value list
        // (distinct from the empty-string case): firstOrNull() yields null -> delegate path.
        val response = newResponse(
            200,
            headers = mapOf(
                ClientCacheKey.HEADER_KEY_CACHE_UID to listOf("uid-X"),
                ClientCacheKey.HEADER_KEY_CACHE_STATUS to listOf(ClientCacheKey.STATUS_DO_CACHE),
            ),
            body = "body",
            requestHeaders = mapOf(ClientCacheKey.HEADER_KEY_CACHE_KEY to emptyList()),
        )
        val result = interceptor.decode(response, String::class.java)

        assertEquals("decoded", result)
        assertEquals(1, delegate.callCount)
    }

    @Test
    fun response_emptyUidHeaderValueList_decodesButSkipsCacheWrite() {
        helper.localCacheEnabled = true
        val delegate = RecordingDecoder("freshly-decoded")
        val interceptor = FeignCacheResponseInterceptor(delegate, helper)

        // cache-uid header key exists but with an EMPTY value list -> cacheUid is null
        // (the null side of isNullOrEmpty, distinct from the blank-string case): decode, never cache.
        val response = newResponse(
            200,
            headers = mapOf(
                ClientCacheKey.HEADER_KEY_CACHE_UID to emptyList(),
                ClientCacheKey.HEADER_KEY_CACHE_STATUS to listOf(ClientCacheKey.STATUS_DO_CACHE),
            ),
            body = "body",
            requestHeaders = mapOf(ClientCacheKey.HEADER_KEY_CACHE_KEY to listOf("cache-1")),
        )
        val result = interceptor.decode(response, String::class.java)

        assertEquals("freshly-decoded", result)
        assertEquals(1, delegate.callCount)
        assertNull(helper.loadFromLocalCache("cache-1"), "null uid must not be written into the local cache")
    }

    @Test
    fun response_status304_localCacheMiss_failsLoudlyInsteadOfReturningNull() {
        // 服务端因为我们上报了 cache-uid 而只回了空 body；此时本地条目却已被淘汰（FEIGN-CACHE 区是有界的，
        // 请求往返期间被 LRU 挤掉很常见），于是既没有 body 可解码，也没有本地副本可用。
        // 旧实现直接返回 null —— 而 Feign 接口在 Kotlin 里通常声明为不可空返回类型，
        // 这个 null 会一路流进业务代码，在无关的位置炸掉或被当成合法值。
        // 现在显式失败：错误可诊断，且重试必定成功（本地无条目 → 下次请求不带 cache-uid → 服务端回完整 body）。
        helper.localCacheEnabled = true // nothing written for "cache-miss"
        val delegate = RecordingDecoder("should-not-be-called")
        val interceptor = FeignCacheResponseInterceptor(delegate, helper)

        val response = newResponse(
            200,
            headers = mapOf(
                ClientCacheKey.HEADER_KEY_CACHE_UID to listOf("uid-A"),
                ClientCacheKey.HEADER_KEY_CACHE_STATUS to listOf(ClientCacheKey.STATUS_USE_CACHE),
            ),
            body = "ignored",
            requestHeaders = mapOf(ClientCacheKey.HEADER_KEY_CACHE_KEY to listOf("cache-miss")),
        )

        val ex = assertFailsWith<DecodeException> { interceptor.decode(response, String::class.java) }

        assertTrue(
            ex.message!!.contains("cache-miss"),
            "异常信息应带上 cacheKey 以便定位，实际: ${ex.message}"
        )
        assertEquals(0, delegate.callCount, "304 分支不应调用真实 decoder（body 是空的）")
    }

    @Test
    fun response_status200_blankUid_decodesButSkipsCacheWrite() {
        helper.localCacheEnabled = true
        val delegate = RecordingDecoder("freshly-decoded")
        val interceptor = FeignCacheResponseInterceptor(delegate, helper)

        // cache-uid header exists but its value is blank -> decode normally, never cache
        val response = newResponse(
            200,
            headers = mapOf(
                ClientCacheKey.HEADER_KEY_CACHE_UID to listOf(""),
                ClientCacheKey.HEADER_KEY_CACHE_STATUS to listOf(ClientCacheKey.STATUS_DO_CACHE),
            ),
            body = "body",
            requestHeaders = mapOf(ClientCacheKey.HEADER_KEY_CACHE_KEY to listOf("cache-1")),
        )
        val result = interceptor.decode(response, String::class.java)

        assertEquals("freshly-decoded", result)
        assertEquals(1, delegate.callCount)
        assertNull(helper.loadFromLocalCache("cache-1"), "blank uid must not be written into the local cache")
    }

    @Test
    fun response_emptyStatusHeaderValues_treatedAsFreshData() {
        helper.localCacheEnabled = true
        val delegate = RecordingDecoder("freshly-decoded")
        val interceptor = FeignCacheResponseInterceptor(delegate, helper)

        // cache-status header key exists with an empty value list -> cacheStatus is null -> fresh-data path
        val response = newResponse(
            200,
            headers = mapOf(
                ClientCacheKey.HEADER_KEY_CACHE_UID to listOf("uid-Z"),
                ClientCacheKey.HEADER_KEY_CACHE_STATUS to emptyList(),
            ),
            body = "body",
            requestHeaders = mapOf(ClientCacheKey.HEADER_KEY_CACHE_KEY to listOf("cache-1")),
        )
        val result = interceptor.decode(response, String::class.java)

        assertEquals("freshly-decoded", result)
        assertEquals(1, delegate.callCount)
        assertEquals("uid-Z", helper.loadFromLocalCache("cache-1")?.uuid)
    }

    // endregion

    // region helpers

    private fun newRequestInterceptor(
        appName: String,
        includeClients: Set<String> = emptySet(),
        excludeClients: Set<String> = emptySet(),
    ): FeignCacheRequestInterceptor {
        return FeignCacheRequestInterceptor(helper, appName, includeClients, excludeClients)
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
