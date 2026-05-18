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
 * [RateLimiter] 注解的实现切面。在被注解方法执行前调用一次 `limit.lua` 脚本：
 *  - 脚本对 `combineKey` 做 `INCR`；首次设置 `EXPIRE time`
 *  - 返回 0 表示已超阈值，本切面将其翻译为 `ServiceException(SC_REQUEST_FREQUENTLY)`
 *
 * 注：所有限流都走 [RedisTemplates.defaultRedisTemplate]，目前不支持多 redis 实例分别限流。
 *
 * @author K
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
     * 进方法前调用一次 lua 计数脚本；超阈值抛 `ServiceException(SC_REQUEST_FREQUENTLY)`。
     * 非业务异常（Redis 故障等）统一包成 `RuntimeException` 透出，避免吞掉问题。
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
            log.debug("限制请求'${count}',当前请求'${number}',缓存key'${combineKey}'")
        } catch (e: ServiceException) {
            throw e
        } catch (e: Exception) {
            val className: String? = point.target.javaClass.getName()
            val signature: MethodSignature = point.signature as MethodSignature
            val methodName: String? = signature.method.name
            log.error("服务器限流异常。${className}.${methodName}")
            throw RuntimeException("服务器限流异常，请稍候再试.", e)
        }
    }

    /**
     * 拼接 Redis 限流计数 key。结构 `rate.limit[<id>-]<class>-<method>`，其中 `<id>` 仅在
     * [LimitType.IP] / [LimitType.USER] 时存在；[LimitType.DEFAULT] 整个进程共享 key。
     */
    fun getCombineKey(rateLimiter: RateLimiter, point: JoinPoint): String {
        val signature = point.signature as MethodSignature
        val method = signature.method
        return buildString {
            append("rate.limit")
            when (rateLimiter.limitType) {
                LimitType.IP -> append(
                    requireNotNull(KudosContextHolder.get().clientInfo?.ip) { "限流(IP)需要 clientInfo.ip" }
                ).append("-")
                LimitType.USER -> append(
                    requireNotNull(KudosContextHolder.get().user?.id) { "限流(USER)需要 user.id" }
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
         * 从 classpath 加载 `limit.lua` 并构造 `RedisScript<Long>`。
         * 加载失败立即抛错（启动期失败优于运行期才发现 lua 缺失）。
         */
        fun buildLuaScript(): RedisScript<Long> {
            val classPathResource = org.springframework.core.io.ClassPathResource("limit.lua")
            try {
                val redisScript: DefaultRedisScript<Long> = DefaultRedisScript()
                redisScript.resultType = Long::class.java
                classPathResource.inputStream.use { /* 仅探测资源存在 */ }
                redisScript.setScriptSource(ResourceScriptSource(classPathResource))
                return redisScript
            } catch (e: Exception) {
                throw RuntimeException("未找到限流lua", e)
            }
        }

        /** lua 脚本入参序列化器（key / arg 均为字符串）。 */
        private fun argSerializer() = StringRedisSerializer()

        /**
         * lua 脚本返回值序列化器：Long ↔ ASCII 字符串。
         * 注：不能用 [org.springframework.data.redis.serializer.GenericToStringSerializer]，
         * 因为 RedisScript 的结果类型由本序列化器单独控制，需精确解码为 Long。
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
