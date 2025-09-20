package io.kudos.ability.log.audit.commobn.annotation

import io.kudos.ability.log.audit.commobn.api.IAuditService
import io.kudos.ability.log.audit.commobn.entity.LogVo
import io.kudos.ability.log.audit.commobn.support.AuditLogTool
import io.kudos.base.data.json.JsonKit
import io.kudos.base.logger.LogFactory
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Autowired


@Aspect
@Lazy(false)
class LogAuditAspect {
    
    @Autowired(required = false)
    private val auditService: IAuditService? = null

    @Pointcut("@annotation(io.kudos.ability.log.audit.commobn.annotation.Audit)")
    fun pointCut() {
    }

    @Before("pointCut()")
    fun before(joinPoint: JoinPoint) {
        if (joinPoint.args.size == 0) {
            return
        }
        val arg = joinPoint.args[0]
        if (arg !is BaseModel) {
            return
        }
        val signature = joinPoint.signature as MethodSignature
        val audit = signature.method.getAnnotation(Audit::class.java)
        val model = arg as BaseModel
        val logVo = AuditLogTool.createLogVo(audit, model, joinPoint)
        model.setLogVo(logVo)
    }

    @After("pointCut()")
    fun after(joinPoint: JoinPoint) {
        if (joinPoint.args.size == 0) {
            return
        }
        val arg: Any? = joinPoint.args[0]
        if (arg !is BaseModel) {
            return
        }
        val model: BaseModel = arg as BaseModel
        val logVo = model.getLogVo() as LogVo?
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
