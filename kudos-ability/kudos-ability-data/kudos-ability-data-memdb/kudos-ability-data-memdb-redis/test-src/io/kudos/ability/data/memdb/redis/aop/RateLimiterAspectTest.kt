package io.kudos.ability.data.memdb.redis.aop

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.base.error.ServiceException
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.context.core.ClientInfo
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import jakarta.annotation.Resource
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.mockito.Mockito
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [RateLimiterAspect] (based on RedisTestContainer; the advice is invoked directly with a
 * mocked [JoinPoint] instead of going through Spring AOP).
 *
 * Why no AspectJ auto-proxy: the aspect injects [RedisTemplates] with `@Autowired @Lazy`, and a lazy
 * injection proxy requires CGLIB-subclassing the *final* class [RedisTemplates], which fails with
 * `AopConfigException: Cannot subclass final class`. That is a suspected main-code bug (recorded, not
 * worked around in main code); these tests bypass it by instantiating the aspect and setting the field
 * reflectively, which still exercises every line of the aspect against a real Redis.
 *
 * Covers: allowing calls up to the threshold then throwing ServiceException, the three
 * [LimitType] key dimensions (DEFAULT / USER / IP) including their missing-context failure
 * paths, wrapping of non-business Redis exceptions into RuntimeException, the lua script
 * loader and the Long result serializer.
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
internal class RateLimiterAspectTest {

