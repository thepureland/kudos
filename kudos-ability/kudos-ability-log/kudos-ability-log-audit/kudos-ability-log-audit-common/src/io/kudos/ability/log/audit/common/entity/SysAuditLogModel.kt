package io.kudos.ability.log.audit.common.entity

import io.kudos.base.data.json.JsonKit
import java.io.Serial
import java.io.Serializable

/**
 * Audit-log aggregate model.
 *
 * Used for MQ delivery and service-interface inputs; packages the "main audit records ([entities])"
 * together with the "matching details ([sysAuditDetailLogs])", plus the subsystem and tenant id
 * needed for cross-process forwarding. toString serializes to JSON directly so the full structure
 * shows up in logs for troubleshooting.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class SysAuditLogModel : Serializable {
    /** Audit detail list, aligned in order with [entities]; one-to-many in batch operations */
    var sysAuditDetailLogs: MutableList<SysAuditDetailLogVo?>? = null

    /** Main audit record list (a single operation may affect multiple entities) */
    var entities: MutableList<SysAuditLogVo>? = null

    /** Subsystem code, used for routing to the correct database in cross-process auditing */
    var subSysCode: String? = null
    /** Tenant id, used for isolation in multi-tenant scenarios */
    var tenantId: String? = null

    /**
     * Serializes directly to a JSON string so the full structure shows up in logs.
     *
     * @author K
     * @since 1.0.0
     */
    override fun toString(): String {
        return JsonKit.toJson(this)
    }

    companion object {
        /** Serializable version uid */
        @Serial
        private val serialVersionUID = -2034863673832068399L
    }
}
