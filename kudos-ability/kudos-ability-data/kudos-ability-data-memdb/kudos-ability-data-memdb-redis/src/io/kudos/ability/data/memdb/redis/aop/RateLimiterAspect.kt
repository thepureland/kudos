package io.kudos.ability.data.memdb.redis.aop

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.base.enums.impl.ErrorStatusEnum
import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import io.kudos.context.core.KudosContextHolder
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.scripting.support.ResourceScriptSource
import org.springframework.stereotype.Component


/**
 * Aspect implementing the [RateLimiter] annotation. Invokes the `limit.lua` script once before the annotated method executes:
 *  - The script performs `INCR` on `combineKey`; on the first hit it sets `EXPIRE time`.
 *  - A return value of 0 indicates the threshold has been exceeded; this aspect translates it into `ServiceException(SC_REQUEST_FREQUENTLY)`.
 *
 * Note: All rate-limiting goes through [RedisTemplates.defaultRedisTemplate]; separate rate-limiting per Redis instance is not currently supported.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
@Aspect
@Lazy
class RateLimiterAspect {

    @Autowired
    @Lazy
    private lateinit var redisTemplates: RedisTemplates

    /**
     * Invokes the lua counter script once before entering the method; throws `ServiceException(SC_REQUEST_FREQUENTLY)` when the threshold is exceeded.
     * Non-business exceptions (Redis failures, etc.) are uniformly wrapped as `RuntimeException` and propagated to avoid swallowing the problem.
     */
    @Before("@annotation(rateLimiter)")
    fun doBefore(point: JoinPoint, rateLimiter: RateLimiter) {
        val time: Int = rateLimiter.time
        val count: Int = rateLimiter.count

        val combineKey = getCombineKey(rateLimiter, point)
        val keys = mutableListOf<Any>(combineKey)
        try {
            val number = redisTemplates.defaultRedisTemplate.execute(
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
            log.error("Server rate-limit exception. ${className}.${methodName}")
            throw RuntimeException("Server rate-limit exception; please try again later.", e)
        }
    }

    /**
     * Assembles the Redis rate-limit counter key. The structure is `rate.limit[<id>-]<class>-<method>`, where `<id>`
     * is only present for [LimitType.IP] / [LimitType.USER]; for [LimitType.DEFAULT] the key is shared across the entire process.
     */
    fun getCombineKey(rateLimiter: RateLimiter, point: JoinPoint): String {
        val signature = point.signature as MethodSignature
        val method = signature.method
        return buildString {
            append("rate.limit")
            when (rateLimiter.limitType) {
                LimitType.IP -> append(
                    requireNotNull(KudosContextHolder.get().clientInfo?.ip) { "Rate-limit (IP) requires clientInfo.ip" }
                ).append("-")
                LimitType.USER -> append(
                    requireNotNull(KudosContextHolder.get().user?.id) { "Rate-limit (USER) requires user.id" }
                ).append("-")
                LimitType.DEFAULT -> Unit
            }
            append(method.declaringClass.name).append("-").append(method.name)
        }
    }

    companion object {
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
        private fun resultSerializer(): RedisSerializer<Long> {
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
