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
 * Audit-log construction / context-write utility.
 *
 * Aspect call stack:
 *  - `LogAuditAspect.before` / `WebLogAuditAspect.before` -> [createLogVo]
 *    constructs a [LogVo] and pushes it into [LogAuditContext].
 *  - `*.after` -> [createSysAuditLogModel] merges method parameters / web request
 *    body and hands the result to [IAuditService.submit].
 *
 * Description-formatter dispatch logic: see the kdoc on [descriptionFormatter]
 * (includes notes on a historical bug).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object AuditLogTool {
    private val LOG = LogFactory.getLog(AuditLogTool::class)
    private val parameterNameDiscoverer: ParameterNameDiscoverer = DefaultParameterNameDiscoverer()

    @Volatile
    private var cachedTenantProvider: ILogSourceTenantProvider? = null

    /**
     * Lazy + cached lookup for [ILogSourceTenantProvider].
     *
     * The old implementation queried the Spring bean once in the `object`'s `init`
     * block — but [AuditLogTool] is a Kotlin `object`, whose init runs on **first
     * access**; that moment is typically before the Spring context is ready (the
     * aspect registration phase already references it), so the bean lookup failed,
     * `tenantProvider` was permanently null, and there was no retry.
     *
     * Changed to look up on demand and cache: when the bean is not yet ready,
     * returns null (falls back to `entity.tenantId` as sourceTenantId per the
     * existing contract); once the first successful lookup succeeds after the
     * bean is ready, the value is cached so subsequent calls do not go through
     * Spring reflection.
     */
    private fun tenantProvider(): ILogSourceTenantProvider? {
        cachedTenantProvider?.let { return it }
        val resolved = runCatching { SpringKit.getBeanOrNull(ILogSourceTenantProvider::class) }
            .onFailure { LOG.debug("Failed to look up ILogSourceTenantProvider: {0}", it.message) }
            .getOrNull()
        if (resolved != null) {
            cachedTenantProvider = resolved
        }
        return resolved
    }

    /**
     * Loads the old data used for the detail view.
     *
     * @param baseLog audit log entry
     * @param joinPoint aspect join point
     */
    private fun processBizData(
        baseLog: BaseLog,
        formatterClazz: KClass<out IAuditLogDetailDescriptionFormatter>,
        joinPoint: JoinPoint
    ) {
        descriptionFormatter(baseLog, formatterClazz)?.let { formatter ->
            baseLog.oldBizData = formatter.loadOldBizData(baseLog, joinPoint.args)
        }
        baseLog.paramArgs = joinPoint.args
    }

    private fun descriptionFormatter(
        baseLog: BaseLog,
        formatterClazz: KClass<out IAuditLogDetailDescriptionFormatter>
    ): IAuditLogDetailDescriptionFormatter? {
        // Historical bug: comparing KClass to ::class.java (Java Class) is always false.
        // As a result, the default formatter's "auto-pick a business-side implementation via needFormat()" branch was never entered;
        // all calls degenerated to fetching the default implementation via SpringKit.getBean, so custom business formatters
        // were only used when explicitly specified in `@Audit(descriptionFormatter = MyFmt::class)`.
        // Fixed by comparing KClass to KClass, restoring the auto-pick path.
        if (formatterClazz == DefaultAuditLogDetailDescriptionFormatter::class) {
            val formatterMap = SpringKit.getBeansOfType<IAuditLogDetailDescriptionFormatter>()
            return formatterMap.values.firstOrNull { it.needFormat(baseLog) }
        } else {
            return SpringKit.getBean(formatterClazz)
        }
    }

    /**
     * Extracts body bytes from the request (only available when wrapped by
     * `ContentCachingRequestWrapper`; otherwise returns "").
     *
     * Safety: the old implementation's `request as HttpServletRequestWrapper`
     * was an unconditional cast that would `ClassCastException` on raw requests
     * not wrapped by Spring Security and the like. Changed to safe `as?` casting
     * plus an early "" return, so deployments without
     * [io.kudos.ability.log.audit.common.filter.WebLogAuditFilter] do not have
     * their entire request path broken by the audit aspect.
     */
    fun getRequestData(request: HttpServletRequest?): String {
        if (request == null) return ""
        // Accept either a direct ContentCachingRequestWrapper or something wrapped around one.
        val cached = when {
            request is ContentCachingRequestWrapper -> request
            request is HttpServletRequestWrapper && request.request is ContentCachingRequestWrapper ->
                request.request as ContentCachingRequestWrapper
            else -> null
        }
        return cached?.let { String(it.contentAsByteArray) } ?: ""
    }

    /**
     * Creates a log object.
     *
     * @param audit audit annotation
     * @param model web request payload
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
            LOG.debug("Recording log: id property not found, ignored..")
        }
        return logVo
    }

    /**
     * Creates a log object.
     *
     * @param audit   audit annotation
     * @param request web request
     */
    fun createLogVo(audit: WebAudit, request: HttpServletRequest, joinPoint: JoinPoint): LogVo {
        val logVo = LogVo()
        val baseLog: BaseLog = logVo.addAuditLog(audit)
        if (audit.ignoreForm == YesNotEnum.NOT) {
            val body: String = getRequestData(request)
            KudosContextHolder.get().clientInfo?.requestContentString = body
        }
        // request.getParameter returns null when the parameter is missing (Java API); use isNullOrBlank to guard against NPE.
        val entityId: String? = request.getParameter("id")
        if (!entityId.isNullOrBlank()) {
            baseLog.entityId = entityId
        }
        processBizData(baseLog, audit.descriptionFormatter, joinPoint)
        return logVo
    }

    /**
     * Creates the log model.
     *
     * @param logVo     log object
     * @param argString serialized method arguments
     */
    fun createSysAuditLogModel(logVo: LogVo, argString: String): SysAuditLogModel? {
        val vos = logVo.logs.takeIf { it.isNotEmpty() } ?: return null
        val entities = mutableListOf<SysAuditLogVo>()
        val detailLogs = LinkedList<SysAuditDetailLogVo?>()
        val modelAudit = SysAuditLogModel().apply {
            this.entities = entities
            sysAuditDetailLogs = detailLogs
        }
        val entityId = try {
            JsonKit.fromJson<Map<*, *>>(argString)
                ?.get("id")?.toString()
                ?.takeIf { NumberKit.isNumber(it) }
        } catch (_: Exception) {
            LOG.debug("Failed to convert parameters; entity id will not be set.")
            null
        }
        for (vo in vos) {
            val sysAuditLogVo = vo.toSysLogVo()
            val sysAuditDetailLog = getAuditDetail(sysAuditLogVo)
            sysAuditDetailLog.objectParams = vo.getObjectParams()
            sysAuditDetailLog.stringParams = vo.getStringParams()
            sysAuditDetailLog.requestFormData = if (vo.ignoreForm == YesNotEnum.YES.bool) "" else argString
            entityId?.let { sysAuditLogVo.entityId = it }
            sysAuditDetailLog.description = detailDescription(vo)
            entities.add(sysAuditLogVo)
            detailLogs.add(sysAuditDetailLog)
        }
        setOperator(modelAudit)
        return modelAudit
    }

    /**
     * Adds a log detail description for the current thread.
     *
     * @param desc description text
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
        detailDescription?.takeIf { it.isNotBlank() }?.let { return it }
        val formatterClass = requireNotNull(vo.descriptionFormatterClass) { "descriptionFormatterClass is null" }
        return descriptionFormatter(vo, formatterClass)?.descriptionFormat(vo) ?: ""
    }

    /**
     * Sets the log operator and the log data source.
     *
     * @param modelAudit the audit log model to populate
     */
    fun setOperator(modelAudit: SysAuditLogModel) {
        val context = KudosContextHolder.get()
        val clientInfo = context.clientInfo
        for (entity in requireNotNull(modelAudit.entities) { "entities is null" }) {
            entity.operateTime = Date()
            clientInfo?.ip?.let { entity.operateIp = IpKit.ipv4StringToLong(it) }

            // Historical TODO: operateIpDictCode / operator(username) / operatorUserType need their corresponding
            // fields on the context to be populated. KudosContext currently does not carry these properties, so leave them empty.
            entity.operatorId = context.user?.id

            entity.clientBrowser = clientInfo?.browser?.first
            entity.clientOs = clientInfo?.os?.first
            entity.requestType = clientInfo?.requestType
            entity.subSysCode = subSysCode
            entity.tenantId = context.tenantId
            entity.sourceTenantId = tenantProvider()?.getSourceTenant(entity.tenantId, entity.operatorId)
                ?: entity.tenantId
        }

        modelAudit.subSysCode = subSysCode
        modelAudit.tenantId = context.tenantId
    }

    private val subSysCode: String?
        get() = KudosContextHolder.get().subSystemCode

    private fun getAuditDetail(sysAuditLog: SysAuditLogVo): SysAuditDetailLogVo =
        SysAuditDetailLogVo().apply {
            id = RandomStringKit.uuid()
            auditId = sysAuditLog.id
            KudosContextHolder.get().clientInfo?.let { info ->
                operateUrl = info.url
                requestReferer = info.requestReferer
                requestFormData = info.requestContentString
            }
        }
}
