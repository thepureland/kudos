package io.kudos.ability.log.audit.common.annotation

import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.common.support.AuditLogTool
import io.kudos.ability.log.audit.common.support.LogAuditContext
import io.kudos.base.data.json.JsonKit
import io.kudos.base.logger.LogFactory
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component


/**
 * Generic `@Audit` aspect (non-web call path).
 *
 * Intercepts methods annotated with [Audit]. During [before], grabs the business
 * model and puts it into [LogAuditContext]; uses `@AfterReturning` /
 * `@AfterThrowing` to distinguish **success** vs **failure** operations:
 *  - Success -> goes to [afterReturning], writes the audit as-is.
 *  - Failure -> goes to [afterThrowing], appends the exception class name and
 *    message to the LogVo description, then writes the audit.
 *
 * **Both** branches call [LogAuditContext.clear] in finally — in thread-pool
 * scenarios, this prevents the next task from reading the previous stale LogVo
 * (the original `@After` version did not clear, which was a potential
 * ThreadLocal leak).
 *
 * Design points:
 * - `auditService` uses `required = false` so startup does not fail when no
 *   implementation is wired downstream; when null, the aspect silently does not write.
 * - The business model position is determined by [Audit.modelArgIndex] (default 0);
 *   on out-of-bounds, falls back to args[0].
 * - Exceptions are only ERROR-logged and not rethrown — auditing is a cross-cutting
 *   concern and must not reverse-block the business flow.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Aspect
@Component
class LogAuditAspect {

    /** Audit-service implementation; required = false lets the aspect degrade to no-op when no audit impl is present. */
    @Autowired(required = false)
    private val auditService: IAuditService? = null

    /**
     * Pointcut: matches all methods annotated with [Audit].
     *
     * @author K
     * @since 1.0.0
     */
    @Pointcut("@annotation(io.kudos.ability.log.audit.common.annotation.Audit)")
    fun pointCut() {
    }

    /**
     * Before advice: takes the first input parameter as the model to audit,
     * combines it with [Audit] metadata to produce a LogVo, and places it into
     * [LogAuditContext].
     *
     * @param joinPoint the join point
     * @author K
     * @since 1.0.0
     */
    @Before("pointCut()")
    fun before(joinPoint: JoinPoint) {
        if (joinPoint.args.isEmpty()) {
            return
        }
        val signature = joinPoint.signature as MethodSignature
        val audit = signature.method.getAnnotation(Audit::class.java)
        val model = resolveModelArg(joinPoint, audit) ?: return
        val logVo = AuditLogTool.createLogVo(audit, model, joinPoint)
        LogAuditContext.set(logVo)
    }

    /**
     * Success path: after the method returns normally, submits the LogVo to [IAuditService].
     *
     * @param joinPoint the join point
     * @author K
     * @since 1.0.0
     */
    @AfterReturning(pointcut = "pointCut()")
    fun afterReturning(joinPoint: JoinPoint) {
        doSubmit(joinPoint, error = null)
    }

    /**
     * Failure path: after the method throws, appends the exception info to
     * LogVo.description and submits the audit.
     *
     * @param joinPoint the join point
     * @param ex the thrown exception
     * @author K
     * @since 1.0.0
     */
    @AfterThrowing(pointcut = "pointCut()", throwing = "ex")
    fun afterThrowing(joinPoint: JoinPoint, ex: Throwable) {
        doSubmit(joinPoint, error = ex)
    }

    /**
     * Resolves the business model by [Audit.modelArgIndex]; on out-of-bounds / null,
     * **falls back to args[0]** for backward compatibility; returns null when the
     * method has no parameters.
     *
     * @param joinPoint the join point
     * @param audit annotation instance
     * @return business model object; null when there are no parameters
     * @author K
     * @since 1.0.0
     */
    private fun resolveModelArg(joinPoint: JoinPoint, audit: Audit): Any? {
        val args = joinPoint.args
        if (args.isEmpty()) return null
        val index = audit.modelArgIndex
        return when {
            index in args.indices && args[index] != null -> args[index]
            else -> args[0]
        }
    }

    /**
     * Actual audit submission: get LogVo -> tag FAILED on failure ->
     * call [IAuditService.submit]. Any exception is only ERROR-logged; the
     * finally block calls [LogAuditContext.clear] to avoid ThreadLocal leaks
     * under thread-pool scenarios.
     *
     * @param joinPoint the join point
     * @param error exception object; null indicates the success path
     * @author K
     * @since 1.0.0
     */
    private fun doSubmit(joinPoint: JoinPoint, error: Throwable?) {
        try {
            if (joinPoint.args.isEmpty()) return
            val logVo = LogAuditContext.getOrNull() ?: return
            val signature = joinPoint.signature as MethodSignature
            val audit = signature.method.getAnnotation(Audit::class.java)
            val model = resolveModelArg(joinPoint, audit) ?: return
            // Failure operation: append the exception class name + message to BaseLog.description so downstream can distinguish them.
            error?.let { ex ->
                val tag = "[FAILED:${ex::class.java.simpleName}:${ex.message.orEmpty()}] "
                logVo.logs.forEach { it.description = tag + it.description.orEmpty() }
            }
            runCatching {
                // Mask @LogDesensitize fields before the serialized model lands in the audit detail's
                // requestFormData — mirrors what the web path does via getRequestData(request, logVo).
                // Without this, the non-web path stored the raw sensitive values verbatim.
                val argJson = AuditLogTool.desensitizeJsonByLogVo(logVo, JsonKit.toJson<Any>(model))
                AuditLogTool.createSysAuditLogModel(logVo, argJson)
                    ?.let { auditService?.submit(it) }
            }.onFailure { LOG.error(it, "Audit log component, interceptor exception!") }
        } finally {
            LogAuditContext.clear()
        }
    }

    /** Logger. */
    private val LOG = LogFactory.getLog(LogAuditAspect::class)

}
