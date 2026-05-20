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
 * [FeignCacheRequestInterceptor] / [FeignCacheResponseInterceptor] 协议契约单测。
 *
 * 不启动 Spring / 不发起真正 HTTP 调用——直接构造 Feign 的 [RequestTemplate] / [Response] 对象，
 * 验证：
 *  - **请求侧**：本地有缓存 → 头部塞 `cache-key` + `cache-uid`
 *  - **请求侧**：无本地缓存（hasLocalCache 返回 false）→ 不塞任何 header
 *  - **响应侧 304**：从本地缓存取数据，原 decoder 不被调用
 *  - **响应侧 200**：原 decoder 解 body + 写本地缓存（带新 UID）
 *  - **响应侧无 cache-uid/cache-status 头**：原样 delegate 解码，不触缓存路径
 *
 * 用内存中的 [StubClientCacheHelper] 替代真实 [ClientCacheHelper]——避免拉起 Spring + Caffeine。
 */
internal class FeignCacheInterceptorsTest {

    private lateinit var helper: StubClientCacheHelper

    @BeforeTest
    fun setup() {
        helper = StubClientCacheHelper()
        // KudosContextHolder 是 InheritableThreadLocal，每个测试方法独立；设 tenant 让 key 算出来稳定
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
            "没本地缓存时不应往请求头塞 cache-key")
        assertNull(template.headers()[ClientCacheKey.HEADER_KEY_CACHE_UID],
            "没本地缓存时不应往请求头塞 cache-uid")
    }

    @Test
    fun request_localCacheEmpty_setsCacheKeyHeader_butNoUid() {
        helper.localCacheEnabled = true
        val interceptor = newRequestInterceptor(appName = "svc-A")
        val template = newRequestTemplate(method = "GET", url = "/users/1")

        interceptor.apply(template)

        // 缓存为空——cache-key 应当被塞（让服务端识别），但 cache-uid 没有
        assertNotNull(template.headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY])
        assertNull(template.headers()[ClientCacheKey.HEADER_KEY_CACHE_UID])
    }

    @Test
    fun request_localCacheHit_setsBothCacheKeyAndUidHeader() {
        helper.localCacheEnabled = true
        val interceptor = newRequestInterceptor(appName = "svc-A")

        // 先调一次 apply 拿到生成的 cacheKey
        val firstTemplate = newRequestTemplate(method = "GET", url = "/users/1")
        interceptor.apply(firstTemplate)
        val cacheKey = firstTemplate.headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY]!!.first()
        // 模拟本地已有该 key 的缓存项
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
        assertEquals(1, delegate.callCount, "无本地缓存时应直接走原始 decoder")
    }

    @Test
    fun response_missingCacheHeaders_fallsThroughToDelegate() {
        helper.localCacheEnabled = true
        val delegate = RecordingDecoder("decoded-value")
        val interceptor = FeignCacheResponseInterceptor(delegate, helper)

        // 响应缺 cache-uid/cache-status 头 → 视为"服务端没启用 @ClientCacheable"，走 delegate
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
        assertEquals(0, delegate.callCount, "304 路径不应调原 decoder")
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
        assertNotNull(cached, "200 路径应当把 decode 后的值写入本地缓存")
        assertEquals("uid-NEW", cached.uuid)
        assertEquals("freshly-decoded", cached.cacheData)
    }

    @Test
    fun response_missingCacheKeyHeader_fallsThroughToDelegate() {
        helper.localCacheEnabled = true
        val delegate = RecordingDecoder("decoded")
        val interceptor = FeignCacheResponseInterceptor(delegate, helper)

        // 服务端发回了 cache-uid/cache-status 但客户端的 request 头没有 cache-key（不应该发生但 defensive）
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

        assertEquals("decoded", result, "缺 cache-key 时仍能正常 decode")
        assertEquals(1, delegate.callCount)
    }

    // endregion

    // region helpers

    private fun newRequestInterceptor(appName: String): FeignCacheRequestInterceptor {
        return FeignCacheRequestInterceptor(helper, appName)
    }

    private fun newRequestTemplate(method: String, url: String, body: ByteArray? = null): RequestTemplate {
        // 生产环境 Feign 在调用 interceptor 之前会自己 `resolve` 一遍；这里手工模拟。
        // [RequestTemplate.resolve] 返回**新对象**，原对象的 resolved 标志不变——务必接住返回值。
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

    /** 把每次 decode 调用记录次数，供测试断言"是否被调用"。 */
    private class RecordingDecoder(private val toReturn: Any?) : Decoder {
        var callCount: Int = 0
            private set
        override fun decode(response: Response, type: Type): Any? {
            callCount++
            return toReturn
        }
    }

    /** 用内存 Map 模拟 [ClientCacheHelper]——跳过 IKeyValueCacheManager 装配。 */
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
