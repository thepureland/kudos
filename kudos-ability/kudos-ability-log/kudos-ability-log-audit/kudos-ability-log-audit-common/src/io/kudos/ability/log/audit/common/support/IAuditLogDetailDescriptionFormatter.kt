package io.kudos.ability.log.audit.common.support

import io.kudos.ability.log.audit.common.entity.BaseLog

/**
 * Formatter for audit-log detail descriptions: business code implements this
 * interface to customize the display text of each audit log on the detail page.
 *
 * Workflow:
 * 1. [needFormat] asks whether to handle this [BaseLog] (default false, i.e. do not take over).
 * 2. When taking over, the aspect first calls [loadOldBizData] to let business
 *    code look up the old data from input parameters (used for "before/after" diffs).
 * 3. Finally [descriptionFormat] turns the [BaseLog] into the display string.
 *
 * The default implementation (web operation log) is in
 * [DefaultAuditLogDetailDescriptionFormatter]; custom business implementations
 * are registered as Spring beans and discovered/called automatically by the aspect.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IAuditLogDetailDescriptionFormatter {
    /**
     * Whether this business needs conversion (i.e. it is not a web operation log).
     *
     * @param baseLog audit log entry
     */
    fun needFormat(baseLog: BaseLog?): Boolean {
        return false
    }

    /**
     * Loads the old data based on the method's input parameters.
     *
     * @param paramObjs method input parameters
     * @return the loaded old business data, or null
     */
    fun loadOldBizData(baseLog: BaseLog?, paramObjs: Array<Any?>?): Any? {
        return null
    }

    /**
     * Converts the log entry into a detail description string.
     *
     * @param baseLog log information
     * @return the description string for the detail view
     */
    fun descriptionFormat(baseLog: BaseLog?): String?
}
