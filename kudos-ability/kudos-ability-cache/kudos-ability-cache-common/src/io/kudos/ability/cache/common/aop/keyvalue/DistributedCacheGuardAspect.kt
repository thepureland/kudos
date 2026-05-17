package io.kudos.ability.cache.common.aop.keyvalue

import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ability.cache.common.support.SpelExpressionCache
import io.kudos.ability.cache.common.support.TenantCacheKeyGenerator
import io.kudos.base.logger.LogFactory
import io.kudos.context.kit.SpringKit
import io.kudos.context.lock.LockTool
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.context.expression.MethodBasedEvaluationContext
import org.springframework.core.annotation.Order

/**
 * 优先于cacheable执行
 */
@Aspect
@Lazy(false)
@Order(-999)
class DistributedCacheGuardAspect {

    private val nameDiscoverer = SpelExpressionCache.parameterNameDiscoverer

    @Pointcut("@annotation(io.kudos.ability.cache.common.aop.keyvalue.DistributedCacheGuard)")
    fun cut() {
    }

    @Around("cut()")
    fun around(pjp: ProceedingJoinPoint): Any? {
        val cachePair = getCachePair(pjp)
        val cacheName = cachePair.first
        val cacheKey = cachePair.second
        val lockKey = "lock:$cacheName:$cacheKey"

        // 1. 先无锁查一次缓存，命中就走人。
        var cached = KeyValueCacheKit.getValue(cacheName, cacheKey)
        if (cached != null) return cached

        // 2. 缺失，竞争分布式锁。改用租约式 tryLock：
        //    旧实现走 lockProvider.lock(key)，Redisson 路径下是 `RLock.lock()` 无 TTL 的阻塞调用 →
        //    进程在加载方法中崩溃就把同一 key 卡死，需要人工到 Redis 删 key 才能恢复。
        //    现在用 tryLock(key, leaseSec)：宕机后租约自动过期，最坏多读一次源；不再死锁。
        val gotLock = lockProvider.tryLock(lockKey, lockLeaseSeconds)
        if (!gotLock) {
            // 抢锁失败说明另一个线程/节点正在加载该 key；不阻塞等待，短退避后再查一次缓存：
            //   - 命中：复用别人加载的结果，省一次 proceed。
            //   - 仍 miss：放行 proceed，最坏退化为多读一次源（"防穿透"层面的最差容忍）。
            // 旧实现的"无限期阻塞等锁"语义不在这里保留：那种语义放大了下游故障半径，且依赖锁有 TTL。
            try {
                Thread.sleep(lockBackoffMillis)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                log.debug("等待分布式缓存锁时被中断 lockKey={0}", lockKey)
            }
            cached = KeyValueCacheKit.getValue(cacheName, cacheKey)
            if (cached != null) return cached
            return pjp.proceed()
        }
        return try {
            // 锁内双重检查
            cached = KeyValueCacheKit.getValue(cacheName, cacheKey)
            if (cached != null) cached else pjp.proceed()
        } finally {
            try {
                lockProvider.unLock(lockKey)
            } catch (e: Exception) {
                // 释放本身不应让业务路径报错；最坏情况靠租约过期兜底。
                log.warn("释放分布式缓存锁失败（将依赖租约过期）lockKey={0}, err={1}", lockKey, e.message)
            }
        }
    }

    private fun getCachePair(pjp: ProceedingJoinPoint): Pair<String, Any> {
        // 1. 获取目标方法和参数
        val signature = pjp.signature as MethodSignature
        val method = signature.method
        // 2. 获取方法上的 @Cacheable 注解
        val cacheable = method.getAnnotation(Cacheable::class.java)
        val tenantCacheable = method.getAnnotation(TenantCacheable::class.java)
        check(!(cacheable == null && tenantCacheable == null)) { "@DistributedCacheGuard 只能和 @Cacheable或@TenantCacheable 一起用！" }
        if (cacheable != null) {
            // 3. 解析 cacheName 和 key
            val cacheName: String =
                (if (cacheable.value.isNotEmpty()) {
                    cacheable.value[0]
                } else (
                        if (cacheable.cacheNames.isNotEmpty()) {
                            cacheable.cacheNames[0]
                        } else {
                            null
                        }
                ))!!

            val keySpel = cacheable.key
            require(!(keySpel.isEmpty())) { "@Cacheable.key 必须指定" }
            // 4. 解析 SpEL key
            val context = MethodBasedEvaluationContext(
                null, method, pjp.args, nameDiscoverer
            )
            val cacheKey = SpelExpressionCache.get(keySpel).getValue(context, String::class.java)
            return Pair<String, Any>(cacheName, cacheKey!!)
        } else {
            // 3. 解析 cacheName 和 key
            val cacheName =
                if (tenantCacheable.value.isNotEmpty()) {
                    tenantCacheable.value[0]
                } else (if (tenantCacheable.cacheNames.isNotEmpty()) {
                    tenantCacheable.cacheNames[0]
                } else null)
            val cacheKey = SpringKit.getBean<TenantCacheKeyGenerator>()
                .generalNormalKey(pjp.target, method, tenantCacheable.suffix, *pjp.args)
            return Pair(cacheName!!, cacheKey)
        }
    }

    companion object {
        private val lockProvider = LockTool.lockProvider
        private val log = LogFactory.getLog(DistributedCacheGuardAspect::class)

        /**
         * 租约时长（秒）：足以容纳一次"回源加载并写缓存"的耗时；上限决定宕机后死锁的最坏窗口。
         * 30s 是经验值，业务侧若有显著慢加载请自行调整或考虑分桶。
         */
        private const val lockLeaseSeconds = 30

        /**
         * 抢锁失败后的退避时长（毫秒），用于一次"短等待 + 再查缓存"。
         * 不要做循环重试：那等价于把阻塞等锁的旧语义搬回来，反而扩大了下游故障半径。
         */
        private const val lockBackoffMillis = 200L
    }
}
