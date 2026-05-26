package io.kudos.ability.log.audit.common.entity

import java.io.Serial
import java.io.Serializable
import java.util.Date

/**
 * System audit-log entity — the "main table" projection corresponding to audit persistence
 * (DB / MQ / other backends).
 *
 * Pairs with [SysAuditDetailLogVo]: each record of this class is linked one-to-one with a detail
 * record via [SysAuditDetailLogVo.auditId] referencing [SysAuditLogVo.id]. Concrete storage is
 * implemented by downstream modules (`kudos-ability-log-audit-rdb-ktorm` /
 * `kudos-ability-log-audit-mq`, etc.).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class SysAuditLogVo : Serializable {
    /**
     * Primary key
     */
    var id: String? = null

    /**
     * Business entity id (the id of the affected object)
     */
    var entityId: String? = null

    /**
     * Operation type (ID)
     */
    var operateTypeId: Int? = null

    /**
     * Operation type
     */
    var operateType: String? = null

    /**
     * Module name (multi-level)
     */
    var moduleName: String? = null

    /**
     * Module type
     */
    var moduleCode: String? = null

    /**
     * Operation description
     */
    var description: String? = null

    /**
     * Operator
     */
    var operator: String? = null

    /**
     * Tenant id
     */
    var tenantId: String? = null
    var sourceTenantId: String? = null

    /**
     * Subsystem code
     */
    var subSysCode: String? = null

    /**
     * Operation time
     */
    var operateTime: Date? = null

    /**
     * Operation (client) IP
     */
    var operateIp: Long? = null

    /**
     * Operator IP region dictionary code
     */
    var operateIpDictCode: String? = null

    /**
     * Operator id
     */
    var operatorId: String? = null

    /**
     * User type (reference: sys_user)
     */
    var operatorUserType: String? = null

    /**
     * Client OS
     */
    var clientOs: String? = null

    /**
     * Client browser
     */
    var clientBrowser: String? = null

    /**
     * Request type (GET|POST)
     */
    var requestType: String? = null
    var moduleId: Int? = null

    companion object {
        @Serial
        private const val serialVersionUID = 2339633147120186063L

        const val AUDIT_LOG: String = "__AUDIT_LOG_TMP__"
    }
}
