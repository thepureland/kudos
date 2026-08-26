package io.kudos.ms.auth.core.provider.invitation.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** One-time capability for binding a verified external principal to a pre-existing local user. */
interface AuthExternalIdentityInvitation : IDbEntity<String, AuthExternalIdentityInvitation> {
    companion object : DbEntityFactory<AuthExternalIdentityInvitation>()

    var tenantId: String
    var userId: String
    var identityProviderId: String
    var tokenHash: String
    var expectedEmailHash: String?
    var maxUses: Int
    var usedCount: Int
    var expiresAt: LocalDateTime
    var active: Boolean
    var createUserId: String
    var createReason: String
    var createTime: LocalDateTime
    var revokeUserId: String?
    var revokeReason: String?
    var revokeTime: LocalDateTime?
    var lastUsedTime: LocalDateTime?
    var consumedSubjectHash: String?
    var updateTime: LocalDateTime?
}
