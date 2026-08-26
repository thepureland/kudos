package io.kudos.ms.auth.core.token.refresh.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** Hash-only record for one member of an opaque refresh-token family. */
interface AuthRefreshToken : IDbEntity<String, AuthRefreshToken> {
    companion object : DbEntityFactory<AuthRefreshToken>()

    var sessionId: String
    var familyId: String
    var parentId: String?
    var replacedById: String?
    var tokenHash: String
    var tokenEpoch: Long
    var issuedAt: LocalDateTime
    var expiresAt: LocalDateTime
    var consumedAt: LocalDateTime?
    var revokedAt: LocalDateTime?
    var revokeReason: String?
}
