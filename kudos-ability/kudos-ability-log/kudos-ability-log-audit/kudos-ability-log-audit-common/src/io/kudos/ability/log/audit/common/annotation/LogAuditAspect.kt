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
        if (joinPoint.args.size == 0) {
            return
        }
        val model = joinPoint.args[0]
        val signature = joinPoint.signature as MethodSignature
        val audit = signature.method.getAnnotation(Audit::class.java)
        val logVo = AuditLogTool.createLogVo(audit, model, joinPoint)
        LogAuditContext.set(logVo)
    }

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
            } catch (e: java.lang.Exception) {
                LOG.error(e, "审计日志组件,拦截器异常!")
            }
        }
    }

    private val LOG = LogFactory.getLog(LogAuditAspect::class)

}
