package io.kudos.ability.cache.common.aop.keyvalue

import io.kudos.ability.cache.common.aop.keyvalue.process.IRemoteCacheProcessor
import io.kudos.context.core.KudosContextHolder
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * [TenantAdvancedCacheEvict] 的切面：方法返回后按 `cacheKey::tenantId` 拼装远端 key
 * 调 [IRemoteCacheProcessor.clearCache] 清缓存。
 *
 * 没注入 `remoteCacheProcess` 实现时（`required=false`）直接 no-op——支持单元测试 / 本地模式跑通流程。
 *
 * @author K
 * @since 1.0.0
 */
@Aspect
@Lazy(false)
@Component
@Order(-100) // 与 [TenantCachingAspect] 同语义（写后 evict），保持同序，避免 evict 路径之间相对顺序不可预期。
class TenantAdvancedCacheEvictAspect {

    @Autowired(required = false)
    private val remoteCacheProcess: IRemoteCacheProcessor? = null

    /**
     * 定义切入点
     *
     * @author K
     * @since 1.0.0
     */
    @Pointcut("@annotation(io.kudos.ability.cache.common.aop.keyvalue.TenantAdvancedCacheEvict)")
    fun cut() {
    }

    @After("cut()")
    @Throws(Throwable::class)
    fun afterAdvice(joinPoint: JoinPoint) {
        if (remoteCacheProcess == null) {
            return
        }
        val signature = joinPoint.signature as MethodSignature
        val cacheable = signature.method.getAnnotation(TenantAdvancedCacheEvict::class.java)
        val tenantId = KudosContextHolder.get().tenantId
        val cacheKey = "${cacheable.cacheKey}::$tenantId"
        remoteCacheProcess.clearCache(cacheKey, cacheable.dataKey, cacheable.allEntries)
    }
}
