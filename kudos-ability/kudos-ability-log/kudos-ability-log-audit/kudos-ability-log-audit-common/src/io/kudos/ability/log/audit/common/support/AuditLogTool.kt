package io.kudos.ability.log.audit.common.support

import io.kudos.ability.log.audit.common.annotation.Audit
import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.entity.*
import io.kudos.base.bean.BeanKit
import io.kudos.base.data.json.JsonKit
import io.kudos.base.enums.impl.YesNotEnum
import io.kudos.base.lang.math.NumberKit
import io.kudos.base.lang.string.RandomStringKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.net.IpKit
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import org.aspectj.lang.JoinPoint
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.ParameterNameDiscoverer
import org.springframework.web.util.ContentCachingRequestWrapper
import java.util.ArrayList
import java.util.Date
import java.util.LinkedList
import kotlin.reflect.KClass

/**
 * 审计日志构造 / 上下文写入工具。
 *
 * Aspect 调用栈：
 *  - `LogAuditAspect.before` / `WebLogAuditAspect.before` → [createLogVo] 构造 [LogVo] 塞到 [LogAuditContext]
 *  - `*.after` → [createSysAuditLogModel] 合并方法参数 / Web request body 后交给 [IAuditService.submit]
 *
 * 描述格式化器分发逻辑：见 [descriptionFormatter] 的 kdoc（含历史 bug 说明）。
 *
 * @author K
 * @since 1.0.0
 */
object AuditLogTool {
    private val LOG = LogFactory.getLog(AuditLogTool::class)
    private val parameterNameDiscoverer: ParameterNameDiscoverer = DefaultParameterNameDiscoverer()
    private var tenantProvider: ILogSourceTenantProvider? = null

    init {
        initSourceTenantProvider()
    }

