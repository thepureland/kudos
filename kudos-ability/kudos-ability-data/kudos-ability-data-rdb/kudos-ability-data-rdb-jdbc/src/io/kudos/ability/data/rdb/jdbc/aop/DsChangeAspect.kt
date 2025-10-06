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
class DsChangeAspect {

    private val log = LogFactory.getLog(this)

    /**
     * 定义切入点
     *
     * @author K
     * @since 1.0.0
     */
    @Pointcut("@annotation(io.kudos.ability.data.rdb.jdbc.aop.DsChange)")
    private fun cut() {
    }

    @Around("cut()")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val dsChange: DsChange = signature.method.getAnnotation(DsChange::class.java)
        if (dsChange.value.isNotBlank()) {
            DbContext.get().forcedDs = dsChange.value
        }
        DbContext.get().readonly = dsChange.readonly
        log.debug("强制指定数据源:ds=" + DbContext.get().forcedDs + ",readonly=" + dsChange.readonly)
        var result: Any?
        try {
            result = joinPoint.proceed()
        } finally {
            DbContext.set(null)
        }
        return result
    }

}
