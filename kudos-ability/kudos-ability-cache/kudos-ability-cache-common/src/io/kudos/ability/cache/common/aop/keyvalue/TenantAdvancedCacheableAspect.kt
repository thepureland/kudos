package io.kudos.ability.cache.common.aop.keyvalue

import io.kudos.ability.cache.common.aop.keyvalue.process.IRemoteCacheProcessor
import io.kudos.context.core.KudosContextHolder
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * 租户高级缓存切面（hash 结构 + TTL）。
 *
 * 与普通 `@Cacheable` 的差异：用 [IRemoteCacheProcessor] 走 Redis hash 结构，
 * 一个 (cacheKey, dataKey) 二元组对应一个缓存项，且支持单项 TTL。
 * `cacheKey` 自动追加租户 id 后缀实现多租户隔离。
 *
 * `remoteCacheProcess` 缺失时静默 proceed（注入设为 required=false），让上层应用没引入远程缓存实现时仍能启动。
 *
 * @author K
 * @since 1.0.0
 */
@Aspect
@Lazy(false)
@Component
@Order(0) // 单条 Cacheable 类切面，与其他 Cacheable 注解互斥，相对顺序不关键；显式标 0 避免与 Spring 默认 LOWEST_PRECEDENCE 混。
class TenantAdvancedCacheableAspect {

    /** 远程缓存处理器；为 null 时切面降级为无操作 */
    @Autowired(required = false)
    private val remoteCacheProcess: IRemoteCacheProcessor? = null

    /**
     * 定义切入点
     *
     * @author K
     * @since 1.0.0
     */
    @Pointcut("@annotation(io.kudos.ability.cache.common.aop.keyvalue.TenantAdvancedCacheable)")
    fun cut() {
    }

    /**
     * 主流程：从 hash 结构取 (cacheKey, dataKey) 命中即返回；miss 时 proceed 后写回（带 TTL）。
     *
     * `cacheKey` 拼上当前租户 id 实现租户隔离；空结果不写回，避免缓存"无意义的 null"占位。
     *
     * @param joinPoint 切入点
     * @return 命中或方法执行结果
     * @author K
     * @since 1.0.0
     */
    @Around("cut()")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        if (remoteCacheProcess == null) {
            return joinPoint.proceed()
        }
        val signature = joinPoint.signature as MethodSignature
        val cacheable = signature.method.getAnnotation(TenantAdvancedCacheable::class.java)
        val tenantId = KudosContextHolder.get().tenantId
        val cacheKey = "${cacheable.cacheKey}::$tenantId"
        val dataKey = cacheable.dataKey
        val timeOut = cacheable.timeOut
        remoteCacheProcess.getCacheData(cacheKey, dataKey)?.let { return it }
        //加载数据，并存入到hash里
        return joinPoint.proceed()?.also {
            remoteCacheProcess.writeCacheData(cacheKey, dataKey, it, timeOut)
        }
    }
}
