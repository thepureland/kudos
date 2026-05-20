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

    @Volatile
    private var cachedTenantProvider: ILogSourceTenantProvider? = null

    /**
     * 懒加载 + 缓存的 [ILogSourceTenantProvider] 查找。
     *
     * 旧实现在 `object` 的 `init` 块里一次性查 Spring bean——但 [AuditLogTool] 是 Kotlin
     * `object`，其 init 在**首次访问**时执行；这个时间点通常是 Spring 上下文还没装好（切面
     * 注册阶段就被引用），所以查 bean 失败、`tenantProvider` 永远为 null、且无重试。
     *
     * 改成每次按需查 + 拿到后缓存：bean 还没就绪时返回 null（按现有契约用 `entity.tenantId`
     * 作为 sourceTenantId 兜底）；bean 就绪后第一次成功查到立即缓存，后续不再走 Spring 反射。
     */
    private fun tenantProvider(): ILogSourceTenantProvider? {
        cachedTenantProvider?.let { return it }
        val resolved = runCatching { SpringKit.getBeanOrNull(ILogSourceTenantProvider::class) }
            .onFailure { LOG.debug("查找 ILogSourceTenantProvider 失败：{0}", it.message) }
            .getOrNull()
        if (resolved != null) {
            cachedTenantProvider = resolved
        }
        return resolved
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
        return cached?.let { String(it.contentAsByteArray) } ?: ""
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
        // request.getParameter 在缺该参数时返回 null（Java API），用 isNullOrBlank 守住 NPE
        val entityId: String? = request.getParameter("id")
        if (!entityId.isNullOrBlank()) {
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
            LOG.debug("转换参数失败，不设置实体ID.")
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
     * 设置日志的操作者
     * 设置日志数据源
     *
     * @param modelAudit
     */
    fun setOperator(modelAudit: SysAuditLogModel) {
        val context = KudosContextHolder.get()
        val clientInfo = context.clientInfo
        for (entity in requireNotNull(modelAudit.entities) { "entities is null" }) {
            entity.operateTime = Date()
            clientInfo?.ip?.let { entity.operateIp = IpKit.ipv4StringToLong(it) }

            // 历史 TODO: operateIpDictCode / operator(username) / operatorUserType 需要从
            // context 拿到对应字段才能填——目前 KudosContext 上没有这几个属性，留空即可。
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
