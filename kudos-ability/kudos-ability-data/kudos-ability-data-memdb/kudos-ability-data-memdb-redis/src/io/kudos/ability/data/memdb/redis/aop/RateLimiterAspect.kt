package io.kudos.ability.data.memdb.redis.aop

import io.kudos.ability.data.memdb.redis.SoulRedisTemplate
import io.kudos.base.enums.impl.ErrorStatusEnum
import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import io.kudos.context.core.KudosContextHolder
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.scripting.support.ResourceScriptSource


@org.springframework.stereotype.Component
@Aspect
@org.springframework.context.annotation.Lazy
class RateLimiterAspect {

    @Autowired
    @org.springframework.context.annotation.Lazy
    private lateinit var soulRedisTemplate: SoulRedisTemplate

    @Before("@annotation(rateLimiter)")
    fun doBefore(point: JoinPoint, rateLimiter: RateLimiter) {
        val time: Int = rateLimiter.time
        val count: Int = rateLimiter.count

        val combineKey = getCombineKey(rateLimiter, point)
        val keys = mutableListOf<Any?>(combineKey)
        try {
            val number = soulRedisTemplate.getDefaultRedisTemplate().execute(
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
            log.debug("限制请求'${count}',当前请求'${number}',缓存key'${combineKey}'")
        } catch (e: ServiceException) {
            throw e
        } catch (e: java.lang.Exception) {
            val className: String? = point.target.javaClass.getName()
            val signature: MethodSignature = point.signature as MethodSignature
            val methodName: String? = signature.method.name
            log.error("服务器限流异常。${className}.${methodName}")
            throw java.lang.RuntimeException("服务器限流异常，请稍候再试.", e)
        }
    }

    fun getCombineKey(rateLimiter: RateLimiter, point: JoinPoint): String {
        val stringBuffer = java.lang.StringBuffer("rate.limit")
        if (rateLimiter.limitType === LimitType.IP) {
            stringBuffer.append(KudosContextHolder.get().clientInfo!!.ip).append("-")
        } else if (rateLimiter.limitType === LimitType.USER) {
            stringBuffer.append(KudosContextHolder.get().user!!.id).append("-")
        }
        val signature: MethodSignature = point.signature as MethodSignature
        val method: java.lang.reflect.Method = signature.method
        val targetClass = method.declaringClass
        stringBuffer.append(targetClass.getName()).append("-").append(method.name)
        return stringBuffer.toString()
    }

    companion object {
        private val log = LogFactory.getLog(RateLimiterAspect::class.java)
        private val luaScriptStr = buildLuaScript()
        private val argSerializer = argSerializer()
        private val resultSerializer: RedisSerializer<Long?> = resultSerializer()

        fun buildLuaScript(): RedisScript<Long?> {
            val classPathResource = org.springframework.core.io.ClassPathResource("limit.lua")
            try {
                val redisScript: DefaultRedisScript<Long?> = DefaultRedisScript()
                redisScript.resultType = Long::class.java as Class<Long?>?
                classPathResource.inputStream //探测资源是否存在
                redisScript.setScriptSource(ResourceScriptSource(classPathResource))
                return redisScript
            } catch (e: java.lang.Exception) {
                throw java.lang.RuntimeException("未找到限流lua", e)
            }
        }

        /**
         * 參數序列化
         */
        private fun argSerializer() = StringRedisSerializer()

        /**
         * 結果序列化
         */
        private fun resultSerializer(): RedisSerializer<Long?> {
            return object : RedisSerializer<Long> {
                override fun serialize(aLong: Long): ByteArray? {
                    return aLong.toString().toByteArray()
                }

                override fun deserialize(bytes: ByteArray): Long {
                    return String(bytes).toLong()
                }
            } as RedisSerializer<Long?>
        }
    }
}
