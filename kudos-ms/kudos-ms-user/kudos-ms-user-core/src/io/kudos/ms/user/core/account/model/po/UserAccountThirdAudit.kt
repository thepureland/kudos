package io.kudos.ms.user.core.account.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** Append-only third-party account binding audit row. */
interface UserAccountThirdAudit : IDbEntity<String, UserAccountThirdAudit> {
    companion object : DbEntityFactory<UserAccountThirdAudit>()

    var bindingId: String?
    var userId: String
    var tenantId: String
    var identityProviderId: String?
    var providerCode: String
    var subjectHash: String
    var action: String
    var success: Boolean
    var reason: String?
    var actorUserId: String
    var operationReason: String?
    var beforeSnapshot: String?
    var afterSnapshot: String?
    var eventTime: LocalDateTime
}
