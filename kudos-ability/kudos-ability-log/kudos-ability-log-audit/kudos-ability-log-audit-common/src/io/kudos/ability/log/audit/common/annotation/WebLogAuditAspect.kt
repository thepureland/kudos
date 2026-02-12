package io.kudos.ability.log.audit.common.annotation

import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.common.support.AuditLogTool
import io.kudos.ability.log.audit.common.support.LogAuditContext
import io.kudos.base.logger.LogFactory
import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder


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
        val requestAttributes = RequestContextHolder.getRequestAttributes()
        val request =
            requireNotNull(requestAttributes) { "requestAttributes is null" }.resolveReference(RequestAttributes.REFERENCE_REQUEST) as HttpServletRequest?

        //note-upgrade-to-spring-3.0-position
        //if (ServletFileUpload.isMultipartContent(request)) {
        //    return;
        //}
        if (request != null && isMultipartContent(request)) {
            return
        }
        val signature = joinPoint.signature as org.aspectj.lang.reflect.MethodSignature
        val audit = signature.method.getAnnotation(WebAudit::class.java)
        val logVo = AuditLogTool.createLogVo(audit, request, joinPoint)
        LogAuditContext.set(logVo)
    }

    @After("pointCut()")
    fun after(point: JoinPoint?) {
        val requestAttributes = RequestContextHolder.getRequestAttributes()
        val request =
            requireNotNull(requestAttributes) { "requestAttributes is null" }.resolveReference(RequestAttributes.REFERENCE_REQUEST) as HttpServletRequest?
        //note-upgrade-to-spring-3.0-position
        //if (ServletFileUpload.isMultipartContent(request)) {
        //    return;
        //}
        if (request != null && isMultipartContent(request)) {
            return
        }
        val logVo = LogAuditContext.get()
        if (logVo != null) {
            try {
                val requestBody = AuditLogTool.getRequestData(request)
                val modelAudit = AuditLogTool.createSysAuditLogModel(logVo, requestBody)
                if (auditService != null && modelAudit != null) {
                    auditService.submit(modelAudit)
                }
            } catch (e: java.lang.Exception) {
                log.error(e, "审计日志组件,拦截器异常!")
            }
        }
    }

    private fun isMultipartContent(request: HttpServletRequest): Boolean {
        val contentType = request.contentType
        if (contentType != null) {
            if (contentType.lowercase().startsWith("multipart/")) {
                return true
            }
        }
        return false
    }

    private val log = LogFactory.getLog(this)
}
