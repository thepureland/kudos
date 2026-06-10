package io.kudos.ability.log.audit.common.support

import io.kudos.ability.log.audit.common.annotation.Audit
import io.kudos.ability.log.audit.common.annotation.LogDesensitize
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.aspectj.lang.JoinPoint
import org.springframework.web.util.ContentCachingRequestWrapper
import java.lang.reflect.Modifier
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

    /**
     * 解析当前 [BaseLog] 该用哪个 [IAuditLogDetailDescriptionFormatter]：
     *
     * - `formatterClazz == DefaultAuditLogDetailDescriptionFormatter::class`（业务没指定具体实现）：
     *   走所有已注册的 formatter，挑首个 `needFormat=true` 的——业务可通过实现 [needFormat] 自动 opt-in
     * - 否则：业务在 `@Audit(descriptionFormatter = MyFmt::class)` 显式指定，直接走 SpringKit.getBean
     *
     * 历史 bug 注：旧实现拿 KClass 与 ::class.java（Java Class）比，always false 导致自动选择路径
     * 永远不进——已修正为 KClass vs KClass。
     *
     * @param baseLog 当前日志
     * @param formatterClazz @Audit 注解上声明的 formatter 类
     * @return 选中的 formatter；都不匹配时 null
     * @author K
     * @since 1.0.0
     */
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
    fun getRequestData(request: HttpServletRequest?): String = getRequestData(request, logVo = null)

    /**
     * Same as [getRequestData] but applies [LogDesensitize] masking when [logVo] carries
     * `requestDesensitizePropertyNames`. Empty / non-JSON / unconfigured names short-circuit
     * to the raw body — masking is purely best-effort and never blocks the audit submission.
     */
    fun getRequestData(request: HttpServletRequest?, logVo: LogVo?): String {
        if (request == null) return ""
        // Accept either a direct ContentCachingRequestWrapper or something wrapped around one.
        val cached = when {
            request is ContentCachingRequestWrapper -> request
            request is HttpServletRequestWrapper && request.request is ContentCachingRequestWrapper ->
                request.request as ContentCachingRequestWrapper
            else -> null
        }
        val raw = cached?.let { String(it.contentAsByteArray) } ?: ""
        return maybeDesensitizeJsonRequestBody(raw, logVo)
    }

    /**
     * Walks the class hierarchy of [joinPoint]'s first argument, collects every field marked
     * `@LogDesensitize`, and stores the names on [logVo] for downstream JSON masking.
     *
     * Inherited fields are included — typical request DTOs extend a `BasePayload` that carries
     * common audit-sensitive fields. Static fields are skipped to dodge companion-object backing
     * fields synthesized by Kotlin.
     *
     * Called by [createLogVo] at `before`-advice time; idempotent — re-invoking with the same
     * arguments overwrites the existing names with the same Set.
     */
    fun applyRequestDesensitizeFromFirstJoinPointArg(joinPoint: JoinPoint?, logVo: LogVo?) {
        if (joinPoint == null || logVo == null) return
        val args = joinPoint.args
        if (args.isNullOrEmpty() || args[0] == null) return
        val first = args[0] ?: return
        val keys = LinkedHashSet<String>()
        collectDesensitizeFieldNamesFromClass(first.javaClass, keys)
        if (keys.isNotEmpty()) {
            logVo.requestDesensitizePropertyNames = keys
        }
    }

    private fun collectDesensitizeFieldNamesFromClass(clazz: Class<*>, out: MutableSet<String>) {
        var c: Class<*>? = clazz
        while (c != null && c != Any::class.java) {
            for (field in c.declaredFields) {
                if (Modifier.isStatic(field.modifiers)) continue
                if (field.isAnnotationPresent(LogDesensitize::class.java)) {
                    out += field.name
                }
            }
            c = c.superclass
        }
    }

    /**
     * Public entry for the non-Web `@Audit` path (where the "request body" is the JSON-serialized
     * model object passed into [createLogVo]). Returns the input unchanged when [logVo] has no
     * desensitize names configured.
     */
    fun desensitizeJsonByLogVo(logVo: LogVo?, json: String): String = maybeDesensitizeJsonRequestBody(json, logVo)

    /**
     * Top-level JSON object: replace every key listed in [logVo].requestDesensitizePropertyNames
     * with [maskHead1Tail3] of its value. Nested objects / arrays are not traversed (matches soul's
     * behavior — the typical request DTO surfaces sensitive fields at the top level).
     *
     * Failures (parse errors, unexpected types) silently fall back to the raw text so a malformed
     * body can never block audit submission.
     */
    private fun maybeDesensitizeJsonRequestBody(raw: String, logVo: LogVo?): String {
        val names = logVo?.requestDesensitizePropertyNames
        if (names.isNullOrEmpty()) return raw
        if (raw.isBlank() || !isLikelyJsonObject(raw)) return raw
        return try {
            val element = Json.parseToJsonElement(raw)
            val obj = element as? JsonObject ?: return raw
            if (obj.isEmpty()) return raw
            var changed = false
            val mutated = LinkedHashMap<String, kotlinx.serialization.json.JsonElement>(obj.size)
            for ((key, value) in obj) {
                if (key in names && value !is JsonNull) {
                    val asText = when (value) {
                        is JsonPrimitive -> value.content
                        else -> value.toString()
                    }
                    mutated[key] = JsonPrimitive(maskHead1Tail3(asText))
                    changed = true
                } else {
                    mutated[key] = value
                }
            }
            if (changed) JsonObject(mutated).toString() else raw
        } catch (e: Exception) {
            LOG.debug("Failed to desensitize JSON request body; falling back to raw text: {0}", e.message)
            raw
        }
    }

    private fun isLikelyJsonObject(s: String): Boolean {
        val t = s.trim()
        return t.startsWith("{") && t.endsWith("}")
    }

    /**
     * Head-1 + `"****"` + tail-3. Values up to 4 chars long are fully masked to `"****"`
     * (otherwise a 4-char value would reveal head + nothing). Mirrors soul's contract.
     */
    private fun maskHead1Tail3(raw: String?): String? {
        if (raw == null) return null
        val len = raw.length
        if (len == 0) return raw
        if (len <= 4) return "****"
        return raw.substring(0, 1) + "****" + raw.substring(len - 3)
    }

    /**
     * Creates a log object.
     *
     * @param audit audit annotation
     * @param model web request payload
     */
    fun createLogVo(audit: Audit, model: Any, joinPoint: JoinPoint): LogVo {
        val logVo = LogVo()
        applyRequestDesensitizeFromFirstJoinPointArg(joinPoint, logVo)
        val baseLog: BaseLog = logVo.addAuditLog(audit)
        if (audit.ignoreForm == YesNotEnum.NOT) {
            // Mask the serialized payload before it lands in the audit-visible client-info bucket;
            // re-serializing through Json keeps formatting compact and avoids leaking the raw value.
            val serialized = JsonKit.toJson(model)
            KudosContextHolder.get().clientInfo?.requestContentString = desensitizeJsonByLogVo(logVo, serialized)
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
        applyRequestDesensitizeFromFirstJoinPointArg(joinPoint, logVo)
        val baseLog: BaseLog = logVo.addAuditLog(audit)
        if (audit.ignoreForm == YesNotEnum.NOT) {
            // Pass logVo so [LogDesensitize]-marked fields are masked at body-extraction time —
            // the same body lands in client-info and in the audit detail.
            val body: String = getRequestData(request, logVo)
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

    /**
     * 取详情描述：先看 context 里业务方主动塞的 detailDescription，没有就走 [IAuditLogDetailDescriptionFormatter]。
     * 二级降级：formatter 也找不到时回空串（不写日志而非抛错——保证审计不影响业务）。
     *
     * @param vo 当前日志
     * @return 详情描述
     * @throws IllegalArgumentException descriptionFormatterClass 为 null（注解未正确装配）
     * @author K
     * @since 1.0.0
     */
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

    /**
     * 构造审计详情记录 (SysAuditDetailLogVo)：与 SysAuditLogVo 通过 auditId 关联，
     * 把 web 请求的 url / referer / 表单数据等"非主表 metadata"放在这里，避免主表过宽。
     *
     * @param sysAuditLog 主审计记录
     * @return 详情记录
     * @author K
     * @since 1.0.0
     */
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