    @Resource
    private lateinit var redisTemplates: RedisTemplates

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry?) {
            RedisTestContainer.startIfNeeded(registry)
        }
    }

    /** Target class providing the advised method signatures; one method per limit dimension. */
    open class RateLimitedService {
        open fun limitedDefault(): String = "ok"
        open fun limitedByUser(): String = "ok"
        open fun limitedByIp(): String = "ok"
    }

    /** Builds an aspect instance with its private (lazy-injected in production) RedisTemplates field set reflectively. */
    private fun aspect(templates: RedisTemplates = redisTemplates): RateLimiterAspect {
        val a = RateLimiterAspect()
        val field = RateLimiterAspect::class.java.getDeclaredField("redisTemplates")
        field.isAccessible = true
        field.set(a, templates)
        return a
    }

    /** Mocks a JoinPoint whose signature points at the given [RateLimitedService] method. */
    private fun joinPoint(methodName: String): JoinPoint {
        val method = RateLimitedService::class.java.getMethod(methodName)
        val signature = Mockito.mock(MethodSignature::class.java)
        Mockito.`when`(signature.method).thenReturn(method)
        val point = Mockito.mock(JoinPoint::class.java)
        Mockito.`when`(point.signature).thenReturn(signature)
        Mockito.`when`(point.target).thenReturn(RateLimitedService())
        return point
    }

    /** Remove all rate-limit counters so reruns within the lua EXPIRE window stay deterministic. */
    @BeforeTest
    fun cleanCounters() {
        val template = redisTemplates.defaultRedisTemplate
        val keys = template.keys("rate.limit*")
        if (!keys.isNullOrEmpty()) {
            template.delete(keys)
        }
    }

    /** Reset the thread-bound context so USER/IP settings never leak into other tests. */
    @AfterTest
    fun resetContext() {
        KudosContextHolder.set(KudosContext())
    }

    @Test
    fun defaultLimit_allowsUpToCount_thenRejects() {
        val aspect = aspect()
        val point = joinPoint("limitedDefault")
        val limiter = RateLimiter(time = 60, count = 2)
        aspect.doBefore(point, limiter)
        aspect.doBefore(point, limiter)
        assertFailsWith<ServiceException> { aspect.doBefore(point, limiter) }
        // counter key is rate.limit<class>-<method> for the DEFAULT dimension
        val expectedKey = "rate.limit${RateLimitedService::class.java.name}-limitedDefault"
        assertTrue(redisTemplates.defaultRedisTemplate.hasKey(expectedKey))
    }

    @Test
    fun userLimit_countsPerUserId() {
        val aspect = aspect()
        val point = joinPoint("limitedByUser")
        val limiter = RateLimiter(time = 60, count = 1, limitType = LimitType.USER)
        KudosContextHolder.get().user = object : IIdEntity<String> {
            override val id: String = "user-001"
        }
        aspect.doBefore(point, limiter)
        assertFailsWith<ServiceException> { aspect.doBefore(point, limiter) }
        val expectedKey = "rate.limituser-001-${RateLimitedService::class.java.name}-limitedByUser"
        assertTrue(redisTemplates.defaultRedisTemplate.hasKey(expectedKey))

        // a different user has an independent counter
        KudosContextHolder.get().user = object : IIdEntity<String> {
            override val id: String = "user-002"
        }
        aspect.doBefore(point, limiter)
    }

    @Test
    fun userLimit_withoutUser_throwsIllegalArgument() {
        KudosContextHolder.set(KudosContext()) // no user
        val limiter = RateLimiter(time = 60, count = 1, limitType = LimitType.USER)
        val ex = assertFailsWith<IllegalArgumentException> {
            aspect().doBefore(joinPoint("limitedByUser"), limiter)
        }
        assertTrue(ex.message!!.contains("user.id"), ex.message)
    }

    @Test
    fun ipLimit_countsPerClientIp() {
        val aspect = aspect()
        val point = joinPoint("limitedByIp")
        val limiter = RateLimiter(time = 60, count = 1, limitType = LimitType.IP)
        KudosContextHolder.get().clientInfo = ClientInfo(ClientInfo.Builder()).apply { ip = "10.0.0.9" }
        aspect.doBefore(point, limiter)
        assertFailsWith<ServiceException> { aspect.doBefore(point, limiter) }
        val expectedKey = "rate.limit10.0.0.9-${RateLimitedService::class.java.name}-limitedByIp"
        assertTrue(redisTemplates.defaultRedisTemplate.hasKey(expectedKey))
    }

    @Test
    fun ipLimit_withoutClientIp_throwsIllegalArgument() {
        KudosContextHolder.set(KudosContext()) // no clientInfo
        val limiter = RateLimiter(time = 60, count = 1, limitType = LimitType.IP)
        val ex = assertFailsWith<IllegalArgumentException> {
            aspect().doBefore(joinPoint("limitedByIp"), limiter)
        }
        assertTrue(ex.message!!.contains("clientInfo.ip"), ex.message)
    }

    @Test
    fun redisFailure_isWrappedAsRuntimeException() {
        // a templates container whose default template has no connection factory fails with a non-business exception
        val broken = RedisTemplates(mutableMapOf(), RedisTemplate())
        val limiter = RateLimiter(time = 60, count = 2)
        val ex = assertFailsWith<RuntimeException> {
            aspect(broken).doBefore(joinPoint("limitedDefault"), limiter)
        }
        assertEquals("Server rate-limit exception; please try again later.", ex.message)
        // the wrapped cause is the raw technical failure, never the business ServiceException
        assertNotNull(ex.cause)
        assertTrue(ex.cause !is ServiceException, "cause: ${ex.cause}")
    }

    @Test
    fun getCombineKey_assemblesPerDimension() {
        val aspect = aspect()
        val className = RateLimitedService::class.java.name
        assertEquals(
            "rate.limit$className-limitedDefault",
            aspect.getCombineKey(RateLimiter(), joinPoint("limitedDefault"))
        )
        KudosContextHolder.get().user = object : IIdEntity<String> {
            override val id: String = "u42"
        }
        assertEquals(
            "rate.limitu42-$className-limitedByUser",
            aspect.getCombineKey(RateLimiter(limitType = LimitType.USER), joinPoint("limitedByUser"))
        )
        KudosContextHolder.get().clientInfo = ClientInfo(ClientInfo.Builder()).apply { ip = "127.0.0.2" }
        assertEquals(
            "rate.limit127.0.0.2-$className-limitedByIp",
            aspect.getCombineKey(RateLimiter(limitType = LimitType.IP), joinPoint("limitedByIp"))
        )
    }

    @Test
    fun buildLuaScript_loadsClasspathScript() {
        val script = RateLimiterAspect.buildLuaScript()
        assertEquals(Long::class.java, script.resultType)
        assertTrue(script.scriptAsString.contains("INCRBY"))
    }

    @Test
    fun buildLuaScript_missingResource_throwsAtOnce() {
        // force class (and its static lua script) initialization with the normal classloader first,
        // otherwise the broken loader below poisons the static initializer for every other test
        RateLimiterAspect.buildLuaScript()
        // ClassPathResource resolves via the thread context classloader; a bootstrap-only loader
        // cannot see limit.lua, triggering the fail-fast wrapping branch
        val original = Thread.currentThread().contextClassLoader
        try {
            Thread.currentThread().contextClassLoader = java.net.URLClassLoader(emptyArray(), null)
            val ex = assertFailsWith<RuntimeException> { RateLimiterAspect.buildLuaScript() }
            assertEquals("Rate-limit lua not found", ex.message)
        } finally {
            Thread.currentThread().contextClassLoader = original
        }
    }

    @Test
    fun resultSerializer_longAsciiRoundTrip() {
        val serializer = RateLimiterAspect.resultSerializer()
        assertContentEquals("12".toByteArray(), serializer.serialize(12L))
        assertContentEquals("null".toByteArray(), serializer.serialize(null))
        assertEquals(34L, serializer.deserialize("34".toByteArray()))
        assertEquals(-5L, serializer.deserialize("-5".toByteArray()))
        assertNull(serializer.deserialize(null))
        assertFailsWith<NumberFormatException> { serializer.deserialize("abc".toByteArray()) }
    }
}
