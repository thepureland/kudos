package io.kudos.ability.log.audit.common.annotation

import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.common.support.AuditLogTool
import io.kudos.ability.log.audit.common.support.LogAuditContext
import io.kudos.base.data.json.JsonKit
import io.kudos.base.logger.LogFactory
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component


/**
 * 审计日志切面（非 Web 调用路径）。
 *
 * 拦截标注 [Audit] 的方法，前置阶段抓取入参第一个对象作为"待审计模型"放入 [LogAuditContext]，
 * 后置阶段把模型 JSON 化并通过 [IAuditService.submit] 异步落盘。
 *
 * 设计要点：
 * - `auditService` 用 `required = false`，避免下游没引入实现时启动失败；为 null 时切面静默不落盘
 * - 仅取 `joinPoint.args[0]`：约定审计方法把"业务模型"放在第一个参数
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
        if (joinPoint.args.size == 0) {
            return
        }
        val model = joinPoint.args[0]
        val signature = joinPoint.signature as MethodSignature
        val audit = signature.method.getAnnotation(Audit::class.java)
        val logVo = AuditLogTool.createLogVo(audit, model, joinPoint)
        LogAuditContext.set(logVo)
    }

    /**
     * 后置增强：从 [LogAuditContext] 取出前置阶段写入的 LogVo，序列化模型并提交给审计服务。
     * 异常仅日志记录，不向调用方传播。
     *
     * @param joinPoint 切入点
     * @author K
     * @since 1.0.0
     */
    @After("pointCut()")
    fun after(joinPoint: JoinPoint) {
        if (joinPoint.args.size == 0) {
            return
        }
        val logVo = LogAuditContext.get()
        val model = joinPoint.args[0]
        if (logVo != null) {
            try {
                val modelAudit = AuditLogTool.createSysAuditLogModel(logVo, JsonKit.toJson<Any>(model))
                if (auditService != null && modelAudit != null) {
                    auditService.submit(modelAudit)
                }
            } catch (e: Exception) {
                LOG.error(e, "审计日志组件,拦截器异常!")
            }
        }
    }

    /** 日志器 */
    private val LOG = LogFactory.getLog(LogAuditAspect::class)

}
