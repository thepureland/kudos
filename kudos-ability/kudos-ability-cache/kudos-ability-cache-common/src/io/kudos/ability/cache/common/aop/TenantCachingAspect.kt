package io.kudos.ability.cache.common.aop

import io.kudos.ability.cache.common.core.MixCacheManager
import io.kudos.ability.cache.common.support.TenantCacheKeyGenerator
import io.kudos.context.kit.TransactionTool
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import java.lang.reflect.Method

@Aspect
@Lazy(false)
class TenantCachingAspect {
    @Autowired
    private val cacheManager: MixCacheManager? = null

    @Autowired
    private val tenantCacheKeyGenerator: TenantCacheKeyGenerator? = null

    @Pointcut("@annotation(io.kudos.ability.cache.common.aop.TenantCaching)")
    private fun cut() {
    }

    @Around("cut()")
    fun around(pjp: ProceedingJoinPoint): Any? {
        val method = (pjp.signature as MethodSignature).method
        val multicast = method.getAnnotation(TenantCaching::class.java)
        val target = pjp.target
        val args = pjp.args

        // 方法前先 evict
        for (ev in multicast.evicts) {
            if (ev.beforeInvocation) {
                doEvict(ev, target, method, args)
            }
        }
        val result = pjp.proceed()
        TransactionTool.doAfterTransactionCommit(Runnable {
            try {
                doAfterEvict(pjp, multicast, target, method, args)
            } catch (e: Throwable) {
                throw RuntimeException(e)
            }
        })
        return result
    }

    @Throws(Throwable::class)
    private fun doAfterEvict(
        pjp: ProceedingJoinPoint?, multicast: TenantCaching,
        target: Any, method: Method, args: Array<Any?>
    ) {
        // 方法后再 evict
        for (ev in multicast.evicts) {
            if (!ev.beforeInvocation) {
                doEvict(ev, target, method, args)
            }
        }
    }

    private fun doEvict(
        ev: TenantCacheEvict,
        target: Any, m: Method, args: Array<Any?>
    ) {
        for (cacheName in ev.cacheNames) {
            val cache = cacheManager!!.getCache(cacheName)
            if (cache != null) {
                val key = tenantCacheKeyGenerator!!.generalNormalKey(target, m, ev.suffix, *args)
                if (ev.allEntries) {
                    // 全清本租户命名空间（keyGenerator 已拼 prefix）
                    // 此处如果触发scan可能会有性能问题
                    cacheManager.evictByPattern(cacheName, key.toString())
                } else {
                    // 单条 key 删除
                    cache.evict(key)
                }
            }
        }
    }
}
