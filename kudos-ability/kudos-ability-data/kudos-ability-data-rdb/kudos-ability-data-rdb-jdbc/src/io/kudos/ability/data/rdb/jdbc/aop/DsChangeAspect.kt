package io.kudos.ability.data.rdb.jdbc.aop

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
 * Aspect that handles the [DsChange] annotation: before method execution it writes the annotation
 * parameters into [DbContext]'s `DbParam`, and after execution it restores the [DbParam] snapshot
 * captured before entering the aspect.
 *
 * The aspect `@Order(-100)` is outer than the routing advisor (-99) — `forcedDs` must be
 * written into the ThreadLocal first so the routing advice can see it.
 *
 * Registered by [io.kudos.ability.data.rdb.jdbc.init.JdbcAutoConfiguration] (lazily, to avoid
 * circular dependencies with early-loaded beans).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Aspect
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
        // findMergedAnnotation also sees annotations declared on interface / bridge methods.
        val dsChange = AnnotatedElementUtils.findMergedAnnotation(signature.method, DsChange::class.java)
            ?: return joinPoint.proceed()
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
