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
import java.util.*
import kotlin.reflect.KClass

/**
 * Create by (admin) on 4/24/15.
 */
object AuditLogTool {
    private val LOG = LogFactory.getLog(AuditLogTool::class)
    private val parameterNameDiscoverer: ParameterNameDiscoverer = DefaultParameterNameDiscoverer()
    private var tenantProvider: ILogSourceTenantProvider? = null

    init {
        initSourceTenantProvider()
    }

    private fun initSourceTenantProvider() {
        try {
            tenantProvider = SpringKit.getBean(ILogSourceTenantProvider::class)
        } catch (e: Exception) {
            tenantProvider = null
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
        if (formatterClazz == DefaultAuditLogDetailDescriptionFormatter::class.java) {
            val formatterMap = SpringKit.getBeansOfType(IAuditLogDetailDescriptionFormatter::class)
            if (formatterMap.isEmpty()) {
                return null
            }
            for (value in formatterMap.values) {
                if (value.needFormat(baseLog)) {
                    return value
                }
            }
            return null
        } else {
            return SpringKit.getBean(formatterClazz)
        }
    }

    fun getRequestData(request: HttpServletRequest?): String {
        val wrapper = request as HttpServletRequestWrapper
        var contentCachingRequestWrapper: ContentCachingRequestWrapper? = null
        if (wrapper is ContentCachingRequestWrapper) {
            contentCachingRequestWrapper = wrapper as ContentCachingRequestWrapper?
        } else if (wrapper.request is ContentCachingRequestWrapper) {
            contentCachingRequestWrapper = wrapper.request as ContentCachingRequestWrapper?
        }
        if (contentCachingRequestWrapper != null) {
            val contentAsByteArray: ByteArray = contentCachingRequestWrapper.contentAsByteArray
            return String(contentAsByteArray)
        }
        return ""
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
        if (audit.ignoreForm === YesNotEnum.NOT) {
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
        if (audit.ignoreForm === YesNotEnum.NOT) {
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
                val sysAuditLogVo = vo!!.toSysLogVo()
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
                modelAudit.entities!!.add(sysAuditLogVo)
                modelAudit.sysAuditDetailLogs!!.add(sysAuditDetailLog)
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
        val descriptionFormatter = descriptionFormatter(vo, vo.descriptionFormatterClass!!)
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
        for (entity in modelAudit.entities!!) {
            entity.operateTime=Date()
            val ip = context.clientInfo?.ip
            if (ip != null) {
                entity.operateIp= IpKit.ipv4StringToLong(ip)
            }

            //TODO ..
            //entity.setOperateIpDictCode(contextParam.getUserIpDictCode());
//            entity.operator=contextParam.getUsername())
            entity.operatorId=context.user?.id
//            entity.setOperatorUserType(contextParam.getUserType())

            entity.clientBrowser = context.clientInfo?.browser?.first
            entity.clientOs=context.clientInfo?.os?.first
            entity.requestType=context.clientInfo?.requestType
            entity.subSysCode=subSysCode
            entity.tenantId=context.tenantId
            if (tenantProvider != null) {
                entity.sourceTenantId=tenantProvider!!.getSourceTenant(entity.tenantId, entity.operatorId)
            } else {
                entity.sourceTenantId=entity.tenantId
            }
        }

        modelAudit.subSysCode=subSysCode
        modelAudit.tenantId=context.tenantId
    }

    private val subSysCode: String?
        get() {
            val subSysCode = KudosContextHolder.get().subSystemCode
            if (!subSysCode.isNullOrBlank()) {
                return subSysCode
            }
            return subSysCode
        }

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