    private fun initSourceTenantProvider() {
        tenantProvider = try {
            SpringKit.getBean<ILogSourceTenantProvider>()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 加载详情用的旧数据
     *
     * @param baseLog
     * @param joinPoint 切片点
     */
    private fun processBizData(
        baseLog: BaseLog,
        formatterClazz: KClass<out IAuditLogDetailDescriptionFormatter>,
        joinPoint: JoinPoint
    ) {
        val iAuditLogDetailDescriptionFormatter = descriptionFormatter(baseLog, formatterClazz)
        if (iAuditLogDetailDescriptionFormatter != null) {
            val oldData = iAuditLogDetailDescriptionFormatter.loadOldBizData(baseLog, joinPoint.args)
            baseLog.oldBizData = oldData
        }
        baseLog.paramArgs = joinPoint.args
    }

    private fun descriptionFormatter(
        baseLog: BaseLog,
        formatterClazz: KClass<out IAuditLogDetailDescriptionFormatter>
    ): IAuditLogDetailDescriptionFormatter? {
        // 历史 bug：比较 KClass 与 ::class.java（Java Class），始终 false。结果：默认 formatter 的
        // "通过 needFormat() 自动选择业务侧实现"分支从来不进；所有调用都退化到走 SpringKit.getBean
        // 拿默认实现 → 业务自定义的 formatter 必须显式在 `@Audit(descriptionFormatter = MyFmt::class)`
        // 才会被用到。修正为 KClass 与 KClass 比较，恢复 auto-pick 路径。
        if (formatterClazz == DefaultAuditLogDetailDescriptionFormatter::class) {
            val formatterMap = SpringKit.getBeansOfType<IAuditLogDetailDescriptionFormatter>()
            return formatterMap.values.firstOrNull { it.needFormat(baseLog) }
        } else {
            return SpringKit.getBean(formatterClazz)
        }
    }

    /**
     * 从请求里抓出 body bytes（被 `ContentCachingRequestWrapper` 包过才有缓存内容；否则返回 ""）。
     *
     * 安全：旧实现 `request as HttpServletRequestWrapper` 是无条件强制类型转换，对未被
     * Spring Security 等包过的原始请求会 `ClassCastException`。改为 `as?` 安全转换 +
     * 早返回 ""，让没启用 [io.kudos.ability.log.audit.common.filter.WebLogAuditFilter]
     * 的部署不会因 audit 切面挂掉整条请求路径。
     */
    fun getRequestData(request: HttpServletRequest?): String {
        if (request == null) return ""
        // 直接是 ContentCachingRequestWrapper 或被它包过一层都接受
        val cached = when {
            request is ContentCachingRequestWrapper -> request
            request is HttpServletRequestWrapper && request.request is ContentCachingRequestWrapper ->
                request.request as ContentCachingRequestWrapper
            else -> null
        }
        return if (cached != null) String(cached.contentAsByteArray) else ""
    }

    /**
     * 创建日志对象
     *
     * @param audit 日志注解
     * @param model web请求
     */
    fun createLogVo(audit: Audit, model: Any, joinPoint: JoinPoint): LogVo {
        val logVo = LogVo()
        val baseLog: BaseLog = logVo.addAuditLog(audit)
        if (audit.ignoreForm == YesNotEnum.NOT) {
            KudosContextHolder.get().clientInfo?.requestContentString = JsonKit.toJson(model)
        }
        try {
            processBizData(baseLog, audit.descriptionFormatter, joinPoint)
            val entityId = BeanKit.getProperty(model, "id") as String?
            if (!entityId.isNullOrBlank()) {
                baseLog.entityId = entityId
            }
        } catch (e: Exception) {
            LOG.debug("记录日志找不到id属性，忽略..")
        }
        return logVo
    }

    /**
     * 创建日志对象
     *
     * @param audit   日志注解
     * @param request web请求
     */
    fun createLogVo(audit: WebAudit, request: HttpServletRequest, joinPoint: JoinPoint): LogVo {
        val logVo = LogVo()
        val baseLog: BaseLog = logVo.addAuditLog(audit)
        if (audit.ignoreForm == YesNotEnum.NOT) {
            val body: String = getRequestData(request)
            KudosContextHolder.get().clientInfo?.requestContentString = body
        }
        val entityId = request.getParameter("id")
        if (entityId.isNotBlank()) {
            baseLog.entityId = entityId
        }
        processBizData(baseLog, audit.descriptionFormatter, joinPoint)
        return logVo
    }

    /**
     * 创建 日志模型
     *
     * @param logVo     日志对象
     * @param argString
     */
    fun createSysAuditLogModel(logVo: LogVo, argString: String): SysAuditLogModel? {
        var modelAudit: SysAuditLogModel? = null
        val vos = logVo.logs
        if (!vos.isNullOrEmpty()) {
            modelAudit = SysAuditLogModel()
            modelAudit.entities = ArrayList()
            modelAudit.sysAuditDetailLogs = LinkedList()
            var entityId: String? = null
            try {
                val body = JsonKit.fromJson<Map<*, *>>(argString)
                if (body != null && body["id"] != null) {
                    if (NumberKit.isNumber(body["id"].toString())) {
                        entityId = body["id"].toString()
                    }
                }
            } catch (e: Exception) {
                LOG.debug("转换参数失败，不设置实体ID.")
            }
            for (vo in vos) {
                val sysAuditLogVo = requireNotNull(vo) { "vo is null" }.toSysLogVo()
                val sysAuditDetailLog = getAuditDetail(sysAuditLogVo)
                sysAuditDetailLog.objectParams = vo.getObjectParams()
                sysAuditDetailLog.stringParams = vo.getStringParams()
                sysAuditDetailLog.requestFormData = argString
                if (vo.ignoreForm == YesNotEnum.YES.bool) {
                    sysAuditDetailLog.requestFormData = ""
                }
                if (entityId != null) {
                    sysAuditLogVo.entityId = entityId
                }
                sysAuditDetailLog.description = detailDescription(vo)
                requireNotNull(modelAudit.entities) { "entities is null" }.add(sysAuditLogVo)
                requireNotNull(modelAudit.sysAuditDetailLogs) { "sysAuditDetailLogs is null" }.add(sysAuditDetailLog)
            }

            setOperator(modelAudit)
        }
        return modelAudit
    }

    /**
     * 当前线程中增加日志详情描述
     *
     * @param desc
     */
    fun addLogDetailDescription(desc: String?) {
        KudosContextHolder.get().addOtherInfos(SysAuditDetailLogVo.AUDIT_LOG_DESC to desc)
    }

    private val detailDescription: String?
        get() {
            val otherInfos = KudosContextHolder.get().otherInfos
            if (otherInfos == null) {
                return ""
            }
            return otherInfos[SysAuditDetailLogVo.AUDIT_LOG_DESC] as String?
        }

    private fun detailDescription(vo: BaseLog): String? {
        val detailDescription = detailDescription
        if (!detailDescription.isNullOrBlank()) {
            return detailDescription
        }
        var result: String? = ""
        val descriptionFormatter = descriptionFormatter(vo, requireNotNull(vo.descriptionFormatterClass) { "descriptionFormatterClass is null" })
        if (descriptionFormatter != null) {
            result = descriptionFormatter.descriptionFormat(vo)
        }
        return result
    }

    /**
     * 设置日志的操作者
     * 设置日志数据源
     *
     * @param modelAudit
     */
    fun setOperator(modelAudit: SysAuditLogModel) {
        val context = KudosContextHolder.get()
        for (entity in requireNotNull(modelAudit.entities) { "entities is null" }) {
            entity.operateTime = Date()
            val ip = context.clientInfo?.ip
            if (ip != null) {
                entity.operateIp = IpKit.ipv4StringToLong(ip)
            }

            // 历史 TODO: operateIpDictCode / operator(username) / operatorUserType 需要从
            // context 拿到对应字段才能填——目前 KudosContext 上没有这几个属性，留空即可。
            entity.operatorId = context.user?.id

            entity.clientBrowser = context.clientInfo?.browser?.first
            entity.clientOs = context.clientInfo?.os?.first
            entity.requestType = context.clientInfo?.requestType
            entity.subSysCode = subSysCode
            entity.tenantId = context.tenantId
            if (tenantProvider != null) {
                entity.sourceTenantId = requireNotNull(tenantProvider) { "tenantProvider is null" }.getSourceTenant(entity.tenantId, entity.operatorId)
            } else {
                entity.sourceTenantId = entity.tenantId
            }
        }

        modelAudit.subSysCode = subSysCode
        modelAudit.tenantId = context.tenantId
    }

    private val subSysCode: String?
        get() = KudosContextHolder.get().subSystemCode

    private fun getAuditDetail(sysAuditLog: SysAuditLogVo): SysAuditDetailLogVo {
        val sysAuditDetailLog = SysAuditDetailLogVo()
        sysAuditDetailLog.id = RandomStringKit.uuid()
        sysAuditDetailLog.auditId = sysAuditLog.id
        val clientInfo = KudosContextHolder.get().clientInfo
        if (clientInfo != null) {
            sysAuditDetailLog.operateUrl = clientInfo.url
            sysAuditDetailLog.requestReferer = clientInfo.requestReferer
            sysAuditDetailLog.requestFormData = clientInfo.requestContentString
        }
        return sysAuditDetailLog
    }
}
