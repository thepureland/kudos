package io.kudos.ability.data.rdb.jdbc.aop

import io.kudos.ability.data.rdb.jdbc.consts.DatasourceConst
import io.kudos.ability.data.rdb.jdbc.context.DbContext
import io.kudos.ability.data.rdb.jdbc.context.DbParam
import io.kudos.base.logger.LogFactory
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.annotation.Order


/**
 * Aspect that handles the [TenantDsChange] annotation: wraps `value` as `_context::<value>` and
 * writes it into `DbParam.forcedDs`, telling the downstream routing advice
 * ([DynamicDataSourceInterceptor]) that this is a "resolve dynamically by context" intent, not a
 * direct data source key.
 *
 * The `@Order(-100)` and lazy-registration rationale is the same as [DsChangeAspect].
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Aspect
@Order(-100)
class TenantDsChangeAspect {

    private val log = LogFactory.getLog(this::class)

    /**
     * Pointcut definition: all methods annotated with [TenantDsChange].
     */
    @Pointcut("@annotation(io.kudos.ability.data.rdb.jdbc.aop.TenantDsChange)")
    private fun cut() {
    }

    /**
     * Around advice. Writes `_context::<serviceCode>` into forcedDs, proceeds the business method,
     * and finally restores the [DbParam] snapshot captured before entering the aspect. If `value`
     * already has the `_context` prefix it is forwarded as-is to avoid nesting.
     */
    @Around("cut()")
    @Throws(Throwable::class)
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        // findMergedAnnotation also sees annotations declared on interface / bridge methods.
        val dsChange = AnnotatedElementUtils.findMergedAnnotation(signature.method, TenantDsChange::class.java)
            ?: return joinPoint.proceed()
        val previous = DbContext.getOrNull()?.copy()
        val current = previous?.copy() ?: DbParam()
        if (dsChange.value.isNotBlank()) {
            current.forcedDs =
                if (dsChange.value.startsWith(DatasourceConst.CONTEXT_DS_PREFIX)) dsChange.value
                else "${DatasourceConst.CONTEXT_DS_PREFIX}::${dsChange.value}"
            current.readonly = dsChange.readonly
            DbContext.set(current)
            log.debug("Forcing data source: ds=${current.forcedDs}, readonly=${dsChange.readonly}")
        }
        return try {
            joinPoint.proceed()
        } finally {
            restore(previous)
        }
    }

    private fun restore(previous: DbParam?) {
        if (previous == null) {
            DbContext.clear()
        } else {
            DbContext.set(previous)
        }
    }

}
