package io.kudos.ability.cache.interservice.client.init

import feign.Request
import feign.RequestTemplate
import feign.Response
import io.kudos.ability.cache.common.core.keyvalue.IKeyValueCacheManager
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.interservice.client.core.ClientCacheHelper
import io.kudos.ability.cache.interservice.client.feign.FeignCacheResponseInterceptor
import org.springframework.beans.factory.ObjectProvider
import org.springframework.cache.Cache
import org.springframework.cache.concurrent.ConcurrentMapCache
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for the bean factory methods of [InterServiceCacheClientAutoConfiguration].
 *
 * Calls each `@Bean` method directly (no Spring container) and asserts:
 *  - default [InterServiceCacheClientProperties] are produced;
 *  - [ClientCacheHelper] respects presence/absence of the local cache manager (ObjectProvider.ifAvailable);
 *  - the request interceptor bean is constructed;
 *  - the global Feign decoder is the caching interceptor wrapping the
 *    Optional/ResponseEntity/Jackson decoding chain, and the chain actually decodes a body;
 *  - the component name is reported.
 *
 * @author K
 * @since 1.0.0
 */
internal class InterServiceCacheClientAutoConfigurationTest {

    private val config = InterServiceCacheClientAutoConfiguration()

    @Test
    fun interServiceCacheClientProperties_returnsDefaults() {
        val properties = config.interServiceCacheClientProperties()
        assertEquals(600, properties.ttlSeconds)
    }

    @Test
    fun clientCacheHelper_usesLocalCacheManagerWhenAvailable() {
        val helper = config.clientCacheHelper(
            InterServiceCacheClientProperties(),
            StaticObjectProvider(NoopCacheManager)
        )
        assertTrue(helper.hasLocalCache())
    }

    @Test
    fun clientCacheHelper_disablesLocalCacheWhenManagerAbsent() {
        val helper = config.clientCacheHelper(
            InterServiceCacheClientProperties(),
            StaticObjectProvider(null)
        )
        assertFalse(helper.hasLocalCache())
    }

    @Test
    fun feignCacheRequestInterceptor_isConstructed() {
        val interceptor = config.feignCacheRequestInterceptor(ClientCacheHelper(), applicationName = null)
        assertNotNull(interceptor)
    }

    @Test
    fun feignDecoder_buildsCachingDecoderChain() {
        val decoder = config.feignDecoder(ObjectMapper(), ClientCacheHelper())

        assertIs<FeignCacheResponseInterceptor>(decoder, "the outermost decoder must add caching capability")

        // Without a local cache the interceptor falls through to the Optional/ResponseEntity/Jackson chain,
        // which must decode the raw body for a String target type.
        val request = Request.create(
            Request.HttpMethod.GET,
            "http://localhost/x",
            emptyMap(),
            Request.Body.empty(),
            RequestTemplate(),
        )
        val response = Response.builder()
            .status(200)
            .headers(emptyMap())
            .request(request)
            .body("hello".toByteArray(StandardCharsets.UTF_8))
            .build()
        assertEquals("hello", decoder.decode(response, String::class.java))
    }

    @Test
    fun getComponentName_returnsModuleName() {
        assertEquals("kudos-ability-cache-interservice-client", config.getComponentName())
    }

    /** Minimal ObjectProvider stub returning a fixed instance (or nothing). */
    private class StaticObjectProvider(
        private val value: IKeyValueCacheManager<*>?
    ) : ObjectProvider<IKeyValueCacheManager<*>> {
        override fun getObject(): IKeyValueCacheManager<*> =
            value ?: throw IllegalStateException("no instance available")

        override fun getIfAvailable(): IKeyValueCacheManager<*>? = value
    }

    /** Cache manager stand-in: only its presence matters for these tests. */
    private object NoopCacheManager : IKeyValueCacheManager<ConcurrentMapCache> {
        override fun createCache(cacheConfig: CacheConfig) = ConcurrentMapCache("noop")
        override fun evictByPattern(cacheName: String, pattern: String) = Unit
        override fun existsKey(cacheName: String, key: Any): Boolean = false
        override fun multiGet(cacheName: String, keys: Collection<Any>): Map<Any, Any?> = emptyMap()
        override fun initCacheAfterSystemInit(cacheConfigMap: Map<String, CacheConfig>) = Unit
        override fun getCache(name: String): Cache? = null
        override fun getCacheNames(): MutableCollection<String> = mutableListOf()
    }
}
