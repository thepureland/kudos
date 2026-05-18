package io.kudos.ability.data.rdb.jdbc.aop

import io.kudos.ability.data.rdb.jdbc.context.DbContext
import io.kudos.base.logger.LogFactory
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component


/**
 * 处理 [TenantDsChange] 注解的切面：把 `value` 包成 `_context::<value>` 写入
 * `DbParam.forcedDs`，告诉下游 [DynamicDataSourceAspect] 这是"按上下文动态解析"的意图，
 * 而不是直接当数据源 key 用。
 *
 * 切面 `@Order(-100)`、`@Lazy` 的原因同 [DsChangeAspect]。
 *
 * @author K
 * @since 1.0.0
 */
@Component
@Aspect
@Lazy
@Order(-100)
class TenantDsChangeAspect {

    private val log = LogFactory.getLog(this::class)

    /**
     * pointcut 定义：所有带 [TenantDsChange] 注解的方法。
     */
    @Pointcut("@annotation(io.kudos.ability.data.rdb.jdbc.aop.TenantDsChange)")
    private fun cut() {
    }

    /**
     * 环绕通知。把 `_context::<serviceCode>` 写入 forcedDs，proceed 业务方法，finally 清空。
     * value 若已带 `_context` 前缀就原样透传，避免重复嵌套。
     */
    @Around("cut()")
    @Throws(Throwable::class)
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val dsChange = signature.method.getAnnotation(TenantDsChange::class.java)
        if (dsChange.value.isNotBlank()) {
            DbContext.get().forcedDs =
                if (dsChange.value.startsWith("_context")) dsChange.value
                else "_context::${dsChange.value}"
            DbContext.get().readonly = dsChange.readonly
            log.debug("强制指定数据源:ds=${DbContext.get().forcedDs},readonly=${dsChange.readonly}")
        }
        return try {
            joinPoint.proceed()
        } finally {
            DbContext.set(null)
        }
    }

}
