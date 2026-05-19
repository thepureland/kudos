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
 * Web 端 `@WebAudit` 切面。语义同 [LogAuditAspect]：
 *  - 成功 → [afterReturning]
 *  - 失败 → [afterThrowing]，把异常类名 + message 标记到 BaseLog.description
 *
 * 二者都在 finally 调 [LogAuditContext.clear]，避免线程池场景下的 ThreadLocal 污染。
 * multipart 请求**仍然**整体跳过——文件上传场景没有审计 body 的成本-收益意义。
 *
 * @author K
 * @since 1.0.0
 */
@Aspect
@Component
class WebLogAuditAspect {

    @Autowired(required = false)
    private val auditService: IAuditService? = null

    @Pointcut("@annotation(io.kudos.ability.log.audit.common.annotation.WebAudit)")
    fun pointCut() {
    }

    @Before("pointCut()")
    fun before(joinPoint: JoinPoint) {
        val request = currentRequest() ?: return
        if (isMultipartContent(request)) return
        val signature = joinPoint.signature as org.aspectj.lang.reflect.MethodSignature
        val audit = signature.method.getAnnotation(WebAudit::class.java)
        val logVo = AuditLogTool.createLogVo(audit, request, joinPoint)
        LogAuditContext.set(logVo)
    }

    @AfterReturning(pointcut = "pointCut()")
    fun afterReturning(joinPoint: JoinPoint) {
        doSubmit(error = null)
    }

    @AfterThrowing(pointcut = "pointCut()", throwing = "ex")
    fun afterThrowing(joinPoint: JoinPoint, ex: Throwable) {
        doSubmit(error = ex)
    }

    private fun doSubmit(error: Throwable?) {
        val request = currentRequest()
        try {
            if (request == null || isMultipartContent(request)) return
            val logVo = LogAuditContext.getOrNull() ?: return
            if (error != null) {
                val tag = "[FAILED:${error::class.java.simpleName}:${error.message ?: ""}] "
                logVo.logs.forEach { base ->
                    base.description = tag + (base.description ?: "")
                }
            }
            try {
                val requestBody = AuditLogTool.getRequestData(request)
                val modelAudit = AuditLogTool.createSysAuditLogModel(logVo, requestBody)
                if (auditService != null && modelAudit != null) {
                    auditService.submit(modelAudit)
                }
            } catch (e: Exception) {
                log.error(e, "审计日志组件,拦截器异常!")
            }
        } finally {
            LogAuditContext.clear()
        }
    }

    private fun currentRequest(): HttpServletRequest? {
        val attrs = RequestContextHolder.getRequestAttributes() ?: return null
        return attrs.resolveReference(RequestAttributes.REFERENCE_REQUEST) as? HttpServletRequest
    }

    private fun isMultipartContent(request: HttpServletRequest): Boolean {
        val contentType = request.contentType ?: return false
        return contentType.lowercase().startsWith("multipart/")
    }

    private val log = LogFactory.getLog(this::class)
}
