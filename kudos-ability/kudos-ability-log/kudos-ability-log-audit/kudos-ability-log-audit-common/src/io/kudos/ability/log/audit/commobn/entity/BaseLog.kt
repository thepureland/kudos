package io.kudos.ability.log.audit.commobn.entity

import io.kudos.ability.log.audit.commobn.annotation.Audit
import io.kudos.ability.log.audit.commobn.annotation.WebAudit
import io.kudos.ability.log.audit.commobn.enums.OperationTypeEnum
import io.kudos.ability.log.audit.commobn.support.IAuditLogDetailDescriptionFormatter
import io.kudos.ability.log.audit.commobn.support.ISysAuditModule
import io.kudos.base.data.json.JsonKit
import io.kudos.base.lang.string.RandomStringKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import java.io.Serial
import java.io.Serializable
import kotlin.reflect.KClass

/**
 * Create by (admin) on 7/7/15.
 * 基础日志信息vo
 */
class BaseLog : Serializable {
    /**
     * 被审计实体
     */
    var entityId: String? = null
    var entityUserId: Int? = null
    var description: String?
    var moduleCode: String?
    var moduleName: String? = null
    var moduleId: Int? = null
    private var opType: OperationTypeEnum? = null
    var ignoreForm: Boolean?

    /**
     * 描述信息国际化参数值,对应参数类型:{0}
     */
    private var stringParams: MutableList<String?>? = null

    /**
     * 描述信息国际化参数值,${user.name}等
     */
    private var objectParams: MutableMap<String?, LogParamVo?>? = null

    /**
     * 旧数据信息: 用于传输到明细转换
     */
    var oldBizData: Any? = null

    /**
     * 方法的参数信息
     */
    var paramArgs: Array<Any?>? = null

    val descriptionFormatterClass: KClass<out IAuditLogDetailDescriptionFormatter>?

    constructor(audit: WebAudit) {
        this.moduleCode = audit.moduleCode
        this.opType = audit.opType
        this.description = audit.desc + audit.opTypeExt
        this.ignoreForm = audit.ignoreForm.getBool()
        this.descriptionFormatterClass = audit.descriptionFormatter
        initModule(audit.subsysCode)
    }

    constructor(audit: Audit) {
        this.moduleCode = audit.moduleCode
        this.opType = audit.opType
        this.description = audit.desc + audit.opTypeExt
        this.ignoreForm = audit.ignoreForm.getBool()
        this.descriptionFormatterClass = audit.descriptionFormatter
        initModule(audit.subSysCode)
    }

    private fun initModule(subsysCode: String) {
        val beansOfType = SpringKit.getBeansOfType(ISysAuditModule::class)
        if (beansOfType.isNotEmpty()) {
            val auditModule = beansOfType.values.first()
            val sysCode = subsysCode.ifBlank { KudosContextHolder.get().subSysCode }
            val module = auditModule.module(sysCode, moduleCode)
            if (module != null) {
                this.moduleId = module.first
                this.moduleName = module.second
            } else {
                this.moduleId = -9999
                this.moduleName = moduleCode
            }
        }
    }

    fun getOpType(): OperationTypeEnum? {
        return opType
    }

    fun setOpType(opType: OperationTypeEnum) {
        this.opType = opType
    }

    fun getStringParams(): String? {
        if (stringParams.isNullOrEmpty()) {
            return null
        }
        val sb = StringBuffer()
        val size: Int = stringParams!!.size
        for (i in 0..<size) {
            val s: String? = stringParams!!.get(i)
            sb.append(s)
            if (i + 1 < size) {
                sb.append(SEPERATOR)
            }
        }
        return sb.toString()
    }

    /**
     * 返回JSON对象字符串
     */
    fun getObjectParams(): String? {
        if (this.objectParams.isNullOrEmpty()) {
            return null
        }
        return JsonKit.toJson(this.objectParams)
    }

    /**
     * 添加字符串参数
     *
     * @param param
     */
    fun addParam(param: String?): BaseLog {
        if (this.stringParams == null) {
            this.stringParams = ArrayList<String?>()
        }
        this.stringParams!!.add(param)
        return this
    }

    /**
     * 添加${}类型参数
     *
     * @param param
     * @param value
     */
    fun addParam(param: String?, value: Any?): BaseLog {
        if (this.objectParams == null) {
            this.objectParams = HashMap<String?, LogParamVo?>()
        }
        this.objectParams!!.put(param, LogParamVo(param, value))
        return this
    }

    /**
     * 添加${}类型参数
     *
     * @param param
     */
    fun addParam(param: LogParamVo): BaseLog {
        if (this.objectParams == null) {
            this.objectParams = HashMap()
        }
        this.objectParams!!.put(param.name, param)
        return this
    }

    /**
     * vo转化为实体
     */
    fun toSysLogVo(): SysAuditLogVo {
        val sysLogVo = SysAuditLogVo()
        try {
            sysLogVo.entityId = entityId
            sysLogVo.moduleName = this.moduleName
            sysLogVo.moduleCode = this.moduleCode
            sysLogVo.operateTypeId = Integer.valueOf(opType!!.code)
            sysLogVo.operateType = opType!!.trans
            sysLogVo.description = description
            sysLogVo.moduleId = this.moduleId
            sysLogVo.id = RandomStringKit.uuid()
        } catch (ex: Exception) {
            LOG.error(ex)
        }
        return sysLogVo
    }

    fun getDescriptionFormatterClass(): KClass<out IAuditLogDetailDescriptionFormatter>? {
        return descriptionFormatterClass
    }

    companion object {
        @Serial
        private val serialVersionUID = -6711100917335692885L

        private val LOG = LogFactory.getLog(BaseLog::class)

        /**
         * 作为字符串参数stringParams中的分隔符
         */
        const val SEPERATOR: String = "┼"
    }
}
