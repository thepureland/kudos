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
 * Web 审计切面（HTTP 调用路径）。
 *
 * 拦截标注 [WebAudit] 的 Controller 方法，[before] 阶段从 [RequestContextHolder] 抓 [HttpServletRequest]，
 * 提取请求元信息（URL、IP、UA 等）写入 [LogAuditContext]。语义同 [LogAuditAspect]：
 *  - 成功 → [afterReturning]
 *  - 失败 → [afterThrowing]，把异常类名 + message 标记到 BaseLog.description
 *
 * 二者都在 finally 调 [LogAuditContext.clear]，避免线程池场景下的 ThreadLocal 污染。
 * `multipart/form-data` 上传请求整体跳过——文件流写入审计库既臃肿又无成本-收益意义。
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
        val request = currentRequest() ?: return
        if (isMultipartContent(request)) return
        val signature = joinPoint.signature as org.aspectj.lang.reflect.MethodSignature
        val audit = signature.method.getAnnotation(WebAudit::class.java)
        val logVo = AuditLogTool.createLogVo(audit, request, joinPoint)
        LogAuditContext.set(logVo)
    }

    /**
     * 成功路径：方法正常返回后，把 LogVo 提交给审计服务。
     *
     * @param joinPoint 切入点
     * @author K
     * @since 1.0.0
     */
    @AfterReturning(pointcut = "pointCut()")
    fun afterReturning(joinPoint: JoinPoint) {
        doSubmit(error = null)
    }

    /**
     * 失败路径：方法抛异常后，把异常信息拼到 LogVo.description 再提交审计。
     *
     * @param joinPoint 切入点
     * @param ex 抛出的异常
     * @author K
     * @since 1.0.0
     */
    @AfterThrowing(pointcut = "pointCut()", throwing = "ex")
    fun afterThrowing(joinPoint: JoinPoint, ex: Throwable) {
        doSubmit(error = ex)
    }

    /**
     * 实际提交审计：取 request → 跳过 multipart → 取 LogVo → 失败时打 FAILED tag → 提交。
     * 异常仅 ERROR 日志，finally 调 [LogAuditContext.clear] 避免线程池下的 ThreadLocal 泄漏。
     *
     * @param error 异常对象，null 表示成功路径
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
            }.onFailure { log.error(it, "审计日志组件,拦截器异常!") }
        } finally {
            LogAuditContext.clear()
        }
    }

    /**
     * 从 Spring 的 [RequestContextHolder] 取当前 HTTP 请求。
     * 非 HTTP 上下文（如内部调度任务）返回 null，让调用方静默跳过。
     *
     * @return 当前 [HttpServletRequest]；非 HTTP 上下文返回 null
     * @author K
     * @since 1.0.0
     */
    private fun currentRequest(): HttpServletRequest? =
        RequestContextHolder.getRequestAttributes()
            ?.resolveReference(RequestAttributes.REFERENCE_REQUEST) as? HttpServletRequest

    /**
     * 判断请求是否为 `multipart/...` 上传请求。
     * 之前依赖 commons-fileupload 的 `ServletFileUpload.isMultipartContent`，升级 Spring 3.0 后改为直接看 Content-Type 头。
     *
     * @param request HTTP 请求
     * @return true 表示 multipart 请求，调用方应跳过审计落盘
     * @author K
     * @since 1.0.0
     */
    private fun isMultipartContent(request: HttpServletRequest): Boolean =
        request.contentType?.lowercase()?.startsWith("multipart/") == true

    /** 日志器 */
    private val log = LogFactory.getLog(this::class)
}
