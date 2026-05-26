package io.kudos.ability.data.rdb.jdbc.aop

import io.kudos.ability.data.rdb.jdbc.context.DbContext
import io.kudos.ability.data.rdb.jdbc.context.DbParam
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
 * Aspect that handles the [DsChange] annotation: before method execution it writes the annotation
 * parameters into [DbContext]'s `DbParam`, and after execution it clears `DbParam` entirely.
 *
 * The aspect `@Order(-100)` is outer than [DynamicDataSourceAspect] (-99) — `forcedDs` must be
 * written into the ThreadLocal first so the routing aspect can see it.
 *
 * `@Lazy` defers bean initialization until first needed to avoid circular dependencies with
 * early-loaded beans.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
@Aspect
@Lazy
@Order(-100)
class DsChangeAspect {

    private val log = LogFactory.getLog(this::class)

    /**
     * Pointcut definition: all methods annotated with [DsChange].
     */
    @Pointcut("@annotation(io.kudos.ability.data.rdb.jdbc.aop.DsChange)")
    private fun cut() {
    }

    /**
     * Around advice. Writes `forcedDs` / `readonly`, proceeds with the business method, and in
     * `finally` restores the [DbParam] snapshot captured before entering the aspect; if there was
     * no thread context on entry, performs a full [DbContext.clear].
     */
    @Around("cut()")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val dsChange: DsChange = signature.method.getAnnotation(DsChange::class.java)
        val previous = DbContext.getOrNull()?.copy()
        val current = previous?.copy() ?: DbParam()
        if (dsChange.value.isNotBlank()) {
            current.forcedDs = dsChange.value
        }
        current.readonly = dsChange.readonly
        DbContext.set(current)
        log.debug("Forcibly specifying data source: ds=${current.forcedDs}, readonly=${dsChange.readonly}")
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
