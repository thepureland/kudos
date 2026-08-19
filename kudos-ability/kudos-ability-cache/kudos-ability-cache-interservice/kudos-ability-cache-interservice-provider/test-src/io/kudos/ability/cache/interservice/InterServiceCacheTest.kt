package io.kudos.ability.cache.interservice

import io.kudos.ability.cache.interservice.client.IMockProxy
import io.kudos.ability.cache.interservice.provider.MockMsApplication
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.SpringApplication
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.service.registry.ImportHttpServices
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame

/**
 * Inter-service cache test cases.
 *
 * **These assertions changed shape with the move off OpenFeign.** They used to assert *identity*
 * (`obj1 === obj2`) for a `@ClientCacheable` endpoint, because the Feign-era decoder handed back the
 * previously decoded object itself. `HttpCacheNegotiationInterceptor` sits below deserialization and
 * caches the raw response bytes instead, so a cache hit now yields an *equal* object, not the same
 * one — see that class for why the seam moved.
 *
 * That old `===` was in fact the visible symptom of a hazard: every caller of a cached endpoint shared
 * one mutable instance, so a caller that mutated its result corrupted the cache for everyone. The
 * `assertNotSame` below pins that this no longer happens.
 *
 * Note what these cases can and cannot prove. Structural equality alone does not prove the cache
 * engaged (a plain refetch is equal too) — the precise proof of the 304 path is
 * `HttpCacheNegotiationInterceptorTest` in the client module, which drives the protocol directly.
 * What these cases do cover is that negotiation is **transparent**: enabling it does not change what
 * callers observe.
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@ActiveProfiles("client")
@ImportHttpServices(group = "interservicetest", types = [IMockProxy::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class InterServiceCacheTest {

    @Resource
    private lateinit var proxy: IMockProxy

    @BeforeAll
    fun setup() {
        println("########## Starting the mock microservice...")
        SpringApplication.run(MockMsApplication::class.java)
        println("########## Mock microservice started successfully.")
    }

    @Test
    fun `cached endpoint returns equal but not shared results`() {
        val obj1 = proxy.same()
        val obj2 = proxy.same()
        assertEquals(obj1, obj2, "a @ClientCacheable endpoint with stable content must read back the same value")
        assertNotSame(obj1, obj2, "callers must not share one mutable instance out of the cache")
    }

    @Test
    fun `endpoint without ClientCacheable is untouched`() {
        val obj1 = proxy.different1()
        val obj2 = proxy.different1()
        assertNotSame(obj1, obj2)
    }

    @Test
    fun `cached endpoint with changing content returns fresh data`() {
        val obj1 = proxy.different2()
        val obj2 = proxy.different2()
        assertNotSame(obj1, obj2)
        // Content really changes per call, so a stale 304 would show up here as equality.
        assertNotEquals(obj1, obj2)
    }

}
