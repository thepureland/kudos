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


@Component
@Aspect
@Lazy
@Order(-100)
class TenantDsChangeAspect {

    private val log = LogFactory.getLog(this)

    /**
     * 定义切入点
     *
     * @author K
     * @since 1.0.0
     */
    @Pointcut("@annotation(io.kudos.ability.data.rdb.jdbc.aop.TenantDsChange)")
    private fun cut() {
    }

    @Around("cut()")
    @Throws(Throwable::class)
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val dsChange = signature.method.getAnnotation(TenantDsChange::class.java)
        if (dsChange.value.isNotBlank()) {
            if (dsChange.value.startsWith("_context")) {
                DbContext.get().forcedDs = dsChange.value
            } else {
                DbContext.get().forcedDs = "_context::" + dsChange.value
            }
            DbContext.get().readonly = dsChange.readonly
            log.debug("强制指定数据源:ds=" + DbContext.get().forcedDs + ",readonly=" + dsChange.readonly)
        }
        var result: Any?
        try {
            result = joinPoint.proceed()
        } finally {
            DbContext.set(null)
        }
        return result
    }

}
