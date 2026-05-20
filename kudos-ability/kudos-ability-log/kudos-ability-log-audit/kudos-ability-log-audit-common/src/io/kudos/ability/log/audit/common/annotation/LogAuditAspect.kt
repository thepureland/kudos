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
 * 通用 `@Audit` 切面（非 Web 调用路径）。
 *
 * 拦截标注 [Audit] 的方法，[before] 阶段抓取业务模型放入 [LogAuditContext]，
 * 通过 `@AfterReturning` / `@AfterThrowing` 分支区分**成功** vs **失败**操作：
 *  - 成功 → 走 [afterReturning]，原样写审计
 *  - 失败 → 走 [afterThrowing]，把异常类名 + message 拼到 LogVo 描述里，再写审计
 *
 * 二者**都**会在 finally 里调 [LogAuditContext.clear]——线程池场景下避免下一次任务
 * 读到上一次的陈旧 LogVo（原 `@After` 版本不 clear，是潜在的 ThreadLocal 泄漏）。
 *
 * 设计要点：
 * - `auditService` 用 `required = false`，避免下游没引入实现时启动失败；为 null 时切面静默不落盘
 * - 业务模型位置由 [Audit.modelArgIndex] 决定（默认 0），越界时回退到 args[0]
 * - 异常仅记 ERROR 不重抛——审计是横切关注点，不应反向阻断业务流程
 *
 * @author K
 * @since 1.0.0
 */
@Aspect
@Component
class LogAuditAspect {

    /** 审计服务实现，required = false 让无审计实现时切面退化为 no-op */
    @Autowired(required = false)
    private val auditService: IAuditService? = null

    /**
     * 切点：匹配所有标注了 [Audit] 注解的方法。
     *
     * @author K
     * @since 1.0.0
     */
    @Pointcut("@annotation(io.kudos.ability.log.audit.common.annotation.Audit)")
    fun pointCut() {
    }

    /**
     * 前置增强：抓取入参第一个对象作为待审计模型，结合 [Audit] 元信息生成 LogVo 放入 [LogAuditContext]。
     *
     * @param joinPoint 切入点
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
     * 成功路径：方法正常返回后，把 LogVo 提交给 [IAuditService]。
     *
     * @param joinPoint 切入点
     * @author K
     * @since 1.0.0
     */
    @AfterReturning(pointcut = "pointCut()")
    fun afterReturning(joinPoint: JoinPoint) {
        doSubmit(joinPoint, error = null)
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
        doSubmit(joinPoint, error = ex)
    }

    /**
     * 按 [Audit.modelArgIndex] 取业务 model；越界 / null 时**回退到 args[0]**
     * 兼容旧行为，方法无参数则返回 null。
     *
     * @param joinPoint 切入点
     * @param audit 注解实例
     * @return 业务模型对象，无参时返回 null
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
     * 实际提交审计：取 LogVo → 失败时打 FAILED tag → 调 [IAuditService.submit]。
     * 任何异常仅 ERROR 日志，finally 调 [LogAuditContext.clear] 避免线程池下的 ThreadLocal 泄漏。
     *
     * @param joinPoint 切入点
     * @param error 异常对象，null 表示成功路径
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
            // 失败操作：把 exception 类名 + message 拼到 BaseLog.description，便于下游区分
            error?.let { ex ->
                val tag = "[FAILED:${ex::class.java.simpleName}:${ex.message.orEmpty()}] "
                logVo.logs.forEach { it.description = tag + it.description.orEmpty() }
            }
            runCatching {
                AuditLogTool.createSysAuditLogModel(logVo, JsonKit.toJson<Any>(model))
                    ?.let { auditService?.submit(it) }
            }.onFailure { LOG.error(it, "审计日志组件,拦截器异常!") }
        } finally {
            LogAuditContext.clear()
        }
    }

    /** 日志器 */
    private val LOG = LogFactory.getLog(LogAuditAspect::class)

}
