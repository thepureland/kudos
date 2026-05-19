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
 * 通用 `@Audit` 切面。
 *
 * 通过 `@AfterReturning` / `@AfterThrowing` 分支区分**成功** vs **失败**操作：
 *  - 成功 → 走 [afterReturning]，原样写审计
 *  - 失败 → 走 [afterThrowing]，把异常类名 + message 拼到 LogVo 描述里，再写审计
 *
 * 二者**都**会在 finally 里调 [LogAuditContext.clear]——线程池场景下避免下一次任务
 * 读到上一次的陈旧 LogVo（原 `@After` 版本不 clear，是潜在的 ThreadLocal 泄漏）。
 *
 * @author K
 * @since 1.0.0
 */
@Aspect
@Component
class LogAuditAspect {

    @Autowired(required = false)
    private val auditService: IAuditService? = null

    @Pointcut("@annotation(io.kudos.ability.log.audit.common.annotation.Audit)")
    fun pointCut() {
    }

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

    @AfterReturning(pointcut = "pointCut()")
    fun afterReturning(joinPoint: JoinPoint) {
        doSubmit(joinPoint, error = null)
    }

    @AfterThrowing(pointcut = "pointCut()", throwing = "ex")
    fun afterThrowing(joinPoint: JoinPoint, ex: Throwable) {
        doSubmit(joinPoint, error = ex)
    }

    /**
     * 按 [Audit.modelArgIndex] 取业务 model；越界 / null 时**回退到 args[0]**
     * 兼容旧行为，方法无参数则返回 null。
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

    private fun doSubmit(joinPoint: JoinPoint, error: Throwable?) {
        try {
            if (joinPoint.args.isEmpty()) return
            val logVo = LogAuditContext.getOrNull() ?: return
            val signature = joinPoint.signature as MethodSignature
            val audit = signature.method.getAnnotation(Audit::class.java)
            val model = resolveModelArg(joinPoint, audit) ?: return
            // 失败操作：把 exception 类名 + message 拼到 BaseLog.description，便于下游区分
            if (error != null) {
                val tag = "[FAILED:${error::class.java.simpleName}:${error.message ?: ""}] "
                logVo.logs.forEach { base ->
                    base.description = tag + (base.description ?: "")
                }
            }
            try {
                val modelAudit = AuditLogTool.createSysAuditLogModel(logVo, JsonKit.toJson<Any>(model))
                if (auditService != null && modelAudit != null) {
                    auditService.submit(modelAudit)
                }
            } catch (e: Exception) {
                LOG.error(e, "审计日志组件,拦截器异常!")
            }
        } finally {
            LogAuditContext.clear()
        }
    }

    private val LOG = LogFactory.getLog(LogAuditAspect::class)

}
