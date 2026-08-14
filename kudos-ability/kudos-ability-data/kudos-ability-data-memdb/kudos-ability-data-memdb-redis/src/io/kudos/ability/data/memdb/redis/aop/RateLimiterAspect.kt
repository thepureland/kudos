package io.kudos.ability.data.memdb.redis.aop

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.data.memdb.redis.consts.CacheKey
import io.kudos.base.enums.impl.ErrorStatusEnum
import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import io.kudos.context.core.KudosContextHolder
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.scripting.support.ResourceScriptSource


/**
 * Aspect implementing the [RateLimiter] annotation. Invokes the `limit.lua` script once before the annotated method executes:
 *  - The script counts in a fixed window: the first allowed request creates the counter with `EXPIRE time`;
 *    later requests only `INCR` it, so the window never slides forward with traffic.
 *  - A return value of 0 indicates the threshold has been exceeded; this aspect translates it into `ServiceException(SC_REQUEST_FREQUENTLY)`.
 *
 * Registered as a bean by `RedisAutoConfiguration` (not component scanning) with plain constructor injection —
 * a `@Lazy` field injection would require CGLIB-subclassing [RedisTemplates] and is deliberately avoided.
 *
 * The counter lives in [RateLimiter.redisName]'s instance (default instance when unset), so a dedicated
 * rate-limit Redis can be configured without replacing this aspect.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Aspect
class RateLimiterAspect(private val redisTemplates: RedisTemplates) {

    /**
     * Invokes the lua counter script once before entering the method; throws `ServiceException(SC_REQUEST_FREQUENTLY)` when the threshold is exceeded.
     *
     * Infrastructure failures (Redis down, timeout, ...) are wrapped as `RuntimeException` and propagated —
     * i.e. fail-closed — unless the annotation opts into [RateLimiter.failOpen], in which case the call is
     * allowed through with a WARN. Misconfiguration (an unknown [RateLimiter.redisName]) always propagates:
     * it is a deployment error, not a runtime outage, and fail-open must not mask it.
     */
    @Before("@annotation(rateLimiter)")
    fun doBefore(point: JoinPoint, rateLimiter: RateLimiter) {
        val time: Int = rateLimiter.time
        val count: Int = rateLimiter.count

        // resolved before the try block so an unknown redisName is never swallowed by failOpen
        val redisTemplate = resolveRedisTemplate(rateLimiter)
        val combineKey = getCombineKey(rateLimiter, point)
        val keys = mutableListOf<Any>(combineKey)
        try {
            val number = redisTemplate.execute(
                luaScriptStr,
                argSerializer,
                resultSerializer,
                keys,
                count.toString(),
                time.toString()
            )
            if (number == 0L) {
                throw ServiceException(ErrorStatusEnum.SC_REQUEST_FREQUENTLY)
            }
            log.debug("Request limit '${count}', current requests '${number}', cache key '${combineKey}'")
        } catch (e: ServiceException) {
            throw e
        } catch (e: Exception) {
            val className: String? = point.target.javaClass.getName()
            val signature: MethodSignature = point.signature as MethodSignature
            val methodName: String? = signature.method.name
            if (rateLimiter.failOpen) {
                log.warn(
                    "Rate-limit backend unavailable; letting the call through (failOpen). {0}.{1}, error={2}",
                    className, methodName, e.toString()
                )
                return
            }
            log.error("Server rate-limit exception. ${className}.${methodName}")
            throw RuntimeException("Server rate-limit exception; please try again later.", e)
        }
    }

    /**
     * Picks the RedisTemplate holding the counter: [RateLimiter.redisName] when set, the default instance
     * otherwise. An unknown name raises immediately, listing the configured instance names.
     */
    private fun resolveRedisTemplate(rateLimiter: RateLimiter) =
        if (rateLimiter.redisName.isBlank()) redisTemplates.defaultRedisTemplate
        else redisTemplates.getRequiredRedisTemplate(rateLimiter.redisName)

    /**
     * Assembles the Redis rate-limit counter key: `rate.limit:[<id>:]<class>-<method>`, joined with the
     * [CacheKey] separator convention. `<id>` is only present for [LimitType.IP] / [LimitType.USER];
     * for [LimitType.DEFAULT] the key is shared across the entire process.
     */
    fun getCombineKey(rateLimiter: RateLimiter, point: JoinPoint): String {
        val signature = point.signature as MethodSignature
        val method = signature.method
        val methodPart = "${method.declaringClass.name}-${method.name}"
        return when (rateLimiter.limitType) {
            LimitType.IP -> CacheKey.getCacheKey(
                RATE_LIMIT_KEY_PREFIX,
                requireNotNull(KudosContextHolder.get().clientInfo?.ip) { "Rate-limit (IP) requires clientInfo.ip" },
                methodPart
            )
            LimitType.USER -> CacheKey.getCacheKey(
                RATE_LIMIT_KEY_PREFIX,
                requireNotNull(KudosContextHolder.get().user?.id) { "Rate-limit (USER) requires user.id" }.toString(),
                methodPart
            )
            LimitType.DEFAULT -> CacheKey.getCacheKey(RATE_LIMIT_KEY_PREFIX, methodPart)
        }
    }

    companion object {
        /** First segment of every rate-limit counter key. */
        const val RATE_LIMIT_KEY_PREFIX = "rate.limit"

        private val log = LogFactory.getLog(RateLimiterAspect::class.java)
        private val luaScriptStr = buildLuaScript()
        private val argSerializer = argSerializer()
        private val resultSerializer: RedisSerializer<Long> = resultSerializer()

        /**
         * Loads `limit.lua` from the classpath and constructs a `RedisScript<Long>`.
         * Throws immediately on load failure (failing at startup is preferable to discovering the missing lua at runtime).
         */
        fun buildLuaScript(): RedisScript<Long> {
            val classPathResource = org.springframework.core.io.ClassPathResource("limit.lua")
            try {
                val redisScript: DefaultRedisScript<Long> = DefaultRedisScript()
                redisScript.resultType = Long::class.java
                classPathResource.inputStream.use { /* Only probe whether the resource exists */ }
                redisScript.setScriptSource(ResourceScriptSource(classPathResource))
                return redisScript
            } catch (e: Exception) {
                throw RuntimeException("Rate-limit lua not found", e)
            }
        }

        /** Argument serializer for the lua script (both key and arg are strings). */
        private fun argSerializer() = StringRedisSerializer()

        /**
         * Return-value serializer for the lua script: Long ↔ ASCII string.
         * Note: [org.springframework.data.redis.serializer.GenericToStringSerializer] cannot be used,
         * because the result type of RedisScript is controlled independently by this serializer and must be decoded precisely as Long.
         */
        internal fun resultSerializer(): RedisSerializer<Long> {
            return object : RedisSerializer<Long> {
                override fun serialize(aLong: Long?): ByteArray {
                    return aLong.toString().toByteArray()
                }

                override fun deserialize(bytes: ByteArray?): Long? {
                    if (bytes == null) return null
                    return String(bytes).toLong()
                }
            }
        }
    }

}
