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


/**
 * Web 审计切面（HTTP 调用路径）。
 *
 * 拦截标注 [WebAudit] 的 Controller 方法，从 [RequestContextHolder] 抓 [HttpServletRequest]，
 * 提取请求元信息（URL、IP、UA 等）写入 [LogAuditContext]，后置阶段把请求体作为模型 JSON 落审计库。
 * `multipart/form-data` 上传请求直接放过——文件流写入审计库既臃肿又意义不大。
 *
 * @author K
 * @since 1.0.0
 */
@Aspect
@Component
class WebLogAuditAspect {

    /** 审计服务实现，required = false 让无审计实现时切面退化为 no-op */
    @Autowired(required = false)
    private val auditService: IAuditService? = null

    /**
     * 切点：匹配所有标注了 [WebAudit] 注解的方法。
     *
     * @author K
     * @since 1.0.0
     */
    @Pointcut("@annotation(io.kudos.ability.log.audit.common.annotation.WebAudit)")
    fun pointCut() {
    }

    /**
     * 前置增强：从 [RequestContextHolder] 取当前 [HttpServletRequest]，结合 [WebAudit] 元信息生成 LogVo
     * 写入 [LogAuditContext]。multipart 请求直接跳过避免污染审计库。
     *
     * @param joinPoint 切入点
     * @author K
     * @since 1.0.0
     */
    @Before("pointCut()")
    fun before(joinPoint: JoinPoint) {
        val requestAttributes = RequestContextHolder.getRequestAttributes()
        val request =
            requireNotNull(requestAttributes) { "requestAttributes is null" }.resolveReference(RequestAttributes.REFERENCE_REQUEST) as HttpServletRequest?

        //note-upgrade-to-spring-3.0-position
        //if (ServletFileUpload.isMultipartContent(request)) {
        //    return;
        //}
        if (request == null || isMultipartContent(request)) {
            return
        }
        val signature = joinPoint.signature as org.aspectj.lang.reflect.MethodSignature
        val audit = signature.method.getAnnotation(WebAudit::class.java)
        val logVo = AuditLogTool.createLogVo(audit, request, joinPoint)
        LogAuditContext.set(logVo)
    }

    /**
     * 后置增强：再次取 request 与 [LogAuditContext] 中前置阶段写入的 LogVo，
     * 把请求体（[AuditLogTool.getRequestData]）作为模型 JSON 提交给审计服务。
     * 与 [before] 相同的 multipart 防御保证两端对称，避免请求体被提交但 LogVo 没建立时的脏数据。
     *
     * @param point 切入点
     * @author K
     * @since 1.0.0
     */
    @After("pointCut()")
    fun after(point: JoinPoint?) {
        val requestAttributes = RequestContextHolder.getRequestAttributes()
        val request =
            requireNotNull(requestAttributes) { "requestAttributes is null" }.resolveReference(RequestAttributes.REFERENCE_REQUEST) as HttpServletRequest?
        //note-upgrade-to-spring-3.0-position
        //if (ServletFileUpload.isMultipartContent(request)) {
        //    return;
        //}
        if (request == null || isMultipartContent(request)) {
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
            } catch (e: Exception) {
                log.error(e, "审计日志组件,拦截器异常!")
            }
        }
    }

    /**
     * 判断请求是否为 `multipart/...` 上传请求。
     * 之前依赖 commons-fileupload 的 `ServletFileUpload.isMultipartContent`，升级 Spring 3.0 后改为直接看 Content-Type 头。
     *
     * @param request HTTP 请求
     * @return true 表示 multipart 请求，调用方应跳过审计落盘
     * @author K
     * @since 1.0.0
     */
    private fun isMultipartContent(request: HttpServletRequest): Boolean {
        val contentType = request.contentType
        if (contentType != null) {
            if (contentType.lowercase().startsWith("multipart/")) {
                return true
            }
        }
        return false
    }

    /** 日志器 */
    private val log = LogFactory.getLog(this::class)
}
