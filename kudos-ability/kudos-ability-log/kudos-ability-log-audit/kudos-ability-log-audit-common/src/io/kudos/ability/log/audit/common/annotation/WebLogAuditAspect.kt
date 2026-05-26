package io.kudos.ability.log.audit.common.annotation

import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.common.support.AuditLogTool
import io.kudos.ability.log.audit.common.support.LogAuditContext
import io.kudos.base.logger.LogFactory
import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder


/**
 * Web audit aspect (HTTP call path).
 *
 * Intercepts Controller methods annotated with [WebAudit]; during [before] it grabs the [HttpServletRequest] from
 * [RequestContextHolder], extracts request metadata (URL, IP, UA, etc.) and writes a LogVo into [LogAuditContext].
 * Same semantics as [LogAuditAspect]:
 *  - success → [afterReturning]
 *  - failure → [afterThrowing], tagging the exception class name + message onto BaseLog.description
 *
 * Both paths call [LogAuditContext.clear] in finally to avoid ThreadLocal pollution under thread-pool scenarios.
 * `multipart/form-data` upload requests are skipped entirely — writing file streams into the audit store is bulky
 * and provides no cost-benefit.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Aspect
@Component
class WebLogAuditAspect {

    /** Audit-service implementation; required = false lets the aspect degrade to no-op when no audit impl is present */
    @Autowired(required = false)
    private val auditService: IAuditService? = null

    /**
     * Pointcut: matches all methods annotated with [WebAudit].
     *
     * @author K
     * @since 1.0.0
     */
    @Pointcut("@annotation(io.kudos.ability.log.audit.common.annotation.WebAudit)")
    fun pointCut() {
    }

    /**
     * Before advice: takes the current [HttpServletRequest] from [RequestContextHolder], combines it with [WebAudit]
     * metadata to build a LogVo and writes it into [LogAuditContext]. Multipart requests are skipped directly to
     * avoid polluting the audit store.
     *
     * @param joinPoint the join point
     * @author K
     * @since 1.0.0
     */
    @Before("pointCut()")
    fun before(joinPoint: JoinPoint) {
        val request = currentRequest() ?: return
        if (isMultipartContent(request)) return
        val signature = joinPoint.signature as org.aspectj.lang.reflect.MethodSignature
        val audit = signature.method.getAnnotation(WebAudit::class.java)
        val logVo = AuditLogTool.createLogVo(audit, request, joinPoint)
        LogAuditContext.set(logVo)
    }

    /**
     * Success path: after the method returns normally, submits the LogVo to the audit service.
     *
     * @param joinPoint the join point
     * @author K
     * @since 1.0.0
     */
    @AfterReturning(pointcut = "pointCut()")
    fun afterReturning(joinPoint: JoinPoint) {
        doSubmit(error = null)
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
        doSubmit(error = ex)
    }

    /**
     * Actual audit submission: get request -> skip multipart -> get LogVo ->
     * tag FAILED on failure -> submit. Exceptions are only ERROR-logged; the
     * finally block calls [LogAuditContext.clear] to avoid ThreadLocal leaks
     * in thread-pool scenarios.
     *
     * @param error exception object; null indicates the success path
     * @author K
     * @since 1.0.0
     */
    private fun doSubmit(error: Throwable?) {
        try {
            val request = currentRequest() ?: return
            if (isMultipartContent(request)) return
            val logVo = LogAuditContext.getOrNull() ?: return
            error?.let { ex ->
                val tag = "[FAILED:${ex::class.java.simpleName}:${ex.message.orEmpty()}] "
                logVo.logs.forEach { it.description = tag + it.description.orEmpty() }
            }
            runCatching {
                val requestBody = AuditLogTool.getRequestData(request)
                AuditLogTool.createSysAuditLogModel(logVo, requestBody)
                    ?.let { auditService?.submit(it) }
            }.onFailure { log.error(it, "Audit log component, interceptor exception!") }
        } finally {
            LogAuditContext.clear()
        }
    }

    /**
     * Returns the current HTTP request from Spring's [RequestContextHolder].
     * Returns null in non-HTTP contexts (e.g. internal scheduling tasks),
     * letting the caller silently skip.
     *
     * @return the current [HttpServletRequest]; null in non-HTTP contexts
     * @author K
     * @since 1.0.0
     */
    private fun currentRequest(): HttpServletRequest? =
        RequestContextHolder.getRequestAttributes()
            ?.resolveReference(RequestAttributes.REFERENCE_REQUEST) as? HttpServletRequest

    /**
     * Determines whether the request is a `multipart/...` upload.
     * Previously relied on commons-fileupload's `ServletFileUpload.isMultipartContent`;
     * after upgrading Spring 3.0, switched to inspecting the Content-Type header directly.
     *
     * @param request HTTP request
     * @return true if it is a multipart request; the caller should skip writing the audit
     * @author K
     * @since 1.0.0
     */
    private fun isMultipartContent(request: HttpServletRequest): Boolean =
        request.contentType?.lowercase()?.startsWith("multipart/") == true

    /** Logger. */
    private val log = LogFactory.getLog(this::class)
}
