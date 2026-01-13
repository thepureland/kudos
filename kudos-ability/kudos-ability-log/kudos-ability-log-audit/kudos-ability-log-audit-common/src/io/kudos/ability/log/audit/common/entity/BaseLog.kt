package io.kudos.ability.log.audit.common.entity

import io.kudos.ability.log.audit.common.annotation.Audit
import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.log.audit.common.support.IAuditLogDetailDescriptionFormatter
import io.kudos.ability.log.audit.common.support.ISysAuditModule
import io.kudos.base.data.json.JsonKit
import io.kudos.base.lang.string.RandomStringKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import java.io.Serial
import java.io.Serializable
import kotlin.reflect.KClass

/**
 * 审计日志基础信息值对象
 * 
 * 用于封装系统审计日志的基本信息，包括实体信息、操作类型、模块信息和参数信息。
 * 
 * 核心属性：
 * - entityId：被审计实体的唯一标识
 * - entityUserId：被审计实体的用户ID
 * - description：操作描述信息，支持国际化参数
 * - moduleCode/moduleName/moduleId：审计模块的编码、名称和ID
 * - opType：操作类型枚举（增删改查等）
 * - ignoreForm：是否忽略表单数据
 * 
 * 参数支持：
 * - stringParams：字符串类型国际化参数列表，对应{0}、{1}等占位符
 * - objectParams：对象类型国际化参数Map，对应${user.name}等表达式
 * - paramArgs：方法参数信息数组
 * - oldBizData：旧业务数据，用于记录变更前的数据
 * 
 * 描述格式化：
 * - 支持自定义描述格式化器（descriptionFormatterClass）
 * - 描述信息支持国际化参数替换
 * - 可以从WebAudit或Audit注解中初始化
 * 
 * 使用场景：
 * - 记录系统操作审计日志
 * - 支持操作记录的查询和追溯
 * - 支持国际化描述信息
 * 
 * 注意事项：
 * - 需要配合Audit或WebAudit注解使用
 * - 模块信息会自动从ISysAuditModule中获取
 * - 参数分隔符使用"┼"符号
 * 
 * @since 1.0.0
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
        this.ignoreForm = audit.ignoreForm.bool
        this.descriptionFormatterClass = audit.descriptionFormatter
        initModule(audit.subsysCode)
    }

    constructor(audit: Audit) {
        this.moduleCode = audit.moduleCode
        this.opType = audit.opType
        this.description = audit.desc + audit.opTypeExt
        this.ignoreForm = audit.ignoreForm.bool
        this.descriptionFormatterClass = audit.descriptionFormatter
        initModule(audit.subSysCode)
    }

    private fun initModule(subsysCode: String) {
        val beansOfType = SpringKit.getBeansOfType(ISysAuditModule::class)
        if (beansOfType.isNotEmpty()) {
            val auditModule = beansOfType.values.first()
            val sysCode = subsysCode.ifBlank { KudosContextHolder.get().subSystemCode }
            val module = auditModule.module(sysCode, moduleCode)
            this.moduleId = module.first
            this.moduleName = module.second
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
