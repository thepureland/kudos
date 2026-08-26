package io.kudos.ms.auth.core.authentication.securityevent.oncall.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.authentication.securityevent.oncall.model.po.AuthSecurityEventOnCallRosterPo
import io.kudos.ms.auth.core.authentication.securityevent.oncall.model.po.AuthSecurityEventOnCallShiftPo
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar

object AuthSecurityEventOnCallRosters :
    StringIdTable<AuthSecurityEventOnCallRosterPo>("auth_security_event_oncall_roster") {
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var rosterCode = varchar("roster_code").bindTo { it.rosterCode }
    var displayName = varchar("display_name").bindTo { it.displayName }
    var enabled = boolean("enabled").bindTo { it.enabled }
    var configVersion = long("config_version").bindTo { it.configVersion }
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }
    var createReason = varchar("create_reason").bindTo { it.createReason }
    var createTime = datetime("create_time").bindTo { it.createTime }
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }
    var updateReason = varchar("update_reason").bindTo { it.updateReason }
    var updateTime = datetime("update_time").bindTo { it.updateTime }
}

object AuthSecurityEventOnCallShifts :
    StringIdTable<AuthSecurityEventOnCallShiftPo>("auth_security_event_oncall_shift") {
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var rosterId = varchar("roster_id").bindTo { it.rosterId }
    var responderUserId = varchar("responder_user_id").bindTo { it.responderUserId }
    var tier = int("tier").bindTo { it.tier }
    var startAt = datetime("start_at").bindTo { it.startAt }
    var endAt = datetime("end_at").bindTo { it.endAt }
}
