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
    private var objectParams: MutableMap<String, LogParamVo>? = null

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
        val beansOfType = SpringKit.getBeansOfType<ISysAuditModule>()
        if (beansOfType.isEmpty()) return
        val sysCode = subsysCode.ifBlank { KudosContextHolder.get().subSystemCode }
        // 多 ISysAuditModule 实现按"链式"解析：第一个返回非空 id 或非空 name 的实现胜出。
        // 既兼容"只有一个实现"的旧场景（first 直接命中），又支持业务侧按子系统拆分注册多个 module 解析器。
        var resolvedId: Int? = null
        var resolvedName: String? = null
        for ((_, module) in beansOfType) {
            val (id, name) = module.module(sysCode, moduleCode)
            if (resolvedId == null) resolvedId = id
            if (resolvedName == null) resolvedName = name
            if (resolvedId != null && resolvedName != null) break
        }
        this.moduleId = resolvedId
        this.moduleName = resolvedName
    }

    fun getOpType(): OperationTypeEnum? {
        return opType
    }

    fun setOpType(opType: OperationTypeEnum) {
        this.opType = opType
    }

    /**
     * 把 stringParams 列表拼成单字符串。
     *
     * **历史 bug**：旧实现直接 `sb.append(s)` + `sb.append(SEPERATOR)`，业务参数自身含
     * `┼` 时反向解析（参考 [splitStringParams]）会错位。本次修复在拼接前对每个 segment
     * 转义 `\\` 和 `┼`，使分隔符语义明确。
     *
     * 与历史数据的兼容：解析侧的 [splitStringParams] 同样按转义规则解；老数据（未转义）
     * 在分隔符不出现于参数内容时**与新规则等价**，无破坏。
     */
    fun getStringParams(): String? {
        if (stringParams.isNullOrEmpty()) {
            return null
        }
        val params = requireNotNull(stringParams) { "stringParams is null" }
        return params.joinToString(SEPERATOR) { escapeSegment(it) }
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
            this.stringParams = ArrayList()
        }
        (this.stringParams ?: ArrayList<String?>().also { this.stringParams = it }).add(param)
        return this
    }

    /**
     * 添加${}类型参数
     *
     * @param param
     * @param value
     */
    fun addParam(param: String, value: Any?): BaseLog {
        val map = objectParams ?: HashMap<String, LogParamVo>().also { objectParams = it }
        map[param] = LogParamVo(param, value)
        return this
    }

    /**
     * 添加${}类型参数
     *
     * @param param
     */
    fun addParam(param: LogParamVo): BaseLog {
        val map = objectParams ?: HashMap<String, LogParamVo>().also { objectParams = it }
        val key = requireNotNull(param.name) { "LogParamVo.name is null" }
        map[key] = param
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
            val op = requireNotNull(opType) { "opType is null" }
            sysLogVo.operateTypeId = Integer.valueOf(op.code)
            sysLogVo.operateType = op.displayText
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
         * 作为字符串参数stringParams中的分隔符。**业务参数自身含此字符时必须先转义**——
         * 拼接走 [escapeSegment]，反向解析走 [splitStringParams]，二者配合保证 round-trip。
         */
        const val SEPERATOR: String = "┼"

        /** 转义字符——保留给 SEPERATOR 自身在参数内容中出现的场景。 */
        private const val ESCAPE: String = "\\"

        /**
         * 转义单条 segment。规则：
         *  - `\` → `\\`（必须先于下面，否则 `\┼` 会被先替换成 `\\\┼`）
         *  - `┼` → `\┼`
         *
         * 把转义信息嵌进内容里，让 [splitStringParams] 可以按"非转义的 `┼`"切分。
         */
        @JvmStatic
        fun escapeSegment(s: String?): String {
            if (s.isNullOrEmpty()) return ""
            return s.replace(ESCAPE, "$ESCAPE$ESCAPE").replace(SEPERATOR, "$ESCAPE$SEPERATOR")
        }

        /**
         * 反向解析 [getStringParams] 拼出来的字符串。按**未转义的** `┼` 切分，再对每段
         * 还原转义。空字符串返回空列表。
         */
        @JvmStatic
        fun splitStringParams(joined: String?): List<String> {
            if (joined.isNullOrEmpty()) return emptyList()
            val out = mutableListOf<String>()
            val current = StringBuilder()
            var i = 0
            while (i < joined.length) {
                val c = joined[i]
                if (c == '\\' && i + 1 < joined.length) {
                    // 转义序列：原样取下一个字符（无论是 `\` 还是 `┼`）
                    current.append(joined[i + 1])
                    i += 2
                } else if (joined.startsWith(SEPERATOR, i)) {
                    out.add(current.toString())
                    current.setLength(0)
                    i += SEPERATOR.length
                } else {
                    current.append(c)
                    i++
                }
            }
            out.add(current.toString())
            return out
        }
    }
}
