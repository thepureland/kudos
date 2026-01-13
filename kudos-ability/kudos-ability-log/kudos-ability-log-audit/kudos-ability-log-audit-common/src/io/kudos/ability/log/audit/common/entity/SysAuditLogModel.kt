package io.kudos.ability.log.audit.common.entity

import io.kudos.base.data.json.JsonKit
import java.io.Serial
import java.io.Serializable

class SysAuditLogModel : Serializable {
    var sysAuditDetailLogs: MutableList<SysAuditDetailLogVo?>? = null

    var entities: MutableList<SysAuditLogVo>? = null

    var subSysCode: String? = null
    var tenantId: String? = null

    override fun toString(): String {
        return JsonKit.toJson(this)
    }

    companion object {
        @Serial
        private val serialVersionUID = -2034863673832068399L
    }
}
