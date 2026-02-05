package io.kudos.ability.cache.common.aop.keyvalue

import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
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

/**
 * 租户缓存切面
 * 
 * 实现@TenantCaching注解的AOP切面，支持租户隔离的缓存清除。
 * 
 * 核心功能：
 * 1. 方法执行前清除：支持在方法执行前清除缓存（beforeInvocation=true）
 * 2. 方法执行后清除：支持在方法执行后清除缓存（事务提交后）
 * 3. 租户隔离：使用TenantCacheKeyGenerator生成包含租户信息的缓存key
 * 4. 模式清除：支持按模式清除整个租户命名空间的缓存
 * 
 * 工作流程：
 * 1. 拦截标注@TenantCaching的方法
 * 2. 执行前清除：遍历evicts配置，执行beforeInvocation=true的清除操作
 * 3. 执行原方法：调用pjp.proceed()执行原始方法
 * 4. 事务后清除：在事务提交后执行beforeInvocation=false的清除操作
 * 
 * 事务同步：
 * - 使用TransactionTool.doAfterTransactionCommit确保在事务提交后执行
 * - 避免在事务回滚时清除缓存，保证数据一致性
 * - 如果事务提交失败，不会执行清除操作
 * 
 * 清除策略：
 * - allEntries=true：按模式清除整个租户命名空间（可能影响性能）
 * - allEntries=false：清除单个key
 * 
 * 注意事项：
 * - 模式清除可能触发scan操作，性能开销较大
 * - 清除操作在事务提交后执行，确保数据已持久化
 * - 使用租户key生成器确保租户隔离
 */
@Aspect
@Lazy(false)
class TenantCachingAspect {
    @Autowired
    private val cacheManager: MixCacheManager? = null

    @Autowired
    private val tenantCacheKeyGenerator: TenantCacheKeyGenerator? = null

    @Pointcut("@annotation(io.kudos.ability.cache.common.aop.keyvalue.TenantCaching)")
    private fun cut() {
    }

    /**
     * 环绕通知，实现租户缓存清除逻辑
     * 
     * 在方法执行前后根据配置清除缓存，支持事务同步。
     * 
     * 工作流程：
     * 1. 获取方法注解：提取@TenantCaching注解配置
     * 2. 执行前清除：遍历evicts，执行beforeInvocation=true的清除
     * 3. 执行原方法：调用pjp.proceed()执行业务方法
     * 4. 注册事务后清除：使用TransactionTool注册事务提交后的清除任务
     * 5. 返回结果：返回业务方法的执行结果
     * 
     * 事务同步：
     * - 清除操作在事务提交后执行
     * - 如果事务回滚，不会执行清除操作
     * - 确保缓存与数据库数据一致
     * 
     * @param pjp 切点信息
     * @return 业务方法的返回值
     */
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

    /**
     * 在事务提交后执行清除操作
     * 
     * 遍历evicts配置，执行beforeInvocation=false的清除操作。
     * 
     * @param pjp 切点信息（未使用）
     * @param multicast @TenantCaching注解
     * @param target 目标对象
     * @param method 目标方法
     * @param args 方法参数
     */
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

    /**
     * 执行缓存清除操作
     * 
     * 根据配置清除指定缓存的key或整个租户命名空间。
     * 
     * 工作流程：
     * 1. 遍历缓存名称：对每个配置的缓存名称执行清除
     * 2. 获取缓存实例：从缓存管理器获取缓存实例
     * 3. 生成缓存key：使用TenantCacheKeyGenerator生成包含租户信息的key
     * 4. 执行清除：
     *    - allEntries=true：按模式清除整个租户命名空间
     *    - allEntries=false：清除单个key
     * 
     * 租户隔离：
     * - key生成器会自动添加租户前缀
     * - 模式清除只影响当前租户的缓存
     * - 确保多租户环境下的数据隔离
     * 
     * 性能考虑：
     * - 模式清除可能触发scan操作，性能开销较大
     * - 建议优先使用单key清除
     * - 模式清除适用于批量更新场景
     * 
     * @param ev 缓存清除配置
     * @param target 目标对象
     * @param m 目标方法
     * @param args 方法参数
     */
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
