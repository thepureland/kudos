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
 * Audit log base-info value object.
 *
 * Wraps the basic information of system audit logs, including entity info, operation type, module info and parameters.
 *
 * Core properties:
 * - entityId: unique identifier of the audited entity
 * - entityUserId: user ID of the audited entity
 * - description: operation description; supports i18n parameters
 * - moduleCode/moduleName/moduleId: code, name and ID of the audit module
 * - opType: operation-type enum (create/read/update/delete, etc.)
 * - ignoreForm: whether to ignore form data
 *
 * Parameter support:
 * - stringParams: list of string-type i18n parameters, matching {0}, {1} placeholders
 * - objectParams: map of object-type i18n parameters, matching ${user.name} expressions
 * - paramArgs: method parameter info array
 * - oldBizData: previous business data used to record pre-change state
 *
 * Description formatting:
 * - Supports a custom description formatter (descriptionFormatterClass)
 * - Description text supports i18n parameter substitution
 * - Can be initialized from a WebAudit or Audit annotation
 *
 * Use cases:
 * - Recording system operation audit logs
 * - Supporting query and traceability of operation records
 * - Supporting i18n descriptions
 *
 * Notes:
 * - Must be used with the Audit or WebAudit annotation
 * - Module info is fetched automatically from ISysAuditModule
 * - Parameter separator is the "┼" character
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class BaseLog : Serializable {
    /**
     * Audited entity
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
     * i18n parameter values for description; matches placeholder type {0}.
     */
    private var stringParams: MutableList<String?>? = null

    /**
     * i18n parameter values for description; matches ${user.name}-style expressions.
     */
    private var objectParams: MutableMap<String, LogParamVo>? = null

    /**
     * Previous data; carried through to the detail conversion.
     */
    var oldBizData: Any? = null

    /**
     * Method parameter info.
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
        // Multiple ISysAuditModule implementations resolve in a chained manner: the first implementation that
        // returns a non-null id or non-null name wins. This keeps the old single-implementation case working
        // (the first hit short-circuits) while supporting the business side registering multiple module resolvers
        // partitioned by subsystem.
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
     * Joins the stringParams list into a single string.
     *
     * **Historical bug**: the old implementation called `sb.append(s)` + `sb.append(SEPERATOR)` directly. When a
     * business parameter itself contained `┼`, reverse parsing (see [splitStringParams]) would split incorrectly.
     * This fix escapes `\\` and `┼` on each segment before joining, giving the separator unambiguous semantics.
     *
     * Backward compatibility: the parser [splitStringParams] follows the same escape rules. Legacy unescaped data
     * **behaves identically to the new rules** when the separator does not appear inside parameter content, so
     * nothing is broken.
     */
    fun getStringParams(): String? =
        stringParams?.takeIf { it.isNotEmpty() }?.joinToString(SEPERATOR) { escapeSegment(it) }

    /**
     * Returns the JSON object string.
     */
    fun getObjectParams(): String? {
        if (this.objectParams.isNullOrEmpty()) {
            return null
        }
        return JsonKit.toJson(this.objectParams)
    }

    /**
     * Adds a string parameter.
     *
     * @param param
     */
    fun addParam(param: String?): BaseLog = apply {
        val list = stringParams ?: mutableListOf<String?>().also { stringParams = it }
        list.add(param)
    }

    /**
     * Adds a ${}-style parameter.
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
     * Adds a ${}-style parameter.
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
     * Converts the VO into an entity.
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
         * Separator used inside the stringParams string. **If a business parameter contains this character, it
         * must be escaped first** — joining uses [escapeSegment] and reverse parsing uses [splitStringParams];
         * together they guarantee a round trip.
         */
        const val SEPERATOR: String = "┼"

        /** Escape character — reserved for the case where SEPERATOR itself appears inside parameter content. */
        private const val ESCAPE: String = "\\"

        /**
         * Escapes a single segment. Rules:
         *  - `\` -> `\\` (must come first; otherwise `\┼` would first be replaced to `\\\┼`)
         *  - `┼` -> `\┼`
         *
         * The escape information is embedded in the content so [splitStringParams] can split on "unescaped `┼`".
         */
        @JvmStatic
        fun escapeSegment(s: String?): String {
            if (s.isNullOrEmpty()) return ""
            return s.replace(ESCAPE, "$ESCAPE$ESCAPE").replace(SEPERATOR, "$ESCAPE$SEPERATOR")
        }

        /**
         * Reverse-parses the string produced by [getStringParams]. Splits on **unescaped** `┼` and unescapes
         * each segment. An empty string returns an empty list.
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
                    // Escape sequence: take the next character as-is (either `\` or `┼`)
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
