package io.kudos.ms.auth.core.authentication.securityevent.oncall.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** One named on-call rotation per tenant. */
interface AuthSecurityEventOnCallRosterPo : IDbEntity<String, AuthSecurityEventOnCallRosterPo> {
    companion object : DbEntityFactory<AuthSecurityEventOnCallRosterPo>()

    var tenantId: String
    var rosterCode: String
    var displayName: String
    var enabled: Boolean
    var configVersion: Long
    var createUserId: String
    var createReason: String
    var createTime: LocalDateTime
    var updateUserId: String
    var updateReason: String
    var updateTime: LocalDateTime
}

/** One rotation entry of a roster; windows are stored as UTC instants. */
interface AuthSecurityEventOnCallShiftPo : IDbEntity<String, AuthSecurityEventOnCallShiftPo> {
    companion object : DbEntityFactory<AuthSecurityEventOnCallShiftPo>()

    var tenantId: String
    var rosterId: String
    var responderUserId: String
    var tier: Int
    var startAt: LocalDateTime
    var endAt: LocalDateTime
}
